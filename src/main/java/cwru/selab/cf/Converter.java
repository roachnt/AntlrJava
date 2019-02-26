package cwru.selab.cf;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Stack;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.TokenStreamRewriter;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

// TODO: If-Else
// TODO: Else-If
// TODO: Phi Function Types

public class Converter extends Java8BaseListener {

  final int IDENTIFIER_TYPE = 102;

  Java8Parser parser;

  HashMap<String, String> dataTypeToObjectMap = new HashMap<String, String>() {
    {
      put("int", "Integer");
      put("double", "Double");
      put("boolean", "Boolean");
      put("float", "Float");
      put("long", "Long");
    }
  };

  // Rewriting mechanism
  TokenStreamRewriter rewriter;

  // Tokens from the program
  TokenStream tokens;

  // Counter for Phi functions
  int phiCounter = -1;

  // Keep phi subscripts in a queue
  LinkedList<Integer> phiSubscriptQueue = new LinkedList<>();

  // Variables in predicate block being assigned
  Stack<HashSet<String>> predicateBlockVariablesStack = new Stack<>();

  // Keep track of all predicate variables
  Stack<HashSet<String>> whileLoopVariableStack = new Stack<>();

  // While loop tokens to change
  ArrayList<Pair<String, Token>> whileLoopTokens = new ArrayList<>();

  // PhiIf declarations deferred for while loop
  Stack<HashMap<Integer, Java8Parser.IfThenStatementContext>> deferredPhiIfDeclarations = new Stack<>();

  // PhiIf merges deferred for while loop
  Stack<HashMap<Java8Parser.IfThenStatementContext, ArrayList<String>>> deferredPhiIfMerges = new Stack<>();

  // Variable subscripts before entering the predicate block
  Stack<HashMap<String, Integer>> varSubscriptsBeforePredicateStack = new Stack<>();

  // Map from SSA-form variable to an array of the variables in its assignment
  HashMap<String, ArrayList<String>> variableConfoundersMap = new HashMap<>();

  // Keep track of current subcript of a variable when converting to SSA
  HashMap<String, Integer> currentVariableSubscriptMap = new HashMap<>();

  // Map base variable name to its type
  HashMap<String, String> variableTypeMap = new HashMap<>();

  int scope = 0;

  String packageName = "";
  String className = "";
  String currentMethodName = "";

  public Converter(Java8Parser parser, TokenStreamRewriter rewriter) {
    this.rewriter = rewriter;
    this.parser = parser;
    this.tokens = parser.getTokenStream();
  }

  @Override
  public void enterBlock(Java8Parser.BlockContext ctx) {
    scope++;
  }

  public void exitBlock(Java8Parser.BlockContext ctx) {
    scope--;
  }

  // Handling basic assignment statements
  @Override
  public void enterLeftHandSide(Java8Parser.LeftHandSideContext ctx) {
    String variable = tokens.getText(ctx);
    int subscript = 0;

    if (currentVariableSubscriptMap.containsKey(variable)) {
      subscript = currentVariableSubscriptMap.get(variable) + 1;
    }

    if (insidePredicateBlock(ctx)) {
      for (HashSet<String> predicateBlockVariables : predicateBlockVariablesStack) {
        predicateBlockVariables.add(variable);
      }

    }

    if (isDescendantOf(ctx, Java8Parser.WhileStatementContext.class)) {
      whileLoopVariableStack.lastElement().add(variable);
    }

  }

  // Handling initializing variables in a method
  @Override
  public void enterLocalVariableDeclaration(Java8Parser.LocalVariableDeclarationContext ctx) {
    String type = tokens.getText(ctx.unannType());
    for (int i = 0; i < ctx.variableDeclaratorList().variableDeclarator().size(); i++) {
      int subscript = 0;
      Java8Parser.VariableDeclaratorIdContext varContext = ctx.variableDeclaratorList().variableDeclarator(i)
          .variableDeclaratorId();
      String variable = tokens.getText(varContext);
      int lineNumber = ctx.getStart().getLine();
      if (!isDescendantOf(ctx, Java8Parser.ForInitContext.class)
          && ctx.variableDeclaratorList().variableDeclarator(i).variableInitializer() != null) {
        currentVariableSubscriptMap.put(variable, 0);
        insertVersionUpdateAfter(ctx.getParent().getParent().getStop(), variable);
        insertRecordStatementAfter(ctx.getParent().getStop(), variable, lineNumber);
      }
      variableTypeMap.put(variable, type);
    }

  }

  // When entering any expression, change the variable to SSA form
  @Override
  public void enterExpressionName(Java8Parser.ExpressionNameContext ctx) {

    // Only changing right hand side in this context
    if (isDescendantOf(ctx, Java8Parser.LeftHandSideContext.class)) {
      return;
    }

    String varName = tokens.getText(ctx);
    if (!currentVariableSubscriptMap.containsKey(varName))
      return;
    int subscript = currentVariableSubscriptMap.get(varName);
    if (isDescendantOf(ctx, Java8Parser.WhileStatementContext.class) && !insidePredicate(ctx)) {
      whileLoopTokens.add(new ImmutablePair<String, Token>(varName, ctx.getStart()));
    }
  }

  // Upon exiting an assignment, increment the subscript counter
  @Override
  public void exitAssignment(Java8Parser.AssignmentContext ctx) {
    String variable = tokens.getText(ctx.leftHandSide());
    int subscript = 0;

    if (currentVariableSubscriptMap.containsKey(variable)) {
      subscript = currentVariableSubscriptMap.get(variable) + 1;
    }
    currentVariableSubscriptMap.put(variable, subscript);
    int lineNumber = ctx.getStart().getLine();

    if (!isDescendantOf(ctx, Java8Parser.ForInitContext.class)) {
      insertVersionUpdateAfter(ctx.getParent().getParent().getStop(), variable);
      insertRecordStatementAfter(ctx.getParent().getParent().getStop(), variable, lineNumber);
    }

    HashSet<String> postFixAlteredVariables = getIncrementAndDecrementVariablesFromAssignment(ctx);
    for (String alteredVariable : postFixAlteredVariables) {
      insertVersionUpdateAfter(ctx.getParent().getParent().getStop(), alteredVariable);
      insertRecordStatementAfter(ctx.getParent().getParent().getStop(), alteredVariable, lineNumber);
    }
  }

  ArrayList<String> methodParameters = new ArrayList<>();

  // Get the parameters from the method and add them to the maps
  @Override
  public void enterFormalParameter(Java8Parser.FormalParameterContext ctx) {
    String varName = tokens.getText(ctx.variableDeclaratorId());
    String varType = tokens.getText(ctx.unannType());
    methodParameters.add(varName);
    variableTypeMap.put(varName, varType);
    currentVariableSubscriptMap.put(varName, 0);
  }

  // When entering the method, create the SSA form of the parameters to the body of the method
  // They will be inserted once the parser exits the method body
  String initializeFormalParams = "";

  @Override
  public void enterMethodBody(Java8Parser.MethodBodyContext ctx) {
    for (HashMap.Entry<String, String> entry : variableTypeMap.entrySet()) {
      if (!currentVariableSubscriptMap.containsKey(entry.getKey()))
        return;
      int subscript = currentVariableSubscriptMap.get(entry.getKey());
      initializeFormalParams += entry.getKey() + "_version" + " = 0" + ";";
    }
  }

  @Override
  public void enterMethodDeclarator(Java8Parser.MethodDeclaratorContext ctx) {
    String methodName = ctx.getChild(0).getText();
    currentMethodName = methodName;
  }

  @Override
  public void enterNormalClassDeclaration(Java8Parser.NormalClassDeclarationContext ctx) {
    String className = ctx.getChild(2).getText();
    this.className = className;
  }

  // When exiting a method, initilize all variables (both from parameters and in body)
  @Override
  public void exitMethodBody(Java8Parser.MethodBodyContext ctx) {
    for (HashMap.Entry<String, Integer> entry : currentVariableSubscriptMap.entrySet()) {
      String variableName = entry.getKey();
      int currentSubscript = entry.getValue();
      String type = variableTypeMap.get(variableName);
      if (!methodParameters.contains(variableName)) {
        rewriter.insertAfter(ctx.getStart(), variableName + "_version" + " = -1;");
      }
    }
    rewriter.insertAfter(ctx.getStart(), initializeFormalParams);
    currentMethodName = "";
  }

  @Override
  public void exitMethodDeclaration(Java8Parser.MethodDeclarationContext ctx) {
    currentVariableSubscriptMap.clear();
  }

  // Replace ++ with intitializaton of a new variable
  @Override
  public void exitPostIncrementExpression(Java8Parser.PostIncrementExpressionContext ctx) {
    String varName = tokens.getText(ctx.postfixExpression().expressionName());
    if (isDescendantOf(ctx, Java8Parser.ForUpdateContext.class))
      return;
    int subscript = currentVariableSubscriptMap.get(varName);
    currentVariableSubscriptMap.put(varName, subscript + 1);

    ParserRuleContext currentContext = ctx;

    while (currentContext.getParent() != null) {
      if (currentContext instanceof Java8Parser.ExpressionStatementContext) {
        insertRecordStatementAfter(currentContext.getStop(), varName, currentContext.getStart().getLine());
        break;
      }
      currentContext = currentContext.getParent();
    }
  }

  // Replace -- with initialization of a new variable
  @Override
  public void exitPostDecrementExpression(Java8Parser.PostDecrementExpressionContext ctx) {
    String varName = tokens.getText(ctx.postfixExpression().expressionName());
    int subscript = currentVariableSubscriptMap.get(varName);
    currentVariableSubscriptMap.put(varName, subscript + 1);
  }

  @Override
  public void enterIfThenStatement(Java8Parser.IfThenStatementContext ctx) {
    updateVariableSubscriptPredicateStack();
    predicateBlockVariablesStack.push(new HashSet<String>());
    phiSubscriptQueue.addLast(phiCounter);
    phiCounter++;
  }

  @Override
  public void exitIfThenStatement(Java8Parser.IfThenStatementContext ctx) {
    HashMap<String, Integer> varSubscriptsBeforePredicate = varSubscriptsBeforePredicateStack.pop();
    String type = "Integer";
    ParserRuleContext exprCtx = ctx.expression();
    int phiSubscript = phiSubscriptQueue.removeFirst();

    // Get the SSA Form predicate to insert into Phi function
    String predicate = extractSSAFormPredicate(ctx.expression(), varSubscriptsBeforePredicate);

    for (String var : predicateBlockVariablesStack.pop()) {
      int subscript = currentVariableSubscriptMap.get(var);
      currentVariableSubscriptMap.put(var, subscript + 1);

      insertVersionUpdateAfter(ctx.getStop(), var);
      insertRecordStatementAfter(ctx.getStop(), var, ctx.getStop().getLine());
    }

  }

  @Override
  public void enterIfThenElseStatement(Java8Parser.IfThenElseStatementContext ctx) {
    updateVariableSubscriptPredicateStack();
    predicateBlockVariablesStack.push(new HashSet<String>());
    phiSubscriptQueue.addLast(phiCounter);
    phiCounter++;
  }

  @Override
  public void exitIfThenElseStatement(Java8Parser.IfThenElseStatementContext ctx) {
    HashMap<String, Integer> varSubscriptsBeforePredicate = varSubscriptsBeforePredicateStack.pop();
    String type = "Integer";
    ParserRuleContext exprCtx = ctx.expression();
    int phiSubscript = phiSubscriptQueue.removeFirst();

    // Get the SSA Form predicate to insert into Phi function
    String predicate = extractSSAFormPredicate(ctx.expression(), varSubscriptsBeforePredicate);

    for (String var : predicateBlockVariablesStack.pop()) {
      int subscript = currentVariableSubscriptMap.get(var);
      currentVariableSubscriptMap.put(var, subscript + 1);

      insertVersionUpdateAfter(ctx.getStop(), var);
      insertRecordStatementAfter(ctx.getStop(), var, ctx.getStop().getLine());
    }

  }

  @Override
  public void enterWhileStatement(Java8Parser.WhileStatementContext ctx) {
    updateVariableSubscriptPredicateStack();
    whileLoopVariableStack.push(new HashSet<String>());
    deferredPhiIfDeclarations.add(new HashMap<Integer, Java8Parser.IfThenStatementContext>());
    deferredPhiIfMerges.add(new HashMap<Java8Parser.IfThenStatementContext, ArrayList<String>>());

    predicateBlockVariablesStack.push(new HashSet<String>());
    phiSubscriptQueue.addLast(phiCounter);
    phiCounter++;
  }

  @Override
  public void exitWhileStatement(Java8Parser.WhileStatementContext ctx) {

    // Get all variables from predicate
    ArrayList<String> expressionNamesList = getAllExpressionNamesFromPredicate(ctx.expression());

    // Replace predicate with true
    rewriter.replace(ctx.expression().getStart(), ctx.expression().getStop(), "true");

    // Get block context of while loop
    Java8Parser.BlockContext blockContext = ctx.statement().statementWithoutTrailingSubstatement().block();

    if (blockContext == null) {
      // Handling a single-statement while loop by wrapping braces around it

      // Insert breaking statement equivalent to negation of original predicate
      rewriter.insertBefore(ctx.statement().statementWithoutTrailingSubstatement().getStart(),
          "if (!(" + ctx.expression().getText() + ")) {break;}");

      // Record predicate variable values
      for (String variable : expressionNamesList) {
        insertRecordStatementBefore(ctx.statement().statementWithoutTrailingSubstatement().getStart(), variable,
            ctx.expression().getStart().getLine());
        insertVersionUpdateBefore(ctx.statement().statementWithoutTrailingSubstatement().getStart(), variable);
      }

      // Wrap braces around the statement
      rewriter.insertBefore(ctx.statement().statementWithoutTrailingSubstatement().getStart(), "{");
      rewriter.insertAfter(ctx.statement().statementWithoutTrailingSubstatement().getStop(), "}");
    } else {
      Token blockContextStart = blockContext.getStart();

      // Record predicate variable values
      for (String variable : expressionNamesList) {
        insertVersionUpdateAfter(blockContextStart, variable);
        insertRecordStatementAfter(blockContextStart, variable, ctx.expression().getStart().getLine());
      }

      // Add breaking if statement equivalent to negation of original predicate
      rewriter.insertAfter(blockContextStart, "if (!(" + ctx.expression().getText() + ")) {break;}");
    }

    // Record values of all variables used inside the predicate block
    for (String variable : predicateBlockVariablesStack.pop()) {
      insertVersionUpdateAfter(ctx.getStop(), variable);
      insertRecordStatementAfter(ctx.getStop(), variable, ctx.getStop().getLine());
    }

  }

  @Override
  public void exitBasicForStatement(Java8Parser.BasicForStatementContext ctx) {
    // for (Method method : Java8Parser.BasicForStatementContext.class.getDeclaredMethods()) {
    //   int modifiers = method.getModifiers();
    //   if (Modifier.isPublic(modifiers) || Modifier.isProtected(modifiers)) {
    //     System.out.println(method);
    //   }
    // }
    rewriter.insertAfter(ctx.getStop(), "}");

    if (ctx.forInit() != null) {
      if (ctx.forInit().localVariableDeclaration() != null) {
        String forInit = "";
        ArrayList<TerminalNode> forInitTokens = getAllTokensFromContext(ctx.forInit().localVariableDeclaration());
        for (TerminalNode token : forInitTokens) {
          String tokenText = token.getText();
          forInit += tokenText + " ";
        }
        forInit += ";";
        for (int i = 0; i < ctx.forInit().localVariableDeclaration().variableDeclaratorList().variableDeclarator()
            .size(); i++) {

          Java8Parser.VariableDeclaratorIdContext varContext = ctx.forInit().localVariableDeclaration()
              .variableDeclaratorList().variableDeclarator(i).variableDeclaratorId();
          String variable = tokens.getText(varContext);
          int lineNumber = ctx.getStart().getLine();
          if (ctx.forInit().localVariableDeclaration().variableDeclaratorList().variableDeclarator(i)
              .variableInitializer() != null) {
            currentVariableSubscriptMap.put(variable, 0);
            insertRecordStatementBefore(ctx.getStart(), variable, lineNumber);
            insertVersionUpdateBefore(ctx.getStart(), variable);
          }
        }
        rewriter.insertBefore(ctx.getStart(), forInit);
      } else if (ctx.forInit().statementExpressionList() != null) {
        // TODO: Write function recording all expressions (assignment, postIncrement, etc.)
        for (int i = 0; i < ctx.forInit().statementExpressionList().statementExpression().size(); i++) {

          // Get for loop initializer
          String forInit = "";
          ArrayList<TerminalNode> forInitTokens = getAllTokensFromContext(
              ctx.forInit().statementExpressionList().statementExpression(i));
          for (TerminalNode token : forInitTokens) {
            String tokenText = token.getText();
            forInit += tokenText + " ";
          }
          forInit += ";";
          // Expression can only be one of assignment, preIncrementExpression, preDecrementExpression, 
          // postIncrementExpression, postDecrementExpression, methodInvocation, classInstanceCreationExpression;
          ParserRuleContext expressionContext = (ParserRuleContext) ctx.forInit().statementExpressionList()
              .statementExpression(i).getChild(0);
          // Handle assignment
          if (expressionContext instanceof Java8Parser.AssignmentContext) {
            Java8Parser.AssignmentContext assignmentContext = (Java8Parser.AssignmentContext) expressionContext;
            String variable = assignmentContext.leftHandSide().getText();
            int lineNumber = assignmentContext.getStart().getLine();
            insertRecordStatementBefore(ctx.getStart(), variable, lineNumber);
            insertVersionUpdateBefore(ctx.getStart(), variable);
          }

          if (expressionContext instanceof Java8Parser.PreIncrementExpressionContext
              || expressionContext instanceof Java8Parser.PreDecrementExpressionContext) {
            String variable = expressionContext.getChild(1).getText();
            int lineNumber = expressionContext.getStart().getLine();
            insertRecordStatementBefore(ctx.getStart(), variable, lineNumber);
            insertVersionUpdateBefore(ctx.getStart(), variable);
          }

          if (expressionContext instanceof Java8Parser.PostIncrementExpressionContext
              || expressionContext instanceof Java8Parser.PostDecrementExpressionContext) {
            if (expressionContext.getChild(0).getChild(0) instanceof Java8Parser.ExpressionNameContext) {
              String variable = expressionContext.getChild(0).getChild(0).getText();
              int lineNumber = expressionContext.getStart().getLine();
              insertRecordStatementBefore(ctx.getStart(), variable, lineNumber);
              insertVersionUpdateBefore(ctx.getStart(), variable);
            }
          }
          rewriter.insertBefore(ctx.getStart(), forInit);

        }
      }
      if (ctx.expression() != null) {
        Token endParenthesis = null;
        for (int i = 0; i < ctx.getChildCount(); i++) {
          if (ctx.getChild(i).getText().equals(")")) {
            TerminalNode node = (TerminalNode) ctx.getChild(i);
            endParenthesis = node.getSymbol();
          }
        }
        if (endParenthesis != null) {
          rewriter.replace(ctx.getStart(), endParenthesis, "while (true)");
        }
        ArrayList<String> expressionNamesList = getAllExpressionNamesFromPredicate(ctx.expression());
        Java8Parser.BlockContext blockContext = ctx.statement().statementWithoutTrailingSubstatement().block();

        if (blockContext != null) {
          for (String variable : expressionNamesList) {
            insertVersionUpdateAfter(ctx.statement().getStart(), variable);
            insertRecordStatementAfter(ctx.statement().getStart(), variable, ctx.expression().getStart().getLine());
          }
          rewriter.insertBefore(ctx.statement().getStart(), "if (!(" + ctx.expression().getText() + ")) {break;}");
        } else {
          rewriter.insertBefore(ctx.statement().getStart(), "if (!(" + ctx.expression().getText() + ")) {break;}");
          for (String variable : expressionNamesList) {
            insertRecordStatementBefore(ctx.statement().getStart(), variable, ctx.expression().getStart().getLine());
            insertVersionUpdateBefore(ctx.statement().getStart(), variable);
          }
          rewriter.insertBefore(ctx.statement().getStart(), "{");
          rewriter.insertAfter(ctx.statement().getStop(), "}");
        }
      }
    }
    // Handle forUpdate
    // assignment, preIncrementExpression, preDecrementExpression, postIncrementExpression, postDecrementExpression

    if (ctx.forUpdate() != null) {

    }

    rewriter.insertBefore(ctx.getStart(), "{");
  }

  public void insertVersionUpdateAfter(Token token, String variableName) {
    rewriter.insertAfter(token, variableName + "_version++;");
  }

  public void insertVersionUpdateBefore(Token token, String variableName) {
    rewriter.insertBefore(token, variableName + "_version++;");
  }

  public void insertRecordStatementAfter(Token token, String variableName, int lineNumber) {
    String variableInQuotes = "\"" + variableName + "\"";
    String packageNameInQuotes = "\"" + packageName + "\"";
    String classNameInQuotes = "\"" + className + "\"";
    String methodNameInQuotes = "\"" + currentMethodName + "\"";
    String variableVersionCounter = variableName + "_version";
    rewriter.insertAfter(token,
        "record(" + packageNameInQuotes + "," + classNameInQuotes + "," + methodNameInQuotes + "," + lineNumber + ","
            + scope + "," + variableInQuotes + "," + variableName + "," + variableVersionCounter + ");");
  }

  public void insertRecordStatementBefore(Token token, String variableName, int lineNumber) {
    String variableInQuotes = "\"" + variableName + "\"";
    String packageNameInQuotes = "\"" + packageName + "\"";
    String classNameInQuotes = "\"" + className + "\"";
    String methodNameInQuotes = "\"" + currentMethodName + "\"";
    String variableVersionCounter = variableName + "_version";
    rewriter.insertBefore(token,
        "record(" + packageNameInQuotes + "," + classNameInQuotes + "," + methodNameInQuotes + "," + lineNumber + ","
            + scope + "," + variableInQuotes + "," + variableName + "," + variableVersionCounter + ");");
  }

  public void writeWhileExit(Java8Parser.WhileStatementContext ctx, HashSet<String> whileLoopVariables,
      HashMap<String, Integer> varSubscriptsBeforePredicate, int phiSubscript) {
    for (String whileLoopVariable : whileLoopVariables) {
      int subscriptBeforePredicate = varSubscriptsBeforePredicate.get(whileLoopVariable);
      int currentSubscript = currentVariableSubscriptMap.get(whileLoopVariable);
      currentVariableSubscriptMap.put(whileLoopVariable, currentSubscript + 1);
    }
  }

  public void writeWhileEntry(Java8Parser.WhileStatementContext ctx, HashSet<String> whileLoopVariables,
      HashMap<String, Integer> varSubscriptsBeforePredicate, int phiSubscript) {
    for (String whileLoopVariable : whileLoopVariables) {
      int subscriptBeforePredicate = varSubscriptsBeforePredicate.get(whileLoopVariable);
      int currentSubscript = currentVariableSubscriptMap.get(whileLoopVariable);
      currentVariableSubscriptMap.put(whileLoopVariable, currentSubscript + 1);
    }
  }

  public ArrayList<String> getAllExpressionNamesFromPredicate(ParserRuleContext expressionContext) {

    ArrayList<String> expressionNamesList = new ArrayList<>();
    ArrayList<TerminalNode> ctxTokens = getAllTokensFromContext(expressionContext);

    for (TerminalNode token : ctxTokens) {
      String tokenText = token.getText();
      int tokenType = token.getSymbol().getType();
      if (tokenType == IDENTIFIER_TYPE) {
        expressionNamesList.add(tokenText);
      }
    }
    return expressionNamesList;
  }

  public void writePhiIfMerge(Java8Parser.IfThenStatementContext ctx, String assignedVariable, String phiSubscript,
      String predicateVariable, String beforePredicateVariable) {
  }

  public void writePhiIfDeclaration(Java8Parser.IfThenStatementContext ctx, String type, int phiSubscript,
      HashMap<String, Integer> varSubscriptsBeforePredicate) {
    String predicate = extractSSAFormPredicate(ctx.expression(), varSubscriptsBeforePredicate);
    String phiObject = "PhiIf<" + type + "> phi" + phiSubscript + " = new PhiIf<>(" + predicate + ");";
  }

  public boolean insidePredicate(ParserRuleContext ctx) {
    while (ctx.getParent().getParent() != null) {
      if (Java8Parser.ExpressionContext.class.isInstance(ctx.getParent())
          && (Java8Parser.IfThenStatementContext.class.isInstance(ctx.getParent().getParent())
              || Java8Parser.WhileStatementContext.class.isInstance(ctx.getParent().getParent())
              || Java8Parser.IfThenElseStatementContext.class.isInstance(ctx.getParent().getParent()))) {
        return true;
      }
      ctx = ctx.getParent();
    }
    return false;
  }

  public boolean insidePredicateBlock(ParserRuleContext ctx) {
    while (ctx.getParent() != null) {
      if (Java8Parser.IfThenStatementContext.class.isInstance(ctx.getParent().getParent())
          || Java8Parser.WhileStatementContext.class.isInstance(ctx.getParent().getParent())
          || Java8Parser.IfThenElseStatementContext.class.isInstance(ctx.getParent().getParent())) {
        return true;
      }
      ctx = ctx.getParent();
    }
    return false;
  }

  public HashSet<String> getAllCurrentPredicateVariables() {
    HashSet<String> allCurrentPredicateVariables = new HashSet<>();
    for (HashSet<String> predicateSet : predicateBlockVariablesStack) {
      allCurrentPredicateVariables.addAll(predicateSet);
    }
    return allCurrentPredicateVariables;
  }

  public String extractSSAFormPredicate(ParserRuleContext expressionContext,
      HashMap<String, Integer> varSubscriptsBeforePredicate) {
    String predicate = "";

    // Get the SSA Form predicate to insert into Phi function
    ArrayList<TerminalNode> ctxTokens = getAllTokensFromContext(expressionContext);
    for (TerminalNode token : ctxTokens) {
      String tokenText = token.getText();
      int tokenType = token.getSymbol().getType();
      if (tokenType == IDENTIFIER_TYPE) {
        int subscript = varSubscriptsBeforePredicate.get(tokenText);
        String ssaFormVariable = tokenText + "_" + subscript;
        predicate += ssaFormVariable;
      } else {
        predicate += tokenText;
      }
      predicate += " ";
    }
    predicate = predicate.trim();

    return predicate;
  }

  public String extractSSAFormUpdatePredicate(ParserRuleContext expressionContext) {
    String predicate = "";

    // Get the SSA Form predicate to insert into Phi function
    ArrayList<TerminalNode> ctxTokens = getAllTokensFromContext(expressionContext);
    for (TerminalNode token : ctxTokens) {
      String tokenText = token.getText();
      int tokenType = token.getSymbol().getType();
      if (tokenType == IDENTIFIER_TYPE) {
        int subscript = currentVariableSubscriptMap.get(tokenText);
        String ssaFormVariable = tokenText + "_" + subscript;
        predicate += ssaFormVariable;
      } else {
        predicate += tokenText;
      }
      predicate += " ";
    }
    predicate = predicate.trim();

    return predicate;
  }

  public void updateVariableSubscriptPredicateStack() {
    HashMap<String, Integer> varSubscriptsBeforePredicate = new HashMap<>(currentVariableSubscriptMap);
    varSubscriptsBeforePredicateStack.push(varSubscriptsBeforePredicate);
  }

  // Get all tokens in a given context
  public ArrayList<TerminalNode> getAllTokensFromContext(ParserRuleContext ctx) {
    ArrayList<TerminalNode> terminalNodes = new ArrayList<>();

    int numChildren = ctx.getChildCount();
    for (int i = 0; i < numChildren; i++) {
      if (ctx.getChild(i) instanceof TerminalNode) {
        terminalNodes.add((TerminalNode) ctx.getChild(i));
      } else if (ctx.getChild(i) instanceof ParserRuleContext) {
        terminalNodes.addAll(getAllTokensFromContext((ParserRuleContext) ctx.getChild(i)));
      }
    }

    return terminalNodes;
  }

  public HashSet<String> getIncrementAndDecrementVariablesFromAssignment(ParserRuleContext ctx) {
    HashSet<String> variables = new HashSet<>();

    for (int i = 0; i < ctx.getChildCount(); i++) {
      if (ctx.getChild(i) instanceof Java8Parser.PostIncrementExpression_lf_postfixExpressionContext) {
        // Get post increment variables
        Java8Parser.PostIncrementExpression_lf_postfixExpressionContext postfixExprCtx = (Java8Parser.PostIncrementExpression_lf_postfixExpressionContext) ctx
            .getChild(i);
        Java8Parser.PostfixExpressionContext parent = (Java8Parser.PostfixExpressionContext) postfixExprCtx.getParent();
        if (parent.expressionName() != null) {
          variables.add(parent.expressionName().getText());
        }

      } else if (ctx.getChild(i) instanceof Java8Parser.PostDecrementExpression_lf_postfixExpressionContext) {
        // Get post decrement variables
        Java8Parser.PostDecrementExpression_lf_postfixExpressionContext postfixExprCtx = (Java8Parser.PostDecrementExpression_lf_postfixExpressionContext) ctx
            .getChild(i);
        Java8Parser.PostfixExpressionContext parent = (Java8Parser.PostfixExpressionContext) postfixExprCtx.getParent();
        if (parent.expressionName() != null) {
          variables.add(parent.expressionName().getText());
        }

      } else if (ctx.getChild(i) instanceof Java8Parser.PreIncrementExpressionContext) {
        Java8Parser.PreIncrementExpressionContext expr = (Java8Parser.PreIncrementExpressionContext) ctx.getChild(i);
        variables.add(expr.unaryExpression().getText());

      } else if (ctx.getChild(i) instanceof Java8Parser.PreDecrementExpressionContext) {
        Java8Parser.PreDecrementExpressionContext expr = (Java8Parser.PreDecrementExpressionContext) ctx.getChild(i);
        variables.add(expr.unaryExpression().getText());
      }

      else if (ctx.getChild(i) instanceof ParserRuleContext) {
        variables.addAll(getIncrementAndDecrementVariablesFromAssignment((ParserRuleContext) ctx.getChild(i)));
      }
    }
    return variables;
  }

  // Checks whether a given context is the descendant of another given context
  public boolean isDescendantOf(ParserRuleContext ctx, Class cls) {
    while (ctx.getParent() != null) {
      if (cls.isInstance(ctx.getParent())) {
        return true;
      }
      ctx = ctx.getParent();
    }
    return false;
  }
}

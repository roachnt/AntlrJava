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
import org.antlr.v4.runtime.tree.ParseTree;
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

  // Keep track of variables in scope
  Stack<HashMap<String, Integer>> variablesInScope = new Stack<>();

  HashSet<String> allLocalVariables = new HashSet<>();

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
    if (!variablesInScope.empty())
      variablesInScope.push(new HashMap<String, Integer>(variablesInScope.lastElement()));
    else {
      HashMap<String, Integer> firstBlockScope = new HashMap<String, Integer>();
      for (String param : methodParameters) {
        firstBlockScope.put(param, 0);
      }
      variablesInScope.push(firstBlockScope);
    }
  }

  public void exitBlock(Java8Parser.BlockContext ctx) {
    scope--;
    variablesInScope.pop();
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
      Java8Parser.VariableDeclaratorIdContext varContext = ctx.variableDeclaratorList().variableDeclarator(i)
          .variableDeclaratorId();
      String variable = tokens.getText(varContext);

      variablesInScope.lastElement().put(variable, -1);
      // System.out.println(variablesInScope);
      int lineNumber = ctx.getStart().getLine();

      if (!isDescendantOf(ctx, Java8Parser.ForInitContext.class)
          && ctx.variableDeclaratorList().variableDeclarator(i).variableInitializer() != null) {
        currentVariableSubscriptMap.put(variable, 0);
        insertVersionUpdateAfter(ctx.getParent().getParent().getStop(), variable);
        insertRecordStatementAfter(ctx.getParent().getStop(), variable, lineNumber);
        HashSet<String> postFixAlteredVariables = getIncrementAndDecrementVariablesFromAssignment(ctx);
        for (String alteredVariable : postFixAlteredVariables) {
          if (insidePredicateBlock(ctx)) {
            for (HashSet<String> predicateBlockVariables : predicateBlockVariablesStack) {
              predicateBlockVariables.add(alteredVariable);
            }
          }
        }
      }
      if (!isDescendantOf(ctx, Java8Parser.ForInitContext.class))
        allLocalVariables.add(variable);
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

    if (!isDescendantOf(ctx, Java8Parser.ForInitContext.class)
        && !isDescendantOf(ctx, Java8Parser.ForUpdateContext.class)) {
      insertVersionUpdateAfter(ctx.getParent().getParent().getStop(), variable);
      insertRecordStatementAfter(ctx.getParent().getParent().getStop(), variable, lineNumber);
    }

    HashSet<String> postFixAlteredVariables = getIncrementAndDecrementVariablesFromAssignment(ctx);
    for (String alteredVariable : postFixAlteredVariables) {
      insertVersionUpdateAfter(ctx.getParent().getParent().getStop(), alteredVariable);
      insertRecordStatementAfter(ctx.getParent().getParent().getStop(), alteredVariable, lineNumber);
      if (insidePredicateBlock(ctx)) {
        for (HashSet<String> predicateBlockVariables : predicateBlockVariablesStack) {
          predicateBlockVariables.add(alteredVariable);
        }
      }
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
      initializeFormalParams += "int " + entry.getKey() + "_version" + " = 0" + ";";
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

  @Override
  public void enterClassBody(Java8Parser.ClassBodyContext ctx) {
    rewriter.insertAfter(ctx.getStart(),
        "public static void record(String packageName,String clazz,String method,int line,int staticScope,String variableName,Object value,int version) {System.out.println(String.format(\"package: %s, class: %s, method: %s, line: %d, static-scope: %d, variable: %s, value: %s, version: %d\",packageName,clazz,method,line,staticScope,variableName,value.toString(),version));}");
  }

  // When exiting a method, initilize all variables (both from parameters and in body)
  @Override
  public void exitMethodBody(Java8Parser.MethodBodyContext ctx) {
    for (String variable : allLocalVariables) {
      if (!methodParameters.contains(variable)) {
        rewriter.insertAfter(ctx.getStart(), "int " + variable + "_version" + " = -1;");
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
        insertVersionUpdateAfter(currentContext.getStop(), varName);
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
    if (isDescendantOf(ctx, Java8Parser.ForUpdateContext.class))
      return;
    int subscript = currentVariableSubscriptMap.get(varName);
    currentVariableSubscriptMap.put(varName, subscript + 1);

    ParserRuleContext currentContext = ctx;

    while (currentContext.getParent() != null) {
      if (currentContext instanceof Java8Parser.ExpressionStatementContext) {
        insertVersionUpdateAfter(currentContext.getStop(), varName);
        insertRecordStatementAfter(currentContext.getStop(), varName, currentContext.getStart().getLine());
        break;
      }
      currentContext = currentContext.getParent();
    }
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
    // String predicate = extractSSAFormPredicate(ctx.expression(), varSubscriptsBeforePredicate);

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
  public void enterBasicForStatement(Java8Parser.BasicForStatementContext ctx) {
    predicateBlockVariablesStack.push(new HashSet<String>());
  }

  @Override
  public void exitBasicForStatement(Java8Parser.BasicForStatementContext ctx) {

    if (ctx.forInit() != null) {
      handleBasicForInit(ctx.forInit(), ctx);
    }

    if (ctx.forUpdate() != null) {
      handleBasicForUpdate(ctx);
    }

    if (ctx.expression() != null) {
      handleBasicForExpression(ctx);
    }

    if (ctx.statement().statementWithoutTrailingSubstatement().block() == null) {
      rewriter.insertBefore(ctx.statement().getStart(), "{");
      rewriter.insertAfter(ctx.statement().getStop(), "}");
    } else {
      rewriter.insertAfter(ctx.statement().getStart(), "if (!(" + ctx.expression().getText() + ")) {break;}");
    }
    rewriter.insertBefore(ctx.getStart(), "{");
    rewriter.insertAfter(ctx.getStop(), "}");

    HashSet<String> forInitDeclarations = getAllForInitDeclarations(ctx.forInit());
    for (String variable : predicateBlockVariablesStack.pop()) {
      if (!(forInitDeclarations.contains(variable))) {
        insertVersionUpdateAfter(ctx.getStop(), variable);
        insertRecordStatementAfter(ctx.getStop(), variable, ctx.getStop().getLine());
      }
    }
  }

  public HashSet<String> getAllForInitDeclarations(Java8Parser.ForInitContext ctx) {
    HashSet<String> forInitDeclarations = new HashSet<>();
    if (ctx.localVariableDeclaration() != null) {
      for (int i = 0; i < ctx.localVariableDeclaration().variableDeclaratorList().variableDeclarator().size(); i++) {
        Java8Parser.VariableDeclaratorIdContext varContext = ctx.localVariableDeclaration().variableDeclaratorList()
            .variableDeclarator(i).variableDeclaratorId();
        String variable = tokens.getText(varContext);
        forInitDeclarations.add(variable);
      }
    }
    return forInitDeclarations;
  }

  @Override
  public void enterBasicForStatementNoShortIf(Java8Parser.BasicForStatementNoShortIfContext ctx) {
    predicateBlockVariablesStack.push(new HashSet<String>());
  }

  public void exitBasicForStatementNoShortIf(Java8Parser.BasicForStatementNoShortIfContext ctx) {

    if (ctx.forInit() != null) {
      handleBasicForInit(ctx.forInit(), ctx);
    }

    if (ctx.forUpdate() != null) {
      handleBasicForNoShortIfUpdate(ctx);
    }

    if (ctx.expression() != null) {
      handleBasicForNoShortIfExpression(ctx);
    }

    if (ctx.statementNoShortIf().statementWithoutTrailingSubstatement().block() == null) {
      rewriter.insertBefore(ctx.statementNoShortIf().getStart(), "{");
      rewriter.insertAfter(ctx.statementNoShortIf().getStop(), "}");
    } else {
      rewriter.insertAfter(ctx.statementNoShortIf().getStart(), "if (!(" + ctx.expression().getText() + ")) {break;}");
    }
    rewriter.insertBefore(ctx.getStart(), "{");
    rewriter.insertAfter(ctx.getStop(), "}");

    HashSet<String> forInitDeclarations = getAllForInitDeclarations(ctx.forInit());
    for (String variable : predicateBlockVariablesStack.pop()) {
      if (!(forInitDeclarations.contains(variable))) {
        insertVersionUpdateAfter(ctx.getStop(), variable);
        insertRecordStatementAfter(ctx.getStop(), variable, ctx.getStop().getLine());
      }
    }
  }

  public void handleBasicForInit(Java8Parser.ForInitContext ctx, ParserRuleContext forCtx) {
    if (ctx.localVariableDeclaration() != null) {
      String forInit = "";
      ArrayList<TerminalNode> forInitTokens = getAllTokensFromContext(ctx.localVariableDeclaration());
      for (TerminalNode token : forInitTokens) {
        String tokenText = token.getText();
        forInit += tokenText + " ";
      }
      forInit += ";";
      for (int i = 0; i < ctx.localVariableDeclaration().variableDeclaratorList().variableDeclarator().size(); i++) {
        Java8Parser.VariableDeclaratorIdContext varContext = ctx.localVariableDeclaration().variableDeclaratorList()
            .variableDeclarator(i).variableDeclaratorId();
        String variable = tokens.getText(varContext);
        int lineNumber = forCtx.getStart().getLine();
        if (ctx.localVariableDeclaration().variableDeclaratorList().variableDeclarator(i)
            .variableInitializer() != null) {
          currentVariableSubscriptMap.put(variable, 0);
          insertRecordStatementBefore(forCtx.getStart(), variable, lineNumber);
          insertVersionUpdateBefore(forCtx.getStart(), variable);
        }
      }
      rewriter.insertBefore(forCtx.getStart(), forInit);
      for (int i = 0; i < ctx.localVariableDeclaration().variableDeclaratorList().variableDeclarator().size(); i++) {
        Java8Parser.VariableDeclaratorIdContext varContext = ctx.localVariableDeclaration().variableDeclaratorList()
            .variableDeclarator(i).variableDeclaratorId();
        String variable = tokens.getText(varContext);
        int lineNumber = forCtx.getStart().getLine();
        currentVariableSubscriptMap.put(variable, 0);
        rewriter.insertBefore(forCtx.getStart(), "int " + variable + "_version = -1;");
      }
    } else if (ctx.statementExpressionList() != null) {

      for (int i = 0; i < ctx.statementExpressionList().statementExpression().size(); i++) {

        // Get for loop initializer
        String forInit = "";
        ArrayList<TerminalNode> forInitTokens = getAllTokensFromContext(
            ctx.statementExpressionList().statementExpression(i));
        for (TerminalNode token : forInitTokens) {
          String tokenText = token.getText();
          forInit += tokenText + " ";
        }
        forInit += ";";
        // Expression can only be one of assignment, preIncrementExpression, preDecrementExpression, 
        // postIncrementExpression, postDecrementExpression, methodInvocation, classInstanceCreationExpression;
        ParserRuleContext expressionContext = (ParserRuleContext) ctx.statementExpressionList().statementExpression(i)
            .getChild(0);
        // Handle assignment
        if (expressionContext instanceof Java8Parser.AssignmentContext) {
          Java8Parser.AssignmentContext assignmentContext = (Java8Parser.AssignmentContext) expressionContext;
          String variable = assignmentContext.leftHandSide().getText();
          int lineNumber = assignmentContext.getStart().getLine();
          HashSet<String> postFixAlteredVariables = getIncrementAndDecrementVariablesFromAssignment(assignmentContext);
          for (String alteredVariable : postFixAlteredVariables) {
            insertRecordStatementBefore(forCtx.getStart(), alteredVariable, lineNumber);
            insertVersionUpdateBefore(forCtx.getStart(), alteredVariable);
          }
          insertRecordStatementBefore(forCtx.getStart(), variable, lineNumber);
          insertVersionUpdateBefore(forCtx.getStart(), variable);
        }

        if (expressionContext instanceof Java8Parser.PreIncrementExpressionContext
            || expressionContext instanceof Java8Parser.PreDecrementExpressionContext) {
          String variable = expressionContext.getChild(1).getText();
          int lineNumber = expressionContext.getStart().getLine();
          insertRecordStatementBefore(forCtx.getStart(), variable, lineNumber);
          insertVersionUpdateBefore(forCtx.getStart(), variable);
        }

        if (expressionContext instanceof Java8Parser.PostIncrementExpressionContext
            || expressionContext instanceof Java8Parser.PostDecrementExpressionContext) {
          if (expressionContext.getChild(0).getChild(0) instanceof Java8Parser.ExpressionNameContext) {
            String variable = expressionContext.getChild(0).getChild(0).getText();
            int lineNumber = expressionContext.getStart().getLine();
            insertRecordStatementBefore(forCtx.getStart(), variable, lineNumber);
            insertVersionUpdateBefore(forCtx.getStart(), variable);
          }
        }
        rewriter.insertBefore(forCtx.getStart(), forInit);

      }
    }
  }

  public void handleBasicForUpdate(Java8Parser.BasicForStatementContext ctx) {
    ArrayList<Java8Parser.ContinueStatementContext> continueContexts = getAllContinueStatementContextsFromForLoop(ctx);

    for (int i = 0; i < ctx.forUpdate().statementExpressionList().statementExpression().size(); i++) {
      ParserRuleContext expressionContext = (ParserRuleContext) ctx.forUpdate().statementExpressionList()
          .statementExpression(i).getChild(0);
      // Handle assignment
      if (expressionContext instanceof Java8Parser.AssignmentContext) {
        Java8Parser.AssignmentContext assignmentContext = (Java8Parser.AssignmentContext) expressionContext;
        String variable = assignmentContext.leftHandSide().getText();
        int lineNumber = assignmentContext.getStart().getLine();
        rewriter.insertAfter(ctx.getStop(), assignmentContext.getText() + ";");
        insertVersionUpdateAfter(ctx.getStop(), variable);
        insertRecordStatementAfter(ctx.getStop(), variable, lineNumber);
        HashSet<String> postFixAlteredVariables = getIncrementAndDecrementVariablesFromAssignment(assignmentContext);
        for (String alteredVariable : postFixAlteredVariables) {
          insertVersionUpdateAfter(ctx.getStop(), alteredVariable);
          insertRecordStatementAfter(ctx.getStop(), alteredVariable, lineNumber);
        }
        for (Java8Parser.ContinueStatementContext continueContext : continueContexts) {
          for (String alteredVariable : postFixAlteredVariables) {
            insertRecordStatementBefore(continueContext.getStart(), alteredVariable, lineNumber);
            insertVersionUpdateBefore(continueContext.getStart(), alteredVariable);
          }
          insertRecordStatementBefore(continueContext.getStart(), variable, lineNumber);
          insertVersionUpdateBefore(continueContext.getStart(), variable);
          rewriter.insertBefore(ctx.statement().getStop(), expressionContext.getText() + ";");
          rewriter.insertBefore(continueContext.getStart(), "{");
          rewriter.insertAfter(continueContext.getStop(), "}");
        }
      }

      if (expressionContext instanceof Java8Parser.PreIncrementExpressionContext
          || expressionContext instanceof Java8Parser.PreDecrementExpressionContext) {
        String variable = expressionContext.getChild(1).getText();
        int lineNumber = expressionContext.getStart().getLine();

        if (ctx.statement().statementWithoutTrailingSubstatement().block() == null) {
          insertVersionUpdateBefore(ctx.statement().getStart(), variable);
          insertRecordStatementBefore(ctx.statement().getStart(), variable, lineNumber);
          rewriter.insertBefore(ctx.statement().getStart(), expressionContext.getText() + ";");
        } else {
          rewriter.insertAfter(ctx.statement().getStart(), expressionContext.getText() + ";");
          insertVersionUpdateAfter(ctx.statement().getStart(), variable);
          insertRecordStatementAfter(ctx.statement().getStart(), variable, lineNumber);
        }
      }

      if (expressionContext instanceof Java8Parser.PostIncrementExpressionContext
          || expressionContext instanceof Java8Parser.PostDecrementExpressionContext) {
        if (expressionContext.getChild(0).getChild(0) instanceof Java8Parser.ExpressionNameContext) {
          String variable = expressionContext.getChild(0).getChild(0).getText();
          int lineNumber = expressionContext.getStart().getLine();
          if (ctx.statement().statementWithoutTrailingSubstatement().block() == null) {
            rewriter.insertAfter(ctx.statement().getStop(), expressionContext.getText() + ";");
            insertVersionUpdateAfter(ctx.statement().getStop(), variable);
            insertRecordStatementAfter(ctx.statement().getStop(), variable, lineNumber);
          } else {
            insertRecordStatementBefore(ctx.statement().getStop(), variable, lineNumber);
            insertVersionUpdateBefore(ctx.statement().getStop(), variable);
            rewriter.insertBefore(ctx.statement().getStop(), expressionContext.getText() + ";");
          }
          for (Java8Parser.ContinueStatementContext continueContext : continueContexts) {
            insertRecordStatementBefore(continueContext.getStart(), variable, lineNumber);
            insertVersionUpdateBefore(continueContext.getStart(), variable);
            rewriter.insertBefore(continueContext.getStart(), expressionContext.getText() + ";");
            rewriter.insertBefore(continueContext.getStart(), "{");
            rewriter.insertAfter(continueContext.getStop(), "}");
          }
        }
      }
    }
  }

  public void handleBasicForNoShortIfUpdate(Java8Parser.BasicForStatementNoShortIfContext ctx) {
    ArrayList<Java8Parser.ContinueStatementContext> continueContexts = getAllContinueStatementContextsFromForLoop(ctx);

    for (int i = 0; i < ctx.forUpdate().statementExpressionList().statementExpression().size(); i++) {
      ParserRuleContext expressionContext = (ParserRuleContext) ctx.forUpdate().statementExpressionList()
          .statementExpression(i).getChild(0);
      // Handle assignment
      if (expressionContext instanceof Java8Parser.AssignmentContext) {
        Java8Parser.AssignmentContext assignmentContext = (Java8Parser.AssignmentContext) expressionContext;
        String variable = assignmentContext.leftHandSide().getText();
        int lineNumber = assignmentContext.getStart().getLine();
        rewriter.insertAfter(ctx.getStop(), assignmentContext.getText() + ";");
        insertVersionUpdateAfter(ctx.getStop(), variable);
        insertRecordStatementAfter(ctx.getStop(), variable, lineNumber);
        HashSet<String> postFixAlteredVariables = getIncrementAndDecrementVariablesFromAssignment(assignmentContext);
        for (String alteredVariable : postFixAlteredVariables) {
          insertVersionUpdateAfter(ctx.getStop(), alteredVariable);
          insertRecordStatementAfter(ctx.getStop(), alteredVariable, lineNumber);
        }

        for (Java8Parser.ContinueStatementContext continueContext : continueContexts) {
          for (String alteredVariable : postFixAlteredVariables) {
            insertRecordStatementBefore(continueContext.getStart(), alteredVariable, lineNumber);
            insertVersionUpdateBefore(continueContext.getStart(), alteredVariable);
          }
          insertRecordStatementBefore(continueContext.getStart(), variable, lineNumber);
          insertVersionUpdateBefore(continueContext.getStart(), variable);
          rewriter.insertBefore(continueContext.getStart(), expressionContext.getText() + ";");
          rewriter.insertBefore(continueContext.getStart(), "{");
          rewriter.insertAfter(continueContext.getStop(), "}");
        }
      }

      if (expressionContext instanceof Java8Parser.PreIncrementExpressionContext
          || expressionContext instanceof Java8Parser.PreDecrementExpressionContext) {
        String variable = expressionContext.getChild(1).getText();
        int lineNumber = expressionContext.getStart().getLine();

        if (ctx.statementNoShortIf().statementWithoutTrailingSubstatement().block() == null) {
          insertVersionUpdateBefore(ctx.statementNoShortIf().getStart(), variable);
          insertRecordStatementBefore(ctx.statementNoShortIf().getStart(), variable, lineNumber);
          rewriter.insertBefore(ctx.statementNoShortIf().getStart(), expressionContext.getText() + ";");
        } else {
          rewriter.insertAfter(ctx.statementNoShortIf().getStart(), expressionContext.getText() + ";");
          insertVersionUpdateAfter(ctx.statementNoShortIf().getStart(), variable);
          insertRecordStatementAfter(ctx.statementNoShortIf().getStart(), variable, lineNumber);
        }
      }

      if (expressionContext instanceof Java8Parser.PostIncrementExpressionContext
          || expressionContext instanceof Java8Parser.PostDecrementExpressionContext) {
        if (expressionContext.getChild(0).getChild(0) instanceof Java8Parser.ExpressionNameContext) {
          String variable = expressionContext.getChild(0).getChild(0).getText();
          int lineNumber = expressionContext.getStart().getLine();
          if (ctx.statementNoShortIf().statementWithoutTrailingSubstatement().block() == null) {
            rewriter.insertAfter(ctx.statementNoShortIf().getStop(), expressionContext.getText() + ";");
            insertVersionUpdateAfter(ctx.statementNoShortIf().getStop(), variable);
            insertRecordStatementAfter(ctx.statementNoShortIf().getStop(), variable, lineNumber);
          } else {
            insertVersionUpdateBefore(ctx.statementNoShortIf().getStop(), variable);
            insertRecordStatementBefore(ctx.statementNoShortIf().getStop(), variable, lineNumber);
            rewriter.insertBefore(ctx.statementNoShortIf().getStop(), expressionContext.getText() + ";");
          }
          for (Java8Parser.ContinueStatementContext continueContext : continueContexts) {
            insertRecordStatementBefore(continueContext.getStart(), variable, lineNumber);
            insertVersionUpdateBefore(continueContext.getStart(), variable);
            rewriter.insertBefore(continueContext.getStart(), expressionContext.getText() + ";");
            rewriter.insertBefore(continueContext.getStart(), "{");
            rewriter.insertAfter(continueContext.getStop(), "}");
          }
        }
      }
    }
  }

  public void handleBasicForExpression(Java8Parser.BasicForStatementContext ctx) {
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
    } else {
      rewriter.insertBefore(ctx.statement().getStart(), "if (!(" + ctx.expression().getText() + ")) {break;}");
      for (String variable : expressionNamesList) {
        insertRecordStatementBefore(ctx.statement().getStart(), variable, ctx.expression().getStart().getLine());
        insertVersionUpdateBefore(ctx.statement().getStart(), variable);
      }
    }
  }

  public void handleBasicForNoShortIfExpression(Java8Parser.BasicForStatementNoShortIfContext ctx) {
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
    Java8Parser.BlockContext blockContext = ctx.statementNoShortIf().statementWithoutTrailingSubstatement().block();

    if (blockContext != null) {
      for (String variable : expressionNamesList) {
        insertVersionUpdateAfter(ctx.statementNoShortIf().getStart(), variable);
        insertRecordStatementAfter(ctx.statementNoShortIf().getStart(), variable,
            ctx.expression().getStart().getLine());
      }
    } else {
      rewriter.insertBefore(ctx.statementNoShortIf().getStart(), "if (!(" + ctx.expression().getText() + ")) {break;}");
      for (String variable : expressionNamesList) {
        insertRecordStatementBefore(ctx.statementNoShortIf().getStart(), variable,
            ctx.expression().getStart().getLine());
        insertVersionUpdateBefore(ctx.statementNoShortIf().getStart(), variable);
      }
    }
  }

  @Override
  public void exitEnhancedForStatement(Java8Parser.EnhancedForStatementContext ctx) {
    TerminalNode forNode = (TerminalNode) ctx.getChild(0);
    TerminalNode startParenthesis = (TerminalNode) ctx.getChild(1);
    TerminalNode endParenthesis = (TerminalNode) ctx.getChild(ctx.getChildCount() - 2);

    String iterableName = ctx.expression().getText();
    String iteratorType = ctx.unannType().getText();
    String iteratorItem = ctx.variableDeclaratorId().getText();

    ArrayList<Java8Parser.ContinueStatementContext> continueContexts = getAllContinueStatementContextsFromForLoop(ctx);
    // TODO: insert .next() call before continue statements

    rewriter.insertBefore(forNode.getSymbol(),
        "java.util.Iterator<" + iteratorType + ">" + iterableName + "_iterator = " + iterableName + ".iterator();");

    if (ctx.statement().statementWithoutTrailingSubstatement().block() == null) {
      rewriter.insertBefore(ctx.statement().getStart(),
          iteratorType + " " + iteratorItem + "=" + iterableName + "_iterator" + ".next();");
    } else {
      rewriter.insertAfter(ctx.statement().getStart(),
          iteratorType + " " + iteratorItem + "=" + iterableName + "_iterator" + ".next();");
    }

    rewriter.replace(forNode.getSymbol(), "while");
    rewriter.replace(startParenthesis.getSymbol(), endParenthesis.getSymbol(),
        "(" + iterableName + "_iterator" + ".hasNext())");

    if (ctx.statement().statementWithoutTrailingSubstatement().block() == null) {
      rewriter.insertBefore(ctx.statement().getStart(), "{");
      rewriter.insertAfter(ctx.statement().getStop(), "}");
    }
  }

  public ArrayList<Java8Parser.ContinueStatementContext> getAllContinueStatementContextsFromForLoop(
      ParserRuleContext ctx) {
    ArrayList<Java8Parser.ContinueStatementContext> continueContexts = new ArrayList<>();

    int numChildren = ctx.getChildCount();
    for (int i = 0; i < numChildren; i++) {
      if (ctx.getChild(i) instanceof Java8Parser.ContinueStatementContext) {
        continueContexts.add((Java8Parser.ContinueStatementContext) ctx.getChild(i));
      } else if (ctx.getChild(i) instanceof ParserRuleContext) {
        continueContexts.addAll(getAllContinueStatementContextsFromForLoop((ParserRuleContext) ctx.getChild(i)));
      }
    }

    return continueContexts;
  }

  public void insertVersionUpdateAfter(Token token, String variableName) {
    int version = variablesInScope.lastElement().get(variableName);
    for (HashMap<String, Integer> versionMap : variablesInScope) {
      if (versionMap.keySet().contains(variableName)) {
        versionMap.put(variableName, version + 1);
      }
    }
    // System.out.println(token.getText());
    // System.out.println(variableName + " " + variablesInScope.lastElement().get(variableName));
    rewriter.insertAfter(token, variableName + "_version = " + variablesInScope.lastElement().get(variableName) + ";");
  }

  public void insertVersionUpdateBefore(Token token, String variableName) {
    int version = variablesInScope.lastElement().get(variableName);
    for (HashMap<String, Integer> versionMap : variablesInScope) {
      if (versionMap.keySet().contains(variableName)) {
        versionMap.put(variableName, version + 1);
      }
    }
    // System.out.println(token.getText());
    // System.out.println(variableName + " " + variablesInScope.lastElement().get(variableName));
    rewriter.insertBefore(token, variableName + "_version = " + variablesInScope.lastElement().get(variableName) + ";");
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
          || Java8Parser.WhileStatementNoShortIfContext.class.isInstance(ctx.getParent().getParent())
          || Java8Parser.IfThenElseStatementContext.class.isInstance(ctx.getParent().getParent())
          || Java8Parser.IfThenElseStatementNoShortIfContext.class.isInstance(ctx.getParent().getParent())
          || Java8Parser.ForStatementContext.class.isInstance(ctx.getParent().getParent())
          || Java8Parser.ForStatementNoShortIfContext.class.isInstance(ctx.getParent().getParent())
          || Java8Parser.EnhancedForStatementContext.class.isInstance(ctx.getParent().getParent())
          || Java8Parser.EnhancedForStatementNoShortIfContext.class.isInstance(ctx.getParent().getParent())) {
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

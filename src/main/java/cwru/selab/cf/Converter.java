package cwru.selab.cf;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Stack;
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

  String className = "";
  String currentMethodName = "";

  public Converter(Java8Parser parser, TokenStreamRewriter rewriter) {
    this.rewriter = rewriter;
    this.parser = parser;
    this.tokens = parser.getTokenStream();
  }

  // Handling basic assignment statements
  @Override
  public void enterLeftHandSide(Java8Parser.LeftHandSideContext ctx) {
    String variable = tokens.getText(ctx);
    int subscript = 0;

    if (currentVariableSubscriptMap.containsKey(variable)) {
      subscript = currentVariableSubscriptMap.get(variable) + 1;
    }

    if (isDescendantOf(ctx, Java8Parser.IfThenStatementContext.class)
        || isDescendantOf(ctx, Java8Parser.WhileStatementContext.class)) {
      predicateBlockVariablesStack.lastElement().add(variable);
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
      Java8Parser.VariableDeclaratorIdContext varContext = ctx.variableDeclaratorList().variableDeclarator(0)
          .variableDeclaratorId();
      String variable = tokens.getText(varContext);
      currentVariableSubscriptMap.put(variable, 0);
      variableTypeMap.put(variable, type);
    }
    rewriter.insertAfter(ctx.getStop(), "\n    record();");
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
  }

  @Override
  public void exitExpressionStatement(Java8Parser.ExpressionStatementContext ctx) {
    if (ctx.getChildCount() == 0) {
      return;
    }

    if (ctx.getChild(0).getChildCount() == 0)
      return;

    if (ctx.getChild(0).getChild(0) instanceof Java8Parser.AssignmentContext) {
      rewriter.insertAfter(ctx.getStop(), "\n    record();");
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
      initializeFormalParams += "\n    " + entry.getKey() + "_version" + " = 0" + ";";
    }
    initializeFormalParams += "\n";
  }

  // When exiting a method, initilize all variables (both from parameters and in body)
  @Override
  public void exitMethodBody(Java8Parser.MethodBodyContext ctx) {
    rewriter.insertAfter(ctx.getStart(), "\n");
    for (HashMap.Entry<String, Integer> entry : currentVariableSubscriptMap.entrySet()) {
      rewriter.insertAfter(ctx.getStart(), "    ");
      String variableName = entry.getKey();
      int currentSubscript = entry.getValue();
      String type = variableTypeMap.get(variableName);
      if (!methodParameters.contains(variableName)) {
        rewriter.insertAfter(ctx.getStart(), variableName + "_version" + " = -1;");
      }
      rewriter.insertAfter(ctx.getStart(), "\n");
    }
    rewriter.insertAfter(ctx.getStart(), initializeFormalParams);
  }

  @Override
  public void exitMethodDeclaration(Java8Parser.MethodDeclarationContext ctx) {
    currentVariableSubscriptMap.clear();
  }

  // Replace ++ with intitializaton of a new variable
  @Override
  public void exitPostIncrementExpression(Java8Parser.PostIncrementExpressionContext ctx) {
    String varName = tokens.getText(ctx.postfixExpression().expressionName());
    int subscript = currentVariableSubscriptMap.get(varName);
    currentVariableSubscriptMap.put(varName, subscript + 1);
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

    // TODO: Type checking and changes for different data types
    if (isDescendantOf(ctx, Java8Parser.WhileStatementContext.class)) {
      deferredPhiIfDeclarations.lastElement().put(phiCounter, ctx);
    } else {
      writePhiIfDeclaration(ctx, type, phiCounter, varSubscriptsBeforePredicate);
    }

    rewriter.insertAfter(ctx.getStop(), "\n");
    for (String var : predicateBlockVariablesStack.pop()) {
      rewriter.insertAfter(ctx.getStop(), "    ");
      int subscript = currentVariableSubscriptMap.get(var);
      int prePredicateSubscript = varSubscriptsBeforePredicate.get(var);

      String beforePredicateVariable = var + "_" + prePredicateSubscript;
      String predicateVariable = var + "_" + subscript;

      currentVariableSubscriptMap.put(var, subscript + 1);
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
    HashMap<String, Integer> varSubscriptsBeforePredicate = varSubscriptsBeforePredicateStack.pop();
    String type = "Integer";
    ParserRuleContext exprCtx = ctx.expression();
    int phiSubscript = phiSubscriptQueue.removeFirst();
    HashSet<String> whileLoopVariables = whileLoopVariableStack.pop();

    // Get the SSA Form predicate to insert into Phi function
    String predicate = extractSSAFormPredicate(ctx.expression(), varSubscriptsBeforePredicate);
    String phiObject = "PhiWhile<" + type + "> phi" + phiSubscript + " = new PhiWhile<>(" + predicate + ");";

    String updatePredicate = extractSSAFormUpdatePredicate(ctx.expression());

    // Take care of SSA variables that had to be put off for while loop

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
              || Java8Parser.WhileStatementContext.class.isInstance(ctx.getParent().getParent()))) {
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

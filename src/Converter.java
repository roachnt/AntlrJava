import java.util.HashMap;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.Stack;

import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.TokenStreamRewriter;
import org.antlr.v4.runtime.tree.TerminalNode;

import org.antlr.v4.runtime.ParserRuleContext;

public class Converter extends Java8BaseListener {

  final int IDENTIFIER_TYPE = 102;

  Java8Parser parser;

  HashMap<String, String> dataTypeToObjectMap = new HashMap<String, String>() {
    {
      put("int", "Integer");
      put("double", "Double");
      put("boolean", "Boolean");
      put("float", "Float");
    }
  };

  // Rewriting mechanism
  TokenStreamRewriter rewriter;

  // Tokens from the program
  TokenStream tokens;

  // Counter for Phi functions
  int phiCounter = 0;

  // Variables in predicate block being assigned
  Stack<HashSet<String>> predicateBlockVariablesStack = new Stack<>();

  // Variable subscripts before entering the predicate block
  Stack<HashMap<String, Integer>> varSubscriptsBeforePredicateStack = new Stack<>();

  // Map from SSA-form variable to an array of the variables in its assignment
  HashMap<String, ArrayList<String>> variableConfoundersMap = new HashMap<>();

  // Keep track of current subcript of a variable when converting to SSA
  HashMap<String, Integer> currentVariableSubscriptMap = new HashMap<>();

  // Map base variable name to its type
  HashMap<String, String> variableTypeMap = new HashMap<>();

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

    if (currentVariableSubscriptMap.containsKey(variable))
      subscript = currentVariableSubscriptMap.get(variable) + 1;

    if (isDescendantOf(ctx, Java8Parser.IfThenStatementContext.class)
        || isDescendantOf(ctx, Java8Parser.WhileStatementContext.class))
      predicateBlockVariablesStack.lastElement().add(variable);
    rewriter.replace(ctx.getStart(), variable + "_" + subscript);
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
      rewriter.replace(varContext.getStart(), "");
      rewriter.replace(varContext.getStop(), "");
      rewriter.replace(ctx.getStart(), variable + "_" + subscript);
      currentVariableSubscriptMap.put(variable, 0);
      variableTypeMap.put(variable, type);
    }
  }

  // When entering any expression, change the variable to SSA form
  @Override
  public void enterExpressionName(Java8Parser.ExpressionNameContext ctx) {
    String varName = tokens.getText(ctx);
    int subscript = currentVariableSubscriptMap.get(varName);
    if (!isDescendantOf(ctx, Java8Parser.LeftHandSideContext.class))
      rewriter.replace(ctx.getStart(), varName + "_" + subscript);
  }

  // Upon exiting an assignment, increment the subscript counter
  @Override
  public void exitAssignment(Java8Parser.AssignmentContext ctx) {
    String variable = tokens.getText(ctx.leftHandSide());
    int subscript = 0;

    if (currentVariableSubscriptMap.containsKey(variable))
      subscript = currentVariableSubscriptMap.get(variable) + 1;
    currentVariableSubscriptMap.put(variable, subscript);
  }

  // Get the parameters from the method and add them to the maps
  @Override
  public void enterFormalParameter(Java8Parser.FormalParameterContext ctx) {
    String varName = tokens.getText(ctx.variableDeclaratorId());
    String varType = tokens.getText(ctx.unannType());
    variableTypeMap.put(varName, varType);
    currentVariableSubscriptMap.put(varName, 0);
  }

  // When entering the method, create the SSA form of the parameters to the body of the method
  // They will be inserted once the parser exits the method body
  String initializeFormalParams = "";

  @Override
  public void enterMethodBody(Java8Parser.MethodBodyContext ctx) {
    for (HashMap.Entry<String, String> entry : variableTypeMap.entrySet()) {
      int subscript = currentVariableSubscriptMap.get(entry.getKey());
      initializeFormalParams += "\n    " + entry.getKey() + "_" + subscript + " = " + entry.getKey() + ";";
    }
  }

  @Override
  public void exitMethodBody(Java8Parser.MethodBodyContext ctx) {
    rewriter.insertAfter(ctx.getStart(), "\n");
    for (HashMap.Entry<String, Integer> entry : currentVariableSubscriptMap.entrySet()) {
      rewriter.insertAfter(ctx.getStart(), "    ");
      String variableName = entry.getKey();
      int currentSubscript = entry.getValue();
      String type = variableTypeMap.get(variableName);
      for (int i = 0; i <= currentSubscript; i++) {
        String object = dataTypeToObjectMap.get(type);
        rewriter.insertAfter(ctx.getStart(), object + " " + variableName + "_" + i + " = null;");
      }
      rewriter.insertAfter(ctx.getStart(), "\n");
    }
    rewriter.insertAfter(ctx.getStart(), initializeFormalParams);
  }

  // Replace ++ with intitializaton of a new variable
  @Override
  public void exitPostIncrementExpression(Java8Parser.PostIncrementExpressionContext ctx) {
    String varName = tokens.getText(ctx.postfixExpression().expressionName());
    int subscript = currentVariableSubscriptMap.get(varName);
    rewriter.replace(ctx.getStart(), varName + "_" + (subscript + 1));
    rewriter.replace(ctx.getStop(), " = " + varName + "_" + subscript + " + 1");
    currentVariableSubscriptMap.put(varName, subscript + 1);

  }

  @Override
  public void enterIfThenStatement(Java8Parser.IfThenStatementContext ctx) {
    updateVariableSubscriptPredicateStack();
    predicateBlockVariablesStack.push(new HashSet<String>());
  }

  @Override
  public void exitIfThenStatement(Java8Parser.IfThenStatementContext ctx) {
    HashMap<String, Integer> varSubscriptsBeforePredicate = varSubscriptsBeforePredicateStack.pop();
    String type = "Integer";
    ParserRuleContext exprCtx = ctx.expression();

    // Get the SSA Form predicate to insert into Phi function
    String predicate = extractSSAFormPredicate(ctx.expression(), varSubscriptsBeforePredicate);

    // TODO: Type checking and changes for different data types
    String phiObject = "PhiIf<" + type + "> phi" + phiCounter + " = new PhiIf(" + predicate + ");";
    rewriter.insertBefore(ctx.getStart(), "\n    " + phiObject + "\n    ");

    rewriter.insertAfter(ctx.getStop(), "\n");
    for (String var : predicateBlockVariablesStack.lastElement()) {
      rewriter.insertAfter(ctx.getStop(), "    ");
      int subscript = currentVariableSubscriptMap.get(var);
      int prePredicateSubscript = varSubscriptsBeforePredicate.get(var);

      String beforePredicateVariable = var + "_" + prePredicateSubscript;
      String predicateVariable = var + "_" + subscript;
      rewriter.insertAfter(ctx.getStop(), var + "_" + (subscript + 1) + " = phi" + phiCounter + ".merge("
          + predicateVariable + "," + beforePredicateVariable + ");\n");
      currentVariableSubscriptMap.put(var, subscript + 1);
    }
    rewriter.replace(exprCtx.getStart(), exprCtx.getStop(), "phi" + phiCounter + ".getPredVal()");
    phiCounter++;
    predicateBlockVariablesStack.pop();
  }

  @Override
  public void enterWhileStatement(Java8Parser.WhileStatementContext ctx) {
    updateVariableSubscriptPredicateStack();
    predicateBlockVariablesStack.push(new HashSet<String>());
  }

  @Override
  public void exitWhileStatement(Java8Parser.WhileStatementContext ctx) {
    HashMap<String, Integer> varSubscriptsBeforePredicate = varSubscriptsBeforePredicateStack.pop();
    String type = "Integer";
    ParserRuleContext exprCtx = ctx.expression();

    // Get the SSA Form predicate to insert into Phi function
    String predicate = extractSSAFormPredicate(ctx.expression(), varSubscriptsBeforePredicate);
    String phiObject = "PhiIf<" + type + "> phi" + phiCounter + " = new PhiWhile(" + predicate + ");";
    rewriter.insertBefore(ctx.getStart(), "\n    " + phiObject + "\n    ");

    rewriter.replace(exprCtx.getStart(), exprCtx.getStop(), "phi" + phiCounter + ".getPredVal()");

    String updatePredicate = extractSSAFormUpdatePredicate(ctx.expression());
    rewriter.insertBefore(ctx.getStop(), "  phi" + phiCounter + ".evalPred(" + updatePredicate + ");\n    ");
    // TODO: phi.entry()
    // TODO: phi.exit()

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
      } else
        predicate += tokenText;
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
      } else
        predicate += tokenText;
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
    for (int i = 0; i < numChildren; i++)
      if (ctx.getChild(i) instanceof TerminalNode)
        terminalNodes.add((TerminalNode) ctx.getChild(i));
      else if (ctx.getChild(i) instanceof ParserRuleContext)
        terminalNodes.addAll(getAllTokensFromContext((ParserRuleContext) ctx.getChild(i)));

    return terminalNodes;
  }

  // Checks whether a given context is the descendant of another given context
  public boolean isDescendantOf(ParserRuleContext ctx, Class cls) {
    while (ctx.getParent() != null) {
      if (cls.isInstance(ctx.getParent()))
        return true;
      ctx = ctx.getParent();
    }
    return false;
  }
}
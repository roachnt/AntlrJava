import java.util.HashMap;
import java.util.ArrayList;

import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.TokenStreamRewriter;

import org.antlr.v4.runtime.ParserRuleContext;

public class Converter extends Java8BaseListener {
  Java8Parser parser;

  // Rewriting mechanism
  TokenStreamRewriter rewriter;

  // Tokens from the program
  TokenStream tokens;

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

    rewriter.replace(ctx.getStart(), variableTypeMap.get(variable) + " " + variable + "_" + subscript);
  }

  // Handling initializing variables in a method
  @Override
  public void enterLocalVariableDeclaration(Java8Parser.LocalVariableDeclarationContext ctx) {
    String type = tokens.getText(ctx.unannType());
    System.out.println(type);
    for (int i = 0; i < ctx.variableDeclaratorList().variableDeclarator().size(); i++) {
      int subscript = 0;
      Java8Parser.VariableDeclaratorIdContext varContext = ctx.variableDeclaratorList().variableDeclarator(0)
          .variableDeclaratorId();
      String variable = tokens.getText(varContext);
      rewriter.replace(varContext.getStart(), variable + "_" + subscript);
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

  // When entering the method, add the SSA form of the parameters to the body of the method
  @Override
  public void enterMethodBody(Java8Parser.MethodBodyContext ctx) {
    String initializeFormalParams = "";
    for (HashMap.Entry<String, String> entry : variableTypeMap.entrySet()) {
      int subscript = currentVariableSubscriptMap.get(entry.getKey());
      initializeFormalParams += "\n    " + entry.getValue() + " " + entry.getKey() + "_" + subscript + " = "
          + entry.getKey() + ";";
    }
    rewriter.insertAfter(ctx.getStart(), initializeFormalParams);
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
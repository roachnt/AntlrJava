import java.util.HashMap;
import java.util.ArrayList;

import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.TokenStreamRewriter;
import org.antlr.v4.runtime.ParserRuleContext;

public class Converter extends Java8BaseListener {
  Java8Parser parser;
  TokenStreamRewriter rewriter;
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
  public void enterAssignment(Java8Parser.AssignmentContext ctx) {
    String variable = tokens.getText(ctx.leftHandSide());

    int subscript;
    if (currentVariableSubscriptMap.containsKey(variable)) {
      int currentSubscript = currentVariableSubscriptMap.get(variable);
      subscript = currentSubscript + 1;
    } else {
      subscript = 0;
    }
    currentVariableSubscriptMap.put(variable, subscript);
    rewriter.replace(ctx.leftHandSide().getStart(), variableTypeMap.get(variable) + " " + variable + "_" + subscript);
  }

  @Override
  public void enterExpressionName(Java8Parser.ExpressionNameContext ctx) {
    String varName = tokens.getText(ctx);
    int subscript = currentVariableSubscriptMap.get(varName) - 1;
    System.out.println(isDescendantOf(ctx, Java8Parser.AssignmentContext.class));
    if (!isDescendantOf(ctx, Java8Parser.LeftHandSideContext.class)
        && isDescendantOf(ctx, Java8Parser.AssignmentContext.class))
      rewriter.replace(ctx.getStart(), varName + "_" + subscript);
  }

  @Override
  public void enterFormalParameter(Java8Parser.FormalParameterContext ctx) {
    String varName = tokens.getText(ctx.variableDeclaratorId());
    String varType = tokens.getText(ctx.unannType());
    variableTypeMap.put(varName, varType);
    currentVariableSubscriptMap.put(varName, 0);
  }

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

  public ArrayList getAllAncestors(ParserRuleContext ctx) {
    ArrayList<Object> ancestors = new ArrayList<>();
    while (ctx.getParent() != null) {
      ancestors.add(ctx.getParent().getClass());
      ctx = ctx.getParent();
    }
    return ancestors;
  }

  public boolean isDescendantOf(ParserRuleContext ctx, Class cls) {
    // TODO: Figure out how to type check ancestor context against ancestor input var
    while (ctx.getParent() != null) {
      if (cls.isInstance(ctx.getParent()))
        return true;
      ctx = ctx.getParent();
    }
    return false;
  }
}
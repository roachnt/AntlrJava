import java.util.HashMap;
import java.util.ArrayList;

import org.antlr.v4.runtime.TokenStream;

public class Converter extends Java8BaseListener {
  Java8Parser parser;

  // Map from SSA-form variable to an array of the variables in its assignment
  HashMap<String, ArrayList<String>> variableConfoundersMap = new HashMap<>();

  // Keep track of current subcript of a variable when converting to SSA
  HashMap<String, Integer> currentVariableSubscriptMap = new HashMap<>();

  public Converter(Java8Parser parser) {
    this.parser = parser;
  }

  public void enterAssignment(Java8Parser.AssignmentContext ctx) {
    TokenStream tokens = parser.getTokenStream();
    String variable = tokens.getText(ctx.leftHandSide());
    if (currentVariableSubscriptMap.containsKey(variable)) {
      int currentSubscript = currentVariableSubscriptMap.get(variable);
      currentVariableSubscriptMap.put(variable, currentSubscript + 1);
    } else {
      currentVariableSubscriptMap.put(variable, 0);
    }
  }
}
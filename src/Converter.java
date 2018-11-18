import java.util.HashMap;
import java.util.ArrayList;

import org.antlr.v4.runtime.TokenStream;

public class Converter extends Java8BaseListener {
  Java8Parser parser;

  // Map from SSA-form variable to an array of the variables in its assignment
  HashMap<String, ArrayList<String>> variableConfoundersMap;

  // Keep track of current subcript of a variable when converting to SSA
  HashMap<String, Integer> currentVariableSubscript;

  public Converter(Java8Parser parser) {
    this.parser = parser;
  }

  public void enterClassDeclaration(Java8Parser.ClassDeclarationContext ctx) {
    TokenStream tokens = parser.getTokenStream();
    System.out.println("Entered Assignment Statment!");
    System.out.println(ctx.normalClassDeclaration().Identifier());
  }

  public void enterAssignment(Java8Parser.AssignmentContext ctx) {
    TokenStream tokens = parser.getTokenStream();
    System.out.println("Entered Assignment Statment!");
    System.out.println(tokens.getText(ctx.expression()));
  }
}
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
import org.apache.commons.lang3.tuple.MutablePair;
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
  HashMap<String, Integer> variableSubscripts = new HashMap<>();

  Stack<HashSet<String>> variablesDeclaredInPredicateStack = new Stack<>();

  HashSet<String> allLocalVariables = new HashSet<>();

  HashMap<String, ArrayList<String>> causalMap = new HashMap<>();

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
  public void enterPackageName(Java8Parser.PackageNameContext ctx) {
    packageName = ctx.getText();
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
    if (ctx.expressionName() == null)
      return;
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
      if (varContext.dims() != null)
        continue;

      String variable = tokens.getText(varContext);

      if (!variableSubscripts.containsKey(variable))
        variableSubscripts.put(variable, -1);
      // System.out.println(variableSubscripts);
      int lineNumber = ctx.getStart().getLine();
      if (insidePredicateBlock(ctx)) {
        variablesDeclaredInPredicateStack.lastElement().add(variable);
      }

      if (!isDescendantOf(ctx, Java8Parser.ForInitContext.class)
          && ctx.variableDeclaratorList().variableDeclarator(i).variableInitializer() != null) {
        ArrayList<String> expressionNames = getAllExpressionNamesFromPredicate(
            ctx.variableDeclaratorList().variableDeclarator(i).variableInitializer());
        ArrayList<String> expressionNamesSSA = new ArrayList<>();
        for (String expressionName : expressionNames) {
          if (variableSubscripts.containsKey(expressionName))
            expressionNamesSSA.add(expressionName + "_" + variableSubscripts.get(expressionName));
        }

        currentVariableSubscriptMap.put(variable, 0);

        insertVersionUpdateAfter(ctx.getParent().getParent().getStop(), variable);
        insertRecordStatementAfter(ctx.getParent().getStop(), variable, lineNumber);

        causalMap.put(variable + "_" + variableSubscripts.get(variable), new ArrayList<String>());
        causalMap.get(variable + "_" + variableSubscripts.get(variable)).addAll(expressionNamesSSA);

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
    if (ctx.leftHandSide().expressionName() == null)
      return;

    ArrayList<String> expressionNames = getAllExpressionNamesFromPredicate(ctx.expression());
    ArrayList<String> expressionNamesSSA = new ArrayList<>();
    for (String expressionName : expressionNames) {
      if (!variableSubscripts.keySet().isEmpty())
        if (variableSubscripts.containsKey(expressionName))
          expressionNamesSSA.add(expressionName + "_" + variableSubscripts.get(expressionName));
    }

    if (currentVariableSubscriptMap.containsKey(variable)) {
      subscript = currentVariableSubscriptMap.get(variable) + 1;
    }
    currentVariableSubscriptMap.put(variable, subscript);
    int lineNumber = ctx.getStart().getLine();

    if (!isDescendantOf(ctx, Java8Parser.ForInitContext.class)
        && !isDescendantOf(ctx, Java8Parser.ForUpdateContext.class)) {
      ParserRuleContext currentContext = ctx;

      while (currentContext.getParent() != null) {
        if (currentContext instanceof Java8Parser.ExpressionStatementContext) {
          insertVersionUpdateAfter(currentContext.getStop(), variable);
          insertRecordStatementAfter(currentContext.getStop(), variable, lineNumber);
          break;
        }
        currentContext = currentContext.getParent();
      }
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

    if (!variableSubscripts.isEmpty()) {
      causalMap.put(variable + "_" + variableSubscripts.get(variable), new ArrayList<String>());
      causalMap.get(variable + "_" + variableSubscripts.get(variable)).addAll(expressionNamesSSA);
    }
  }

  ArrayList<String> methodParameters = new ArrayList<>();

  // Get the parameters from the method and add them to the maps
  @Override
  public void enterFormalParameter(Java8Parser.FormalParameterContext ctx) {
    if (ctx.variableDeclaratorId().dims() != null)
      return;
    String varName = tokens.getText(ctx.variableDeclaratorId());
    String varType = tokens.getText(ctx.unannType());
    methodParameters.add(varName);
    variableTypeMap.put(varName, varType);
    currentVariableSubscriptMap.put(varName, 0);
    if (!variableSubscripts.containsKey(varName))
      variableSubscripts.put(varName, 0);
  }

  // When entering the method, create the SSA form of the parameters to the body of the method
  // They will be inserted once the parser exits the method body
  String initializeFormalParams = "";

  @Override
  public void enterMethodDeclarator(Java8Parser.MethodDeclaratorContext ctx) {
    String methodName = ctx.getChild(0).getText();
    System.out.println(methodName);
    currentMethodName = methodName;
  }

  @Override
  public void enterNormalClassDeclaration(Java8Parser.NormalClassDeclarationContext ctx) {
    String className = ctx.getChild(2).getText();
    this.className = className;
    rewriter.insertBefore(ctx.getStart(),
        "import java.io.BufferedWriter;import java.io.FileWriter;import java.io.IOException;");
  }

  @Override
  public void enterClassBody(Java8Parser.ClassBodyContext ctx) {
    rewriter.insertAfter(ctx.getStart(),
        "public static void record(String packageName, String clazz, String method, int line, int staticScope,String variableName, Object value, int version) {BufferedWriter writer = null;try {writer = new BufferedWriter(new FileWriter(clazz + \"_output.txt\", true));} catch (IOException e) {System.out.println(e.getMessage());}try {writer.append(packageName + \",\" + clazz + \",\" + method + \",\" + line + \",\" + staticScope + \",\" + variableName + \",\"+ version + \",\" + value + \"\\n\");writer.close();} catch (Exception e) {System.out.println(e.getMessage());}}");
  }

  HashMap<String, Integer> subscriptsBeforeMethod = new HashMap<>();

  @Override
  public void enterMethodBody(Java8Parser.MethodBodyContext ctx) {
    subscriptsBeforeMethod.putAll(variableSubscripts);
    for (String variable : methodParameters) {
      int subscript = currentVariableSubscriptMap.get(variable);
      if (variableSubscripts.containsKey(variable))
        initializeFormalParams += "int " + variable + "_version" + " = " + variableSubscripts.get(variable) + ";";
      else
        initializeFormalParams += "int " + variable + "_version" + " = 0" + ";";
    }
  }

  // When exiting a method, initilize all variables (both from parameters and in body)
  @Override
  public void exitMethodBody(Java8Parser.MethodBodyContext ctx) {
    System.out.println(subscriptsBeforeMethod);
    System.out.println(variableSubscripts);
    for (String variable : allLocalVariables) {
      if (!methodParameters.contains(variable)) {
        if (subscriptsBeforeMethod.containsKey(variable))
          rewriter.insertAfter(ctx.getStart(),
              "int " + variable + "_version" + " = " + subscriptsBeforeMethod.get(variable) + ";");
        else
          rewriter.insertAfter(ctx.getStart(), "int " + variable + "_version" + " = -1;");
      }
    }
    rewriter.insertAfter(ctx.getStart(), initializeFormalParams);
    currentMethodName = "";
    initializeFormalParams = "";
    allLocalVariables.clear();
    methodParameters.clear();
  }

  @Override
  public void exitMethodDeclaration(Java8Parser.MethodDeclarationContext ctx) {
    currentVariableSubscriptMap.clear();
  }

  // Replace ++ with intitializaton of a new variable
  @Override
  public void exitPostIncrementExpression(Java8Parser.PostIncrementExpressionContext ctx) {

    if (isDescendantOf(ctx, Java8Parser.ForUpdateContext.class))
      return;

    String varName = tokens.getText(ctx.postfixExpression().expressionName());

    int subscript = currentVariableSubscriptMap.get(varName);
    currentVariableSubscriptMap.put(varName, subscript + 1);

    ParserRuleContext currentContext = ctx;

    ArrayList<String> confounders = new ArrayList<>();
    if (variableSubscripts.containsKey(varName))
      confounders.add(varName + "_" + variableSubscripts.get(varName));

    while (currentContext.getParent() != null) {
      if (currentContext instanceof Java8Parser.ExpressionStatementContext) {
        insertVersionUpdateAfter(currentContext.getStop(), varName);
        insertRecordStatementAfter(currentContext.getStop(), varName, currentContext.getStart().getLine());
        break;
      }
      currentContext = currentContext.getParent();
    }
    causalMap.put(varName + "_" + variableSubscripts.get(varName), new ArrayList<String>());
    causalMap.get(varName + "_" + variableSubscripts.get(varName)).addAll(confounders);
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

    ArrayList<String> confounders = new ArrayList<>();
    if (variableSubscripts.containsKey(varName))
      confounders.add(varName + "_" + variableSubscripts.get(varName));
    while (currentContext.getParent() != null) {
      if (currentContext instanceof Java8Parser.ExpressionStatementContext) {
        insertVersionUpdateAfter(currentContext.getStop(), varName);
        insertRecordStatementAfter(currentContext.getStop(), varName, currentContext.getStart().getLine());
        break;
      }
      currentContext = currentContext.getParent();
    }
    causalMap.put(varName + "_" + variableSubscripts.get(varName), new ArrayList<String>());
    causalMap.get(varName + "_" + variableSubscripts.get(varName)).addAll(confounders);
  }

  @Override
  public void enterIfThenStatement(Java8Parser.IfThenStatementContext ctx) {
    updateVariableSubscriptPredicateStack();
    variablesDeclaredInPredicateStack.push(new HashSet<String>());
    predicateBlockVariablesStack.push(new HashSet<String>());
    phiSubscriptQueue.addLast(phiCounter);
    phiCounter++;
  }

  @Override
  public void exitIfThenStatement(Java8Parser.IfThenStatementContext ctx) {
    HashMap<String, Integer> varSubscriptsBeforePredicate = varSubscriptsBeforePredicateStack.pop();
    variablesDeclaredInPredicateStack.pop();
    String type = "Integer";
    ParserRuleContext exprCtx = ctx.expression();
    int phiSubscript = phiSubscriptQueue.removeFirst();

    // Get the SSA Form predicate to insert into Phi function
    // String predicate = extractSSAFormPredicate(ctx.expression(), varSubscriptsBeforePredicate);

    rewriter.insertBefore(ctx.statement().getStart(), "{");
    rewriter.insertAfter(ctx.statement().getStop(), "}");
    for (String var : predicateBlockVariablesStack.pop()) {
      if (!variableSubscripts.containsKey(var))
        continue;
      int subscript = currentVariableSubscriptMap.get(var);
      ArrayList<String> confounders = new ArrayList<>();
      confounders.add(var + "_" + varSubscriptsBeforePredicate.get(var));
      confounders.add(var + "_" + variableSubscripts.get(var));
      currentVariableSubscriptMap.put(var, subscript + 1);

      insertVersionUpdateAfter(ctx.getStop(), var);
      insertRecordStatementAfter(ctx.getStop(), var, ctx.getStop().getLine());
      causalMap.put(var + "_" + variableSubscripts.get(var), new ArrayList<String>());
      causalMap.get(var + "_" + variableSubscripts.get(var)).addAll(confounders);
    }

  }

  Stack<MutablePair<HashMap<String, Integer>, HashMap<String, Integer>>> ifThenElseBranchStack = new Stack<>();

  @Override
  public void enterIfThenElseStatement(Java8Parser.IfThenElseStatementContext ctx) {
    updateVariableSubscriptPredicateStack();
    predicateBlockVariablesStack.push(new HashSet<String>());
    variablesDeclaredInPredicateStack.push(new HashSet<String>());
    ifThenElseBranchStack.push(new MutablePair<HashMap<String, Integer>, HashMap<String, Integer>>());
    phiSubscriptQueue.addLast(phiCounter);
    phiCounter++;
  }

  @Override
  public void exitIfThenElseStatement(Java8Parser.IfThenElseStatementContext ctx) {
    variablesDeclaredInPredicateStack.pop();
    // System.out.println(ctx.getStart().getLine());
    HashMap<String, Integer> varSubscriptsBeforePredicate = varSubscriptsBeforePredicateStack.pop();
    String type = "Integer";
    ParserRuleContext exprCtx = ctx.expression();
    int phiSubscript = phiSubscriptQueue.removeFirst();
    MutablePair<HashMap<String, Integer>, HashMap<String, Integer>> ifThenElseBranchedVariables = ifThenElseBranchStack
        .pop();
    HashMap<String, Integer> ifBranchVariableSubscripts = ifThenElseBranchedVariables.left;
    HashMap<String, Integer> elseBranchVariableSubscripts = ifThenElseBranchedVariables.right;

    // Get the SSA Form predicate to insert into Phi function
    // String predicate = extractSSAFormPredicate(ctx.expression(), varSubscriptsBeforePredicate);
    // ArrayList<Java8Parser.AssignmentContext> ifBranchAssignments = getAllAssignmentContexts(ctx.statementNoShortIf());
    // ArrayList<Java8Parser.AssignmentContext> elseBranchAssignments = getAllAssignmentContexts(ctx.statement());

    rewriter.insertBefore(ctx.statementNoShortIf().getStart(), "{");
    rewriter.insertAfter(ctx.statementNoShortIf().getStop(), "}");
    HashSet<String> ifBlockVariables = getAllAlteredVariablesFromContext(ctx.statementNoShortIf());

    rewriter.insertBefore(ctx.statement().getStart(), "{");
    rewriter.insertAfter(ctx.statement().getStop(), "}");
    HashSet<String> elseBlockVariables = getAllAlteredVariablesFromContext(ctx.statement());

    HashSet<String> predicateBlockVariables = predicateBlockVariablesStack.pop();
    for (String var : predicateBlockVariables) {
      int subscript = currentVariableSubscriptMap.get(var);
      currentVariableSubscriptMap.put(var, subscript + 1);

      insertVersionUpdateAfter(ctx.getStop(), var);
      insertRecordStatementAfter(ctx.getStop(), var, ctx.getStop().getLine());
    }

    HashSet<String> allAddedVariables = new HashSet<>();
    for (String var : ifBlockVariables) {
      if (!variableSubscripts.containsKey(var))
        continue;
      causalMap.put(var + "_" + variableSubscripts.get(var), new ArrayList<String>());

      causalMap.get(var + "_" + variableSubscripts.get(var)).add(var + "_" + ifBranchVariableSubscripts.get(var));
      allAddedVariables.add(var);
    }

    for (String var : elseBlockVariables) {
      if (!variableSubscripts.containsKey(var))
        continue;
      if (!causalMap.containsKey(var + "_" + variableSubscripts.get(var)))
        causalMap.put(var + "_" + variableSubscripts.get(var), new ArrayList<String>());

      causalMap.get(var + "_" + variableSubscripts.get(var)).add(var + "_" + elseBranchVariableSubscripts.get(var));
      allAddedVariables.add(var);
    }

    for (String var : allAddedVariables) {
      if (!variableSubscripts.containsKey(var))
        continue;
      if (causalMap.get(var + "_" + variableSubscripts.get(var)).size() == 1) {
        causalMap.get(var + "_" + variableSubscripts.get(var)).add(var + "_" + varSubscriptsBeforePredicate.get(var));
      }
    }

  }

  @Override
  public void enterIfThenElseStatementNoShortIf(Java8Parser.IfThenElseStatementNoShortIfContext ctx) {
    variablesDeclaredInPredicateStack.push(new HashSet<String>());
    // System.out.println(ctx.getStart().getLine());
    updateVariableSubscriptPredicateStack();
    predicateBlockVariablesStack.push(new HashSet<String>());
    ifThenElseBranchStack.push(new MutablePair<HashMap<String, Integer>, HashMap<String, Integer>>());
    phiSubscriptQueue.addLast(phiCounter);
    phiCounter++;
  }

  @Override
  public void exitIfThenElseStatementNoShortIf(Java8Parser.IfThenElseStatementNoShortIfContext ctx) {
    variablesDeclaredInPredicateStack.pop();
    HashMap<String, Integer> varSubscriptsBeforePredicate = varSubscriptsBeforePredicateStack.pop();
    String type = "Integer";
    ParserRuleContext exprCtx = ctx.expression();
    int phiSubscript = phiSubscriptQueue.removeFirst();
    MutablePair<HashMap<String, Integer>, HashMap<String, Integer>> ifThenElseBranchedVariables = ifThenElseBranchStack
        .pop();
    HashMap<String, Integer> ifBranchVariableSubscripts = ifThenElseBranchedVariables.left;
    HashMap<String, Integer> elseBranchVariableSubscripts = ifThenElseBranchedVariables.right;

    for (Java8Parser.StatementNoShortIfContext statementNoShortIfContext : ctx.statementNoShortIf()) {
      rewriter.insertBefore(statementNoShortIfContext.getStart(), "{");
      rewriter.insertAfter(statementNoShortIfContext.getStop(), "}");
    }
    HashSet<String> ifBlockVariables = getAllAlteredVariablesFromContext(ctx.statementNoShortIf().get(0));
    HashSet<String> elseBlockVariables = getAllAlteredVariablesFromContext(ctx.statementNoShortIf().get(1));

    for (String var : predicateBlockVariablesStack.pop()) {
      int subscript = currentVariableSubscriptMap.get(var);
      currentVariableSubscriptMap.put(var, subscript + 1);

      insertVersionUpdateAfter(ctx.getStop(), var);
      insertRecordStatementAfter(ctx.getStop(), var, ctx.getStop().getLine());
    }

    HashSet<String> allAddedVariables = new HashSet<>();
    for (String var : ifBlockVariables) {
      if (!variableSubscripts.containsKey(var))
        continue;
      causalMap.put(var + "_" + variableSubscripts.get(var), new ArrayList<String>());

      causalMap.get(var + "_" + variableSubscripts.get(var)).add(var + "_" + ifBranchVariableSubscripts.get(var));
      allAddedVariables.add(var);
    }

    for (String var : elseBlockVariables) {
      if (!variableSubscripts.containsKey(var))
        continue;
      if (!causalMap.containsKey(var + "_" + variableSubscripts.get(var)))
        causalMap.put(var + "_" + variableSubscripts.get(var), new ArrayList<String>());

      causalMap.get(var + "_" + variableSubscripts.get(var)).add(var + "_" + elseBranchVariableSubscripts.get(var));
      allAddedVariables.add(var);
    }

    for (String var : allAddedVariables) {
      if (!variableSubscripts.containsKey(var))
        continue;
      if (causalMap.get(var + "_" + variableSubscripts.get(var)).size() == 1) {
        causalMap.get(var + "_" + variableSubscripts.get(var)).add(var + "_" + varSubscriptsBeforePredicate.get(var));
      }
    }

  }

  @Override
  public void exitStatementNoShortIf(Java8Parser.StatementNoShortIfContext ctx) {
    ParserRuleContext parentContext = ctx.getParent();
    if (!(parentContext instanceof Java8Parser.IfThenElseStatementContext)
        && !(parentContext instanceof Java8Parser.IfThenElseStatementNoShortIfContext))
      return;

    HashMap<String, Integer> currentVariableSubscripts = variableSubscripts;

    // Check if it is if branch or else branch
    // 1. Figure out which child of parent context it is
    for (int i = 0; i < parentContext.getChildCount(); i++) {
      if (parentContext.getChild(i) instanceof Java8Parser.StatementNoShortIfContext) {
        Java8Parser.StatementNoShortIfContext childCtx = (Java8Parser.StatementNoShortIfContext) parentContext
            .getChild(i);
        // If the child is the statement context
        if (childCtx.equals(ctx)) {
          // if block condition
          if (parentContext.getChild(i - 1).getText().equals(")")) {
            ifThenElseBranchStack.lastElement().setLeft(new HashMap<>(currentVariableSubscripts));
          } else if (parentContext.getChild(i - 1).getText().equals("else")) {
            ifThenElseBranchStack.lastElement().setRight(new HashMap<>(currentVariableSubscripts));
          }
        }
      }
    }
  }

  @Override
  public void exitStatement(Java8Parser.StatementContext ctx) {
    ParserRuleContext parentContext = ctx.getParent();
    if (!(parentContext instanceof Java8Parser.IfThenElseStatementContext)
        && !(parentContext instanceof Java8Parser.IfThenElseStatementNoShortIfContext))
      return;
    HashMap<String, Integer> currentVariableSubscripts = variableSubscripts;
    ifThenElseBranchStack.lastElement().setRight(new HashMap<>(currentVariableSubscripts));
  }

  @Override
  public void enterWhileStatement(Java8Parser.WhileStatementContext ctx) {
    updateVariableSubscriptPredicateStack();
    whileLoopVariableStack.push(new HashSet<String>());
    deferredPhiIfDeclarations.add(new HashMap<Integer, Java8Parser.IfThenStatementContext>());
    deferredPhiIfMerges.add(new HashMap<Java8Parser.IfThenStatementContext, ArrayList<String>>());

    variablesDeclaredInPredicateStack.push(new HashSet<String>());
    predicateBlockVariablesStack.push(new HashSet<String>());
    phiSubscriptQueue.addLast(phiCounter);
    phiCounter++;
  }

  @Override
  public void exitWhileStatement(Java8Parser.WhileStatementContext ctx) {
    HashMap<String, Integer> varSubscriptsBeforePredicate = varSubscriptsBeforePredicateStack.pop();
    HashSet<String> variablesDeclaredInPredicate = variablesDeclaredInPredicateStack.pop();

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
      if (variablesDeclaredInPredicate.contains(variable) || !variableSubscripts.containsKey(variable))
        continue;

      ArrayList<String> confounders = new ArrayList<>();
      confounders.add(variable + "_" + varSubscriptsBeforePredicate.get(variable));
      confounders.add(variable + "_" + variableSubscripts.get(variable));

      insertVersionUpdateAfter(ctx.getStop(), variable);
      insertRecordStatementAfter(ctx.getStop(), variable, ctx.getStop().getLine());

      causalMap.put(variable + "_" + variableSubscripts.get(variable), new ArrayList<String>());
      causalMap.get(variable + "_" + variableSubscripts.get(variable)).addAll(confounders);
    }

  }

  @Override
  public void enterWhileStatementNoShortIf(Java8Parser.WhileStatementNoShortIfContext ctx) {
    updateVariableSubscriptPredicateStack();
    whileLoopVariableStack.push(new HashSet<String>());
    deferredPhiIfDeclarations.add(new HashMap<Integer, Java8Parser.IfThenStatementContext>());
    deferredPhiIfMerges.add(new HashMap<Java8Parser.IfThenStatementContext, ArrayList<String>>());

    predicateBlockVariablesStack.push(new HashSet<String>());
    variablesDeclaredInPredicateStack.push(new HashSet<String>());
    phiSubscriptQueue.addLast(phiCounter);
    phiCounter++;
  }

  @Override
  public void exitWhileStatementNoShortIf(Java8Parser.WhileStatementNoShortIfContext ctx) {
    HashMap<String, Integer> varSubscriptsBeforePredicate = varSubscriptsBeforePredicateStack.pop();
    HashSet<String> variablesDeclaredInPredicate = variablesDeclaredInPredicateStack.pop();

    // Get all variables from predicate
    ArrayList<String> expressionNamesList = getAllExpressionNamesFromPredicate(ctx.expression());

    // Replace predicate with true
    rewriter.replace(ctx.expression().getStart(), ctx.expression().getStop(), "true");

    // Get block context of while loop
    Java8Parser.BlockContext blockContext = ctx.statementNoShortIf().statementWithoutTrailingSubstatement().block();

    if (blockContext == null) {
      // Handling a single-statement while loop by wrapping braces around it

      // Insert breaking statement equivalent to negation of original predicate
      rewriter.insertBefore(ctx.statementNoShortIf().statementWithoutTrailingSubstatement().getStart(),
          "if (!(" + ctx.expression().getText() + ")) {break;}");

      // Record predicate variable values
      for (String variable : expressionNamesList) {
        insertRecordStatementBefore(ctx.statementNoShortIf().statementWithoutTrailingSubstatement().getStart(),
            variable, ctx.expression().getStart().getLine());
        insertVersionUpdateBefore(ctx.statementNoShortIf().statementWithoutTrailingSubstatement().getStart(), variable);
      }

      // Wrap braces around the statement
      rewriter.insertBefore(ctx.statementNoShortIf().statementWithoutTrailingSubstatement().getStart(), "{");
      rewriter.insertAfter(ctx.statementNoShortIf().statementWithoutTrailingSubstatement().getStop(), "}");
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
      if (variablesDeclaredInPredicate.contains(variable) || !variableSubscripts.containsKey(variable))
        continue;

      ArrayList<String> confounders = new ArrayList<>();
      confounders.add(variable + "_" + varSubscriptsBeforePredicate.get(variable));
      confounders.add(variable + "_" + variableSubscripts.get(variable));

      insertVersionUpdateAfter(ctx.getStop(), variable);
      insertRecordStatementAfter(ctx.getStop(), variable, ctx.getStop().getLine());

      causalMap.put(variable + "_" + variableSubscripts.get(variable), new ArrayList<String>());
      causalMap.get(variable + "_" + variableSubscripts.get(variable)).addAll(confounders);
    }

  }

  @Override
  public void enterBasicForStatement(Java8Parser.BasicForStatementContext ctx) {
    updateVariableSubscriptPredicateStack();
    predicateBlockVariablesStack.push(new HashSet<String>());
    variablesDeclaredInPredicateStack.push(new HashSet<String>());
    if (ctx.forInit() != null) {
      handleBasicForInit(ctx.forInit(), ctx);
    }

    if (ctx.expression() != null) {
      handleBasicForExpression(ctx);
    }
  }

  @Override
  public void exitBasicForStatement(Java8Parser.BasicForStatementContext ctx) {
    HashMap<String, Integer> varSubscriptsBeforePredicate = varSubscriptsBeforePredicateStack.pop();
    HashSet<String> variablesDeclaredInPredicate = variablesDeclaredInPredicateStack.pop();
    HashSet<String> forInitDeclarations = getAllForInitDeclarations(ctx.forInit());

    if (ctx.forUpdate() != null) {
      handleBasicForUpdate(ctx);
    }

    if (ctx.statement().statementWithoutTrailingSubstatement().block() == null) {
      rewriter.insertBefore(ctx.statement().getStart(), "{");
      rewriter.insertAfter(ctx.statement().getStop(), "}");
    } else {
      rewriter.insertAfter(ctx.statement().getStart(), "if (!(" + ctx.expression().getText() + ")) {break;}");
    }
    rewriter.insertBefore(ctx.getStart(), "{");
    rewriter.insertAfter(ctx.getStop(), "}");
    // System.out.println(ctx.getStart().getLine());
    // System.out.println("predicateBlockVariablesStack: " + predicateBlockVariablesStack);
    for (String variable : forInitDeclarations) {
      for (HashSet<String> predicateBlockVariables : predicateBlockVariablesStack) {
        if (predicateBlockVariables.contains(variable))
          predicateBlockVariables.remove(variable);
      }
    }

    for (String variable : predicateBlockVariablesStack.pop()) {
      if (!(forInitDeclarations.contains(variable))) {
        if (variablesDeclaredInPredicate.contains(variable) || !variableSubscripts.containsKey(variable))
          continue;

        ArrayList<String> confounders = new ArrayList<>();
        int lineNumber = ctx.getStop().getLine();
        confounders.add(variable + "_" + varSubscriptsBeforePredicate.get(variable));
        confounders.add(variable + "_" + variableSubscripts.get(variable));

        insertVersionUpdateAfter(ctx.getStop(), variable);
        insertRecordStatementAfter(ctx.getStop(), variable, lineNumber);

        causalMap.put(variable + "_" + variableSubscripts.get(variable), new ArrayList<String>());
        causalMap.get(variable + "_" + variableSubscripts.get(variable)).addAll(confounders);
      }
    }
  }

  public HashSet<String> getAllForInitDeclarations(Java8Parser.ForInitContext ctx) {
    if (ctx == null)
      return new HashSet<String>();
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
    updateVariableSubscriptPredicateStack();
    predicateBlockVariablesStack.push(new HashSet<String>());
    variablesDeclaredInPredicateStack.push(new HashSet<String>());
    if (ctx.forInit() != null) {
      handleBasicForInit(ctx.forInit(), ctx);
    }

    if (ctx.expression() != null) {
      handleBasicForNoShortIfExpression(ctx);
    }
  }

  public void exitBasicForStatementNoShortIf(Java8Parser.BasicForStatementNoShortIfContext ctx) {
    HashMap<String, Integer> varSubscriptsBeforePredicate = varSubscriptsBeforePredicateStack.pop();
    HashSet<String> variablesDeclaredInPredicate = variablesDeclaredInPredicateStack.pop();

    if (ctx.forUpdate() != null) {
      handleBasicForNoShortIfUpdate(ctx);
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
        if (variablesDeclaredInPredicate.contains(variable) || !variableSubscripts.containsKey(variable))
          continue;
        ArrayList<String> confounders = new ArrayList<>();
        confounders.add(variable + "_" + varSubscriptsBeforePredicate.get(variable));
        confounders.add(variable + "_" + variableSubscripts.get(variable));

        insertVersionUpdateAfter(ctx.getStop(), variable);
        insertRecordStatementAfter(ctx.getStop(), variable, ctx.getStop().getLine());

        causalMap.put(variable + "_" + variableSubscripts.get(variable), new ArrayList<String>());
        causalMap.get(variable + "_" + variableSubscripts.get(variable)).addAll(confounders);

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
      String initializer = "";
      for (int i = 0; i < ctx.localVariableDeclaration().variableDeclaratorList().variableDeclarator().size(); i++) {
        Java8Parser.VariableDeclaratorIdContext varContext = ctx.localVariableDeclaration().variableDeclaratorList()
            .variableDeclarator(i).variableDeclaratorId();
        String variable = tokens.getText(varContext);
        int lineNumber = forCtx.getStart().getLine();
        currentVariableSubscriptMap.put(variable, 0);
        if (variableSubscripts.containsKey(variable)) {
          initializer += "int " + variable + "_version = " + variableSubscripts.get(variable) + ";";
        } else {
          initializer += "int " + variable + "_version = -1;";
        }
      }
      for (int i = 0; i < ctx.localVariableDeclaration().variableDeclaratorList().variableDeclarator().size(); i++) {
        Java8Parser.VariableDeclaratorIdContext varContext = ctx.localVariableDeclaration().variableDeclaratorList()
            .variableDeclarator(i).variableDeclaratorId();
        String variable = tokens.getText(varContext);
        int lineNumber = forCtx.getStart().getLine();
        if (ctx.localVariableDeclaration().variableDeclaratorList().variableDeclarator(i)
            .variableInitializer() != null) {
          currentVariableSubscriptMap.put(variable, 0);
          if (!variableSubscripts.containsKey(variable))
            variableSubscripts.put(variable, -1);

          insertRecordStatementBefore(forCtx.getStart(), variable, lineNumber);
          insertVersionUpdateBefore(forCtx.getStart(), variable);
        }
      }
      rewriter.insertBefore(forCtx.getStart(), forInit);
      rewriter.insertBefore(forCtx.getStart(), initializer);

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

        if (ctx.statement().statementWithoutTrailingSubstatement().block() == null) {
          rewriter.insertAfter(ctx.statement().getStop(), assignmentContext.getText() + ";");
          insertVersionUpdateAfter(ctx.statement().getStop(), variable);
          insertRecordStatementAfter(ctx.statement().getStop(), variable, lineNumber);
        } else {
          insertRecordStatementBefore(ctx.statement().getStop(), variable, lineNumber);
          insertVersionUpdateBefore(ctx.statement().getStop(), variable);
          rewriter.insertBefore(ctx.statement().getStop(), assignmentContext.getText() + ";");
        }

        ArrayList<String> confounders = new ArrayList<>();
        if (variableSubscripts.containsKey(variable))
          confounders.add(variable + "_" + variableSubscripts.get(variable));
        causalMap.put(variable + "_" + variableSubscripts.get(variable), new ArrayList<String>());
        causalMap.get(variable + "_" + variableSubscripts.get(variable)).addAll(confounders);

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

        ArrayList<String> confounders = new ArrayList<>();

        if (variableSubscripts.containsValue(variable))
          confounders.add(variable + "_" + variableSubscripts.get(variable));
        if (ctx.statement().statementWithoutTrailingSubstatement().block() == null) {
          insertVersionUpdateBefore(ctx.statement().getStart(), variable);
          insertRecordStatementBefore(ctx.statement().getStart(), variable, lineNumber);
          rewriter.insertBefore(ctx.statement().getStart(), expressionContext.getText() + ";");
        } else {
          rewriter.insertAfter(ctx.statement().getStart(), expressionContext.getText() + ";");
          insertVersionUpdateAfter(ctx.statement().getStart(), variable);
          insertRecordStatementAfter(ctx.statement().getStart(), variable, lineNumber);
        }
        causalMap.put(variable + "_" + variableSubscripts.get(variable), new ArrayList<String>());
        causalMap.get(variable + "_" + variableSubscripts.get(variable)).addAll(confounders);
      }

      if (expressionContext instanceof Java8Parser.PostIncrementExpressionContext
          || expressionContext instanceof Java8Parser.PostDecrementExpressionContext) {
        if (expressionContext.getChild(0).getChild(0) instanceof Java8Parser.ExpressionNameContext) {
          String variable = expressionContext.getChild(0).getChild(0).getText();
          int lineNumber = expressionContext.getStart().getLine();

          ArrayList<String> confounders = new ArrayList<>();
          if (variableSubscripts.containsKey(variable))
            confounders.add(variable + "_" + variableSubscripts.get(variable));
          if (ctx.statement().statementWithoutTrailingSubstatement().block() == null) {
            rewriter.insertAfter(ctx.statement().getStop(), expressionContext.getText() + ";");
            insertVersionUpdateAfter(ctx.statement().getStop(), variable);
            insertRecordStatementAfter(ctx.statement().getStop(), variable, lineNumber);
          } else {
            insertRecordStatementBefore(ctx.statement().getStop(), variable, lineNumber);
            insertVersionUpdateBefore(ctx.statement().getStop(), variable);
            rewriter.insertBefore(ctx.statement().getStop(), expressionContext.getText() + ";");
          }
          causalMap.put(variable + "_" + variableSubscripts.get(variable), new ArrayList<String>());
          causalMap.get(variable + "_" + variableSubscripts.get(variable)).addAll(confounders);
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

        if (ctx.statement().statementWithoutTrailingSubstatement().block() == null) {
          rewriter.insertAfter(ctx.statement().getStop(), assignmentContext.getText() + ";");
          insertVersionUpdateAfter(ctx.statement().getStop(), variable);
          insertRecordStatementAfter(ctx.statement().getStop(), variable, lineNumber);
        } else {
          insertRecordStatementBefore(ctx.statement().getStop(), variable, lineNumber);
          insertVersionUpdateBefore(ctx.statement().getStop(), variable);
          rewriter.insertBefore(ctx.statement().getStop(), assignmentContext.getText() + ";");
        }

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
        // System.out.println(variable);
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
  public void enterEnhancedForStatement(Java8Parser.EnhancedForStatementContext ctx) {
    predicateBlockVariablesStack.push(new HashSet<String>());
    variablesDeclaredInPredicateStack.push(new HashSet<String>());
  }

  @Override
  public void exitEnhancedForStatement(Java8Parser.EnhancedForStatementContext ctx) {
    variablesDeclaredInPredicateStack.pop();
    TerminalNode forNode = (TerminalNode) ctx.getChild(0);
    TerminalNode startParenthesis = (TerminalNode) ctx.getChild(1);
    TerminalNode endParenthesis = (TerminalNode) ctx.getChild(ctx.getChildCount() - 2);

    String iterableName = ctx.expression().getText();
    String iteratorType = ctx.unannType().getText();
    String iteratorItem = ctx.variableDeclaratorId().getText();

    if (!variableSubscripts.containsKey(iteratorItem)) {
      rewriter.insertBefore(forNode.getSymbol(), "int " + iteratorItem + "_version = -1;");
      variableSubscripts.put(iteratorItem, -1);
    } else {
      rewriter.insertBefore(forNode.getSymbol(),
          "int " + iteratorItem + "_version = " + variableSubscripts.get(iteratorItem) + ";");

    }

    rewriter.insertBefore(forNode.getSymbol(),
        "java.util.Iterator<" + iteratorType + ">" + iterableName + "_iterator = " + iterableName + ".iterator();");

    if (ctx.statement().statementWithoutTrailingSubstatement().block() == null) {
      insertRecordStatementBefore(ctx.statement().getStart(), iteratorItem, ctx.statement().getStart().getLine());
      insertVersionUpdateBefore(ctx.statement().getStart(), iteratorItem);
      rewriter.insertBefore(ctx.statement().getStart(),
          iteratorType + " " + iteratorItem + "=" + iterableName + "_iterator" + ".next();");
    } else {
      rewriter.insertAfter(ctx.statement().getStart(),
          iteratorType + " " + iteratorItem + "=" + iterableName + "_iterator" + ".next();");
      insertVersionUpdateAfter(ctx.statement().getStart(), iteratorItem);
      insertRecordStatementAfter(ctx.statement().getStart(), iteratorItem, ctx.statement().getStart().getLine());
    }

    rewriter.replace(forNode.getSymbol(), "while");
    rewriter.replace(startParenthesis.getSymbol(), endParenthesis.getSymbol(),
        "(" + iterableName + "_iterator" + ".hasNext())");

    if (ctx.statement().statementWithoutTrailingSubstatement().block() == null) {
      rewriter.insertBefore(ctx.statement().getStart(), "{");
      rewriter.insertAfter(ctx.statement().getStop(), "}");
    }

    rewriter.insertBefore(ctx.getStart(), "{");
    rewriter.insertAfter(ctx.getStop(), "}");

    for (String variable : predicateBlockVariablesStack.pop()) {
      // if (!(forInitDeclarations.contains(variable))) {
      insertVersionUpdateAfter(ctx.getStop(), variable);
      insertRecordStatementAfter(ctx.getStop(), variable, ctx.getStop().getLine());
      // }
    }
  }

  @Override
  public void enterEnhancedForStatementNoShortIf(Java8Parser.EnhancedForStatementNoShortIfContext ctx) {
    predicateBlockVariablesStack.push(new HashSet<String>());
    variablesDeclaredInPredicateStack.push(new HashSet<String>());
  }

  @Override
  public void exitEnhancedForStatementNoShortIf(Java8Parser.EnhancedForStatementNoShortIfContext ctx) {
    variablesDeclaredInPredicateStack.pop();
    TerminalNode forNode = (TerminalNode) ctx.getChild(0);
    TerminalNode startParenthesis = (TerminalNode) ctx.getChild(1);
    TerminalNode endParenthesis = (TerminalNode) ctx.getChild(ctx.getChildCount() - 2);

    String iterableName = ctx.expression().getText();
    String iteratorType = ctx.unannType().getText();
    String iteratorItem = ctx.variableDeclaratorId().getText();

    if (!variableSubscripts.containsKey(iteratorItem)) {
      rewriter.insertBefore(forNode.getSymbol(), "int " + iteratorItem + "_version = -1;");
      variableSubscripts.put(iteratorItem, -1);
    } else {
      rewriter.insertBefore(forNode.getSymbol(),
          "int " + iteratorItem + "_version = " + variableSubscripts.get(iteratorItem) + ";");
    }

    rewriter.insertBefore(forNode.getSymbol(),
        "java.util.Iterator<" + iteratorType + ">" + iterableName + "_iterator = " + iterableName + ".iterator();");

    if (ctx.statementNoShortIf().statementWithoutTrailingSubstatement().block() == null) {
      insertRecordStatementBefore(ctx.statementNoShortIf().getStart(), iteratorItem,
          ctx.statementNoShortIf().getStart().getLine());
      insertVersionUpdateBefore(ctx.statementNoShortIf().getStart(), iteratorItem);
      rewriter.insertBefore(ctx.statementNoShortIf().getStart(),
          iteratorType + " " + iteratorItem + "=" + iterableName + "_iterator" + ".next();");
    } else {
      rewriter.insertAfter(ctx.statementNoShortIf().getStart(),
          iteratorType + " " + iteratorItem + "=" + iterableName + "_iterator" + ".next();");
      insertVersionUpdateAfter(ctx.statementNoShortIf().getStart(), iteratorItem);
      insertRecordStatementAfter(ctx.statementNoShortIf().getStart(), iteratorItem,
          ctx.statementNoShortIf().getStart().getLine());
    }

    rewriter.replace(forNode.getSymbol(), "while");
    rewriter.replace(startParenthesis.getSymbol(), endParenthesis.getSymbol(),
        "(" + iterableName + "_iterator" + ".hasNext())");

    if (ctx.statementNoShortIf().statementWithoutTrailingSubstatement().block() == null) {
      rewriter.insertBefore(ctx.statementNoShortIf().getStart(), "{");
      rewriter.insertAfter(ctx.statementNoShortIf().getStop(), "}");
    }

    rewriter.insertBefore(ctx.getStart(), "{");
    rewriter.insertAfter(ctx.getStop(), "}");

    for (String variable : predicateBlockVariablesStack.pop()) {
      // if (!(forInitDeclarations.contains(variable))) {
      insertVersionUpdateAfter(ctx.getStop(), variable);
      insertRecordStatementAfter(ctx.getStop(), variable, ctx.getStop().getLine());
      // }
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

  public ArrayList<Java8Parser.AssignmentContext> getAllAssignmentContexts(ParserRuleContext ctx) {
    ArrayList<Java8Parser.AssignmentContext> assignmentContexts = new ArrayList<>();

    int numChildren = ctx.getChildCount();
    for (int i = 0; i < numChildren; i++) {
      if (ctx.getChild(i) instanceof Java8Parser.AssignmentContext) {
        assignmentContexts.add((Java8Parser.AssignmentContext) ctx.getChild(i));
      } else if (ctx.getChild(i) instanceof ParserRuleContext) {
        assignmentContexts.addAll(getAllAssignmentContexts((ParserRuleContext) ctx.getChild(i)));
      }
    }

    return assignmentContexts;
  }

  public HashSet<String> getAllAlteredVariablesFromContext(ParserRuleContext ctx) {
    HashSet<String> allAlteredVariables = new HashSet<>();

    int numChildren = ctx.getChildCount();
    for (int i = 0; i < numChildren; i++) {
      if (ctx instanceof Java8Parser.AssignmentContext) {
        Java8Parser.AssignmentContext assignmentCtx = (Java8Parser.AssignmentContext) ctx;
        allAlteredVariables.add(assignmentCtx.leftHandSide().getText());
        allAlteredVariables.addAll(getIncrementAndDecrementVariablesFromAssignment(assignmentCtx));
      } else if (ctx.getChild(i) instanceof Java8Parser.PreIncrementExpressionContext) {
        Java8Parser.PreIncrementExpressionContext expr = (Java8Parser.PreIncrementExpressionContext) ctx.getChild(i);
        allAlteredVariables.add(expr.unaryExpression().getText());
      } else if (ctx.getChild(i) instanceof Java8Parser.PreDecrementExpressionContext) {
        Java8Parser.PreDecrementExpressionContext expr = (Java8Parser.PreDecrementExpressionContext) ctx.getChild(i);
        allAlteredVariables.add(expr.unaryExpression().getText());
      } else if (ctx.getChild(i) instanceof Java8Parser.PostIncrementExpressionContext) {
        Java8Parser.PostIncrementExpressionContext expr = (Java8Parser.PostIncrementExpressionContext) ctx.getChild(i);
        String varName = tokens.getText(expr.postfixExpression().expressionName());
        allAlteredVariables.add(varName);
      } else if (ctx.getChild(i) instanceof Java8Parser.PostDecrementExpressionContext) {
        Java8Parser.PostDecrementExpressionContext expr = (Java8Parser.PostDecrementExpressionContext) ctx.getChild(i);
        String varName = tokens.getText(expr.postfixExpression().expressionName());
        allAlteredVariables.add(varName);
      } else if (ctx.getChild(i) instanceof ParserRuleContext) {
        allAlteredVariables.addAll(getAllAlteredVariablesFromContext((ParserRuleContext) ctx.getChild(i)));
      }
    }

    return allAlteredVariables;
  }

  public void insertVersionUpdateAfter(Token token, String variableName) {
    if (!variableSubscripts.isEmpty()) {
      if (!variableSubscripts.containsKey(variableName))
        return;
      int version = variableSubscripts.get(variableName);
      if (variableSubscripts.keySet().contains(variableName)) {
        variableSubscripts.put(variableName, version + 1);
      }

      // System.out.println(token.getText());
      // System.out.println(variableName + " " + variableSubscripts.get(variableName));
      rewriter.insertAfter(token, variableName + "_version = " + variableSubscripts.get(variableName) + ";");
    }
  }

  public void insertVersionUpdateBefore(Token token, String variableName) {
    if (!variableSubscripts.isEmpty()) {
      if (!variableSubscripts.containsKey(variableName))
        return;
      int version = variableSubscripts.get(variableName);

      if (variableSubscripts.keySet().contains(variableName)) {
        variableSubscripts.put(variableName, version + 1);
      }

      rewriter.insertBefore(token, variableName + "_version = " + variableSubscripts.get(variableName) + ";");
    }
  }

  public void insertRecordStatementAfter(Token token, String variableName, int lineNumber) {
    if (!variableSubscripts.containsKey(variableName))
      return;
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
    if (!variableSubscripts.containsKey(variableName))
      return;
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

    int numChildren = expressionContext.getChildCount();
    for (int i = 0; i < numChildren; i++) {
      if (expressionContext.getChild(i) instanceof Java8Parser.ExpressionNameContext) {
        expressionNamesList.add(expressionContext.getChild(i).getText());
      } else if (expressionContext.getChild(i) instanceof ParserRuleContext) {
        expressionNamesList
            .addAll(getAllExpressionNamesFromPredicate((ParserRuleContext) expressionContext.getChild(i)));
      }
    }
    // return false;
    // for (TerminalNode token : ctxTokens) {
    //   String tokenText = token.getText();
    //   int tokenType = token.getSymbol().getType();
    //   if (tokenType == IDENTIFIER_TYPE) {
    //     expressionNamesList.add(tokenText);
    //   }
    // }
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
              || Java8Parser.WhileStatementNoShortIfContext.class.isInstance(ctx.getParent().getParent())
              || Java8Parser.BasicForStatementContext.class.isInstance(ctx.getParent().getParent())
              || Java8Parser.BasicForStatementNoShortIfContext.class.isInstance(ctx.getParent().getParent())
              || Java8Parser.IfThenElseStatementContext.class.isInstance(ctx.getParent().getParent())
              || Java8Parser.IfThenElseStatementNoShortIfContext.class.isInstance(ctx.getParent().getParent()))) {
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
    HashMap<String, Integer> varSubscriptsBeforePredicate = new HashMap<>(variableSubscripts);
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

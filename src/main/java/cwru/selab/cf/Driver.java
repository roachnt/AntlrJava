package cwru.selab.cf;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;

public class Driver {
  public static void main(String[] args) throws IOException {
    // create a CharStream that reads from a file at path in the first arg
    CharStream input = CharStreams.fromFileName(args[0]);

    // create a lexer that feeds off of the input CharStream
    Java8Lexer lexer = new Java8Lexer(input);

    // create a buffer of tokens pulled from the lexer
    CommonTokenStream tokens = new CommonTokenStream(lexer);

    // create a parser that feeds off the tokens buffer
    Java8Parser parser = new Java8Parser(tokens);

    TokenStreamRewriter rewriter = new TokenStreamRewriter(tokens);

    ParseTree tree = parser.compilationUnit(); // begin parsing at init rule

    ParseTreeWalker walker = new ParseTreeWalker(); // Standard walker

    Converter converter = new Converter(parser, rewriter);

    walker.walk(converter, tree);

    BufferedWriter writer = new BufferedWriter(new FileWriter("out.java"));
    writer.write(rewriter.getText());
    writer.close();
    // for (int i = 0; i < parser.getVocabulary().getMaxTokenType(); i++) {
    //   System.out.println(
    //       i + " = " + parser.getVocabulary().getDisplayName(i) + ", " +
    // parser.getVocabulary().getLiteralName(i));
    // }
    System.out.println(rewriter.getText());
    // System.out.println(tree.toStringTree(parser)); // print LISP-style tree
  }
}

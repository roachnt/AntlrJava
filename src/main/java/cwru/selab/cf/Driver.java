package cwru.selab.cf;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;
import com.google.googlejavaformat.java.Formatter;
import java.util.HashMap;
import java.util.List;
import java.util.HashSet;
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;

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
    String formattedSource = "";
    BufferedWriter writer = new BufferedWriter(new FileWriter("out.java"));
    try {
      formattedSource = new Formatter().formatSource(rewriter.getText());
    } catch (Exception e) {
      System.out.println(rewriter.getText());
      System.out.println(e.getMessage());
      writer.write(rewriter.getText());
      writer.close();
      return;
    }
    writer.write(formattedSource);
    writer.close();
    // for (int i = 0; i < parser.getVocabulary().getMaxTokenType(); i++) {
    //   System.out.println(
    //       i + " = " + parser.getVocabulary().getDisplayName(i) + ", " +
    // parser.getVocabulary().getLiteralName(i));
    // }
    System.out.println(formattedSource);
    System.out.println(converter.causalMap);
    try {
      genRForCFmeansRF("RforCFmeansRF_TestShimple.R", "TestShimple_fault_binerrs_all", "TestShimple_fault_binerrs", "Y",
          converter.getCausalMap());
    } catch (IOException e) {
      e.printStackTrace();
    }
    // System.out.println(tree.toStringTree(parser)); // print LISP-style tree
  }

  private static void genRForCFmeansRF(String RFileName, String varFrameName, String prefix, String outName,
      HashMap<String, HashSet<String>> covariant) throws IOException {

    OutputStream out = new FileOutputStream(RFileName);
    BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out));

    writer.write("genCFmeansRF_" + prefix + " <- function() {\n\n");
    // for RF
    writer.write("results <- data.frame(row.names=seq(1, 10))\n\n");
    // for esp
    //        writer.write("results <- data.frame(row.names = \"mean\")\n\n");

    for (String t : covariant.keySet()) {
      //        for (Value t : treatNames){
      String vfn = varFrameName;
      // for confounder
      String tfn = prefix + "_" + t + "_treat_df";
      // for no confounder
      String tfn_nocnfd = prefix + "_" + t + "_treat_nocnfd_df";

      //            // for tfn
      writer.write(tfn + " <- data.frame(" + outName + "=" + vfn + "$" + outName + ", " + t + "=" + vfn + "$" + t);
      HashSet<String> set = covariant.get(t);
      for (String c : set) {
        writer.write(", " + c + "=" + vfn + "$" + c);
      }

      // for tfn_nocnfd
      //            writer.write(tfn_nocnfd + " <- data.frame(" + outName + "=" + vfn + "$" + outName + ", " + t + "=" + vfn + "$" + t);

      writer.write(")\n");

      // to remove NA
      //            writer.write(tfn + " <- " + tfn + "[complete.cases(" + tfn + "),]" + '\n');

      // Only treatement, no confounder (ESP)
      //            writer.write("results[[\"" + t + "\"]] <- CFmeansForESP(" + tfn_nocnfd + ", \"" + outName + "\", \"" + t + "\"");

      // For random forest
      writer
          .write("results[[\"" + t + "\"]] <- CFmeansForDecileBinsRF(" + tfn + ", \"" + outName + "\", \"" + t + "\"");

      // For LM and LASSO
      //            writer.write("results[[\"" + t + "\"]] <- CFmeansForDecileBinsLM(" + tfn + ", \"" + outName + "\", \"" + t + "\"");

      writer.write(")\n\n");
    }
    writer.write("return(results)\n\n");
    writer.write("}\n");
    writer.flush();
    writer.close();
  }
}

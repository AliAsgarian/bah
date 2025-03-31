import java.io.*;
import absyn.*;
import java.nio.file.Path;
import java.nio.file.Paths;

class CM {
  static public void main(String argv[]) {    
    
    boolean a_flag = false;
    boolean s_flag = false;
    boolean c_flag = false;
    
    for (String s: argv) {
      if(s.equals("-a"))
        a_flag = true;
      if (s.equals("-s"))
        s_flag = true;
      if (s.equals("-c"))
        c_flag = true;
    }
    
    /* Start the parser */
    try {
      parser p = new parser(new Lexer(new FileReader(argv[0])));
      Absyn result = (Absyn)(p.parse().value);      
      
      Path path = Paths.get(argv[0]);
      Path file = path.getFileName();
      String filename = file.toString();
      filename = filename.substring(0, filename.lastIndexOf("."));

      if (a_flag && result != null) {
        PrintStream out = new PrintStream(new FileOutputStream("test/" + filename + ".abs"));
        System.setOut(out);
        System.out.println("The abstract syntax tree is:");
        ShowTreeVisitor visitor = new ShowTreeVisitor();
        result.accept(visitor, 0, false); 
      }

      if (s_flag && result != null) {
        PrintStream out = new PrintStream(new FileOutputStream("test/" + filename + ".sym"));
        System.setOut(out);
        System.out.println("Entering the global scope:");
        SemanticAnalyzer visitor = new SemanticAnalyzer();
        result.accept(visitor, 0, false);
        visitor.printSymbolTable(1);
        System.out.println("Leaving the global scope");
      }

      if (c_flag && result != null) {
        /* Need to verify that SemanticAnalyzer produces without errors before running CodeGenerator */
        PrintStream out = new PrintStream(new FileOutputStream("test/" + filename + ".tm"));
        System.setOut(out);
        System.out.println("* C-Minus Compilation to TM Code");
        System.out.println("* File: " + filename + ".tm");
        CodeGenerator visitor = new CodeGenerator();
        visitor.visit(result);
      }
    } catch (Exception e) {
      /* do cleanup here -- possibly rethrow e */
      e.printStackTrace();
    }
  }
}


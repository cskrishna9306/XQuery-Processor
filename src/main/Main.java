import org.antlr.v4.runtime.ANTLRFileStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

import com.example.antlr4.ExpLexer;
import com.example.antlr4.ExpParser;

public class Main {
    public static void main(String[] args) throws Exception {
        String fname = args[0];
        ExpLexer lexer = new ExpLexer(new ANTLRFileStream(fname));
        ExpParser parser = new ExpParser(new CommonTokenStream(lexer));
        ParseTree tree = parser.eval();
        System.out.println(compute(tree));
    }

   private static double compute(ParseTree t) {
     if (t instanceof ExpParser.EvalContext) {
        return compute(t.getChild(0));
     }
     if (t instanceof ExpParser.AdditionExpContext) {
        double soFar = compute(t.getChild(0));
        for (int i = 1; i < t.getChildCount(); i+=2) {
           double nextExpVal = compute(t.getChild(i+1));        
   	   if (t.getChild(i).getText().equals("+")) {
             soFar = soFar + nextExpVal;
           } else {
             soFar = soFar - nextExpVal;
           }           
        }
        return soFar;
     }           
     if (t instanceof ExpParser.MultiplyExpContext) {
        double soFar = compute(t.getChild(0));
        for (int i = 1; i < t.getChildCount(); i+=2) {
           double nextExpVal = compute(t.getChild(i+1));        
   	   if (t.getChild(i).getText().equals("*")) {
             soFar = soFar * nextExpVal;
           } else {
             soFar = soFar / nextExpVal;
           }           
        }           
        return soFar;
     }
     if (t instanceof ExpParser.AtomExpContext) {
       if (t.getChildCount() > 1) {
         return compute(t.getChild(1));
       }
       return Double.parseDouble(t.getChild(0).getText());
     }
     // will never reach here, just pleasing Java compiler    
     return 0.0;
   }
}

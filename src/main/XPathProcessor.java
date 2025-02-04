// ANTLR import statements
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

// Custom import packages
import com.example.antlr4.XPathLexer;
import com.example.antlr4.XPathParser;

public class XPathProcessor {

    public static ParseTree parse(String xpath) throws Exception {
        XPathLexer lexer = new XPathLexer(new ANTLRInputStream(xpath));
        XPathParser parser = new XPathParser(new CommonTokenStream(lexer));
        ParseTree tree = parser.eval();
        System.out.println(parser.fileName().getText());
//        System.out.println(tree.toStringTree(parser));
        return tree;
    }

    public static void main(String[] args) {
        // Step 1: Read the XPath query
        // Step 2: Extract the file name from the absolute path, and build the DOM tree of this file
        // Step 3: Process the rest of the XPath
        try {
            ParseTree AST = parse(args[0]);
//            System.out.println(AST.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
//        Document DOMTree = XMLToDOMParser.parse("adads");
    } 
}

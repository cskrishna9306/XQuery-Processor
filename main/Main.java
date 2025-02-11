import org.w3c.dom.*;

// ANTLR import statements
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

// Custom import packages
import com.example.antlr4.XPathLexer;
import com.example.antlr4.XPathParser;

import java.util.List;

public class Main {
    public static void main(String[] args) {
        // Step 1: Read the XPath query
        // Step 2: Extract the file name from the absolute path, and build the DOM tree of this file
        // Step 3: Process the rest of the XPath
        try {
            XPathLexer lexer = new XPathLexer(CharStreams.fromString(args[0]));
            XPathParser parser = new XPathParser(new CommonTokenStream(lexer));

            ParseTree AST = parser.eval();

            List<Node> result = XPathProcessor.parse(null, AST);
            XMLToDOMParser.exportToXML(result, "result.xml");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

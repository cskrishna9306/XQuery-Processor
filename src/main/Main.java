import org.w3c.dom.*;

import java.io.BufferedReader;
import java.io.FileReader;

// ANTLR import statements
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

// Custom import packages
//import com.example.antlr4.XPathLexer;
//import com.example.antlr4.XPathParser;

import java.util.List;

public class Main {
    public static void main(String[] args) {
        // Step 1: Read the XPath query
        // Step 2: Extract the file name from the absolute path, and build the DOM tree of this file
        // Step 3: Process the rest of the XPath
        try {
            // args[0] - path to XML file
            Document DOMTree = XMLToDOMParser.parse(args[0]);

            // args[1] - contains the input XPath query
            BufferedReader br = new BufferedReader(new FileReader(args[1]));
//            String xpathQuery = br.readLine();

//            String xPathQuery = Files.readString(Paths.get(args[1]), null);

            XPathLexer lexer = new XPathLexer(CharStreams.fromString(br.readLine()));
            XPathParser parser = new XPathParser(new CommonTokenStream(lexer));

            ParseTree AST = parser.eval();

            // args[2] - output file
            List<Node> result = XPathProcessor.parse(DOMTree.getDocumentElement(), AST);
            XMLToDOMParser.exportToXML(result, args[2]);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

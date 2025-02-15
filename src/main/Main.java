import org.w3c.dom.*;

// ANTLR import statements
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.List;

import com.example.antlr4.XQueryLexer;
import com.example.antlr4.XQueryParser;

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

            XQueryLexer lexer = new XQueryLexer(CharStreams.fromString(br.readLine()));
            XQueryParser parser = new XQueryParser(new CommonTokenStream(lexer));

            ParseTree AST = parser.eval();

            // args[2] - output file
            List<Node> result = XQueryProcessor.parse(DOMTree.getDocumentElement(), AST);
            XMLToDOMParser.exportToXML(result, args[2]);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
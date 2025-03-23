import org.w3c.dom.*;

// ANTLR import statements
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import java.util.stream.Collectors;

import com.example.antlr4.XQueryLexer;
import com.example.antlr4.XQueryParser;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

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
            String content = br.lines().collect(Collectors.joining("\n")); // Read all lines

            XQueryLexer lexer = new XQueryLexer(CharStreams.fromString(content));
            XQueryParser parser = new XQueryParser(new CommonTokenStream(lexer));

            ParseTree AST = parser.eval();

            // args[2] - rewrite file
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document resultDocument = builder.newDocument();

            assert DOMTree != null;
            XQueryProcessor processor = new XQueryProcessor(DOMTree.getDocumentElement(), new File(args[2]), resultDocument);

            // args[3] - output file
            processor.parse(AST, new HashMap<>());
            XMLToDOMParser.exportToXML(resultDocument, args[3]);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
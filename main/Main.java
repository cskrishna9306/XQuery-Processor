package main;
import org.w3c.dom.*;

// ANTLR import statements
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import main.antlr.XQueryLexer;
import main.antlr.XQueryParser;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import java.io.File;

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

            // args[1] - path to rewrite
            // dummy code to test impl
//            System.out.println("args2: " + args[2]);
//            DocumentBuilderFactory factory1 = DocumentBuilderFactory.newInstance();
//            DocumentBuilder builder1 = factory1.newDocumentBuilder();
//            Document resultDocument1 = builder1.newDocument();
//            XQueryProcessor processor1 = new XQueryProcessor(DOMTree.getDocumentElement(), resultDocument1);
//
//            List<Node> result1 = processor1.parse(AST, new HashMap<String, List<Node>>());
//            System.out.println("result : " + result);
//            for (Node node : result){
//                System.out.println("node : " + node.getTextContent());
//            }
//            XMLToDOMParser.exportToXML(resultDocument1, args[2]);


            // args[3] - output file
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document resultDocument = builder.newDocument();
            XQueryProcessor processor = new XQueryProcessor(DOMTree.getDocumentElement(), resultDocument);


            List<Node> result = processor.parse(AST, new HashMap<String, List<Node>>());
//            System.out.println("resultsize : " + result.size());

            if (result.size() > 1) {
                Document tempDoc = builder.newDocument();
                // Create a root element to wrap nodes (optional)
                Element root = tempDoc.createElement("RESULT");
                tempDoc.appendChild(root);

                for (Node node : result) {
                    Node importedNode = tempDoc.importNode(node, true);
                    root.appendChild(importedNode);
                }
                resultDocument = tempDoc;
            }

            XMLToDOMParser.exportToXML(resultDocument, args[3]);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
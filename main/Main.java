package main;
import org.w3c.dom.*;

// ANTLR import statements
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

import java.util.List;
import java.io.File;
import java.io.IOException;
import java.util.Scanner;


import main.antlr.XPathLexer;
import main.antlr.XPathParser;



public class Main {
    public static void main(String[] args) {
        // Step 1: Read the XPath query
        // Step 2: Extract the file name from the absolute path, and build the DOM tree of this file
        // Step 3: Process the rest of the XPath
        try {
            System.out.println("A0 - xml file: " + args[0]);
            System.out.println("A1 - query path: "  + args[1]);
            System.out.println("A2 - output path: "  + args[2]);
            String inputXMLPath = args[0];
            String queryTxtPath = args[1];
            String outputFilename = args[2];
            try (Scanner scanner = new Scanner(new File(queryTxtPath))) {
                while (scanner.hasNextLine()) {
                    // Process each line here
                    String query = scanner.nextLine();
                    System.out.println("Query: " + query);
                    XPathLexer lexer = new XPathLexer(CharStreams.fromString(query));
                    XPathParser parser = new XPathParser(new CommonTokenStream(lexer));

                    ParseTree AST = parser.eval();
                    XPathProcessor xpp = new XPathProcessor(inputXMLPath);
                    List<Node> result = xpp.parse(null, AST);
                    XMLToDOMParser.exportToXML(result, outputFilename);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

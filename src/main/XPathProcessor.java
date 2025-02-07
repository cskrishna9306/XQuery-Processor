import org.antlr.v4.runtime.ParserRuleContext;
import org.w3c.dom.*;

import java.util.*;

// ANTLR import statements
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

// Custom import packages
import com.example.antlr4.XPathLexer;
import com.example.antlr4.XPathParser;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.events.EndElement;

public class XPathProcessor {

    public static ParseTree generateAST(String xpath) throws Exception {

        XPathLexer lexer = new XPathLexer(CharStreams.fromString(xpath));
        XPathParser parser = new XPathParser(new CommonTokenStream(lexer));

        return parser.eval();
    }

    private static List<Node> parse(Element DOMElement, ParseTree AST) {
        if (AST instanceof XPathParser.EvalContext) {
            return parse(null, AST.getChild(0));
        }

        if (AST instanceof XPathParser.AbsolutePathContext) {
            return parseAbsolutePath(AST);
        }

        if (AST instanceof XPathParser.RelativePathContext) {
            return parseRelativePath(DOMElement, AST);
        }

        return null;
    }

    private static List<Node> parseAbsolutePath(ParseTree AST) {

        String fileName = ((XPathParser.AbsolutePathContext) AST).fileName().STRING().toString();
        Document DOMTree = XMLToDOMParser.parse("src/main/" + fileName);

        switch (AST.getChild(3).getText()) {
            case "/": {
                return parse(DOMTree.getDocumentElement(), ((XPathParser.AbsolutePathContext) AST).relativePath());
            }
            case "//": {
                XPathParser.RelativePathContext relativePath = ((XPathParser.AbsolutePathContext) AST).relativePath();
//                List<Node> results = new ArrayList<>();
//
//                // Apply relative path parsing to each descendant
//                for (Element descendant : getAllDescendants(DOMTree.getDocumentElement())) {
//                    results.addAll(parse(descendant, relativePath));
//                }

                Set<Node> result = new LinkedHashSet<>(); // Ensuring uniqueness
                for (Node node : parseRelativePath(DOMTree.getDocumentElement(), relativePath)) {
                    if (node.getNodeType() == Node.ELEMENT_NODE) {
                        result.addAll(parseRelativePath((Element) node, relativePath));
                        for (Element descendant : getAllDescendants(DOMTree.getDocumentElement())) {
                            result.addAll(parse(descendant, relativePath));
                        }
                    }
                }
                List<Node> t = new ArrayList<>();
                t.addAll(result);
                return t;
            }
        }
        return null;
    }

    private static List<Element> getAllDescendants(Element DOMElement) {
        List<Element> descendants = new ArrayList<>();
        NodeList allElements = DOMElement.getElementsByTagName("*");  // Gets all elements

        for (int i = 0; i < allElements.getLength(); i++) {
            descendants.add((Element) allElements.item(i));
        }

        return descendants;
    }


    private static List<Node> parseRelativePath(Element DOMElement, ParseTree AST) {

        List<Node> result = new ArrayList<>();
        // can i manipulate the number of children for this AST?
        // if childcount == 1, then first 5 cases
        // if childcount == 2, one case
        // if childcount == 3, 4 cases
        // if childcount == 4, one case w/ filter
        switch (AST.getChildCount()) {
            case 1: {
                ParseTree child = AST.getChild(0);
                if (child instanceof XPathParser.TagNameContext) {

                    // Search for matching elements in the DOM
                    for (Node node : children(DOMElement)) {
                        if (node.getNodeName().equals(child.getText())) {
                            result.add(node);
                        }
                    }
                } else if (child.getText().equals("*")) {
                    result.addAll(children(DOMElement));
                } else if (child.getText().equals(".")) {
                    result.add(DOMElement);
                } else if (child.getText().equals("..")) {
                    result.add(DOMElement.getParentNode());
                }

                if (child.getText().equals("text()")) {
//                    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
//                    DocumentBuilder builder = factory.newDocumentBuilder();
//                    Document doc = builder.newDocument();
//
//                    result.add(doc.createTextNode(DOMElement.getTextContent()));
                    result.add(DOMElement.getFirstChild());
//                    result.add(DOMElement.TEXT_NODE);

                }

//                switch (AST.getChild(0).getText()) {
//
//                }

                break;
            }
            case 2: {
                ParseTree child = AST.getChild(1);
                if (child instanceof XPathParser.AttributeNameContext) {
                    // implement attribute helper function
                    result.addAll((Collection<? extends Node>) DOMElement.getAttributeNode(child.getText()));
//                    DOMElement.getAttribute()
//                    DOMElement
                }
                break;
            }
            case 3: {

                if (AST.getChild(0) instanceof XPathParser.RelativePathContext) {

                    ParseTree rp1 = AST.getChild(0);
                    ParseTree rp2 = AST.getChild(2);

                    switch (AST.getChild(1).getText()) {
                        case "/": {
                            Set<Node> uniqueNodes = new LinkedHashSet<>(); // Ensuring uniqueness
                            for (Node node : parseRelativePath(DOMElement, rp1)) {
                                if (node.getNodeType() == Node.ELEMENT_NODE) {
                                    uniqueNodes.addAll(parseRelativePath((Element) node, rp2));
                                }
                            }

                            result.addAll(uniqueNodes);
                            break;
                        }
                        case "//": {
                            Set<Node> uniqueNodes = new LinkedHashSet<>(); // Ensuring uniqueness
                            for (Node node : parseRelativePath(DOMElement, rp1)) {
                                if (node.getNodeType() == Node.ELEMENT_NODE) {
                                    uniqueNodes.addAll(parseRelativePath((Element) node, rp2));
                                    for (Element descendant : getAllDescendants(DOMElement)) {
                                        uniqueNodes.addAll(parse(descendant, rp2));
                                    }
                                }
                            }

                            result.addAll(uniqueNodes);
                            break;
                        }
                        case ",": {
                            result.addAll(parseRelativePath(DOMElement, rp1));
                            result.addAll(parseRelativePath(DOMElement, rp2));
                            break;
                        }
                    }
                } else {
                    return parseRelativePath(DOMElement, AST.getChild(1));
                }
                break;
            }
            case 4: {
                ParseTree rp = AST.getChild(0);
                ParseTree filter = AST.getChild(2);

                if (rp instanceof XPathParser.RelativePathContext && filter instanceof XPathParser.FilterContext) {
                    // implement the rp[filter] case
//                    System.out.println("here");
                    for (Node intermediate : parseRelativePath(DOMElement, rp)) {
                        if (parseFilter((Element) intermediate, filter)) {
                            result.add(intermediate);
                        }
                    }
                }
            }
        }

        return result;
    }

    private static boolean parseFilter(Element DOMElement, ParseTree AST) {
//        System.out.println(AST.getChildCount());
        // can i manipulate the number of children for this AST?
        // if childcount == 1, then 1 case
        // if childcount == 2, then 1 case
        // if childcount == 3, 7 cases
        // if childcount == 5, one case
        switch (AST.getChildCount()) {
            case 1: {
                ParseTree child = AST.getChild(0);
                if (child instanceof XPathParser.RelativePathContext) {
                    if (!parseRelativePath(DOMElement, child).isEmpty()) {
                        return true;
                    }
                }
                break;
            }
            case 2: {
                ParseTree child = AST.getChild(1);
                if (AST.getChild(0).getText().equals("not") && child instanceof XPathParser.FilterContext) {
                    // implement not filter case
                    if (!parseFilter(DOMElement, child)) {
                        return true;
                    }
                }
                break;
            }
            case 3: {
                ParseTree child = AST.getChild(0);
                if (child instanceof XPathParser.RelativePathContext) {

                    ParseTree rp1 = AST.getChild(0);
                    ParseTree rp2 = AST.getChild(2);

                    switch (AST.getChild(1).getText()) {
                        case "=":
                        case "eq": {
                            break;
                        }
                        case "==":
                        case "is": {
                            break;
                        }
                    }

                } else {
                    if (AST.getChild(0) instanceof XPathParser.FilterContext) {
                        switch (AST.getChild(1).getText()) {
                            case "and": {
                                return parseFilter(DOMElement, AST.getChild(0)) && parseFilter(DOMElement, AST.getChild(2));
                            }
                            case "or": {
                                return parseFilter(DOMElement, AST.getChild(0)) || parseFilter(DOMElement, AST.getChild(2));
                            }
                        }
                    } else {
                        return parseFilter(DOMElement, AST.getChild(1));
                    }
                }
                break;
            }
            case 5: {
                ParseTree rp = AST.getChild(0);
                ParseTree string = AST.getChild(3);
//                System.out.println("here");
                if (rp instanceof XPathParser.RelativePathContext) {
//                     implement the rp[filter] case
                    for (Node n : parseRelativePath(DOMElement, rp)) {
                        System.out.println(n.getTextContent() + "," + string.getText());
                        if (n.getTextContent().equals(string.getText())) {
//                            System.out.println("here");
                            return true;
                        }
                    }
//                    ((XPathParser.FilterContext) AST).STRING().getText();
                }
            }
        }

        return false;
    }

    public static List<Node> children(Element parent) {
        List<Node> childrenList = new ArrayList<>();
        NodeList children = parent.getChildNodes();

        for (int i = 0; i < children.getLength(); i++) {
            childrenList.add(children.item(i));
        }

        return childrenList;
    }

    public static void printNodes(List<Node> nodes) {
        for (Node node : nodes) {
            System.out.println("Found Node: " + node.getNodeName() + " -> " + node.getTextContent().trim());
        }
    }

    public static void main(String[] args) {
        // Step 1: Read the XPath query
        // Step 2: Extract the file name from the absolute path, and build the DOM tree of this file
        // Step 3: Process the rest of the XPath
        try {
//            Document DOMTree = XMLToDOMParser.parse("src/main/j_caesar.xml");
            ParseTree AST = generateAST(args[0]);
//            System.out.println(AST.getChild());
            List<Node> result = parse(null, AST);
            printNodes(result);
            XMLToDOMParser.exportToXML(result, "result.xml");

        } catch (Exception e) {
            e.printStackTrace();
        }
    } 
}

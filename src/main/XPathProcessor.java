import org.w3c.dom.*;

import java.util.*;

// ANTLR import statements
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

// Custom import packages
import com.example.antlr4.XPathLexer;
import com.example.antlr4.XPathParser;

public class XPathProcessor {

    public static List<Node> getChildren(Element parent) {
        List<Node> childrenList = new ArrayList<>();
        NodeList children = parent.getChildNodes();

        for (int i = 0; i < children.getLength(); i++) {
            childrenList.add(children.item(i));
        }

        return childrenList;
    }

    private static List<Node> getAllDescendants(Element DOMElement) {
        List<Node> descendants = new ArrayList<>();
//        NodeList nodeList = DOMElement.getChildNodes();

//        for (int i = 0; i < nodeList.getLength(); i++) {
//            Node child = nodeList.item(i);
//             // Add the child node
//            if (child.getNodeType() == Node.ELEMENT_NODE) {
//                descendants.add((Element) child);
//                descendants.addAll(getAllDescendants((Element) child));
//            } else {
//                System.out.println(child.getNodeType() + " " + child.getTextContent());
//            }
//        }

        for (Node child : getChildren(DOMElement)) {
            // we add all children (element, attribute, and text)
            if (child.getNodeType() == Node.TEXT_NODE || child.getNodeType() == Node.ELEMENT_NODE) {
                descendants.add(child);
                // we only recurse on element nodes
                if (child.getNodeType() == Node.ELEMENT_NODE) {
                    descendants.addAll(getAllDescendants((Element) child));
                }
            } else {
                System.out.println(child.getNodeType() + " " + child.getTextContent());
            }
        }

        return descendants;
    }

    public static void printNodes(List<Node> nodes) {
        for (Node node : nodes) {
            System.out.println("Found Node: " + node.getNodeName() + " -> " + node.getTextContent().trim());
        }
    }

    private static List<Node> parse(Node DOMElement, ParseTree AST) {
        if (AST instanceof XPathParser.EvalContext) {
            return parse(null, ((XPathParser.EvalContext) AST).absolutePath());
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
                // need to get all children of the root/DOMElement
                // perform child/rp on each child and then descendants
                Set<Node> results = new LinkedHashSet<>(); // Ensuring uniqueness

                for (Node n : getAllDescendants(DOMTree.getDocumentElement())) {
                     results.addAll(parse(n, ((XPathParser.AbsolutePathContext) AST).relativePath()));
                }

                return new ArrayList<>(results);
            }
        }
        return null;
    }

    private static List<Node> parseRelativePath(Node DOMElement, ParseTree AST) {

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
                    if (DOMElement.getNodeType() == Node.ELEMENT_NODE) {
                        for (Node node : getChildren((Element) DOMElement)) {
                            if (node.getNodeName().equals(child.getText())) {
                                result.add(node);
                            }
                        }
                    }
                } else {
                    switch (child.getText()) {
                        case "*": {

                            result.addAll(getChildren((Element) DOMElement));
                            break;
                        }
                        case ".": {
                            result.add(DOMElement);
                            break;
                        }
                        case "..": {
                            result.add(DOMElement.getParentNode());
                            break;
                        }
                        case "text()": {
                            // will be of type TEXT_NODE
                            result.add(DOMElement.getFirstChild());
                            break;
                        }
                    }
                }
                break;
            }
            case 2: {
                ParseTree child = AST.getChild(1);
                if (child instanceof XPathParser.AttributeNameContext) {
                    // implement attribute helper function
//                    result.addAll((Collection<? extends Node>) DOMElement.getAttributeNode(child.getText()));
                    System.out.println(DOMElement.getAttributes().toString());
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
                            for (Node node : parse(DOMElement, rp1)) {
//                                if (node.getNodeType() == Node.ELEMENT_NODE ) {
                                    uniqueNodes.addAll(parse((Element) node, rp2));
//                                }
                            }

                            result.addAll(uniqueNodes);
                            break;
                        }
                        case "//": {
                            Set<Node> uniqueNodes = new LinkedHashSet<>(); // Ensuring uniqueness

                            for (Node descendant : getAllDescendants((Element) DOMElement)) {
                                uniqueNodes.addAll(parse((Element) descendant, rp2));
                            }

                            result.addAll(uniqueNodes);
                            break;
                        }
                        case ",": {
                            result.addAll(parse(DOMElement, rp1));
                            result.addAll(parse(DOMElement, rp2));
                            break;
                        }
                    }
                } else {
                    result.addAll(parse(DOMElement, AST.getChild(1)));
                }
                break;
            }
            case 4: {
                ParseTree rp = AST.getChild(0);
                ParseTree filter = AST.getChild(2);

                if (rp instanceof XPathParser.RelativePathContext && filter instanceof XPathParser.FilterContext) {
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
                            List<Node> rp1Nodes = parseRelativePath(DOMElement, rp1);
                            List<Node> rp2Nodes = parseRelativePath(DOMElement, rp2);
                            for (Node n1 : rp1Nodes) {
                                for (Node n2 : rp2Nodes) {
                                    if (n1.isEqualNode(n2)) {
                                        return true;
                                    }
                                }
                            }
                        }
                        case "==":
                        case "is": {
                            List<Node> rp1Nodes = parseRelativePath(DOMElement, rp1);
                            List<Node> rp2Nodes = parseRelativePath(DOMElement, rp2);
                            for (Node n1 : rp1Nodes) {
                                for (Node n2 : rp2Nodes) {
                                    if (n1.isSameNode(n2)) {
                                        return true;
                                    }
                                }
                            }
                        }
                    }

                } else if (child instanceof XPathParser.FilterContext) {
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
                break;
            }
            case 5: {
                ParseTree rp = AST.getChild(0);
                ParseTree string = AST.getChild(3);

                if (rp instanceof XPathParser.RelativePathContext) {
                    for (Node n : parseRelativePath(DOMElement, rp)) {
                        if (n.getTextContent().equals(string.getText())) {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    public static void main(String[] args) {
        // Step 1: Read the XPath query
        // Step 2: Extract the file name from the absolute path, and build the DOM tree of this file
        // Step 3: Process the rest of the XPath
        try {
            XPathLexer lexer = new XPathLexer(CharStreams.fromString(args[0]));
            XPathParser parser = new XPathParser(new CommonTokenStream(lexer));

            ParseTree AST = parser.eval();

            List<Node> result = parse(null, AST);
            XMLToDOMParser.exportToXML(result, "result.xml");

        } catch (Exception e) {
            e.printStackTrace();
        }
    } 
}

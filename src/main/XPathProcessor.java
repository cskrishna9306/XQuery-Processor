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

    /**
     * This function retrieves all the direct children of the node parent.
     * The node parent will always be of type Element because Text and Attr type nodes do not have children.
     * The function returns a list of children nodes encompassing Element, Text, and Attr type nodes.
     * In case of Text node, this function will return an empty list since text nodes have no children.
     * In case of Attr node, this function will return a singleton list containing a text node with the value of the attribute.
     *
     * @param parent the element to find children of
     * @return list of children nodes of parent
     */
    private static List<Node> getChildren(Node parent) {
        List<Node> childrenList = new ArrayList<>();
        NodeList children = parent.getChildNodes();

        for (int i = 0; i < children.getLength(); i++)
            childrenList.add(children.item(i));

        return childrenList;
    }

    /**
     * This function retrieves all the descendants of the current DOM node.
     * Descendants do NOT include Attr nodes, they only include Element and Text nodes.
     * If the DOM node is of type Text, then it returns an empty list.
     *
     * @param DOMElement the element to find descendants of
     * @return list of descendant nodes of DOMElement
     */
    private static List<Node> getDescendants(Element DOMElement) {
        List<Node> descendants = new ArrayList<>();

        for (Node child : getChildren(DOMElement)) {
            // skip all attribute nodes
            if (child.getNodeType() == Node.ATTRIBUTE_NODE)
                continue;

            // only include nodes of type Element and Text
            descendants.add(child);

            // recurse only on nodes of type Element
            if (child.getNodeType() == Node.ELEMENT_NODE)
                descendants.addAll(getDescendants((Element) child));
        }

        return descendants;
    }

    /**
     * Helper method to print resulting nodes
     *
     * @param nodes list of result nodes to display
     */
    private static void printNodes(List<Node> nodes) {
        for (Node node : nodes)
            System.out.println("Found Node: " + node.getNodeName() + " -> " + node.getTextContent().trim());
    }

    /**
     * Entry point function to parse and evaluate all XPath expressions.
     * This function takes a starting DOM node, and Abstract Syntax Tree (AST) as parameters.
     * Can this DOM node be of type Text and Attr as well???
     * I do NOT think so, because when calling text() we are at a parent node of type Element, and searching its children for Text.
     *
     * @param DOMElement the current DOM tree element
     * @param AST the current position in the AST
     * @return the list of nodes fitting the XPath query
     */
    public static List<Node> parse(Node DOMElement, ParseTree AST) {

        // evaluate the entry point
        if (AST instanceof XPathParser.EvalContext)
            return parse(null, ((XPathParser.EvalContext) AST).absolutePath());

        // evaluate absolute path expression
        if (AST instanceof XPathParser.AbsolutePathContext)
            return parseAbsolutePath(AST);

        // evaluate relative path expression
        if (AST instanceof XPathParser.RelativePathContext)
            return parseRelativePath(DOMElement, AST);

        return null;
    }

    /**
     * This function evaluates the absolute path expression from the root of the DOM tree.
     * The function performs 2 operations:
     *  i. Dynamically generates the DOM tree of the input XML file
     *  ii. Evaluates the XPath query over the generated DOM tree
     *
     * @param AST the current position in the AST
     * @return the list of nodes satisfying the XPath query
     */
    private static List<Node> parseAbsolutePath(ParseTree AST) {

        // retrieve and store the file name mentioned in the XPath query
        String fileName = ((XPathParser.AbsolutePathContext) AST).fileName().STRING().toString();
        // dynamically create the DOM tree for the specified XML file
        Document DOMTree = XMLToDOMParser.parse("src/main/" + fileName);

        // here we evaluate absolute path in one of 2 cases:
        //  i. "/" - evaluate at the current element (root)
        // ii. "//" - evaluate at all the descendants of the root, this includes all the Element and Text nodes from the root
        switch (AST.getChild(3).getText()) {
            case "/": {
                // the first cases operates from the root's perspective, evaluating all the children of the root
                return parse(DOMTree.getDocumentElement(), ((XPathParser.AbsolutePathContext) AST).relativePath());
            }
            case "//": {
                // the second case operates on all the root's descendants which includes the root's direct and indirect children
                Set<Node> found = new LinkedHashSet<>(); // Ensuring uniqueness
                List<Node> result = new ArrayList<>();
                // we parse over all the root's descendants satisfying the relative path
                for (Node n : getDescendants(DOMTree.getDocumentElement())) {
                    if (n.getNodeName().equals(AST.getChild(4).getText()) && !found.contains(n)) {
                        result.add(n);
                        found.add(n);
                    }
                }
                return new ArrayList<>(result);
            }
        }
        return null;
    }

    /**
     * This function evaluates the relative path expression.
     *
     * @param DOMElement the current DOM tree element
     * @param AST the current position in the AST
     * @return the list of nodes satisfying this XPath query
     */
    private static List<Node> parseRelativePath(Node DOMElement, ParseTree AST) {

        // list containing the final result
        List<Node> result = new ArrayList<>();

        // differentiate relative path cases by # of children in this AST node
        switch (AST.getChildCount()) {
            case 1: {
                ParseTree child = AST.getChild(0);
                // CASE tagName
                if (child instanceof XPathParser.TagNameContext) {
                    // search for matching Element nodes in the DOM
                    for (Node node : getChildren(DOMElement))
                        if (node.getNodeName().equals(child.getText()) && node.getNodeType() == Node.ELEMENT_NODE)
                            result.add(node);
                } else {
                    // differentiate single child AST node by its text
                    switch (child.getText()) {
                        case "*": {
                            result.addAll(getChildren(DOMElement));
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
                            // iterate over all children and return text nodes
                            for (Node node : getChildren(DOMElement))
                                if (node.getNodeType() == Node.TEXT_NODE)
                                    result.add(node);
                            break;
                        }
                    }
                }
                break;
            }
            case 2: {
                // the attribute case
                ParseTree child = AST.getChild(1);
                if (child instanceof XPathParser.AttributeNameContext) {
                    // iterate over all children and return attribute nodes
                    for (Node node : getChildren(DOMElement))
                        // only add this node if it is an Attr node and shares the same name as the one we are searching for
                        if (node.getNodeType() == Node.ATTRIBUTE_NODE && node.getNodeName().equals(child.getText()))
                            result.add(node);
                }
                break;
            }
            case 3: {

                if (AST.getChild(0) instanceof XPathParser.RelativePathContext) {

                    // rp1 has to be an element node
                    ParseTree rp1 = AST.getChild(0);
                    ParseTree rp2 = AST.getChild(2);

                    // NOTE: Do we need any node type validation here?
                    switch (AST.getChild(1).getText()) {
                        case "/": {
                            // recurse only over direct children
//                            if (DOMElement.getNodeType() == Node.ELEMENT_NODE) {
                                Set<Node> uniqueNodes = new LinkedHashSet<>(); // Ensuring uniqueness
                                // first, retrieve all the children of the current DOM node satisfying rp1
                                for (Node node : parse(DOMElement, rp1))
                                    // then, we evaluate rp2 on each of the above retrieved children
                                    uniqueNodes.addAll(parse(node, rp2));

                                result.addAll(uniqueNodes);
//                            }
                            break;
                        }
                        case "//": {
                            Set<Node> found = new LinkedHashSet<>(); // Ensuring uniqueness
                            // next, we evaluate for DOMElement/descendant/rp2
                            for (Node descendant : getDescendants((Element) DOMElement)) {
                                if (descendant.getNodeName().equals(AST.getChild(2).getText()) && !found.contains(descendant)) {
                                    result.add(descendant);
                                    found.add(descendant);
                                }
                            }
                            break;
                        }
                        case ",": {
                            // case to account for simple concatenation
                            result.addAll(parse(DOMElement, rp1));
                            result.addAll(parse(DOMElement, rp2));
                            break;
                        }
                    }
                } else {
                    // simply recurse again
                    result.addAll(parse(DOMElement, AST.getChild(1)));
                }
                break;
            }
            case 4: {
                ParseTree rp = AST.getChild(0);
                ParseTree filter = AST.getChild(2);

                if (rp instanceof XPathParser.RelativePathContext && filter instanceof XPathParser.FilterContext) {
                    // first, we retrieve all children fitting DOMElement/rp
                    for (Node intermediate : parseRelativePath(DOMElement, rp))
                        // next, we evaluate the filter over each of DOMElement/rp
                        if (parseFilter((Element) intermediate, filter))
                            result.add(intermediate);
                }
            }
        }

        return result;
    }

    /**
     * This function evaluates the filter over the current DOM node.
     *
     * @param DOMElement the current DOM tree element
     * @param AST the current position in the AST
     * @return true if the filter holds at DOMElement, otherwise false
     */
    private static boolean parseFilter(Element DOMElement, ParseTree AST) {
        // can i manipulate the number of children for this AST?
        // if childcount == 1, then 1 case
        // if childcount == 2, then 1 case
        // if childcount == 3, 7 cases
        // if childcount == 5, one case
        switch (AST.getChildCount()) {
            case 1: {
                ParseTree child = AST.getChild(0);
                if (child instanceof XPathParser.RelativePathContext)
                    if (!parseRelativePath(DOMElement, child).isEmpty())
                        return true;
                break;
            }
            case 2: {
                ParseTree child = AST.getChild(1);
                if (AST.getChild(0).getText().equals("not") && child instanceof XPathParser.FilterContext) {
                    // implement not filter case
                    if (!parseFilter(DOMElement, child))
                        return true;
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

                            for (Node n1 : rp1Nodes)
                                for (Node n2 : rp2Nodes)
                                    if (n1.isEqualNode(n2))
                                        return true;
                        }
                        case "==":
                        case "is": {
                            List<Node> rp1Nodes = parseRelativePath(DOMElement, rp1);
                            List<Node> rp2Nodes = parseRelativePath(DOMElement, rp2);

                            for (Node n1 : rp1Nodes)
                                for (Node n2 : rp2Nodes)
                                    if (n1.isSameNode(n2))
                                        return true;
                        }
                    }

                } else if (child instanceof XPathParser.FilterContext) {
                    switch (AST.getChild(1).getText()) {
                        case "and":
                            return parseFilter(DOMElement, AST.getChild(0)) && parseFilter(DOMElement, AST.getChild(2));
                        case "or":
                            return parseFilter(DOMElement, AST.getChild(0)) || parseFilter(DOMElement, AST.getChild(2));
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
                    for (Node n : parseRelativePath(DOMElement, rp))
                        if (n.getTextContent().equals(string.getText()))
                            return true;
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

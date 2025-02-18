import org.antlr.v4.runtime.tree.TerminalNode;
import org.w3c.dom.*;

import java.util.*;

// ANTLR import statements
import org.antlr.v4.runtime.tree.ParseTree;

// Custom import packages
import com.example.antlr4.XQueryParser;
import com.example.antlr4.XQueryLexer;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

public class XQueryProcessor {

    private static Element makeElement(Document resultDoc, String tagName, List<Node> children) {
        System.out.println("tagName: " + tagName);
        System.out.println(resultDoc.getChildNodes());


        Element element = resultDoc.createElement(tagName);

        resultDoc.appendChild(element);

        // Import nodes into the new document and append them
        for (Node node : children) {
            Node importedNode = resultDoc.importNode(node, true);
            element.appendChild(importedNode);
        }
        System.out.println(resultDoc.getChildNodes());
        return element;
    }

    private static Element makeText(String s) {
        return null;
    }

    /**
     * Entry point function to parse and evaluate all XQuery expressions.
     * This function takes a starting DOM node, and Abstract Syntax Tree (AST) as parameters.
     * Can this DOM node be of type Text and Attr as well???
     * I do NOT think so, because when calling text() we are at a parent node of type Element, and searching its children for Text.
     *
     * @param DOMElement the current DOM tree element
     * @param AST the current position in the AST
     * @return the list of nodes fitting the XQuery query
     */
    public static List<Node> parse(Document resultDoc, Node DOMElement, ParseTree AST) {
//        super.parse();
        // evaluate the entry point
        if (AST instanceof XQueryParser.EvalContext)
            return parse(resultDoc, DOMElement, ((XQueryParser.EvalContext) AST).xQuery());

        // evaluate XQuery expression
        if (AST instanceof XQueryParser.XQueryContext)
            return parseXQuery(resultDoc, (Element) DOMElement, AST);

        return null;
    }

    /**
     * This function evaluates the XQuery expression from the root of the DOM tree.
     * The function performs 2 operations:
     *  i. Dynamically generates the DOM tree of the input XML file
     *  ii. Evaluates the XPath query over the generated DOM tree
     *
     * Do we need a parameter reference to DOMElement, and AST for XQuery because we will only be calling XPathProcessor.parse() from the root element always?
     *
     * @param AST the current position in the AST
     * @return the list of nodes satisfying the XPath query
     */
    private static List<Node> parseXQuery(Document resultDoc, Element DOMElement, ParseTree AST) {

        // list containing the final result
        List<Node> result = new ArrayList<>();

        switch (AST.getChildCount()) {
            case 1: {
                ParseTree child = AST.getChild(0);

                if (child instanceof TerminalNode) {
                    if (((TerminalNode) child).getSymbol().getType() == XQueryLexer.VAR) {
                        System.out.println(child.getText().substring(1));
                        break;
                    } else if (((TerminalNode) child).getSymbol().getType() == XQueryLexer.STRING)
                        break;

                } else if (child instanceof XQueryParser.AbsolutePathContext)
                    result.addAll(XPathProcessor.parse(DOMElement, child));
                break;
            }
            case 2: {
                break;
            }
            case 3: {
                ParseTree separator = AST.getChild(1);

                switch (separator.getText()) {
                    case ",": {
                        // case to account for simple concatenation
                        result.addAll(parse(resultDoc, DOMElement, AST.getChild(0)));
                        result.addAll(parse(resultDoc, DOMElement, AST.getChild(2)));
                        break;
                    }
                    case "/": {
                        // recurse only over direct children
//                            if (DOMElement.getNodeType() == Node.ELEMENT_NODE) {
                        Set<Node> uniqueNodes = new LinkedHashSet<>(); // Ensuring uniqueness
                        // first, retrieve all the children of the current DOM node satisfying the XQuery
                        for (Node node : parse(resultDoc, DOMElement, AST.getChild(0)))
                            // then, we evaluate rp on each of the above retrieved children
                            uniqueNodes.addAll(XPathProcessor.parse(node, AST.getChild(2)));

                        result.addAll(uniqueNodes);
//                            }
                        break;
                    }
                    case "//": {
                        // first, include all DOMElement/rp cases
                        // Ensuring uniqueness
                        Set<Node> uniqueNodes = new LinkedHashSet<>(XPathProcessor.parse(DOMElement, AST.getChild(2)));

                        for (Node node : parse(resultDoc, DOMElement, AST.getChild(0)))
                            // next, we evaluate for DOMElement/descendant/rp
                            for (Node descendant : XPathProcessor.getDescendants((Element) node))
                                uniqueNodes.addAll(XPathProcessor.parse(descendant, AST.getChild(2)));

                        result.addAll(uniqueNodes);
                        break;
                    }
                    default: {
                        // evaluate ( xQuery )
                        if (separator instanceof XQueryParser.XQueryContext)
                            result.addAll(parse(resultDoc, DOMElement, separator));
                        break;
                    }
                }
                break;
            }
            case 9: {
                result.add(makeElement(resultDoc, AST.getChild(1).getText(), parse(resultDoc, DOMElement, AST.getChild(4))));
                break;
            }
        }

        return result;
    }

    /**
     * This function evaluates the condition over the current DOM node.
     *
     * @param DOMElement the current DOM tree element
     * @param AST the current position in the AST
     * @return true if the condition holds at DOMElement, otherwise false
     */
    private static boolean parseCondition(Document resultDoc, Element DOMElement, ParseTree AST) {

        switch (AST.getChildCount()) {
            case 2: {
                ParseTree child = AST.getChild(1);
                if (AST.getChild(0).getText().equals("not") && child instanceof XQueryParser.ConditionContext) {
                    // implement not condition case
                    if (!parseCondition(resultDoc, DOMElement, child))
                        return true;
                }
                break;
            }
            case 3: {
                ParseTree child = AST.getChild(0);

                if (child instanceof XQueryParser.XQueryContext) {

                    List<Node> xq1Nodes = parseXQuery(resultDoc, DOMElement, AST.getChild(0));
                    List<Node> xq2Nodes = parseXQuery(resultDoc, DOMElement, AST.getChild(2));

                    switch (AST.getChild(1).getText()) {
                        case "=":
                        case "eq": {

                            for (Node n1 : xq1Nodes)
                                for (Node n2 : xq2Nodes)
                                    if (n1.isEqualNode(n2))
                                        return true;
                            break;
                        }
                        case "==":
                        case "is": {

                            for (Node n1 : xq1Nodes)
                                for (Node n2 : xq2Nodes)
                                    if (n1.isSameNode(n2))
                                        return true;
                            break;
                        }
                    }
                } else if (child instanceof XQueryParser.ConditionContext) {
                    switch (AST.getChild(1).getText()) {
                        case "and":
                            return parseCondition(resultDoc, DOMElement, AST.getChild(0)) && parseCondition(resultDoc, DOMElement, AST.getChild(2));
                        case "or":
                            return parseCondition(resultDoc, DOMElement, AST.getChild(0)) || parseCondition(resultDoc, DOMElement, AST.getChild(2));
                    }
                } else if (child.getText().equals("empty(")) {
                    ParseTree xq = AST.getChild(1);
                    if (xq instanceof XQueryParser.XQueryContext)
                        return parseXQuery(resultDoc, DOMElement, child).isEmpty();
                    break;
                } else if (child.getText().equals("(")) {
                    return parseCondition(resultDoc, DOMElement, AST.getChild(1));
                }
                break;
            }
            default: {
                break;
            }
        }

        return false;
    }

}

import org.antlr.v4.runtime.tree.TerminalNode;
import org.antlr.v4.runtime.tree.TerminalNodeImpl;
import org.w3c.dom.*;

import java.util.*;

// ANTLR import statements
import org.antlr.v4.runtime.tree.ParseTree;

// Custom import packages
import com.example.antlr4.XQueryParser;
import com.example.antlr4.XQueryLexer;

public class XQueryProcessor {

    private static Element makeElement(String tagName, List<Node> children) {
//        Element element = ;
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
    public static List<Node> parse(Node DOMElement, ParseTree AST, HashMap<String, List<Node>> context) {
//        super.parse();
        // evaluate the entry point
        if (AST instanceof XQueryParser.EvalContext)
            return parse(DOMElement, ((XQueryParser.EvalContext) AST).xQuery(), context);

        // evaluate XQuery expression
        if (AST instanceof XQueryParser.XQueryContext)
            return parseXQuery((Element) DOMElement, AST, context);

        return null;
    }

    /**
     * This function evaluates the XQuery expression from the root of the DOM tree.
     * The function performs 2 operations:
     *  i. Dynamically generates the DOM tree of the input XML file
     *  ii. Evaluates the XPath query over the generated DOM tree
     *
     * @param AST the current position in the AST
     * @return the list of nodes satisfying the XPath query
     */
    private static List<Node> parseXQuery(Element DOMElement, ParseTree AST, HashMap<String, List<Node>> context) {
        HashMap<String, List<Node>> newContext = new HashMap<>();
        for (Map.Entry<String, List<Node>> entry : context.entrySet()) {
            newContext.put(entry.getKey(), new ArrayList<>(entry.getValue()));
        }
        // list containing the final result
        List<Node> result = new ArrayList<>();

        switch (AST.getChildCount()) {
            // Case for:
            // [Var]
            // [StringConst]
            // [AbsolutePath]
            case 1: {
                ParseTree child = AST.getChild(0);

                if (child instanceof TerminalNode) {
                    if (((TerminalNode) child).getSymbol().getType() == XQueryLexer.VAR) {
                        System.out.println(child.getText().substring(1));
                        System.out.println("Value: " + context.get(child.getText().substring(1)));
                        return context.get(child.getText().substring(1));
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
                        result.addAll(parse(DOMElement, AST.getChild(0), newContext));
                        result.addAll(parse(DOMElement, AST.getChild(2), newContext));
                        break;
                    }
                    case "/": {
                        // recurse only over direct children
//                            if (DOMElement.getNodeType() == Node.ELEMENT_NODE) {
                        Set<Node> uniqueNodes = new LinkedHashSet<>(); // Ensuring uniqueness
                        // first, retrieve all the children of the current DOM node satisfying the XQuery
                        for (Node node : parse(DOMElement, AST.getChild(0), newContext))
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

                        for (Node node : parse(DOMElement, AST.getChild(0), newContext))
                            // next, we evaluate for DOMElement/descendant/rp
                            for (Node descendant : XPathProcessor.getDescendants((Element) node))
                                uniqueNodes.addAll(XPathProcessor.parse(descendant, AST.getChild(2)));

                        result.addAll(uniqueNodes);
                        break;
                    }
                    default: {
                        // evaluate ( xQuery )
                        if (separator instanceof XQueryParser.XQueryContext)
                            result.addAll(parse(DOMElement, separator, newContext));
                        break;
                    }
                }

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
    private static boolean parseCondition(Element DOMElement, ParseTree AST, HashMap<String, List<Node>> context) {
        HashMap<String, List<Node>> newContext = new HashMap<>();
        for (Map.Entry<String, List<Node>> entry : context.entrySet()) {
            newContext.put(entry.getKey(), new ArrayList<>(entry.getValue()));
        }
        switch (AST.getChildCount()) {
            case 2: {
                ParseTree child = AST.getChild(1);
                if (AST.getChild(0).getText().equals("not") && child instanceof XQueryParser.ConditionContext) {
                    // implement not condition case
                    if (!parseCondition(DOMElement, child, newContext))
                        return true;
                }
                break;
            }
            case 3: {
                ParseTree child = AST.getChild(0);

                if (child instanceof XQueryParser.XQueryContext) {

                    List<Node> xq1Nodes = parseXQuery(DOMElement, AST.getChild(0), newContext);
                    List<Node> xq2Nodes = parseXQuery(DOMElement, AST.getChild(2), newContext);

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
                            return parseCondition(DOMElement, AST.getChild(0), newContext) && parseCondition(DOMElement, AST.getChild(2), newContext);
                        case "or":
                            return parseCondition(DOMElement, AST.getChild(0), newContext) || parseCondition(DOMElement, AST.getChild(2), newContext);
                    }
                } else if (child.getText().equals("empty(")) {
                    ParseTree xq = AST.getChild(1);
                    if (xq instanceof XQueryParser.XQueryContext)
                        return parseXQuery(DOMElement, child, newContext).isEmpty();
                    break;
                } else if (child.getText().equals("(")) {
                    return parseCondition(DOMElement, AST.getChild(1), newContext);
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

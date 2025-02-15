import org.w3c.dom.*;

import java.util.*;

// ANTLR import statements
import org.antlr.v4.runtime.tree.ParseTree;

// Custom import packages
import com.example.antlr4.XQueryParser;

public class XQueryProcessor {

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
    public static List<Node> parse(Node DOMElement, ParseTree AST) {

        // evaluate the entry point
        if (AST instanceof XQueryParser.EvalContext)
            return parse(DOMElement, ((XQueryParser.EvalContext) AST).xQuery());

        // evaluate XQuery expression
        if (AST instanceof XQueryParser.XQueryContext)
            return parseXQuery((Element) DOMElement, AST);

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
    private static List<Node> parseXQuery(Element DOMElement, ParseTree AST) {
        ParseTree child = AST.getChild(0);

        if (child instanceof XQueryParser.AbsolutePathContext) {
            return XPathProcessor.parse(DOMElement, child);
        }

        return null;
    }

    /**
     * This function evaluates the condition over the current DOM node.
     *
     * @param DOMElement the current DOM tree element
     * @param AST the current position in the AST
     * @return true if the condition holds at DOMElement, otherwise false
     */
    private static boolean parseCondition(Element DOMElement, ParseTree AST) {

        switch (AST.getChildCount()) {
            case 2: {
                ParseTree child = AST.getChild(1);
                if (AST.getChild(0).getText().equals("not") && child instanceof XQueryParser.ConditionContext) {
                    // implement not condition case
                    if (!parseCondition(DOMElement, child))
                        return true;
                }
                break;
            }
            case 3: {
                ParseTree child = AST.getChild(0);

                if (child instanceof XQueryParser.XQueryContext) {

                    List<Node> xq1Nodes = parseXQuery(DOMElement, AST.getChild(0));
                    List<Node> xq2Nodes = parseXQuery(DOMElement, AST.getChild(2));

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
                            return parseCondition(DOMElement, AST.getChild(0)) && parseCondition(DOMElement, AST.getChild(2));
                        case "or":
                            return parseCondition(DOMElement, AST.getChild(0)) || parseCondition(DOMElement, AST.getChild(2));
                    }
                } else if (child.getText().equals("empty(")) {
                    ParseTree xq = AST.getChild(1);
                    if (xq instanceof XQueryParser.XQueryContext)
                        return parseXQuery(DOMElement, child).isEmpty();
                    break;
                } else if (child.getText().equals("(")) {
                    return parseCondition(DOMElement, AST.getChild(1));
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

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
        return false;
    }

}

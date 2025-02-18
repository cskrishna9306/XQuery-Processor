import org.antlr.v4.runtime.tree.TerminalNode;
import org.w3c.dom.*;

import java.util.*;

// ANTLR import statements
import org.antlr.v4.runtime.tree.ParseTree;

// Custom import packages
import com.example.antlr4.XQueryParser;
import com.example.antlr4.XQueryLexer;

public class XQueryProcessor {

    private final Node DOMElement;
    private final Document resultDocument;

    public XQueryProcessor(Node DOMElement, Document resultDocument) {
        this.DOMElement = DOMElement;
        this.resultDocument = resultDocument;
    }

    private Element makeElement(String tagName, List<Node> children) {

        if (this.resultDocument.getDocumentElement() != null)
            this.resultDocument.removeChild(this.resultDocument.getDocumentElement());

        Element element = this.resultDocument.createElement(tagName);
        this.resultDocument.appendChild(element);

        // Import nodes into the new resultDocument and append them
        for (Node node : children) {
            Node importedNode = this.resultDocument.importNode(node, true);
            element.appendChild(importedNode);
        }

        return element;
    }

    private Text makeText(String s) {
        return this.resultDocument.createTextNode(s);
    }

    /**
     * Entry point function to parse and evaluate all XQuery expressions.
     * This function takes a starting DOM node, and Abstract Syntax Tree (AST) as parameters.
     * Can this DOM node be of type Text and Attr as well???
     * I do NOT think so, because when calling text() we are at a parent node of type Element, and searching its children for Text.
     *
     * @param AST the current position in the AST
     * @return the list of nodes fitting the XQuery query
     */
    public List<Node> parse(ParseTree AST, HashMap<String, List<Node>> context) {
//        super.parse();
        // evaluate the entry point
        if (AST instanceof XQueryParser.EvalContext)
            return parse(((XQueryParser.EvalContext) AST).xQuery(), context);

        // evaluate XQuery expression
        if (AST instanceof XQueryParser.XQueryContext)
            return parseXQuery(AST, context);

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
    private List<Node> parseXQuery(ParseTree AST, HashMap<String, List<Node>> context) {

        // list containing the final result
        List<Node> result = new ArrayList<>();

        switch (AST.getChildCount()) {
            case 1: {
                ParseTree child = AST.getChild(0);

                if (child instanceof TerminalNode) {
                    if (((TerminalNode) child).getSymbol().getType() == XQueryLexer.VAR)
                        result.addAll(context.get(child.getText().substring(1)));
                    else if (((TerminalNode) child).getSymbol().getType() == XQueryLexer.STRING)
                        result.add(makeText(child.getText().substring(1, child.getText().length() - 1)));
                } else if (child instanceof XQueryParser.AbsolutePathContext)
                    result.addAll(XPathProcessor.parse(this.DOMElement, child));
                break;
            }
            case 2: {
                ParseTree letClause = AST.getChild(0);
                ParseTree xQuery = AST.getChild(1);

                // Step 1: Create a new context as a copy of the original one
                HashMap<String, List<Node>> newContext = new HashMap<>();
                // Clone list to prevent modifications
                for (Map.Entry<String, List<Node>> entry : context.entrySet())
                    newContext.put(entry.getKey(), new ArrayList<>(entry.getValue()));

                // Step 2: Build new context with let clause
                // letClause.getChild(i) is the variable name
                // letClause.getChild(i + 2) is the xQuery
                for (int i = 1; i < letClause.getChildCount(); i += 4)
                    newContext.put(letClause.getChild(i).getText().substring(1),
                            parse(letClause.getChild(i + 2), newContext));

                // Step 3: Evaluate the XQuery following the let clause on the new context
                result.addAll(parse(xQuery, newContext));
                break;
            }
            case 3: {
                ParseTree separator = AST.getChild(1);

                switch (separator.getText()) {
                    case ",": {
                        // case to account for simple concatenation
                        result.addAll(parse(AST.getChild(0), context));
                        result.addAll(parse(AST.getChild(2), context));
                        break;
                    }
                    case "/": {
                        // recurse only over direct children
                        Set<Node> uniqueNodes = new LinkedHashSet<>(); // Ensuring uniqueness
                        // first, retrieve all the children of the current DOM node satisfying the XQuery
                        for (Node node : parse(AST.getChild(0), context))
                            // then, we evaluate rp on each of the above retrieved children
                            uniqueNodes.addAll(XPathProcessor.parse(node, AST.getChild(2)));

                        result.addAll(uniqueNodes);
                        break;
                    }
                    case "//": {
                        // first, include all DOMElement/rp cases
                        // Ensuring uniqueness
                        Set<Node> uniqueNodes = new LinkedHashSet<>(XPathProcessor.parse(DOMElement, AST.getChild(2)));

                        for (Node node : parse(AST.getChild(0), context))
                            // next, we evaluate for DOMElement/descendant/rp
                            for (Node descendant : XPathProcessor.getDescendants((Element) node))
                                uniqueNodes.addAll(XPathProcessor.parse(descendant, AST.getChild(2)));

                        result.addAll(uniqueNodes);
                        break;
                    }
                    default: {
                        // evaluate ( xQuery )
                        if (separator instanceof XQueryParser.XQueryContext)
                            result.addAll(parse(separator, context));
                        break;
                    }
                }
                break;
            }
            case 9: {
                result.add(makeElement(AST.getChild(1).getText(), parse(AST.getChild(4), context)));
                break;
            }
        }

        return result;
    }

    /**
     * This function evaluates the condition over the current DOM node.
     *
     * @param AST the current position in the AST
     * @return true if the condition holds at DOMElement, otherwise false
     */
    private boolean parseCondition(ParseTree AST, HashMap<String, List<Node>> context) {

        switch (AST.getChildCount()) {
            case 2: {
                ParseTree child = AST.getChild(1);
                if (AST.getChild(0).getText().equals("not") && child instanceof XQueryParser.ConditionContext) {
                    // implement not condition case
                    if (!parseCondition(child, context))
                        return true;
                }
                break;
            }
            case 3: {
                ParseTree child = AST.getChild(0);

                if (child instanceof XQueryParser.XQueryContext) {

                    List<Node> xq1Nodes = parseXQuery(AST.getChild(0), context);
                    List<Node> xq2Nodes = parseXQuery(AST.getChild(2), context);

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
                            return parseCondition(AST.getChild(0), context) && parseCondition(AST.getChild(2), context);
                        case "or":
                            return parseCondition(AST.getChild(0), context) || parseCondition(AST.getChild(2), context);
                    }
                } else if (child.getText().equals("empty(")) {
                    ParseTree xq = AST.getChild(1);
                    if (xq instanceof XQueryParser.XQueryContext)
                        return parse(child, context).isEmpty();
                    break;
                } else if (child.getText().equals("(")) {
                    return parseCondition(AST.getChild(1), context);
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

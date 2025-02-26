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

    private List<Node> searchFor(ParseTree forClause, ParseTree letClause, ParseTree whereClause, ParseTree returnClause, HashMap<String, List<Node>> context, int i) {
        // Base case: end $var in xQuery, evaluate the FLWR expression at leaf
        System.out.println("-------------NEW LEVEL-------------");
        System.out.println("search: " + i + " text: " + forClause.getChild(i));
        if (i >= forClause.getChildCount() - 2) {
            System.out.println("LEAF: " + i + " text: " + forClause.getChild(i - 4));
            return evalFor(context, letClause, whereClause, returnClause);
        }

        List<Node> result = new ArrayList<>();
        ParseTree var = forClause.getChild(i);
        ParseTree xQuery = forClause.getChild(i + 2);
        System.out.println("var: " + var.getText() + " xQuery: " + xQuery.getText());
        // Create a new context as a copy of the original one
        HashMap<String, List<Node>> newContext = new HashMap<>();
        // Clone list to prevent modifications
        for (Map.Entry<String, List<Node>> entry : context.entrySet())
            newContext.put(entry.getKey(), new ArrayList<>(entry.getValue()));
        String key = var.getText().substring(1);
        List<Node> values = parse(xQuery, context);
        System.out.println("values: " + values + " context: " + context);
        for (Node value : values) {
            List<Node> valueList =  new ArrayList<>(Collections.singletonList(value));
            newContext.put(key, valueList);
            System.out.println("next lvl: " + i + " text: " + forClause.getChild(i + 4).getText());
            // Goes to next layer of $var in xQuery
            result.addAll(searchFor(forClause, letClause, whereClause, returnClause, newContext, i + 4));
        }
        newContext.put(var.getText().substring(1),
                parse(xQuery, context));
        return result;
    }

    private List<Node> evalFor(HashMap<String, List<Node>> context, ParseTree letClause, ParseTree whereClause, ParseTree returnClause){
        // Evaluate FOR expression
        // Create a new context as a copy of the original one
        HashMap<String, List<Node>> newContext = new HashMap<>();
        // Clone list to prevent modifications
        for (Map.Entry<String, List<Node>> entry : context.entrySet())
            newContext.put(entry.getKey(), new ArrayList<>(entry.getValue()));
        List<Node> result = new ArrayList<>();
        // Step 2: Evaluate the let clause (Optional)
        if (letClause != null) {
            for (int j = 1; j < letClause.getChildCount(); j += 4) {
                String key = letClause.getChild(j).getText().substring(1);
                List<Node> value =  parse(letClause.getChild(j + 2), newContext);
                System.out.println("Adding Context: " + letClause.getChild(j).getText().substring(1) + " value: " + value);
                newContext.put(key, value);
            }
        }

        // Step 3: Evaluate the where clause
        boolean conditionFlag = true;
        if (whereClause != null) {
            conditionFlag = parseCondition(whereClause.getChild(1), newContext);
        }
        // Step 4: Evaluate return clause
        if (conditionFlag) {
            result.addAll(parse(returnClause.getChild(1), newContext));
        }
        return result;
    }

    private List<Node> evalFLWR(ParseTree AST, HashMap<String, List<Node>> context){
        // Evaluate FLWR expression
        System.out.println("FLWR expression");
        List<Node> result = new ArrayList<>();
        ParseTree forClause = AST.getChild(0);;
        ParseTree letClause = null;
        ParseTree whereClause = null;
        ParseTree returnClause = null;
        if (AST.getChildCount() == 2) {
            returnClause = AST.getChild(1);
        } else if (AST.getChildCount() == 3) {
            if (AST.getChild(1) instanceof XQueryParser.LetClauseContext) {
                letClause = AST.getChild(1);
            } else {
                whereClause = AST.getChild(1);
            }
            returnClause = AST.getChild(2);
        } else if (AST.getChildCount() == 4) {
            letClause = AST.getChild(1);
            whereClause = AST.getChild(2);
            returnClause = AST.getChild(3);
        };

        if (forClause != null) {
            System.out.print("forClause: " + forClause.getText() + " ");
        }
        if (letClause != null) {
            System.out.print("letClause: " + letClause.getText() + " ");
        }
        if (whereClause != null) {
            System.out.print("whereClause: " + whereClause.getText() + " ");
        }
        if (returnClause != null) {
            System.out.print("returnClause: " + returnClause.getText());
        }

        result.addAll(searchFor(forClause, letClause, whereClause, returnClause,context, 1));

//        // Step 1: Evaluate the for clause
//        for (int i = 1; i < forClause.getChildCount() - 2; i += 4) {
//
//
//
//
//
////            result.addAll(evalFor(AST, context, letClause, whereClause, returnClause));
//
//        }
        return result;
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
     *
     * @param AST the current position in the AST
     * @return the list of nodes satisfying the XPath query
     */
    private List<Node> parseXQuery(ParseTree AST, HashMap<String, List<Node>> context) {

        // list containing the final result
        List<Node> result = new ArrayList<>();
        System.out.println("AT: " + AST.getText() + " count: " + AST.getChildCount());
        if (AST.getChild(0) instanceof XQueryParser.ForClauseContext){
            result.addAll(evalFLWR(AST, context));
        }
        switch (AST.getChildCount()) {
            case 1: {
                ParseTree child = AST.getChild(0);
                if (child instanceof TerminalNode) {
                    if (((TerminalNode) child).getSymbol().getType() == XQueryLexer.VAR) {
                        String key = child.getText().substring(1);
                        List<Node> value = context.get(key);
                        System.out.println("key: " + key + "found value: " + value);
                        if (value != null) {
                            result.addAll(value);
                        }
                    }
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
                        for (Node node : parse(AST.getChild(0), context)) {
                            System.out.println("//: " + " left: " + node + " right: " + AST.getChild(2).getText());
//                            System.out.println("//desc: " + XPathProcessor.getDescendants((Element) node));
                            // next, we evaluate for DOMElement/descendant/rp
                            for (Node descendant : XPathProcessor.getDescendants((Element) node)) {
                                uniqueNodes.addAll(XPathProcessor.parse(descendant, AST.getChild(2)));
                                if (descendant.getNodeName().equals("SCENE")) {
                                    System.out.println("//desc found: " + XPathProcessor.parse(descendant, AST.getChild(2)));
                                    System.out.println("//desc: " + uniqueNodes);
                                }
                            }
                        }
                        System.out.println("// found: " + uniqueNodes);
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
            default: {
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
                // Evaluate the "some VAR in XQuery satisfies condition" rule

                // Step 1: Create a new context as a copy of the original one
                HashMap<String, List<Node>> newContext = new HashMap<>();
                // Clone list to prevent modifications
                for (Map.Entry<String, List<Node>> entry : context.entrySet())
                    newContext.put(entry.getKey(), new ArrayList<>(entry.getValue()));

                // Step 2: Build new context with some clause
                // letClause.getChild(i) is the variable name
                // letClause.getChild(i + 2) is the xQuery
                for (int i = 1; i < AST.getChildCount() - 2; i += 4)
                    newContext.put(AST.getChild(i).getText().substring(1),
                            parse(AST.getChild(i + 2), newContext));

                // Step 3: Evaluate the new context against the specified condition
                if (parseCondition(AST.getChild(AST.getChildCount() - 1), newContext))
                    return true;

                break;
            }
        }

        return false;
    }

}

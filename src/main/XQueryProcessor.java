import org.antlr.v4.runtime.tree.TerminalNode;
import org.w3c.dom.*;

import java.io.File;
import java.util.*;

// ANTLR import statements
import org.antlr.v4.runtime.tree.ParseTree;

// Custom import packages
import com.example.antlr4.XQueryParser;
import com.example.antlr4.XQueryLexer;

public class XQueryProcessor {

    private final Node DOMElement;
    private final Document resultDocument;
    private final File rewriteFile;

    /**
     * Constructor for the XQuery Processor class.
     *
     * @param DOMElement the root element of the DOM tree
     * @param resultDocument the document object for the resulting XML
     */
    public XQueryProcessor(Node DOMElement, File rewriteFile, Document resultDocument) {
        this.DOMElement = DOMElement;
        this.rewriteFile = rewriteFile;
        this.resultDocument = resultDocument;
    }

    /**
     * This function makes the element node with the provided parameters.
     *
     * @param tagName the tag name for the node being created
     * @param children the children of this element
     * @return the created element node with the provided tag name and children
     */
    private Element makeElement(String tagName, List<Node> children) {

        // clear the document of its root if it exists (reset)
        if (this.resultDocument.getDocumentElement() != null)
            this.resultDocument.removeChild(this.resultDocument.getDocumentElement());

        // create the new root element
        Element element = this.resultDocument.createElement(tagName);
        this.resultDocument.appendChild(element);

        // Import nodes into the new resultDocument and append them
        for (Node node : children) {
            Node importedNode = this.resultDocument.importNode(node, true);
            element.appendChild(importedNode);
        }

        return element;
    }

    /**
     * This element creates and returns a text node.
     *
     * @param s the string of the text node
     * @return the text node
     */
    private Text makeText(String s) {
        return this.resultDocument.createTextNode(s);
    }

    private List<Node> parseFLWR(ParseTree forClause, ParseTree letClause, ParseTree whereClause, ParseTree returnClause, HashMap<String, List<Node>> context, int i) {

        // Base case: end $var in xQuery, evaluate the FLWR expression at leaf
        if (i >= forClause.getChildCount() - 2)
            return evalFLWR(context, letClause, whereClause, returnClause);

        // list of result nodes
        List<Node> result = new ArrayList<>();

        ParseTree var = forClause.getChild(i);
        ParseTree xQuery = forClause.getChild(i + 2);

        // Create a new context as a copy of the original one
        HashMap<String, List<Node>> newContext = new HashMap<>();
        // Clone list to prevent modifications
        for (Map.Entry<String, List<Node>> entry : context.entrySet())
            newContext.put(entry.getKey(), new ArrayList<>(entry.getValue()));

        String key = var.getText().substring(1);
        for (Node value : parse(xQuery, newContext)) {
            List<Node> valueList =  new ArrayList<>(Collections.singletonList(value));
            newContext.put(key, valueList);
            // Recursively traverses to next layer of $var in xQuery
            result.addAll(parseFLWR(forClause, letClause, whereClause, returnClause, newContext, i + 4));
        }

        // what is this?
        newContext.put(var.getText().substring(1),
                parse(xQuery, context));

        return result;
    }

    private List<Node> evalFLWR(HashMap<String, List<Node>> context, ParseTree letClause, ParseTree whereClause, ParseTree returnClause){

        // list of result nodes
        List<Node> result = new ArrayList<>();

        // Evaluate FOR expression
        // Create a new context as a copy of the original one
        HashMap<String, List<Node>> newContext = new HashMap<>();
        // Clone list to prevent modifications
        for (Map.Entry<String, List<Node>> entry : context.entrySet())
            newContext.put(entry.getKey(), new ArrayList<>(entry.getValue()));

        // Check to see if the let clause exists
        if (letClause.getChildCount() != 0) {
            // Step 2: Evaluate the let clause
            for (int i = 1; i < letClause.getChildCount(); i += 4)
                newContext.put(letClause.getChild(i).getText().substring(1),
                        parse(letClause.getChild(i + 2), newContext));
        }

        // Check to see if the where clause exists
        if (whereClause.getChildCount() != 0) {
            // Step 3: Evaluate the where clause
            if (parseCondition(whereClause.getChild(1), newContext))
                // Step 4: Evaluate return clause
                result.addAll(parse(returnClause.getChild(1), newContext));
        } else
            // Step 4: Evaluate return clause
            result.addAll(parse(returnClause.getChild(1), newContext));

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

        return new ArrayList<>();
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

        switch (AST.getChildCount()) {
            case 1: {
                ParseTree child = AST.getChild(0);
                if (child instanceof TerminalNode) {
                    if (((TerminalNode) child).getSymbol().getType() == XQueryLexer.VAR) {
                        String key = child.getText().substring(1);
                        List<Node> value = context.get(key);
                        if (value != null) {
                            result.addAll(value);
                        }
                    } else if (((TerminalNode) child).getSymbol().getType() == XQueryLexer.STRING)
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
                        // Ensuring uniqueness
                        Set<Node> uniqueNodes = new LinkedHashSet<>(XPathProcessor.parse(DOMElement, AST.getChild(2)));
                        for (Node node : parse(AST.getChild(0), context)) {
                            // first, include all DOMElement/rp cases
                            uniqueNodes.addAll(XPathProcessor.parse(node, AST.getChild(2)));
                            // next, we evaluate for DOMElement/descendant/rp
                            for (Node descendant : XPathProcessor.getDescendants((Element) node))
                                uniqueNodes.addAll(XPathProcessor.parse(descendant, AST.getChild(2)));
                        }

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
            case 4: {
                // evaluate FLWR expression
                ParseTree forClause = AST.getChild(0);
                ParseTree letClause = AST.getChild(1);
                ParseTree whereClause = AST.getChild(2);
                ParseTree returnClause = AST.getChild(3);

                // Step 1: Rewrite the for clause with the join operator
                // This hash map represents the root parent of the variable
                HashMap<String, String> dependency = new HashMap<>();
                // This hash map represents the disjoint set of connected components rooted at the key
//                HashMap<String, HashMap<String, List<String>>> connectedComponents = new HashMap<>();
                HashMap<String, ConnectedComponent> connectedComponents = new HashMap<>();

                // Step 1a: Build the dependency graph of all the dependant "for clause" variables
                for (int i = 1; i < forClause.getChildCount() - 2; i += 4) {
                    // Extract the variable name
                    String variable = forClause.getChild(i).getText().substring(1);

                    // Check if the XQuery is an absolute path
                    ParseTree xQuery = forClause.getChild(i + 2).getChild(0);
                    if (xQuery instanceof XQueryParser.AbsolutePathContext) {
                        // Update the connected components hash map
//                        connectedComponents.put(variable, new HashMap<String, List<String>>() {{
//                            put("Variable Group", new ArrayList<>());
//                            put("Filters", new ArrayList<>());
//                            put("Join Operator", new ArrayList<>());
//                        }});
                        connectedComponents.put(variable, new ConnectedComponent(variable));
                        // Update the dependency hash map
                        dependency.put(variable, variable);
                    } else if (xQuery instanceof XQueryParser.XQueryContext) {
                        // Add the dependency to its respective connected component
                        String dependentVariable = xQuery.getText().substring(1);
                        // Update the connected components hash map
//                        connectedComponents.get(dependentVariable).get("Variable Group").add(variable);
                        connectedComponents.get(dependentVariable).addVariable(variable);
                        // Update the dependency hash map
                        dependency.put(variable, dependency.get(dependentVariable));
                    }
                }

                // Step 1b: Now, evaluate the where clause to determine the joins across the connected components
                // Since we have the connected components, determine the attributes participating in the join

                // Maintain a queue of the conditions
                Queue<ParseTree> queue = new LinkedList<>();
                // we observe the conditions from the parent
                queue.add(whereClause.getChild(1));

                while (!queue.isEmpty()) {
                    ParseTree parent = queue.remove();
                    ParseTree child1 = parent.getChild(0);
                    ParseTree child2 = parent.getChild(2);

                    // Base case: Both are XQuery (if child 1 is XQuery then child 2 is also XQuery)
                    if (child1 instanceof XQueryParser.XQueryContext) {
                        String variable = child1.getChild(0).getText().substring(1);
                        TerminalNode operator = (TerminalNode) child2.getChild(0);

                        if (operator.getSymbol().getType() == XQueryLexer.VAR) {
//                            connectedComponents
//                                    .get(dependency.get(variable))
//                                    .get("Join Operator")
//                                    .add(variable);
                            connectedComponents
                                    .get(dependency.get(variable))
                                    .addJoin(new AbstractMap.SimpleEntry<>(variable, operator.getText().substring(1)));
//                            connectedComponents
//                                    .get(dependency.get(operator.getText().substring(1)))
//                                    .get("Join Operator")
//                                    .add(operator.getText().substring(1));
                            connectedComponents
                                    .get(dependency.get(operator.getText().substring(1)))
                                    .addJoin(new AbstractMap.SimpleEntry<>(operator.getText().substring(1), variable));
                        } else if (operator.getSymbol().getType() == XQueryLexer.STRING)
//                            connectedComponents
//                                    .get(dependency.get(variable))
//                                    .get("Filters")
//                                    .add(variable);
                            connectedComponents
                                    .get(dependency.get(variable))
                                    .addFilter(variable);
                        continue;
                    }

                    // Recursive Case: Both are conditions (if child 1 is condition then child 2 also has to be condition)
                    queue.add(child1);
                    queue.add(child2);
                }

                System.out.println(connectedComponents);

                // Step 2: Use hash-join for an optimized join operation

                result.addAll(parseFLWR(forClause, letClause, whereClause, returnClause, context, 1));
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
                    ParseTree xQuery = AST.getChild(1);
                    if (xQuery instanceof XQueryParser.XQueryContext)
                        return parse(xQuery, context).isEmpty();
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

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.w3c.dom.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.*;
import java.util.stream.Collectors;

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

    /**
     * Function to retrieve text content of a **direct child** element.
     *
     * @param parent the parent element
     * @param tagName the tag name to be searched for in this parent's children
     * @return the text within the parent's relevant children, otherwise empty string
     */
    public String getDirectChildText(Element parent, String tagName) {
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE && node.getNodeName().equals(tagName))
                // Return direct child's text
                return node.getTextContent().trim();
        }
        return ""; // Default if not found
    }

    /**
     * This function implements the optimized hash-join operation.
     *
     * @param joinClause the join clause to evaluate
     * @param context the current context
     * @return the list of joined tuples based on the specified join operations
     */
    private List<Node> parseJoin(ParseTree joinClause, HashMap<String, List<Node>> context) {

        // List of result nodes
        List<Node> result = new ArrayList<>();

        // Extract the tuples from the first join operand
        List<Node> list1 = parse(joinClause.getChild(1), context);
        // Extract the tuples from the second join operand
        List<Node> list2 = parse(joinClause.getChild(3), context);

        // extract the join attributes from the first join operand
        List<String> attributeList1 = Arrays.stream(joinClause.getChild(5)
                                            .getText()
                                            .replaceAll("[\\[\\]]", "")
                                            .trim()
                                            .split("\\s*,\\s*"))
                                            .collect(Collectors.toList());
        // extract the join attributes from the second join operand
        List<String> attributeList2 = Arrays.stream(joinClause.getChild(7)
                                            .getText()
                                            .replaceAll("[\\[\\]]", "")
                                            .trim()
                                            .split("\\s*,\\s*"))
                                            .collect(Collectors.toList());

        // Check if the join attributes are valid
        if (attributeList1.size() != attributeList2.size()) {
            System.err.println("Join attributes mismatch");
            return result;
        }

        // Configure the first list to be the smaller of the 2 lists
        if (list1.size() > list2.size()) {
            // Swap the 2 join operands
            List<Node> tempList = list1;
            list1 = list2;
            list2 = tempList;
            // Swap the 2 join attribute lists
            List<String> tempAttributeList = attributeList1;
            attributeList1 = attributeList2;
            attributeList2 = tempAttributeList;
        }

        // Create a hash-map for the tuples from the first join
        HashMap<List<String>, List<Node>> hashJoin = new HashMap<>();

        // Iterating through the
        for (Node tuple : list1) {
            // Construct hash key for each attribute combination using Stream API
            List<String> key = attributeList1.stream()
                    .map(attr -> getDirectChildText((Element) tuple, attr))
                    .collect(Collectors.toList());

            // Use `computeIfAbsent` for efficient insertion
            hashJoin.computeIfAbsent(key, k -> new ArrayList<>()).add(tuple);
        }

        // Iterate over the second list of tuples
        for (Node tuple : list2) {
            // Construct hash key for each attribute combination using Stream API
            List<String> key = attributeList2.stream()
                    .map(attr -> getDirectChildText((Element) tuple, attr))
                    .collect(Collectors.toList());

            // Check to see if there is a match from the first table, otherwise skip
            if (hashJoin.containsKey(key)) {
                // Join the 2 nodes and append to result
                for (Node node : hashJoin.get(key)) {
                    // Create a deep copy of tuple1
                    Element copiedTuple = (Element) node.cloneNode(true);

                    NodeList children = tuple.getChildNodes();
                    for (int i = 0; i < children.getLength(); i++)
                        copiedTuple.appendChild(children.item(i).cloneNode(true));

                    result.add(copiedTuple);
                }
            }
        }

        return result;
    }

    /**
     * This function recursively parses the nested for loops in the for clause.
     *
     * @param forClause the for clause of the FLWR expression
     * @param letClause the optional let clause of the FLWR expression
     * @param whereClause the optional where clause of the FLWR expression
     * @param returnClause the return clause of the FLWR expression
     * @param context the current context
     * @param i the positional argument we are looking at in the for clause
     * @return the list of nodes fitting the FLWR expression
     */
    private List<Node> parseFLWR(ParseTree forClause, ParseTree letClause, ParseTree whereClause, ParseTree returnClause, HashMap<String, List<Node>> context, int i) {

        // Base case: end $var in xQuery, evaluate the FLWR expression at leaf
        if (i >= forClause.getChildCount() - 2)
            return evalFLWR(letClause, whereClause, returnClause, context);

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

        return result;
    }

    /**
     * This function serves as the base case of the FLWR expression.
     * Here, we evaluate the  let, where, and return clauses with the provided context.
     *
     * @param letClause the optional let clause of the FLWR expression
     * @param whereClause the optional where clause of the FLWR expression
     * @param returnClause the return clause of the FLWR expression
     * @param context the current context to be evaluated at
     * @return the list of nodes fitting the FLWR expression
     */
    private List<Node> evalFLWR(ParseTree letClause, ParseTree whereClause, ParseTree returnClause, HashMap<String, List<Node>> context){

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
                        // evaluate if the terminal node is a variable lexer
                        String key = child.getText().substring(1);
                        List<Node> value = context.get(key);
                        if (value != null) {
                            result.addAll(value);
                        }
                    } else if (((TerminalNode) child).getSymbol().getType() == XQueryLexer.STRING)
                        // evaluate if the terminal node is a string lexer
                        result.add(makeText(child.getText().substring(1, child.getText().length() - 1)));
                } else if (child instanceof XQueryParser.AbsolutePathContext) {
                    // evaluate the absolute path
                    result.addAll(XPathProcessor.parse(this.DOMElement, child));
                } else if (child instanceof XQueryParser.JoinClauseContext) {
                    // evaluate the optimized join operation
                    result.addAll(parseJoin(child, context));
                }
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

                // Instantiate the XQuery rewriter class
                XQueryRewriter rewriter = new XQueryRewriter(forClause, whereClause, returnClause);

                // Initialize the connected components
                rewriter.setConnectedComponents();

                // Check to see if the FLWR expression requires a rewrite
                if (rewriter.isJoin()) {
                    // create the rewrite file
                    // we have to rewrite the expression into rewrite file
                    rewriter.rewrite(this.rewriteFile);

                    try {
                        // read the rewrite file, build AST on it again
                        BufferedReader br = new BufferedReader(new FileReader(this.rewriteFile));
                        String rewriteQuery = br.lines().collect(Collectors.joining("\n"));

                        XQueryLexer lexer = new XQueryLexer(CharStreams.fromString(rewriteQuery));
                        XQueryParser parser = new XQueryParser(new CommonTokenStream(lexer));

                        // build the AST for the rewritten query
                        ParseTree rewriteAST = parser.eval();
                        // parse the rewritten query
                        result.addAll(parse(rewriteAST, new HashMap<>()));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else
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

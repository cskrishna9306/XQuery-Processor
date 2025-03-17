package main;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.w3c.dom.*;
import java.io.FileWriter;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

// ANTLR import statements
import org.antlr.v4.runtime.tree.ParseTree;

// Custom import packages
import main.antlr.XQueryParser;
import main.antlr.XQueryLexer;
import main.ConnectedComponent;

public class XQueryProcessor {

    private final Node DOMElement;
    private final Document resultDocument;
    private final String rewriteFilename;

    public XQueryProcessor(Node DOMElement, Document resultDocument, String rewriteFilename) {
        this.DOMElement = DOMElement;
        this.resultDocument = resultDocument;
        this.rewriteFilename = rewriteFilename;
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

    private String rewriteJoins(LinkedHashMap<String, ConnectedComponent> connectedComponents, String returnClauseString){
        StringBuilder result = new StringBuilder();
        try {
            FileWriter myWriter = new FileWriter(rewriteFilename);
            StringBuilder attrCond1;
            StringBuilder attrCond2;
            int componentIter = 0;
            List<ConnectedComponent> components = new ArrayList<>(connectedComponents.values());

            // Store next components to join based on avaliable joins
            Queue<ConnectedComponent> queue = new LinkedList<>();
            queue.add(components.get(0));
            HashSet<List<String>> visitedJoins = new HashSet<>();
            // store joins from components that have been joined with other components
            // but still require another non visited component
            HashSet<List<String>> skippedJoins = new HashSet<>();
            // components that have been joined
            HashSet<String> visitedComponents = new HashSet<>();
            visitedComponents.add(components.get(0).getRoot());

            // Loop through all connected components
            while (!queue.isEmpty()){
                ConnectedComponent component = queue.remove();
                int numVariables = component.getVariables().size();
                // Build the for clause
                int varIter = 0; // track iterations of variables in the component for adding commas
                result.append("for ");
                for (List<String> node : component.getVariables()) {
                    result.append(node.get(1));
                    if (varIter != numVariables - 1)
                        result.append(", ");
                    result.append("\n");
                    varIter += 1;
                }

                // Build the where clause if filter for this connected component exists
                if (!component.getFilters().isEmpty()) {
                    List<List<String>> ccFilters = component.getFilters();
                    result.append("where ");
                    for (int i = 0; i < ccFilters.size(); i++) {
                        result.append("$")
                                .append(ccFilters.get(i).get(0))
                                .append(" eq ")
                                .append("\"")
                                .append(ccFilters.get(i).get(1))
                                .append("\"");
                        if (i != ccFilters.size() - 1)
                            result.append(" and ");
                        result.append("\n");
                    }
                }

                // Build the return clause
                result.append("return <tuple>{ ");
                varIter = 0;
                for (List<String> node : component.getVariables()) {
                    result.append("<" + node.get(0) + ">{$" + node.get(0) + "}</" + node.get(0) + ">");
                    if (varIter != numVariables - 1)
                        result.append(", ");
                    varIter += 1;
                }
                result.append(" }</tuple>, ");
                result.append("\n");



                if (componentIter == 0) {
                    // Add the joins of the first component, done once
                    for (AbstractMap.SimpleEntry<String, String> join : component.getJoins()) {
                        String var2 = join.getKey();
                        String var1 = join.getValue();
                        skippedJoins.add(Arrays.asList(var1, var2));
                        skippedJoins.add(Arrays.asList(var2, var1));
                    }
                } else {
                    // Build the join condition, done for all components except the first
                    attrCond1 = new StringBuilder();
                    attrCond2 = new StringBuilder();
                    attrCond1.append("[");
                    attrCond2.append("[");

                    for (AbstractMap.SimpleEntry<String, String> join : component.getJoins()) {
                        String var2 = join.getKey();
                        String var1 = join.getValue();
                        if (visitedJoins.contains(Arrays.asList(var1, var2)) || visitedJoins.contains(Arrays.asList(var2, var1)))
                            continue;
                        if (skippedJoins.contains(Arrays.asList(var1, var2)) || skippedJoins.contains(Arrays.asList(var2, var1))) {
                            attrCond2.append(join.getKey());
                            attrCond1.append(join.getValue());
                            visitedJoins.add(Arrays.asList(var1, var2));
                            visitedJoins.add(Arrays.asList(var2, var1));
                            skippedJoins.remove(Arrays.asList(var1, var2));
                            skippedJoins.remove(Arrays.asList(var2, var1));
                        } else {
                            skippedJoins.add(Arrays.asList(var1, var2));
                            skippedJoins.add(Arrays.asList(var2, var1));
                        }
                    }
                    attrCond1.append("], ");
                    attrCond2.append("])");
                    result.append(attrCond1.toString());
                    result.append(attrCond2.toString());
                    if (!skippedJoins.isEmpty()){
                        result.append(", ");
                    }
                    result.append("\n");
                }

                // Find if next component exist to join
                if (!skippedJoins.isEmpty()) {
                    List<String> newJoin = skippedJoins.iterator().next();
                    AbstractMap.SimpleEntry<String, String> joinTarget1 = new AbstractMap.SimpleEntry<>(newJoin.get(0), newJoin.get(1));
                    AbstractMap.SimpleEntry<String, String> joinTarget2 = new AbstractMap.SimpleEntry<>(newJoin.get(1), newJoin.get(0));
                    for (ConnectedComponent nextComponent : connectedComponents.values()) {
                        if (visitedComponents.contains(nextComponent.getRoot()))
                            continue;
                        if (nextComponent.getJoins().contains(joinTarget1) || nextComponent.getJoins().contains(joinTarget2)) {
                            queue.add(nextComponent);
                            visitedComponents.add(nextComponent.getRoot());
                            break;
                        }
                    }
                }
                componentIter += 1;
            }

            // Add the return clause
            result.append(returnClauseString);

            // Add the join keyword
            for(int i = 0; i < componentIter - 2; i++) {
                if (componentIter > 1) {
                    result.insert(0, "join ( \n");
                }
            }

            // add the top level join keyword
            result.insert(0, "for $tuple in join ( \n");
            myWriter.write(result.toString());
            myWriter.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return result.toString();
    }



    private List<Node> searchFor(ParseTree forClause, ParseTree letClause, ParseTree whereClause, ParseTree returnClause, HashMap<String, List<Node>> context, int i) {
        // Base case: end $var in xQuery, evaluate the FLWR expression at leaf
        if (i >= forClause.getChildCount() - 2)
            return evalFor(context, letClause, whereClause, returnClause);

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
        List<Node> list = parse(xQuery, context);
        for (Node value : list) {
            List<Node> valueList =  new ArrayList<>(Collections.singletonList(value));
            newContext.put(key, valueList);
            // Recursively traverses to next layer of $var in xQuery
            result.addAll(searchFor(forClause, letClause, whereClause, returnClause, newContext, i + 4));
        }

        // what is this?
        newContext.put(var.getText().substring(1),
                parse(xQuery, context));
        return result;
    }

    private List<Node> evalFor(HashMap<String, List<Node>> context, ParseTree letClause, ParseTree whereClause, ParseTree returnClause){
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
//                String key = letClause.getChild(i).getText().substring(1);
//                List<Node> value =  parse(letClause.getChild(i + 2), newContext);
//                newContext.put(key, value);
                newContext.put(letClause.getChild(i).getText().substring(1),
                        parse(letClause.getChild(i + 2), newContext));
        }
        // Check to see if the where clause exists
        if (whereClause.getChildCount() != 0) {
            // Step 3: Evaluate the where clause
            if (parseCondition(whereClause.getChild(1), newContext)) {
                // Step 4: Evaluate return clause
                List<Node> parseRes = parse(returnClause.getChild(1), newContext);
                result.addAll(parseRes);
            }
        } else
            // Step 4: Evaluate return clause
            result.addAll(parse(returnClause.getChild(1), newContext));
        return result;
    }

    // Function to retrieve text content of a **direct child** element
    public static String getDirectChildText(Element parent, String tagName) {
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE && node.getNodeName().equals(tagName)) {
                return node.getTextContent().trim(); // Return direct child's text
            }
        }
        return ""; // Default if not found
    }


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

        // we need to hash the attributes value
        HashMap<List<String>, List<Node>> hashJoin = new HashMap<>();
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
        if (AST instanceof XQueryParser.XQueryContext) {
            return parseXQuery(AST, context);
        }

        return new ArrayList<Node>();
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
                } else if (child instanceof XQueryParser.AbsolutePathContext){
                    result.addAll(XPathProcessor.parse(this.DOMElement, child));
                } else if (child instanceof XQueryParser.JoinClauseContext){
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
                        // first, include all DOMElement/rp cases
                        // Ensuring uniqueness
                        Set<Node> uniqueNodes = new LinkedHashSet<>(XPathProcessor.parse(DOMElement, AST.getChild(2)));
                        for (Node node : parse(AST.getChild(0), context)) {
                            // next, we evaluate for DOMElement/descendant/rp
                            List<Node> descendants = XPathProcessor.getDescendants((Element) node);
                            List<Node> foundMatchingDirectDescendents = XPathProcessor.parse(node, AST.getChild(2));
                            uniqueNodes.addAll(foundMatchingDirectDescendents);
                            for (Node descendant : descendants) {
                                    List<Node> foundMatchingDescendents = XPathProcessor.parse(descendant, AST.getChild(2));
                                    uniqueNodes.addAll(foundMatchingDescendents);
                            }
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
                LinkedHashMap<String, ConnectedComponent> connectedComponents = new LinkedHashMap<>();

                // Step 1a: Build the dependency graph of all the dependant "for clause" variables
                for (int i = 1; i < forClause.getChildCount() - 2; i += 4) {
                    // Extract the variable name
                    String variable = forClause.getChild(i).getText().substring(1);

                    // extract query string
                    String queryString = forClause.getChild(i).getText();
                    for (int j = i + 1; j < i + 3; j++) {
                        queryString = queryString.concat(" " + forClause.getChild(j).getText());
                    }

                    // Check if the XQuery is an absolute path
                    ParseTree xQuery = forClause.getChild(i + 2).getChild(0);
                    if (xQuery instanceof XQueryParser.AbsolutePathContext) {
                        // Update the connected components hash map
                        connectedComponents.put(variable, new ConnectedComponent(variable, queryString));
                        // Update the dependency hash map
                        dependency.put(variable, variable);
                    } else if (xQuery instanceof XQueryParser.XQueryContext) {
                        // Add the dependency to its respective connected component
                        String dependentVariable = xQuery.getText().substring(1);
                        // Update the connected components hash map
                        connectedComponents.get(dependentVariable).addVariable(variable, queryString);
                        // Update the dependency hash map
                        dependency.put(variable, dependency.get(dependentVariable));
                    }
                }

                // Step 1b: Now, evaluate the where clause to determine the joins across the connected components
                // Since we have the connected components, determine the attributes participating in the join

                // Maintain a queue of the conditions
                Queue<ParseTree> queue = new LinkedList<>();
                // we observe the conditions from the parent
                if (whereClause.getChild(1) != null){
                    queue.add(whereClause.getChild(1));
                }

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
                                    .addFilter(variable, operator.getText().substring(1, operator.getText().length() - 1));
                        continue;
                    }

                    // Recursive Case: Both are conditions (if child 1 is condition then child 2 also has to be condition)
                    queue.add(child1);
                    queue.add(child2);
                }

                if (connectedComponents.size() > 1) {

                    String rewriteReturnClause = returnClause.getText()
                            .replace("return", "return ")
                            .replaceAll("\\$([A-Za-z0-9_]+)", "\\$tuple/$1/*")
                            .replaceAll(",", ",\n");

                    CharStream rewriteQuery = CharStreams.fromString(rewriteJoins(connectedComponents, rewriteReturnClause));
                    XQueryLexer lexer = new XQueryLexer(rewriteQuery);
                    XQueryParser parser = new XQueryParser(new CommonTokenStream(lexer));
                    ParseTree rewriteAST = parser.eval();
                    result.addAll(parse(rewriteAST, new HashMap<String, List<Node>>()));

                }else {
                    result.addAll(searchFor(forClause, letClause, whereClause, returnClause, context, 1));
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
                    if (xq instanceof XQueryParser.XQueryContext) {
                        List<Node> parseResult = parse(xq, context);
                        return parseResult.isEmpty();
                    }
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
package main;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.w3c.dom.*;

import java.util.*;

// ANTLR import statements
import org.antlr.v4.runtime.tree.ParseTree;

// Custom import packages
import main.antlr.XQueryParser;
import main.antlr.XQueryLexer;

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

    private List<Node> parseJoin(ParseTree joinClause, HashMap<String, List<Node>> context) {
        List<Node> result = new ArrayList<>();
        List<Node> tupleList1 = parse(joinClause.getChild(1), context);
        List<Node> tupleList2 = parse(joinClause.getChild(3), context);
        List<String> attrList1 = new ArrayList<>();
        List<String> attrList2 = new ArrayList<>();
        for (int i = 0; i < joinClause.getChild(5).getChildCount(); i++ ) {
            if (((TerminalNode) joinClause.getChild(5).getChild(i)).getSymbol().getType() != XQueryLexer.TAGNAME)
                continue;
            attrList1.add(joinClause.getChild(5).getChild(i).getText());
        }
        for (int i = 0; i < joinClause.getChild(7).getChildCount(); i++ ){
            if (((TerminalNode) joinClause.getChild(7).getChild(i)).getSymbol().getType() != XQueryLexer.TAGNAME)
                continue;
            attrList2.add(joinClause.getChild(7).getChild(i).getText());
        }
        if (attrList1.size() != attrList2.size()) {
            System.out.println("Attributes in join clause are not the same length.");
            return result;
        }
        // swap tupleList1 to be the smaller list
//        if (tupleList1.size() > tupleList2.size()){
//            List<Node> temp = tupleList1;
//            tupleList1 = tupleList2;
//            tupleList2 = temp;
//            List<String> tempAttr = attrList1;
//            attrList1 = attrList2;
//            attrList2 = tempAttr;
//        }

        // iterate over all tuples in tupleList1 and tupleList2
        for (Node tuple1 : tupleList1) {
            for (Node tuple2 : tupleList2) {
                boolean isMatching = false;
                // iterate through join conditions
                String id1 = "";
                String id2 = "";
                for (int i = 0; i < attrList1.size(); i++) {
                    String attr1 = attrList1.get(i);
                    String attr2 = attrList2.get(i);
                    // check for matching attributes per tuple1 and tuple2 pair
                    for (int j = 0; j < tuple1.getChildNodes().getLength(); j++) {
//                        System.out.println("Expeected Attr: " + attr1 + " Actual Attr: " + tuple1.getChildNodes().item(j).getNodeName());
                        if (tuple1.getChildNodes().item(j).getNodeName().equals("id1")){
                            id1 = tuple1.getChildNodes().item(j).getTextContent();
                        }
                        if (!attr1.equals(tuple1.getChildNodes().item(j).getNodeName()))
                            continue;
                        for (int k = 0; k < tuple2.getChildNodes().getLength(); k++) {
                            if (tuple2.getChildNodes().item(k).getNodeName().equals("id2")){
                                id2 = tuple2.getChildNodes().item(k).getTextContent();
                            }
                            if (!attr2.equals(tuple2.getChildNodes().item(k).getNodeName()))
                                continue;
                            String attr1Value = tuple1.getChildNodes().item(j).getTextContent();
                            String attr2Value = tuple2.getChildNodes().item(k).getTextContent();
                            if (attr1Value.equals(attr2Value)) {
                                // if all attributes match, flag to merge the tuples
//                                System.out.println("Tuple " + id1 + " and Tuple " + id2 + " are matching.");
                                isMatching = true;
                            }
                        }
                    }
                }
                // if all attributes match, merge the tuples
                if (isMatching){
                    // cast to elem
//                    Element tuple1Elem = (Element) tuple1;
//                    Element tuple2Elem = (Element) tuple2;

//                    System.out.print("Tuple1" + tuple1.getChildNodes() + "Tuple2" + tuple2.getChildNodes() + "\n");

                    // merge tuple's nodes
                    //TODO: same attribute names in different tuples
                    for (int i = 0; i < tuple2.getChildNodes().getLength(); i++) {
                        Node node = tuple2.getChildNodes().item(i);
                        Node importedNode = this.resultDocument.importNode(node, true);
                        tuple1.appendChild(importedNode);
                    }

//                    // merge tuple's attributes
//                    NamedNodeMap tuple2Attr = tuple2Elem.getAttributes();
//                    for (int i = 0; i < tuple2Attr.getLength(); i++) {
//                        Attr attr = (Attr) tuple2Attr.item(i);
//                        if (!tuple1Elem.hasAttribute(attr.getName())) {
//                            // If attribute doesn't exist, add it
//                            tuple1Elem.setAttribute(attr.getName(), attr.getValue());
//                        } else {
//                            // Handle conflicts (overwrite or append values)
//                            String existingValue = tuple1Elem.getAttribute(attr.getName());
//                            tuple1Elem.setAttribute(attr.getName(), existingValue + "," + attr.getValue());
//                        }
//                    }
//                    result.add((Node) tuple1Elem);
//                    for (int j = 0; j < tuple1.getChildNodes().getLength(); j++) {
//                        System.out.println("Tuple1" + tuple1.getChildNodes().item(j).getTextContent());
//                    }
                    result.add(tuple1);
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
                result.addAll(searchFor(forClause, letClause, whereClause, returnClause,context, 1));

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
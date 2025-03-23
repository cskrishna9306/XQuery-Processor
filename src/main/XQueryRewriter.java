import org.antlr.v4.runtime.tree.TerminalNode;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

// ANTLR import statements
import org.antlr.v4.runtime.tree.ParseTree;

// Custom import packages
import com.example.antlr4.XQueryParser;
import com.example.antlr4.XQueryLexer;

public class XQueryRewriter {

    private final ParseTree forClause;
    private final ParseTree whereClause;
    private final ParseTree returnClause;
    private HashMap<String, ConnectedComponent> connectedComponents;
    private List<List<String>> joinConditions;

    public XQueryRewriter(ParseTree forClause, ParseTree whereClause, ParseTree returnClause) {
        this.forClause = forClause;
        this.whereClause = whereClause;
        this.returnClause = returnClause;
        // This hash map represents the disjoint set of connected components rooted at the key
        this.connectedComponents = new HashMap<>();
        this.joinConditions = new ArrayList<>();
    }

    public void setConnectedComponents() {
        // Step 1: Rewrite the for clause with the join operator
        // This hash map represents the root parent of the variable
        HashMap<String, String> dependency = new HashMap<>();

        // Step 1a: Build the dependency graph of all the dependant "for clause" variables
        for (int i = 1; i < this.forClause.getChildCount() - 2; i += 4) {
            // Extract the variable name
            String variable = this.forClause.getChild(i).getText().substring(1);

            // Extract query string
            StringBuilder queryString = new StringBuilder(forClause.getChild(i).getText());
            for (int j = i + 1; j < i + 3; j++)
                queryString.append(" ").append(forClause.getChild(j).getText());

            // Check if the XQuery is an absolute path
            ParseTree xQuery = this.forClause.getChild(i + 2).getChild(0);
            if (xQuery instanceof XQueryParser.AbsolutePathContext) {
                // Update the connected components hash map
                this.connectedComponents.put(variable, new ConnectedComponent(variable, queryString.toString()));
                // Update the dependency hash map
                dependency.put(variable, variable);
            } else if (xQuery instanceof XQueryParser.XQueryContext) {
                // Add the dependency to its respective connected component
                String dependentVariable = xQuery.getText().substring(1);
                // Update the connected components hash map
                this.connectedComponents.get(dependentVariable).addVariable(variable, queryString.toString());
                // Update the dependency hash map
                dependency.put(variable, dependency.get(dependentVariable));
            }
        }

        // Step 1b: Now, evaluate the where clause to determine the joins across the connected components
        // Since we have the connected components, determine the attributes participating in the join

        // Maintain a queue of the conditions
        Queue<ParseTree> queue = new LinkedList<>();
        // we observe the conditions from the parent
        queue.add(this.whereClause.getChild(1));

        while (!queue.isEmpty()) {
            ParseTree parent = queue.remove();
            ParseTree child1 = parent.getChild(0);
            ParseTree child2 = parent.getChild(2);

            // Base case: Both are XQuery (if child 1 is XQuery then child 2 is also XQuery)
            if (child1 instanceof XQueryParser.XQueryContext) {
                String variable = child1.getChild(0).getText().substring(1);
                TerminalNode operator = (TerminalNode) child2.getChild(0);

                if (operator.getSymbol().getType() == XQueryLexer.VAR) {
                    this.connectedComponents
                            .get(dependency.get(variable))
                            .addJoin(new AbstractMap.SimpleEntry<>(variable, operator.getText().substring(1)));
                    this.connectedComponents
                            .get(dependency.get(operator.getText().substring(1)))
                            .addJoin(new AbstractMap.SimpleEntry<>(operator.getText().substring(1), variable));
                    this.joinConditions.add(new LinkedList<>(Arrays.asList(variable, operator.getText().substring(1))));
                } else if (operator.getSymbol().getType() == XQueryLexer.STRING)
                    this.connectedComponents
                            .get(dependency.get(variable))
                            .addFilter(variable, operator.getText().substring(1, operator.getText().length() - 1));
                continue;
            }

            // Recursive Case: Both are conditions (if child 1 is condition then child 2 also has to be condition)
            queue.add(child1);
            queue.add(child2);
        }

    }

    public boolean isJoin() {
        return this.connectedComponents.size() > 1;
    }

    private String createJoinPlan() {

        // the rewritten join query
        StringBuilder joinQuery = new StringBuilder();

        // list of connected components
        List<ConnectedComponent> components = new ArrayList<>(this.connectedComponents.values());

        // First, maintain a set of all the visited variables (connected components)
        // Initially we add the first element

        StringBuilder attrCond1;
        StringBuilder attrCond2;

        // Store next components to join based on available joins
        Queue<ConnectedComponent> queue = new LinkedList<>();
        // Add the first connected component
        queue.add(components.get(0));

        joinQuery.append(components.get(0).toString());

        HashSet<List<String>> visitedJoins = new HashSet<>();
        // store joins from components that have been joined with other components
        // but still require another non visited component
        HashSet<List<String>> skippedJoins = new HashSet<>();
        // components that have been joined
        HashSet<String> visitedComponents = new HashSet<>();
        visitedComponents.add(components.get(0).getRoot());

        // iterate, until we have visited all connected components
        while (!queue.isEmpty()) {
            StringBuilder newJoinQuery = new StringBuilder();
            ConnectedComponent currentComponent = queue.remove();

            newJoinQuery.append("join (")
                        .append(joinQuery)
                        .append(", ")
                        .append(currentComponent.toString())
                        .append(", ");

            attrCond1 = new StringBuilder();
            attrCond2 = new StringBuilder();
            attrCond1.append("[");
            attrCond2.append("[");

            for (AbstractMap.SimpleEntry<String, String> join : currentComponent.getJoins()) {
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

            attrCond1.append("]");
            attrCond2.append("]");
            newJoinQuery.append(attrCond1)
                        .append(", ")
                        .append(attrCond2)
                        .append(")");

            // Find if next component exist to join
            if (!skippedJoins.isEmpty()) {
                List<String> newJoin = skippedJoins.iterator().next();
                AbstractMap.SimpleEntry<String, String> joinTarget1 = new AbstractMap.SimpleEntry<>(newJoin.get(0), newJoin.get(1));
                AbstractMap.SimpleEntry<String, String> joinTarget2 = new AbstractMap.SimpleEntry<>(newJoin.get(1), newJoin.get(0));
                for (ConnectedComponent nextComponent : this.connectedComponents.values()) {
                    if (visitedComponents.contains(nextComponent.getRoot()))
                        continue;
                    if (nextComponent.getJoins().contains(joinTarget1) || nextComponent.getJoins().contains(joinTarget2)) {
                        queue.add(nextComponent);
                        visitedComponents.add(nextComponent.getRoot());
                        break;
                    }
                }
            }

            // update the join query
            joinQuery = newJoinQuery;
        }

        return joinQuery.toString();
    }

    public void rewrite(File rewriteFile) {
        try (FileWriter writer = new FileWriter(rewriteFile)) {
            // the rewritten query with join
            StringBuilder rewrittenQuery = new StringBuilder();

            // rewrite the for clause with join operator
            rewrittenQuery.append("for $tuple in ")
                          .append(createJoinPlan())
                          .append("\n");

            // rewrite the return clause
            rewrittenQuery.append(this.returnClause.getText()
                          .replace("return", "return ")
                          .replaceAll("\\$([A-Za-z0-9_]+)", "\\$tuple/$1/*")
                          .replaceAll(",", ",\n"));

            writer.write(rewrittenQuery.toString());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

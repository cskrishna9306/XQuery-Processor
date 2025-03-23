import java.util.*;

public class ConnectedComponent {

    private final String root;
    private final List<List<String>> variables;
    private final List<List<String>> filters;
    private final List<AbstractMap.SimpleEntry<String, String>> joins;

    public ConnectedComponent(String root, String xQuery) {
        this.root = root;
        this.variables = new LinkedList<List<String>>() {{
            add(new LinkedList<>(Arrays.asList(root, xQuery)));
        }};
        this.filters = new LinkedList<>();
        this.joins = new LinkedList<>();
    }

    public void addVariable(String var, String xQuery) {
        this.variables.add(new LinkedList<>(Arrays.asList(var, xQuery)));
    }

    public void addFilter(String var, String filter) {
        this.filters.add(new LinkedList<>(Arrays.asList(var, filter)));
    }

    public void addJoin(AbstractMap.SimpleEntry<String, String> join) {
        this.joins.add(join);
    }

    public String getRoot() {
        return this.root;
    }

    public List<List<String>> getVariables() {
        return this.variables;
    }

    public List<AbstractMap.SimpleEntry<String, String>> getJoins() {
        return this.joins;
    }

    @Override
    public String toString() {

        // The rewritten query for the connected component
        StringBuilder ccString = new StringBuilder();

        // Build the for clause of the rewritten query
        ccString.append("for ");
        for (int i = 0; i < this.variables.size(); i++) {
            ccString.append(this.variables.get(i).get(1));
            if (i != this.variables.size() - 1)
                ccString.append(",");
            ccString.append("\n");
        }

        // Build the where clause if filter for this connected component exists
        if (!this.filters.isEmpty()) {
            ccString.append("where ");
            for (int i = 0; i < this.filters.size(); i++) {
                ccString.append("$")
                        .append(this.filters.get(i).get(0))
                        .append(" eq ")
                        .append("\"")
                        .append(this.filters.get(i).get(1))
                        .append("\"");
                if (i != this.filters.size() - 1)
                    ccString.append(" and ");
                ccString.append("\n");
            }
        }

        // Build the return clause for the rewritten query
        ccString.append("return <tuple>{ ");
        for (int i = 0; i < this.variables.size(); i++) {
            ccString.append("<")
                    .append(this.variables.get(i).get(0))
                    .append(">{$")
                    .append(this.variables.get(i).get(0))
                    .append("}</")
                    .append(this.variables.get(i).get(0))
                    .append(">");
            if (i != this.variables.size() - 1)
                ccString.append(",");
        }

        ccString.append(" }</tuple> </return>")
                .append("\n");

        return ccString.toString();
    }
}

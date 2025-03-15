package main;
import java.util.*;

public class ConnectedComponent {

    private final String root;
    private final Set<List<String>> variables;
    private final List<List<String>> filters;
    private final Set<AbstractMap.SimpleEntry<String, String>> joins;

    public ConnectedComponent(String root, String queryString) {
        this.root = root;
        this.variables = new LinkedHashSet<List<String>>() {{
            List<String> node = new ArrayList<>();
            node.add(root);
            node.add(queryString);
            add(node);
        }};
        this.filters = new LinkedList<>();
        this.joins = new LinkedHashSet<>();
    }

    public void addVariable(String var, String queryString) {
        List<String> node = new ArrayList<>();
        node.add(var);
        node.add(queryString);
        this.variables.add(node);
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

    public Set<List<String>> getVariables() {
        return this.variables;
    }

    public List<List<String>> getFilters() {
        return this.filters;
    }

    public Set<AbstractMap.SimpleEntry<String, String>> getJoins() {
        return this.joins;
    }

    @Override
    public String toString() {

        return "ConnectedComponent{" +
                "root='" + this.root + '\'' +
                ", variables=" + this.variables +
                ", filters=" + this.filters +
                ", joins=" + this.joins +
                '}';
    }
}
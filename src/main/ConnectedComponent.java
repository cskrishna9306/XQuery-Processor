import java.util.*;

public class ConnectedComponent {

    private final String root;
    private final Set<String> variables;
    private final Set<String> filters;
    private final Set<AbstractMap.SimpleEntry<String, String>> joins;

    public ConnectedComponent(String root) {
        this.root = root;
        this.variables = new LinkedHashSet<String>() {{
            add(root);
        }};
        this.filters = new LinkedHashSet<>();
        this.joins = new LinkedHashSet<>();
    }

    public void addVariable(String var) {
        this.variables.add(var);
    }

    public void addFilter(String filter) {
        this.filters.add(filter);
    }

    public void addJoin(AbstractMap.SimpleEntry<String, String> join) {
        this.joins.add(join);
    }

    public String getRoot() {
        return this.root;
    }

    public Set<String> getVariables() {
        return this.variables;
    }

    public Set<String> getFilters() {
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

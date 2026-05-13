package org.azertio.lsp;

import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.Mark;
import org.yaml.snakeyaml.error.MarkedYAMLException;
import org.yaml.snakeyaml.nodes.*;

import java.io.StringReader;
import java.util.*;

/**
 * Validates the structure of an azertio.yaml document and returns LSP diagnostics.
 */
public class YamlDiagnosticsProvider {

    private static final String SOURCE = "azertio";
    private static final Set<String> ALLOWED_ROOT_KEYS = Set.of("project", "plugins", "configuration", "profiles");
    private static final Set<String> ALLOWED_PROJECT_KEYS = Set.of("name", "organization", "description", "test-suites");
    private static final Set<String> ALLOWED_SUITE_KEYS = Set.of("name", "description", "tag-expression");

    public List<Diagnostic> validate(String content) {
        var diagnostics = new ArrayList<Diagnostic>();

        Node root = parseToNode(content, diagnostics);
        if (root == null) return diagnostics;

        if (!(root instanceof MappingNode rootMap)) {
            diagnostics.add(error(root, "azertio.yaml must be a YAML mapping at the root level"));
            return diagnostics;
        }

        checkDuplicateKeys(rootMap, diagnostics);
        checkUnknownKeys(rootMap, ALLOWED_ROOT_KEYS, diagnostics);

        validateProject(findValue(rootMap, "project"), diagnostics);
        validatePlugins(findValue(rootMap, "plugins"), diagnostics);
        validateConfiguration(findValue(rootMap, "configuration"), diagnostics);
        validateProfiles(findValue(rootMap, "profiles"), diagnostics);

        return diagnostics;
    }

    // ─── Section validators ───────────────────────────────────────────────────

    private void validateProject(Node projectNode, List<Diagnostic> diagnostics) {
        if (projectNode == null) {
            diagnostics.add(errorAt(0, 0, "Missing required section 'project'"));
            return;
        }
        if (!(projectNode instanceof MappingNode projectMap)) {
            diagnostics.add(error(projectNode, "'project' must be a mapping"));
            return;
        }
        checkDuplicateKeys(projectMap, diagnostics);
        checkUnknownKeys(projectMap, ALLOWED_PROJECT_KEYS, diagnostics);

        if (findValue(projectMap, "name") == null) {
            diagnostics.add(error(projectNode, "Missing required field 'project.name'"));
        }
        if (findValue(projectMap, "organization") == null) {
            diagnostics.add(error(projectNode, "Missing required field 'project.organization'"));
        }

        Node suitesNode = findValue(projectMap, "test-suites");
        if (suitesNode != null) {
            if (!(suitesNode instanceof SequenceNode suitesSeq)) {
                diagnostics.add(error(suitesNode, "'project.test-suites' must be a list"));
            } else {
                for (Node suite : suitesSeq.getValue()) {
                    validateSuite(suite, diagnostics);
                }
            }
        }
    }

    private void validateSuite(Node suiteNode, List<Diagnostic> diagnostics) {
        if (!(suiteNode instanceof MappingNode suiteMap)) {
            diagnostics.add(error(suiteNode, "Each test suite entry must be a mapping"));
            return;
        }
        checkDuplicateKeys(suiteMap, diagnostics);
        checkUnknownKeys(suiteMap, ALLOWED_SUITE_KEYS, diagnostics);
        if (findValue(suiteMap, "name") == null) {
            diagnostics.add(error(suiteNode, "Test suite is missing required field 'name'"));
        }
    }

    private void validatePlugins(Node pluginsNode, List<Diagnostic> diagnostics) {
        if (pluginsNode != null && !(pluginsNode instanceof SequenceNode)) {
            diagnostics.add(error(pluginsNode, "'plugins' must be a list"));
        }
    }

    private void validateConfiguration(Node configNode, List<Diagnostic> diagnostics) {
        if (configNode != null && !(configNode instanceof MappingNode)) {
            diagnostics.add(error(configNode, "'configuration' must be a mapping"));
        }
    }

    private void validateProfiles(Node profilesNode, List<Diagnostic> diagnostics) {
        if (profilesNode == null) return;
        if (!(profilesNode instanceof MappingNode profilesMap)) {
            diagnostics.add(error(profilesNode, "'profiles' must be a mapping"));
            return;
        }
        checkDuplicateKeys(profilesMap, diagnostics);
        for (var entry : profilesMap.getValue()) {
            if (!(entry.getValueNode() instanceof MappingNode profileMap)) {
                diagnostics.add(error(entry.getValueNode(), "Each profile must be a mapping"));
            } else {
                checkDuplicateKeys(profileMap, diagnostics);
            }
        }
    }

    // ─── Structural checks ────────────────────────────────────────────────────

    private void checkDuplicateKeys(MappingNode node, List<Diagnostic> diagnostics) {
        var seen = new LinkedHashSet<String>();
        for (var tuple : node.getValue()) {
            String key = scalarValue(tuple.getKeyNode());
            if (key != null && !seen.add(key)) {
                diagnostics.add(error(tuple.getKeyNode(), "Duplicate key '" + key + "'"));
            }
        }
    }

    private void checkUnknownKeys(MappingNode node, Set<String> allowed, List<Diagnostic> diagnostics) {
        for (var tuple : node.getValue()) {
            String key = scalarValue(tuple.getKeyNode());
            if (key != null && !allowed.contains(key)) {
                diagnostics.add(warning(tuple.getKeyNode(), "Unknown key '" + key + "'"));
            }
        }
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private Node parseToNode(String content, List<Diagnostic> diagnostics) {
        try {
            return new Yaml().compose(new StringReader(content));
        } catch (MarkedYAMLException e) {
            Mark mark = e.getProblemMark();
            var pos = mark != null ? new Position(mark.getLine(), mark.getColumn()) : new Position(0, 0);
            diagnostics.add(new Diagnostic(new Range(pos, pos), e.getProblem(), DiagnosticSeverity.Error, SOURCE));
            return null;
        }
    }

    private Node findValue(MappingNode node, String key) {
        return node.getValue().stream()
            .filter(t -> key.equals(scalarValue(t.getKeyNode())))
            .map(NodeTuple::getValueNode)
            .findFirst().orElse(null);
    }

    private String scalarValue(Node node) {
        return node instanceof ScalarNode s ? s.getValue() : null;
    }

    private Diagnostic error(Node node, String message) {
        return new Diagnostic(rangeOf(node), message, DiagnosticSeverity.Error, SOURCE);
    }

    private Diagnostic errorAt(int line, int col, String message) {
        var pos = new Position(line, col);
        return new Diagnostic(new Range(pos, pos), message, DiagnosticSeverity.Error, SOURCE);
    }

    private Diagnostic warning(Node node, String message) {
        return new Diagnostic(rangeOf(node), message, DiagnosticSeverity.Warning, SOURCE);
    }

    private Range rangeOf(Node node) {
        Mark start = node.getStartMark();
        Mark end   = node.getEndMark();
        var startPos = new Position(start.getLine(), start.getColumn());
        var endPos   = end != null
            ? new Position(end.getLine(), end.getColumn())
            : startPos;
        return new Range(startPos, endPos);
    }
}

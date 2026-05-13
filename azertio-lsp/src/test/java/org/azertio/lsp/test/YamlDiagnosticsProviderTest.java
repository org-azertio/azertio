package org.azertio.lsp.test;

import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.junit.jupiter.api.Test;
import org.azertio.lsp.YamlDiagnosticsProvider;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class YamlDiagnosticsProviderTest {

    private final YamlDiagnosticsProvider provider = new YamlDiagnosticsProvider();

    private static final String VALID_YAML = """
        project:
          name: MyProject
          organization: MyOrg
          description: A test project
          test-suites:
            - name: smoke
              description: Smoke tests
              tag-expression: "@smoke"
        plugins:
          - gherkin-azertio-plugin
        configuration:
          key1: value1
        profiles:
          fast:
            key1: override
        """;

    @Test
    void validYamlProducesNoDiagnostics() {
        List<Diagnostic> diags = provider.validate(VALID_YAML);
        assertThat(diags).isEmpty();
    }

    @Test
    void invalidYamlSyntaxProducesParseError() {
        String badYaml = "project: {\n  name: unclosed";
        List<Diagnostic> diags = provider.validate(badYaml);
        assertThat(diags).isNotEmpty();
        assertThat(diags.get(0).getSeverity()).isEqualTo(DiagnosticSeverity.Error);
    }

    @Test
    void missingProjectProducesError() {
        String yaml = "plugins:\n  - myplugin\n";
        List<Diagnostic> diags = provider.validate(yaml);
        assertThat(diags).anyMatch(d ->
            msg(d).contains("project") && d.getSeverity() == DiagnosticSeverity.Error);
    }

    @Test
    void missingProjectNameProducesError() {
        String yaml = """
            project:
              organization: MyOrg
            """;
        List<Diagnostic> diags = provider.validate(yaml);
        assertThat(diags).anyMatch(d -> msg(d).contains("project.name"));
    }

    @Test
    void missingProjectOrganizationProducesError() {
        String yaml = """
            project:
              name: MyProject
            """;
        List<Diagnostic> diags = provider.validate(yaml);
        assertThat(diags).anyMatch(d -> msg(d).contains("project.organization"));
    }

    @Test
    void unknownRootKeyProducesWarning() {
        String yaml = """
            project:
              name: MyProject
              organization: MyOrg
            unknown-key: value
            """;
        List<Diagnostic> diags = provider.validate(yaml);
        assertThat(diags).anyMatch(d ->
            d.getSeverity() == DiagnosticSeverity.Warning && msg(d).contains("unknown-key"));
    }

    @Test
    void duplicateRootKeyProducesError() {
        String yaml = """
            project:
              name: MyProject
              organization: MyOrg
            project:
              name: Duplicate
              organization: OtherOrg
            """;
        List<Diagnostic> diags = provider.validate(yaml);
        assertThat(diags).anyMatch(d ->
            d.getSeverity() == DiagnosticSeverity.Error && msg(d).contains("Duplicate key"));
    }

    @Test
    void pluginsNotAListProducesError() {
        String yaml = """
            project:
              name: MyProject
              organization: MyOrg
            plugins:
              notalist: true
            """;
        List<Diagnostic> diags = provider.validate(yaml);
        assertThat(diags).anyMatch(d ->
            msg(d).contains("plugins") && d.getSeverity() == DiagnosticSeverity.Error);
    }

    @Test
    void configurationNotAMappingProducesError() {
        String yaml = """
            project:
              name: MyProject
              organization: MyOrg
            configuration:
              - item1
            """;
        List<Diagnostic> diags = provider.validate(yaml);
        assertThat(diags).anyMatch(d ->
            msg(d).contains("configuration") && d.getSeverity() == DiagnosticSeverity.Error);
    }

    @Test
    void profilesNotAMappingProducesError() {
        String yaml = """
            project:
              name: MyProject
              organization: MyOrg
            profiles:
              - item
            """;
        List<Diagnostic> diags = provider.validate(yaml);
        assertThat(diags).anyMatch(d ->
            msg(d).contains("profiles") && d.getSeverity() == DiagnosticSeverity.Error);
    }

    @Test
    void profileValueNotAMappingProducesError() {
        String yaml = """
            project:
              name: MyProject
              organization: MyOrg
            profiles:
              fast: not-a-mapping
            """;
        List<Diagnostic> diags = provider.validate(yaml);
        assertThat(diags).anyMatch(d -> d.getSeverity() == DiagnosticSeverity.Error);
    }

    @Test
    void suitesMissingNameProducesError() {
        String yaml = """
            project:
              name: MyProject
              organization: MyOrg
              test-suites:
                - description: A suite with no name
            """;
        List<Diagnostic> diags = provider.validate(yaml);
        assertThat(diags).anyMatch(d ->
            msg(d).contains("name") && d.getSeverity() == DiagnosticSeverity.Error);
    }

    @Test
    void suitesNotAListProducesError() {
        String yaml = """
            project:
              name: MyProject
              organization: MyOrg
              test-suites: not-a-list
            """;
        List<Diagnostic> diags = provider.validate(yaml);
        assertThat(diags).anyMatch(d ->
            msg(d).contains("test-suites") && d.getSeverity() == DiagnosticSeverity.Error);
    }

    @Test
    void suiteEntryNotAMappingProducesError() {
        String yaml = """
            project:
              name: MyProject
              organization: MyOrg
              test-suites:
                - just-a-string
            """;
        List<Diagnostic> diags = provider.validate(yaml);
        assertThat(diags).anyMatch(d -> d.getSeverity() == DiagnosticSeverity.Error);
    }

    @Test
    void unknownProjectKeyProducesWarning() {
        String yaml = """
            project:
              name: MyProject
              organization: MyOrg
              unknown-project-key: value
            """;
        List<Diagnostic> diags = provider.validate(yaml);
        assertThat(diags).anyMatch(d ->
            d.getSeverity() == DiagnosticSeverity.Warning &&
            msg(d).contains("unknown-project-key"));
    }

    @Test
    void unknownSuiteKeyProducesWarning() {
        String yaml = """
            project:
              name: MyProject
              organization: MyOrg
              test-suites:
                - name: smoke
                  unknown-suite-key: value
            """;
        List<Diagnostic> diags = provider.validate(yaml);
        assertThat(diags).anyMatch(d ->
            d.getSeverity() == DiagnosticSeverity.Warning &&
            msg(d).contains("unknown-suite-key"));
    }

    @Test
    void rootNotAMappingProducesError() {
        String yaml = "- item1\n- item2\n";
        List<Diagnostic> diags = provider.validate(yaml);
        assertThat(diags).anyMatch(d -> d.getSeverity() == DiagnosticSeverity.Error);
    }

    @Test
    void emptyYamlProducesNoDiagnostics() {
        // Empty YAML parses to a null node — no document to validate
        List<Diagnostic> diags = provider.validate("");
        assertThat(diags).isEmpty();
    }

    @Test
    void duplicateProfileKeyProducesError() {
        String yaml = """
            project:
              name: MyProject
              organization: MyOrg
            profiles:
              fast:
                key1: val1
                key1: val2
            """;
        List<Diagnostic> diags = provider.validate(yaml);
        assertThat(diags).anyMatch(d ->
            d.getSeverity() == DiagnosticSeverity.Error && msg(d).contains("Duplicate key"));
    }

    private static String msg(Diagnostic d) {
        var m = d.getMessage();
        return m.isLeft() ? m.getLeft() : m.getRight().getValue();
    }
}
package org.azertio.lsp.test;

import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.myjtools.gherkinparser.DefaultKeywordMapProvider;
import org.myjtools.imconfig.Config;
import org.azertio.core.AzertioConfig;
import org.azertio.core.AzertioRuntime;
import org.azertio.core.backend.StepProviderBackend;
import org.azertio.lsp.FeatureDiagnosticsProvider;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("unchecked")

class FeatureDiagnosticsProviderTest {

    static FeatureDiagnosticsProvider provider;

    @BeforeAll
    static void setUp() {
        // Minimal runtime with no step providers — isValidStep() returns false for all steps
        var config = Config.ofMap(Map.of(AzertioConfig.ENV_PATH, "target/.azertio-lsp-test"));
        var runtime = new AzertioRuntime(config);
        var backend = new StepProviderBackend(runtime);
        provider = new FeatureDiagnosticsProvider(backend, new DefaultKeywordMapProvider());
    }

    @Test
    void emptyContentProducesNoDiagnostics() {
        assertThat(provider.validate("")).isEmpty();
    }

    @Test
    void blankLinesProducesNoDiagnostics() {
        assertThat(provider.validate("\n\n   \n")).isEmpty();
    }

    @Test
    void commentLinesAreSkipped() {
        String content = """
            # This is a comment
            # language: en
            # another comment
            """;
        assertThat(provider.validate(content)).isEmpty();
    }

    @Test
    void tagLinesAreSkipped() {
        String content = """
            @smoke @regression
            Feature: My feature
            """;
        assertThat(provider.validate(content)).isEmpty();
    }

    @Test
    void tableRowLinesAreSkipped() {
        String content = """
            Feature: My feature
              Scenario: A scenario
                | col1 | col2 |
                | val1 | val2 |
            """;
        assertThat(provider.validate(content)).isEmpty();
    }

    @Test
    void docStringIsSkipped() {
        String content = """
            Feature: My feature
              Scenario: A scenario
                Given a step
                \"\"\"
                some doc string content
                that spans multiple lines
                \"\"\"
            """;
        // Steps are all invalid with empty backend, but docstring content is skipped
        List<Diagnostic> diags = provider.validate(content);
        // The "Given a step" line is an invalid step → one error
        // The docstring content lines are skipped
        assertThat(diags).hasSize(1);
        assertThat(diags.get(0).getSeverity()).isEqualTo(DiagnosticSeverity.Error);
        assertThat(msg(diags.get(0))).contains("a step");
    }

    @Test
    void stepLineProducesErrorWhenNoMatchingStep() {
        String content = """
            Feature: My feature
              Scenario: A scenario
                Given an unrecognized step here
            """;
        List<Diagnostic> diags = provider.validate(content);
        assertThat(diags).hasSize(1);
        assertThat(diags.get(0).getSeverity()).isEqualTo(DiagnosticSeverity.Error);
        assertThat(msg(diags.get(0))).contains("an unrecognized step here");
        assertThat(diags.get(0).getSource()).isEqualTo("azertio-step");
    }

    @Test
    void multipleInvalidStepsProducesMultipleErrors() {
        String content = """
            Feature: My feature
              Scenario: A scenario
                Given step one
                When step two
                Then step three
            """;
        List<Diagnostic> diags = provider.validate(content);
        assertThat(diags).hasSize(3);
        assertThat(diags).allMatch(d -> d.getSeverity() == DiagnosticSeverity.Error);
    }

    @Test
    void structureKeywordsAloneProduceNoDiagnostics() {
        String content = """
            Feature: My feature
              Description of the feature
              Background:
                Description of background
              Scenario: A scenario
                Description of scenario
              Scenario Outline: Outline
                Examples:
            """;
        // No step lines → no validation errors
        assertThat(provider.validate(content)).isEmpty();
    }

    @Test
    void unrecognizedKeywordAfterStepProducesError() {
        String content = """
            Feature: My feature
              Scenario: A scenario
                Given a step
                UnknownKeyword: something
            """;
        List<Diagnostic> diags = provider.validate(content);
        // "Given a step" → invalid step (1 error)
        // "UnknownKeyword: something" → unrecognized keyword after being in step context (1 error)
        assertThat(diags).hasSize(2);
    }

    @Test
    void hintsForReturnsListForStep() {
        List<String> hints = provider.hintsFor("some step text", Locale.ENGLISH);
        // With an empty backend there are no step providers, so no hints
        assertThat(hints).isNotNull();
    }

    @Test
    void singleQuoteDocStringIsSkipped() {
        String content = """
            Feature: My feature
              Scenario: A scenario
                Given a step
                '''
                single quote docstring
                '''
            """;
        List<Diagnostic> diags = provider.validate(content);
        assertThat(diags).hasSize(1); // only the Given step error
    }

    @Test
    void andButStepsAreRecognized() {
        String content = """
            Feature: My feature
              Scenario: A scenario
                Given a step
                And another step
                But yet another step
            """;
        List<Diagnostic> diags = provider.validate(content);
        assertThat(diags).hasSize(3); // all invalid steps
        assertThat(diags).allMatch(d -> d.getSeverity() == DiagnosticSeverity.Error);
    }

    @Test
    void blankStepTextIsNotValidated() {
        // A step keyword followed only by whitespace should not trigger validation
        String content = "Feature: My feature\n  Scenario: s\n    Given    \n";
        List<Diagnostic> diags = provider.validate(content);
        assertThat(diags).isEmpty();
    }

    private static String msg(Diagnostic d) {
        var m = d.getMessage();
        return m.isLeft() ? m.getLeft() : m.getRight().getValue();
    }
}
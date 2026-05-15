package org.azertio.core.test.docgen;

import org.junit.jupiter.api.Test;
import org.azertio.core.docgen.ParameterDoc;
import org.azertio.core.docgen.ScenarioExample;
import org.azertio.core.docgen.StepDocEntry;
import org.azertio.core.docgen.StepDocMarkdownGenerator;
import org.azertio.core.docgen.StepLanguageEntry;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class StepDocMarkdownGeneratorTest {

    private final StepDocMarkdownGenerator generator = new StepDocMarkdownGenerator();

    @Test
    void generate_emptySteps_returnsOnlyTitle() {
        String result = generator.generate("My Title", Map.of());
        assertThat(result).startsWith("# My Title\n\n");
        assertThat(result.trim()).isEqualTo("# My Title");
    }

    @Test
    void generate_stepWithDescription_includesSeparatorAndId() {
        var step = new StepDocEntry(null, "A description", List.of(), null, Map.of());
        String result = generator.generate("Steps", Map.of("my.step", step));
        assertThat(result).contains("---\n\n## `my.step`\n\n");
        assertThat(result).contains("A description\n\n");
    }

    @Test
    void generate_stepWithRole_includesRoleLine() {
        var step = new StepDocEntry("action", "desc", List.of(), null, Map.of());
        String result = generator.generate("Steps", Map.of("s", step));
        assertThat(result).contains("**Role:** `action`\n\n");
    }

    @Test
    void generate_stepWithoutRole_omitsRoleLine() {
        var step = new StepDocEntry(null, "desc", List.of(), null, Map.of());
        String result = generator.generate("Steps", Map.of("s", step));
        assertThat(result).doesNotContain("**Role:**");
    }

    @Test
    void generate_stepWithParameters_includesParameterTable() {
        var params = List.of(
            new ParameterDoc("url", "text", "The target URL"),
            new ParameterDoc("timeout", "integer", "Max wait time")
        );
        var step = new StepDocEntry(null, "desc", params, null, Map.of());
        String result = generator.generate("Steps", Map.of("s", step));
        assertThat(result).contains("### Parameters\n\n");
        assertThat(result).contains("| Name | Type | Description |");
        assertThat(result).contains("| `url` | text | The target URL |");
        assertThat(result).contains("| `timeout` | integer | Max wait time |");
    }

    @Test
    void generate_stepWithEmptyParameters_omitsParameterSection() {
        var step = new StepDocEntry(null, "desc", List.of(), null, Map.of());
        String result = generator.generate("Steps", Map.of("s", step));
        assertThat(result).doesNotContain("### Parameters");
    }

    @Test
    void generate_stepWithAdditionalData_includesSection() {
        var step = new StepDocEntry(null, "desc", List.of(), "Extra info here", Map.of());
        String result = generator.generate("Steps", Map.of("s", step));
        assertThat(result).contains("### Additional data\n\n");
        assertThat(result).contains("Extra info here\n\n");
    }

    @Test
    void generate_stepWithNullAdditionalData_omitsSection() {
        var step = new StepDocEntry(null, "desc", List.of(), null, Map.of());
        String result = generator.generate("Steps", Map.of("s", step));
        assertThat(result).doesNotContain("### Additional data");
    }

    @Test
    void generate_languageEntryWithExpression_includesExpression() {
        var lang = new StepLanguageEntry("I do {x:text}", null, List.of(), List.of());
        var step = new StepDocEntry(null, "desc", List.of(), null, Map.of("en", lang));
        String result = generator.generate("Steps", Map.of("s", step));
        assertThat(result).contains("### `en`\n\n");
        assertThat(result).contains("**Expression:** `I do {x:text}`\n\n");
    }

    @Test
    void generate_languageEntryWithoutExpression_omitsExpressionLine() {
        var lang = new StepLanguageEntry(null, null, List.of(), List.of());
        var step = new StepDocEntry(null, "desc", List.of(), null, Map.of("en", lang));
        String result = generator.generate("Steps", Map.of("s", step));
        assertThat(result).doesNotContain("**Expression:**");
    }

    @Test
    void generate_languageEntryWithAssertionHints_includesBullets() {
        var lang = new StepLanguageEntry(null, null, List.of(), List.of("hint A", "hint B"));
        var step = new StepDocEntry(null, "desc", List.of(), null, Map.of("en", lang));
        String result = generator.generate("Steps", Map.of("s", step));
        assertThat(result).contains("**Assertion expressions:**\n\n");
        assertThat(result).contains("- `hint A`\n");
        assertThat(result).contains("- `hint B`\n");
    }

    @Test
    void generate_languageEntryWithExample_includesGherkinBlock() {
        var lang = new StepLanguageEntry(null, "Given I do something", List.of(), List.of());
        var step = new StepDocEntry(null, "desc", List.of(), null, Map.of("en", lang));
        String result = generator.generate("Steps", Map.of("s", step));
        assertThat(result).contains("**Example:**\n\n```gherkin\nGiven I do something\n```\n\n");
    }

    @Test
    void generate_languageEntryWithScenarios_includesAllScenarios() {
        var scenarios = List.of(
            new ScenarioExample("Happy path", "Given A\nWhen B\nThen C"),
            new ScenarioExample(null, "Given X\nThen Y")
        );
        var lang = new StepLanguageEntry(null, null, scenarios, List.of());
        var step = new StepDocEntry(null, "desc", List.of(), null, Map.of("en", lang));
        String result = generator.generate("Steps", Map.of("s", step));
        assertThat(result).contains("**Scenarios:**\n\n");
        assertThat(result).contains("*Happy path*\n\n");
        assertThat(result).contains("```gherkin\nGiven A\nWhen B\nThen C\n```\n\n");
        assertThat(result).contains("```gherkin\nGiven X\nThen Y\n```\n\n");
    }

    @Test
    void generate_multipleSteps_allStepsPresent() {
        var steps = new LinkedHashMap<String, StepDocEntry>();
        steps.put("step.one", new StepDocEntry(null, "First", List.of(), null, Map.of()));
        steps.put("step.two", new StepDocEntry(null, "Second", List.of(), null, Map.of()));
        String result = generator.generate("Steps", steps);
        assertThat(result).contains("## `step.one`");
        assertThat(result).contains("## `step.two`");
    }
}
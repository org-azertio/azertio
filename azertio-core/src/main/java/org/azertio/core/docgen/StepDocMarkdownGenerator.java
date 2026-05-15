package org.azertio.core.docgen;

import java.util.Map;

public class StepDocMarkdownGenerator {

    public String generate(String title, Map<String, StepDocEntry> steps) {
        var sb = new StringBuilder();
        sb.append("# ").append(title).append("\n\n");
        for (var stepEntry : steps.entrySet()) {
            appendStep(sb, stepEntry.getKey(), stepEntry.getValue());
        }
        return sb.toString();
    }

    private void appendStep(StringBuilder sb, String id, StepDocEntry entry) {
        sb.append("---\n\n");
        sb.append("## `").append(id).append("`\n\n");

        if (entry.role() != null) {
            sb.append("**Role:** `").append(entry.role()).append("`\n\n");
        }

        sb.append(entry.description()).append("\n\n");

        if (entry.parameters() != null && !entry.parameters().isEmpty()) {
            sb.append("### Parameters\n\n");
            sb.append("| Name | Type | Description |\n");
            sb.append("|------|------|-------------|\n");
            for (var param : entry.parameters()) {
                sb.append("| `").append(param.name()).append("` | ")
                  .append(param.type()).append(" | ")
                  .append(param.description()).append(" |\n");
            }
            sb.append("\n");
        }

        if (entry.additionalData() != null) {
            sb.append("### Additional data\n\n");
            sb.append(entry.additionalData()).append("\n\n");
        }

        if (entry.language() != null && !entry.language().isEmpty()) {
            for (var langEntry : entry.language().entrySet()) {
                appendLanguageSection(sb, langEntry.getKey(), langEntry.getValue());
            }
        }
    }

    private void appendLanguageSection(StringBuilder sb, String lang, StepLanguageEntry entry) {
        sb.append("### `").append(lang).append("`\n\n");

        if (entry.expression() != null) {
            sb.append("**Expression:** `").append(entry.expression()).append("`\n\n");
        }

        if (entry.assertionHints() != null && !entry.assertionHints().isEmpty()) {
            sb.append("**Assertion expressions:**\n\n");
            for (var hint : entry.assertionHints()) {
                sb.append("- `").append(hint).append("`\n");
            }
            sb.append("\n");
        }

        if (entry.example() != null) {
            sb.append("**Example:**\n\n");
            sb.append("```gherkin\n");
            sb.append(entry.example()).append("\n");
            sb.append("```\n\n");
        }

        if (entry.scenarios() != null && !entry.scenarios().isEmpty()) {
            sb.append("**Scenarios:**\n\n");
            for (var scenario : entry.scenarios()) {
                if (scenario.title() != null) {
                    sb.append("*").append(scenario.title()).append("*\n\n");
                }
                sb.append("```gherkin\n");
                sb.append(scenario.gherkin()).append("\n");
                sb.append("```\n\n");
            }
        }
    }
}
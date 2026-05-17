package org.azertio.core.docgen;

import java.util.Map;
import java.util.stream.Collectors;

public class ConfigDocMarkdownGenerator {

    public String generate(String title, Map<String, ConfigDocEntry> entries) {
        var sb = new StringBuilder();
        sb.append("# ").append(title).append("\n\n");
        for (var entry : entries.entrySet()) {
            appendEntry(sb, entry.getKey(), entry.getValue());
        }
        return sb.toString();
    }

    private void appendEntry(StringBuilder sb, String key, ConfigDocEntry entry) {
        sb.append("---\n\n");
        sb.append("## `").append(key).append("`\n\n");

        if (entry.description() != null && !entry.description().isBlank()) {
            sb.append(entry.description().stripTrailing()).append("\n\n");
        }

        sb.append("**Type** ").append(entry.type()).append("\n\n");

        if (entry.required()) {
            sb.append("**Required**\n\n");
        }
        if (entry.defaultValue() != null) {
            sb.append("**Default value** `").append(entry.defaultValue()).append("`\n\n");
        }
        if (entry.constraintPattern() != null) {
            sb.append("**Pattern** `").append(entry.constraintPattern()).append("`\n\n");
        }
        if (entry.constraintMin() != null) {
            sb.append("**Min** `").append(entry.constraintMin()).append("` ");
        }
        if (entry.constraintMax() != null) {
            sb.append("**Max** `").append(entry.constraintMax()).append("` ");
        }
        if (entry.constraintMin() != null || entry.constraintMax() != null) {
            sb.append("\n\n");
        }
        if (entry.constraintValues() != null && !entry.constraintValues().isEmpty()) {
            String values = entry.constraintValues().stream()
                .map(v -> "`" + v + "`")
                .collect(Collectors.joining(", "));
            sb.append("**Values**  ").append(values).append(" \n\n");
        }

        sb.append("\n\n");
    }
}
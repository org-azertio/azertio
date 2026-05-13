package org.myjtools.openbbt.docgen;

import org.myjtools.openbbt.core.docgen.ParameterDoc;
import org.myjtools.openbbt.core.docgen.StepDocEntry;
import org.myjtools.openbbt.core.docgen.StepLanguageEntry;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Generates a compact JSON index of step definitions, suitable for use as
 * context in AI-assisted autocompletion. Entries are grouped by locale so
 * consumers can filter to the language relevant to the current file.
 */
public class StepIndexJsonGenerator {

    public String generate(Map<String, StepDocEntry> steps) {
        var sb = new StringBuilder();
        var list = new ArrayList<>(steps.entrySet());
        sb.append("[\n");
        for (int i = 0; i < list.size(); i++) {
            appendStep(sb, list.get(i).getKey(), list.get(i).getValue());
            sb.append(i < list.size() - 1 ? ",\n" : "\n");
        }
        sb.append("]\n");
        return sb.toString();
    }

    private void appendStep(StringBuilder sb, String id, StepDocEntry entry) {
        sb.append("  {\n");
        sb.append("    \"id\": ").append(str(id)).append(",\n");
        sb.append("    \"description\": ").append(str(trimmed(entry.description()))).append(",\n");
        appendParameters(sb, entry.parameters());
        appendLocaleStringMap(sb, "expressions", entry.language(), StepLanguageEntry::expression, true);
        appendLocaleStringMap(sb, "examples", entry.language(), e -> trimmed(e.example()), true);
        appendLocaleListMap(sb, "assertionHints", entry.language(), StepLanguageEntry::assertionHints, false);
        sb.append("  }");
    }

    private void appendParameters(StringBuilder sb, List<ParameterDoc> params) {
        sb.append("    \"parameters\": [");
        if (params == null || params.isEmpty()) {
            sb.append("],\n");
            return;
        }
        sb.append("\n");
        for (int i = 0; i < params.size(); i++) {
            var p = params.get(i);
            sb.append("      {\"name\": ").append(str(p.name()))
              .append(", \"type\": ").append(str(p.type()))
              .append(", \"description\": ").append(str(p.description()))
              .append("}");
            if (i < params.size() - 1) sb.append(",");
            sb.append("\n");
        }
        sb.append("    ],\n");
    }

    private void appendLocaleStringMap(
            StringBuilder sb, String key,
            Map<String, StepLanguageEntry> language,
            Function<StepLanguageEntry, String> extractor,
            boolean trailingComma) {
        sb.append("    \"").append(key).append("\": {");
        if (language == null || language.isEmpty()) {
            sb.append("}").append(trailingComma ? "," : "").append("\n");
            return;
        }
        sb.append("\n");
        var locales = new ArrayList<>(language.entrySet());
        for (int i = 0; i < locales.size(); i++) {
            var e = locales.get(i);
            sb.append("      ").append(str(e.getKey())).append(": ")
              .append(str(extractor.apply(e.getValue())));
            if (i < locales.size() - 1) sb.append(",");
            sb.append("\n");
        }
        sb.append("    }").append(trailingComma ? "," : "").append("\n");
    }

    private void appendLocaleListMap(
            StringBuilder sb, String key,
            Map<String, StepLanguageEntry> language,
            Function<StepLanguageEntry, List<String>> extractor,
            boolean trailingComma) {
        sb.append("    \"").append(key).append("\": {");
        if (language == null || language.isEmpty()) {
            sb.append("}").append(trailingComma ? "," : "").append("\n");
            return;
        }
        sb.append("\n");
        var locales = new ArrayList<>(language.entrySet());
        for (int i = 0; i < locales.size(); i++) {
            var e = locales.get(i);
            List<String> vals = extractor.apply(e.getValue());
            sb.append("      ").append(str(e.getKey())).append(": ");
            if (vals == null || vals.isEmpty()) {
                sb.append("[]");
            } else {
                sb.append("[");
                for (int j = 0; j < vals.size(); j++) {
                    sb.append(str(vals.get(j)));
                    if (j < vals.size() - 1) sb.append(", ");
                }
                sb.append("]");
            }
            if (i < locales.size() - 1) sb.append(",");
            sb.append("\n");
        }
        sb.append("    }").append(trailingComma ? "," : "").append("\n");
    }

    private static String trimmed(String s) {
        return s != null ? s.strip() : null;
    }

    private static String str(String s) {
        if (s == null) return "null";
        return "\"" + s.replace("\\", "\\\\")
                        .replace("\"", "\\\"")
                        .replace("\n", "\\n")
                        .replace("\r", "")
                        .replace("\t", "\\t")
               + "\"";
    }
}
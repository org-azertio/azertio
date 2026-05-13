package org.azertio.lsp;

import org.eclipse.lsp4j.*;
import org.myjtools.gherkinparser.DefaultKeywordMapProvider;
import org.myjtools.gherkinparser.KeywordType;
import org.azertio.core.backend.StepProviderBackend;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class FeatureDiagnosticsProvider {

    static final String SOURCE = "azertio-step";
    static final int MAX_HINTS = 5;

    private static final List<KeywordType> STRUCTURE_KEYWORD_TYPES = List.of(
        KeywordType.FEATURE, KeywordType.BACKGROUND, KeywordType.SCENARIO,
        KeywordType.SCENARIO_OUTLINE, KeywordType.EXAMPLES
    );
    private static final List<KeywordType> STEP_KEYWORD_TYPES = List.of(
        KeywordType.GIVEN, KeywordType.WHEN, KeywordType.THEN, KeywordType.AND, KeywordType.BUT
    );
    private static final List<String> DEFAULT_ALL_KEYWORDS = List.of(
        "Feature:", "Background:", "Scenario:", "Scenario Outline:", "Examples:",
        "Given ", "When ", "Then ", "And ", "But "
    );

    private final StepProviderBackend backend;
    private final DefaultKeywordMapProvider keywordProvider;

    public FeatureDiagnosticsProvider(StepProviderBackend backend, DefaultKeywordMapProvider keywordProvider) {
        this.backend = backend;
        this.keywordProvider = keywordProvider;
    }

    public List<Diagnostic> validate(String content) {
        Locale locale = localeFromContent(content);
        List<String> stepKeywords = stepKeywordsForLocale(locale);
        List<String> allKeywords  = allKeywordsForLocale(locale);
        String[] lines = content.split("\n", -1);
        var diagnostics = new ArrayList<Diagnostic>();
        boolean inDocString = false;
        // After a structure keyword (Feature, Scenario, etc.) free-text description
        // lines are allowed until the first step keyword is encountered.
        boolean inDescription = true;

        for (int i = 0; i < lines.length; i++) {
            String line    = lines[i];
            String trimmed = line.stripLeading();

            if (trimmed.isBlank()) continue;

            // Toggle docstring block
            if (trimmed.startsWith("\"\"\"") || trimmed.startsWith("'''")) {
                inDocString = !inDocString;
                continue;
            }
            if (inDocString) continue;

            // Skip comments, tags, table rows
            if (trimmed.startsWith("#") || trimmed.startsWith("@") || trimmed.startsWith("|")) continue;

            // --- Step keyword check ---
            boolean foundStepKeyword = false;
            for (String kw : stepKeywords) {
                if (!trimmed.toLowerCase().startsWith((kw + " ").toLowerCase())) continue;
                foundStepKeyword = true;
                inDescription = false;
                String stepText = trimmed.substring(kw.length() + 1).stripTrailing();
                if (!stepText.isBlank()) {
                    try {
                        if (!backend.isValidStep(stepText, locale)) {
                            int startCol = line.indexOf(kw) + kw.length() + 1;
                            int endCol   = line.stripTrailing().length();
                            var range = new Range(new Position(i, startCol), new Position(i, endCol));
                            diagnostics.add(new Diagnostic(range, "Undefined step: '" + stepText + "'",
                                DiagnosticSeverity.Error, SOURCE));
                        }
                    } catch (Exception ignored) {
                        // skip if matcher cannot be built for this step
                    }
                }
                break;
            }

            if (foundStepKeyword) continue;

            // --- Structure keyword / description check ---
            boolean recognized = allKeywords.stream()
                .anyMatch(kw -> trimmed.toLowerCase().startsWith(kw.toLowerCase()));
            if (recognized) {
                // Entering a new section: description lines are allowed until the first step
                inDescription = true;
            } else if (!inDescription) {
                // Not inside a description block → unrecognized keyword
                int end = trimmed.indexOf(':');
                if (end < 0) end = trimmed.indexOf(' ');
                if (end < 0) end = trimmed.length();
                int startCol = line.length() - trimmed.length();
                var range = new Range(new Position(i, startCol), new Position(i, startCol + end));
                diagnostics.add(new Diagnostic(range,
                    "Unrecognized keyword: '" + trimmed.substring(0, end) + "'",
                    DiagnosticSeverity.Error, SOURCE));
            }
            // else: inDescription == true → free-text description line, skip silently
        }
        return diagnostics;
    }

    public List<String> hintsFor(String stepText, Locale locale) {
        return backend.hintsForStep(stepText, locale, MAX_HINTS);
    }

    private List<String> stepKeywordsForLocale(Locale locale) {
        return keywordProvider.keywordMap(locale)
            .map(km -> STEP_KEYWORD_TYPES.stream()
                .map(km::keywords)
                .filter(Objects::nonNull)
                .flatMap(List::stream)
                .distinct()
                .sorted(Comparator.comparingInt(String::length).reversed())
                .toList())
            .orElse(List.of("Given", "When", "Then", "And", "But"));
    }

    /** Returns all recognized keywords (structure with colon, step with trailing space). */
    private List<String> allKeywordsForLocale(Locale locale) {
        return keywordProvider.keywordMap(locale)
            .map(km -> {
                List<String> kws = new ArrayList<>();
                STRUCTURE_KEYWORD_TYPES.stream()
                    .map(km::keywords)
                    .filter(Objects::nonNull)
                    .flatMap(List::stream)
                    .forEach(kws::add);
                STEP_KEYWORD_TYPES.stream()
                    .map(km::keywords)
                    .filter(Objects::nonNull)
                    .flatMap(List::stream)
                    .distinct()
                    .map(kw -> kw + " ")
                    .forEach(kws::add);
                return kws;
            })
            .orElse(DEFAULT_ALL_KEYWORDS);
    }

    private Locale localeFromContent(String content) {
        for (String line : content.split("\n", 5)) {
            String trimmed = line.trim();
            if (trimmed.startsWith("# language:")) {
                return Locale.forLanguageTag(trimmed.substring("# language:".length()).trim());
            }
        }
        return Locale.getDefault();
    }
}

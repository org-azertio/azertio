package org.azertio.core.docgen;

import org.yaml.snakeyaml.Yaml;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class StepDocLoader {

    @SuppressWarnings("unchecked")
    public static Map<String, StepDocEntry> load(Path path) throws IOException {
        return load(Files.newInputStream(path));
    }

    public static Map<String, StepDocEntry> load(Path mainPath, Map<String, Path> langPaths) throws IOException {
        var langStreams = new LinkedHashMap<String, InputStream>();
        for (var entry : langPaths.entrySet()) {
            langStreams.put(entry.getKey(), Files.newInputStream(entry.getValue()));
        }
        return load(Files.newInputStream(mainPath), langStreams);
    }

    public static Map<String, StepDocEntry> load(InputStream mainStream, Map<String, InputStream> langStreams) throws IOException {
        var result = load(mainStream);
        for (var langEntry : langStreams.entrySet()) {
            var overlay = loadLanguageOverlay(langEntry.getValue());
            overlay.forEach((stepId, langDef) -> {
                var entry = result.get(stepId);
                if (entry != null) {
                    entry.language().put(langEntry.getKey(), langDef);
                }
            });
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    public static Map<String, StepLanguageEntry> loadLanguageOverlay(InputStream inputStream) throws IOException {
        Yaml yaml = new Yaml();
        Map<String, Map<String, Object>> raw;
        try (var reader = new BufferedInputStream(inputStream)) {
            raw = yaml.load(reader);
        }
        var result = new LinkedHashMap<String, StepLanguageEntry>();
        if (raw != null) {
            for (var entry : raw.entrySet()) {
                result.put(entry.getKey(), parseLanguageEntry(entry.getValue()));
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    public static Map<String, StepDocEntry> load(InputStream inputStream) throws IOException {
        Yaml yaml = new Yaml();
        Map<String, Map<String, Object>> raw;
        try (var reader = new BufferedInputStream(inputStream)) {
            raw = yaml.load(reader);
        }
        var result = new LinkedHashMap<String, StepDocEntry>();
        for (var entry : raw.entrySet()) {
            result.put(entry.getKey(), parseEntry(entry.getValue()));
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private static StepDocEntry parseEntry(Map<String, Object> map) {
        String role = (String) map.get("role");
        String description = trimTrailingWhitespace((String) map.get("description"));
        String additionalData = trimTrailingWhitespace((String) map.get("additional-data"));

        List<ParameterDoc> parameters = new ArrayList<>();
        var rawParams = (List<Map<String, Object>>) map.get("parameters");
        if (rawParams != null) {
            for (var p : rawParams) {
                parameters.add(new ParameterDoc(
                    (String) p.get("name"),
                    (String) p.get("type"),
                    (String) p.get("description")
                ));
            }
        }

        Map<String, StepLanguageEntry> language = new LinkedHashMap<>();
        var rawLanguage = (Map<String, Map<String, Object>>) map.get("language");
        if (rawLanguage != null) {
            for (var langEntry : rawLanguage.entrySet()) {
                language.put(langEntry.getKey(), parseLanguageEntry(langEntry.getValue()));
            }
        }

        return new StepDocEntry(role, description, parameters, additionalData, language);
    }

    @SuppressWarnings("unchecked")
    private static StepLanguageEntry parseLanguageEntry(Map<String, Object> map) {
        String expression = (String) map.get("expression");
        String example = trimTrailingWhitespace((String) map.get("example"));

        List<ScenarioExample> scenarios = new ArrayList<>();
        var rawScenarios = (List<Map<String, Object>>) map.get("scenarios");
        if (rawScenarios != null) {
            for (var s : rawScenarios) {
                scenarios.add(new ScenarioExample(
                    (String) s.get("title"),
                    trimTrailingWhitespace((String) s.get("gherkin"))
                ));
            }
        }

        List<String> assertionHints = new ArrayList<>();
        var rawHints = (List<String>) map.get("assertion-hints");
        if (rawHints != null) {
            assertionHints.addAll(rawHints);
        }

        return new StepLanguageEntry(expression, example, scenarios, assertionHints);
    }

    private static String trimTrailingWhitespace(String s) {
        if (s == null) return null;
        return s.stripTrailing();
    }
}
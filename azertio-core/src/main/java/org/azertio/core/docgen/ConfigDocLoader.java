package org.azertio.core.docgen;

import org.yaml.snakeyaml.Yaml;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ConfigDocLoader {

    @SuppressWarnings("unchecked")
    public static Map<String, ConfigDocEntry> load(Path path) throws IOException {
        return load(Files.newInputStream(path));
    }

    @SuppressWarnings("unchecked")
    public static Map<String, ConfigDocEntry> load(InputStream inputStream) throws IOException {
        Yaml yaml = new Yaml();
        Map<String, Map<String, Object>> raw;
        try (var reader = new BufferedInputStream(inputStream)) {
            raw = yaml.load(reader);
        }
        var result = new LinkedHashMap<String, ConfigDocEntry>();
        for (var entry : raw.entrySet()) {
            result.put(entry.getKey(), parseEntry(entry.getValue()));
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private static ConfigDocEntry parseEntry(Map<String, Object> map) {
        String description = (String) map.get("description");
        String type = (String) map.get("type");
        boolean required = Boolean.TRUE.equals(map.get("required"));
        Object defaultValue = map.get("defaultValue");

        String constraintPattern = null;
        Number constraintMin = null;
        Number constraintMax = null;
        List<String> constraintValues = null;

        Map<String, Object> constraints = (Map<String, Object>) map.get("constraints");
        if (constraints != null) {
            constraintPattern = (String) constraints.get("pattern");
            constraintMin = (Number) constraints.get("min");
            constraintMax = (Number) constraints.get("max");
            constraintValues = (List<String>) constraints.get("values");
        }

        return new ConfigDocEntry(
            description, type, required, defaultValue,
            constraintPattern, constraintMin, constraintMax, constraintValues
        );
    }
}
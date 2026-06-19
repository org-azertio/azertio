package org.azertio.core.execution;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class OutputRegistry {

    private final Map<String, String> values = Collections.synchronizedMap(new LinkedHashMap<>());

    public void set(String key, String value) {
        values.put(key, value);
    }

    public String get(String key) {
        return values.get(key);
    }

    public List<String> resolveOutputs(List<String> keys) {
        return keys.stream().filter(values::containsKey).toList();
    }

    public Map<String, String> asMap() {
        return Collections.unmodifiableMap(values);
    }
}
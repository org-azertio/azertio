package org.azertio.core.test.docgen;

import org.junit.jupiter.api.Test;
import org.azertio.core.docgen.ConfigDocEntry;
import org.azertio.core.docgen.ConfigDocMarkdownGenerator;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ConfigDocMarkdownGeneratorTest {

    private final ConfigDocMarkdownGenerator generator = new ConfigDocMarkdownGenerator();

    private static ConfigDocEntry entry(String description, String type, boolean required,
                                        Object defaultValue, String pattern,
                                        Number min, Number max, List<String> values) {
        return new ConfigDocEntry(description, type, required, defaultValue, pattern, min, max, values);
    }

    @Test
    void generate_emptyEntries_returnsOnlyTitle() {
        String result = generator.generate("My Config", Map.of());
        assertThat(result).startsWith("# My Config\n\n");
    }

    @Test
    void generate_entryWithType_includesSeparatorKeyAndType() {
        var e = entry(null, "text", false, null, null, null, null, null);
        String result = generator.generate("Config", Map.of("my.key", e));
        assertThat(result).contains("---\n\n## `my.key`\n\n");
        assertThat(result).contains("**Type** text\n\n");
    }

    @Test
    void generate_entryWithDescription_includesDescription() {
        var e = entry("A description", "text", false, null, null, null, null, null);
        String result = generator.generate("Config", Map.of("k", e));
        assertThat(result).contains("A description\n\n");
    }

    @Test
    void generate_entryWithBlankDescription_omitsDescription() {
        var e = entry("   ", "text", false, null, null, null, null, null);
        String result = generator.generate("Config", Map.of("k", e));
        assertThat(result).doesNotContain("   \n\n");
    }

    @Test
    void generate_entryRequired_includesRequiredMarker() {
        var e = entry(null, "text", true, null, null, null, null, null);
        String result = generator.generate("Config", Map.of("k", e));
        assertThat(result).contains("**Required**\n\n");
    }

    @Test
    void generate_entryNotRequired_omitsRequiredMarker() {
        var e = entry(null, "text", false, null, null, null, null, null);
        String result = generator.generate("Config", Map.of("k", e));
        assertThat(result).doesNotContain("**Required**");
    }

    @Test
    void generate_entryWithDefaultValue_includesDefaultValue() {
        var e = entry(null, "integer", false, 42, null, null, null, null);
        String result = generator.generate("Config", Map.of("k", e));
        assertThat(result).contains("**Default value** `42`\n\n");
    }

    @Test
    void generate_entryWithNullDefaultValue_omitsDefaultValue() {
        var e = entry(null, "text", false, null, null, null, null, null);
        String result = generator.generate("Config", Map.of("k", e));
        assertThat(result).doesNotContain("**Default value**");
    }

    @Test
    void generate_entryWithPattern_includesPattern() {
        var e = entry(null, "text", false, null, "A\\d\\dB", null, null, null);
        String result = generator.generate("Config", Map.of("k", e));
        assertThat(result).contains("**Pattern** `A\\d\\dB`\n\n");
    }

    @Test
    void generate_entryWithMinOnly_includesMin() {
        var e = entry(null, "integer", false, null, null, 2, null, null);
        String result = generator.generate("Config", Map.of("k", e));
        assertThat(result).contains("**Min** `2`");
        assertThat(result).doesNotContain("**Max**");
    }

    @Test
    void generate_entryWithMaxOnly_includesMax() {
        var e = entry(null, "integer", false, null, null, null, 10, null);
        String result = generator.generate("Config", Map.of("k", e));
        assertThat(result).contains("**Max** `10`");
        assertThat(result).doesNotContain("**Min**");
    }

    @Test
    void generate_entryWithMinAndMax_includesBoth() {
        var e = entry(null, "integer", false, null, null, 1, 5, null);
        String result = generator.generate("Config", Map.of("k", e));
        assertThat(result).contains("**Min** `1`");
        assertThat(result).contains("**Max** `5`");
    }

    @Test
    void generate_entryWithConstraintValues_includesValuesList() {
        var e = entry(null, "enum", false, null, null, null, null, List.of("red", "green", "blue"));
        String result = generator.generate("Config", Map.of("k", e));
        assertThat(result).contains("**Values**");
        assertThat(result).contains("`red`");
        assertThat(result).contains("`green`");
        assertThat(result).contains("`blue`");
    }

    @Test
    void generate_entryWithEmptyConstraintValues_omitsValuesSection() {
        var e = entry(null, "text", false, null, null, null, null, List.of());
        String result = generator.generate("Config", Map.of("k", e));
        assertThat(result).doesNotContain("**Values**");
    }

    @Test
    void generate_multipleEntries_allPresent() {
        var entries = new LinkedHashMap<String, ConfigDocEntry>();
        entries.put("key.one", entry(null, "text", false, null, null, null, null, null));
        entries.put("key.two", entry(null, "integer", true, 0, null, null, null, null));
        String result = generator.generate("Config", entries);
        assertThat(result).contains("## `key.one`");
        assertThat(result).contains("## `key.two`");
    }
}
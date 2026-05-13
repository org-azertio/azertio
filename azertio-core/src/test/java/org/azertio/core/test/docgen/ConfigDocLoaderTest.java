package org.azertio.core.test.docgen;

import org.junit.jupiter.api.Test;
import org.azertio.core.docgen.ConfigDocLoader;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class ConfigDocLoaderTest {

    private Path testResource(String name) throws URISyntaxException {
        return Path.of(getClass().getResource("/" + name).toURI());
    }

    @Test
    void load_parsesAllEntries() throws IOException, URISyntaxException {
        var entries = ConfigDocLoader.load(testResource("config.yaml"));
        assertThat(entries).hasSize(6);
    }

    @Test
    void load_preservesInsertionOrder() throws IOException, URISyntaxException {
        var entries = ConfigDocLoader.load(testResource("config.yaml"));
        assertThat(entries.keySet()).startsWith(
            "defined.property.required",
            "defined.property.with-default-value",
            "defined.property.regex-text"
        );
    }

    @Test
    void load_parsesDescription() throws IOException, URISyntaxException {
        var entries = ConfigDocLoader.load(testResource("config.yaml"));
        assertThat(entries.get("defined.property.required").description())
            .isEqualTo("This is a test property that is required");
    }

    @Test
    void load_parsesType() throws IOException, URISyntaxException {
        var entries = ConfigDocLoader.load(testResource("config.yaml"));
        assertThat(entries.get("defined.property.required").type()).isEqualTo("text");
        assertThat(entries.get("defined.property.with-default-value").type()).isEqualTo("integer");
        assertThat(entries.get("defined.property.boolean").type()).isEqualTo("boolean");
    }

    @Test
    void load_parsesRequiredTrue() throws IOException, URISyntaxException {
        var entries = ConfigDocLoader.load(testResource("config.yaml"));
        assertThat(entries.get("defined.property.required").required()).isTrue();
    }

    @Test
    void load_requiredIsFalseByDefault() throws IOException, URISyntaxException {
        var entries = ConfigDocLoader.load(testResource("config.yaml"));
        assertThat(entries.get("defined.property.with-default-value").required()).isFalse();
    }

    @Test
    void load_parsesDefaultValue() throws IOException, URISyntaxException {
        var entries = ConfigDocLoader.load(testResource("config.yaml"));
        assertThat(entries.get("defined.property.with-default-value").defaultValue()).isEqualTo(5);
    }

    @Test
    void load_defaultValueIsNullWhenAbsent() throws IOException, URISyntaxException {
        var entries = ConfigDocLoader.load(testResource("config.yaml"));
        assertThat(entries.get("defined.property.required").defaultValue()).isNull();
    }

    @Test
    void load_parsesPatternConstraint() throws IOException, URISyntaxException {
        var entries = ConfigDocLoader.load(testResource("config.yaml"));
        assertThat(entries.get("defined.property.regex-text").constraintPattern()).isEqualTo("A\\d\\dB");
    }

    @Test
    void load_parsesMinMaxConstraints() throws IOException, URISyntaxException {
        var entries = ConfigDocLoader.load(testResource("config.yaml"));
        var entry = entries.get("defined.property.min-max-number");
        assertThat(entry.constraintMin()).isEqualTo(2);
        assertThat(entry.constraintMax()).isEqualTo(3);
    }

    @Test
    void load_parsesEnumValues() throws IOException, URISyntaxException {
        var entries = ConfigDocLoader.load(testResource("config.yaml"));
        assertThat(entries.get("defined.property.enumeration").constraintValues())
            .containsExactly("red", "yellow", "orange");
    }

    @Test
    void load_constraintsAreNullWhenAbsent() throws IOException, URISyntaxException {
        var entries = ConfigDocLoader.load(testResource("config.yaml"));
        var entry = entries.get("defined.property.required");
        assertThat(entry.constraintPattern()).isNull();
        assertThat(entry.constraintMin()).isNull();
        assertThat(entry.constraintMax()).isNull();
        assertThat(entry.constraintValues()).isNull();
    }

}
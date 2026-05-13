package org.azertio.docgen.test;

import org.junit.jupiter.api.Test;
import org.azertio.core.docgen.ConfigDocLoader;
import org.azertio.docgen.ConfigDocMarkdownGenerator;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.assertj.core.api.Assertions.assertThat;

class ConfigDocMarkdownGeneratorTest {

    private final ConfigDocMarkdownGenerator generator = new ConfigDocMarkdownGenerator();

    private Path testResource(String name) throws URISyntaxException {
        return Path.of(getClass().getResource("/" + name).toURI());
    }

    @Test
    void load_parsesAllEntries() throws IOException, URISyntaxException {
        var entries = ConfigDocLoader.load(testResource("config.yaml"));
        var result = generator.generate("Config Docs", "config.yaml", entries);
        assertThat(result).isEqualTo(Files.readString(testResource("config.md")));
    }



}
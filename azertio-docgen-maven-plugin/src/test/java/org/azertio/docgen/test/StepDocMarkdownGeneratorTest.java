package org.azertio.docgen.test;

import org.junit.jupiter.api.Test;
import org.azertio.core.docgen.StepDocLoader;
import org.azertio.docgen.StepDocMarkdownGenerator;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.assertj.core.api.Assertions.assertThat;

class StepDocMarkdownGeneratorTest {

    private final StepDocMarkdownGenerator generator = new StepDocMarkdownGenerator();

    private Path testResource(String name) throws URISyntaxException {
        return Path.of(getClass().getResource("/" + name).toURI());
    }

    @Test
    void generate_includesTitleAsH1() throws URISyntaxException, IOException {
        var steps = StepDocLoader.load(testResource("step-doc.yaml"));
        var result = generator.generate("My steps", "step-doc.yaml", steps);
        System.out.println(result);
        assertThat(result).isEqualTo(Files.readString(testResource("step-doc.md")));
    }


}
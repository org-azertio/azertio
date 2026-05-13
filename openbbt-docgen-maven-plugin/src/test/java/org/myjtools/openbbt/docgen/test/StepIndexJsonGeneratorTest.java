package org.myjtools.openbbt.docgen.test;

import org.junit.jupiter.api.Test;
import org.myjtools.openbbt.core.docgen.StepDocLoader;
import org.myjtools.openbbt.docgen.StepIndexJsonGenerator;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class StepIndexJsonGeneratorTest {

    private final StepIndexJsonGenerator generator = new StepIndexJsonGenerator();

    private Path testResource(String name) throws URISyntaxException {
        return Path.of(getClass().getResource("/" + name).toURI());
    }

    @Test
    void generate_producesJsonArray() throws URISyntaxException, IOException {
        var steps = StepDocLoader.load(testResource("step-doc.yaml"));
        var result = generator.generate(steps);

        assertThat(result).startsWith("[\n");
        assertThat(result).endsWith("]\n");
    }

    @Test
    void generate_includesStepIds() throws URISyntaxException, IOException {
        var steps = StepDocLoader.load(testResource("step-doc.yaml"));
        var result = generator.generate(steps);

        assertThat(result).contains("\"id\": \"rest.request.GET\"");
        assertThat(result).contains("\"id\": \"rest.request.POST\"");
    }

    @Test
    void generate_includesExpressionsByLocale() throws URISyntaxException, IOException {
        var steps = StepDocLoader.load(testResource("step-doc.yaml"));
        var result = generator.generate(steps);

        assertThat(result).contains("\"en\": \"I make a GET request to {endpoint:text}\"");
        assertThat(result).contains("\"es\": \"Hago una petición GET a {endpoint:text}\"");
    }

    @Test
    void generate_includesParameters() throws URISyntaxException, IOException {
        var steps = StepDocLoader.load(testResource("step-doc.yaml"));
        var result = generator.generate(steps);

        assertThat(result).contains("\"name\": \"endpoint\"");
        assertThat(result).contains("\"type\": \"text\"");
    }

    @Test
    void generate_includesAssertionHints() throws URISyntaxException, IOException {
        var steps = StepDocLoader.load(testResource("step-doc.yaml"));
        var result = generator.generate(steps);

        assertThat(result).contains("\"assertionHints\"");
    }

    @Test
    void generate_handlesNullExample() throws URISyntaxException, IOException {
        var steps = StepDocLoader.load(testResource("step-doc.yaml"));
        var result = generator.generate(steps);

        // rest.request.POST has no 'es' example — should emit null, not throw
        assertThat(result).contains("\"id\": \"rest.request.POST\"");
    }
}
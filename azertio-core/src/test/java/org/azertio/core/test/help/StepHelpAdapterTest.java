package org.azertio.core.test.help;

import org.junit.jupiter.api.Test;
import org.azertio.core.help.StepHelpAdapter;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class StepHelpAdapterTest {

    static class TestStepAdapter extends StepHelpAdapter {
        TestStepAdapter(String resource) {
            super("test.id", "Test", "Test Title", resource, Map.of());
        }
    }

    @Test
    void help_whenResourceNotFound_returnsEmpty() {
        assertThat(new TestStepAdapter("nonexistent-resource.yaml").help()).isEmpty();
    }

    @Test
    void help_whenResourceFound_returnsMarkdownWithTitle() {
        String result = new TestStepAdapter("step-doc.yaml").help();
        assertThat(result).isNotEmpty();
        assertThat(result).startsWith("# Test Title");
    }
}
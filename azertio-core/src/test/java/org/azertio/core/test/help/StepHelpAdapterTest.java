package org.azertio.core.test.help;

import org.junit.jupiter.api.Test;
import org.azertio.core.help.StepHelpAdapter;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class StepHelpAdapterTest {

    @Test
    void help_whenResourceNotFound_returnsEmpty() {
        var adapter = new StepHelpAdapter() {
            @Override public String id() { return "test"; }
            @Override public String displayName() { return "Test"; }
            @Override protected String resource() { return "nonexistent-resource.yaml"; }
            @Override protected String title() { return "Test"; }
            @Override protected Map<String, String> languageResources() { return Map.of(); }
        };
        assertThat(adapter.help()).isEmpty();
    }

    @Test
    void help_whenResourceFound_returnsMarkdownWithTitle() {
        var adapter = new StepHelpAdapter() {
            @Override public String id() { return "test"; }
            @Override public String displayName() { return "Test"; }
            @Override protected String resource() { return "step-doc.yaml"; }
            @Override protected String title() { return "Test Steps"; }
            @Override protected Map<String, String> languageResources() { return Map.of(); }
        };
        String result = adapter.help();
        assertThat(result).isNotEmpty();
        assertThat(result).startsWith("# Test Steps");
    }
}
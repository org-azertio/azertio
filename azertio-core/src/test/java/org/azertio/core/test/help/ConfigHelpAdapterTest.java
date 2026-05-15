package org.azertio.core.test.help;

import org.junit.jupiter.api.Test;
import org.azertio.core.help.ConfigHelpAdapter;

import static org.assertj.core.api.Assertions.assertThat;

class ConfigHelpAdapterTest {

    @Test
    void help_whenResourceNotFound_returnsEmpty() {
        var adapter = new ConfigHelpAdapter() {
            @Override public String id() { return "test"; }
            @Override public String displayName() { return "Test"; }
            @Override protected String resource() { return "nonexistent-resource.yaml"; }
            @Override protected String title() { return "Test"; }
        };
        assertThat(adapter.help()).isEmpty();
    }

    @Test
    void help_whenResourceFound_returnsMarkdownWithTitle() {
        var adapter = new ConfigHelpAdapter() {
            @Override public String id() { return "test"; }
            @Override public String displayName() { return "Test"; }
            @Override protected String resource() { return "config.yaml"; }
            @Override protected String title() { return "Test Configuration"; }
        };
        String result = adapter.help();
        assertThat(result).isNotEmpty();
        assertThat(result).startsWith("# Test Configuration");
    }
}
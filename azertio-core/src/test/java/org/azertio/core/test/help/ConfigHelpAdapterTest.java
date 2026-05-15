package org.azertio.core.test.help;

import org.junit.jupiter.api.Test;
import org.azertio.core.help.ConfigHelpAdapter;

import static org.assertj.core.api.Assertions.assertThat;

class ConfigHelpAdapterTest {

    static class TestConfigAdapter extends ConfigHelpAdapter {
        TestConfigAdapter(String resource) {
            super("test.id", "Test", "Test Config", resource);
        }
    }

    @Test
    void help_whenResourceNotFound_returnsEmpty() {
        assertThat(new TestConfigAdapter("nonexistent-resource.yaml").help()).isEmpty();
    }

    @Test
    void help_whenResourceFound_returnsMarkdownWithTitle() {
        String result = new TestConfigAdapter("config.yaml").help();
        assertThat(result).isNotEmpty();
        assertThat(result).startsWith("# Test Config");
    }
}
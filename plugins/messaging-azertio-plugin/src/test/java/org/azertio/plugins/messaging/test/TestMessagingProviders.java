package org.azertio.plugins.messaging.test;

import org.junit.jupiter.api.Test;
import org.azertio.plugins.messaging.MessagingAIIndexProvider;
import org.azertio.plugins.messaging.MessagingConfigHelpProvider;
import org.azertio.plugins.messaging.MessagingConfigProvider;
import org.azertio.plugins.messaging.MessagingMessageProvider;
import org.azertio.plugins.messaging.MessagingStepHelpProvider;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TestMessagingProviders {

    static class ExposedConfigProvider extends MessagingConfigProvider {
        String resourceName() { return resource(); }
    }

    static class ExposedMessageProvider extends MessagingMessageProvider {
        Map<String, String> languages() { return languageResources(); }
    }

    @Test
    void configProvider_resourceIsConfigYaml() {
        assertThat(new ExposedConfigProvider().resourceName()).isEqualTo("config.yaml");
    }

    @Test
    void configHelpProvider_instantiates() {
        assertThat(new MessagingConfigHelpProvider()).isNotNull();
    }

    @Test
    void stepHelpProvider_instantiates() {
        assertThat(new MessagingStepHelpProvider()).isNotNull();
    }

    @Test
    void messageProvider_providerFor_matchesStepProviderCategory() {
        MessagingMessageProvider provider = new MessagingMessageProvider();
        assertThat(provider.providerFor("MessagingStepProvider")).isTrue();
        assertThat(provider.providerFor("Other")).isFalse();
    }

    @Test
    void messageProvider_languageResourcesContainsAllLocales() {
        var resources = new ExposedMessageProvider().languages();
        assertThat(resources).containsKeys("dsl", "en", "es");
    }

    @Test
    void aiIndexProvider_stepIndexJson_returnsContent() {
        String result = new MessagingAIIndexProvider().stepIndexJson();
        assertThat(result).isNotNull();
    }

}
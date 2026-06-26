package org.azertio.plugins.messaging.test;

import org.apache.activemq.broker.BrokerService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.azertio.core.AzertioRuntime;
import org.azertio.core.ContentTypes;
import org.azertio.core.testplan.Document;
import org.azertio.plugins.messaging.JmsMessagingEngine;
import org.azertio.plugins.messaging.MessagingStepProvider;
import org.myjtools.imconfig.Config;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TestMessagingStepProvider {

    @Test
    void pluginLoadsConfiguration() {
        Config config = Config.ofMap(Map.of(
            "core.resourcePath", "src/test/resources",
            "core.resourceFilter", "**/*.feature"
        ));
        AzertioRuntime runtime = new AzertioRuntime(config);
        assertThat(runtime).isNotNull();
    }

    static class TestableProvider extends MessagingStepProvider {
        @Override
        protected String interpolate(String text) {
            return text;
        }
    }

    @Nested
    class WithBroker {

        private BrokerService broker;
        private JmsMessagingEngine engine;
        private TestableProvider provider;
        private String brokerUrl;

        @BeforeEach
        void setUp() throws Exception {
            broker = new BrokerService();
            broker.setPersistent(false);
            broker.setUseJmx(false);
            var connector = broker.addConnector("tcp://localhost:0");
            broker.start();
            brokerUrl = connector.getConnectUri().toString();

            engine = new JmsMessagingEngine();
            engine.init("org.apache.activemq.ActiveMQConnectionFactory", brokerUrl, "", "");

            provider = new TestableProvider();
            setField(provider, "contentTypes", ContentTypes.of(List.of()));
            setField(provider, "defaultTimeout", 5);
            engines(provider).put("main", engine);
        }

        @AfterEach
        void tearDown() throws Exception {
            engine.close();
            broker.stop();
        }

        @Test
        void use_withKnownAlias_doesNotThrow() {
            provider.use("main");
        }

        @Test
        void use_withUnknownAlias_throws() {
            assertThatThrownBy(() -> provider.use("missing"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("missing");
        }

        @Test
        void singleEngine_autoSelectedOnSubscribe() {
            provider.subscribe("topic://auto");
        }

        @Test
        void multipleEngines_withoutUse_throws() throws Exception {
            engines(provider).put("secondary", engine);
            assertThatThrownBy(() -> provider.subscribe("topic://any"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Multiple messaging systems");
        }

        @Test
        void publish_andAssertReceived_textContains() {
            provider.use("main");
            provider.subscribe("topic://orders");
            provider.publish("topic://orders", Document.of("text/plain", "order-123"));
            provider.assertReceived("topic://orders", Document.of("text/plain", "order-123"));
        }

        @Test
        void publishKeyed_andAssertReceived() {
            provider.use("main");
            provider.subscribe("topic://keyed");
            provider.publishKeyed("topic://keyed", "key-1", Document.of("text/plain", "keyed body"));
            provider.assertReceived("topic://keyed", Document.of("text/plain", "keyed body"));
        }

        @Test
        void assertReceivedExact_withMatchingContent() {
            provider.use("main");
            provider.subscribe("topic://exact");
            provider.publish("topic://exact", Document.of("text/plain", "exact msg"));
            provider.assertReceivedExact("topic://exact", Document.of("text/plain", "exact msg"));
        }

        @Test
        void assertReceivedExact_withWrongContent_throws() {
            provider.use("main");
            provider.subscribe("topic://fail");
            provider.publish("topic://fail", Document.of("text/plain", "actual"));
            assertThatThrownBy(() -> provider.assertReceivedExact("topic://fail", Document.of("text/plain", "expected")))
                .isInstanceOf(AssertionError.class);
        }

        @Test
        void close_doesNotThrow() {
            provider.close();
        }

        @Test
        void init_withValidConfig_connectsAndCreatesEngine() throws Exception {
            Config config = Config.ofMap(Map.of(
                "messaging.timeout", "5",
                "messaging.systems.mybroker.connectionFactoryClass", "org.apache.activemq.ActiveMQConnectionFactory",
                "messaging.systems.mybroker.brokerUrl", brokerUrl
            ));
            TestableProvider fresh = new TestableProvider();
            setField(fresh, "contentTypes", ContentTypes.of(List.of()));
            fresh.init(config);
            fresh.subscribe("topic://init-test");
            fresh.close();
        }

        @Test
        void assertReceived_contentNotFound_throws() {
            provider.use("main");
            provider.subscribe("topic://partial");
            provider.publish("topic://partial", Document.of("text/plain", "actual content"));
            assertThatThrownBy(() -> provider.assertReceived("topic://partial", Document.of("text/plain", "not present")))
                .isInstanceOf(AssertionError.class);
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, JmsMessagingEngine> engines(MessagingStepProvider p) throws Exception {
        Field f = MessagingStepProvider.class.getDeclaredField("engines");
        f.setAccessible(true);
        return (Map<String, JmsMessagingEngine>) f.get(p);
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field f = MessagingStepProvider.class.getDeclaredField(name);
        f.setAccessible(true);
        f.set(target, value);
    }

}
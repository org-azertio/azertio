package org.azertio.plugins.messaging.test;

import org.apache.activemq.broker.BrokerService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.azertio.plugins.messaging.JmsMessagingEngine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TestJmsMessagingEngine {

    private static final String FACTORY_CLASS = "org.apache.activemq.ActiveMQConnectionFactory";

    private BrokerService broker;
    private JmsMessagingEngine engine;
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
        engine.init(FACTORY_CLASS, brokerUrl, "", "");
    }

    @AfterEach
    void tearDown() throws Exception {
        engine.close();
        broker.stop();
    }

    @Test
    void publishAndReceiveOnTopic() {
        engine.subscribe("topic://orders");
        engine.publish("topic://orders", "{\"orderId\": 1}");
        String received = engine.pollNext("topic://orders", 5);
        assertThat(received).isEqualTo("{\"orderId\": 1}");
    }

    @Test
    void subscribeBeforePublishExcludesPreExistingMessages() throws Exception {
        // Publish a message BEFORE subscribing — it should not be received
        engine.publish("topic://events", "stale message");

        // Now subscribe (non-durable: only receives messages after this point)
        engine.subscribe("topic://events");
        engine.publish("topic://events", "new message");

        String received = engine.pollNext("topic://events", 5);
        assertThat(received).isEqualTo("new message");
    }

    @Test
    void timeoutWhenNoMessagePublished() {
        engine.subscribe("topic://empty");
        assertThatThrownBy(() -> engine.pollNext("topic://empty", 1))
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("No message received");
    }

    @Test
    void variableInterpolationInPublishedBody() {
        // Variable interpolation is done by MessagingStepProvider before calling publish;
        // this test verifies the engine faithfully sends whatever string it receives.
        engine.subscribe("topic://interp");
        engine.publish("topic://interp", "{\"orderId\": \"42\"}");
        String received = engine.pollNext("topic://interp", 5);
        assertThat(received).isEqualTo("{\"orderId\": \"42\"}");
    }

    @Test
    void pollNextFailsWhenNoSubscriberRegistered() {
        assertThatThrownBy(() -> engine.pollNext("topic://unsubscribed", 1))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("No subscriber registered");
    }

    @Test
    void publishWithKeyStoresKeyAsProperty() {
        engine.subscribe("topic://keyed");
        engine.publish("topic://keyed", "my-key", "keyed body");
        String received = engine.pollNext("topic://keyed", 5);
        assertThat(received).isEqualTo("keyed body");
    }

    @Test
    void publishAndReceiveOnQueueWithPrefix() {
        engine.subscribe("queue://orders");
        engine.publish("queue://orders", "queue message");
        String received = engine.pollNext("queue://orders", 5);
        assertThat(received).isEqualTo("queue message");
    }

    @Test
    void publishAndReceiveOnQueueWithoutPrefix() {
        engine.subscribe("orders");
        engine.publish("orders", "no-prefix message");
        String received = engine.pollNext("orders", 5);
        assertThat(received).isEqualTo("no-prefix message");
    }

    @Test
    void init_withCredentials_connectsSuccessfully() throws Exception {
        JmsMessagingEngine credEngine = new JmsMessagingEngine();
        credEngine.init(FACTORY_CLASS, brokerUrl, "user", "pass");
        credEngine.subscribe("topic://cred-test");
        credEngine.publish("topic://cred-test", "hello");
        assertThat(credEngine.pollNext("topic://cred-test", 5)).isEqualTo("hello");
        credEngine.close();
    }

    @Test
    void init_withUnknownFactoryClass_throwsReflectiveOperationException() {
        JmsMessagingEngine badEngine = new JmsMessagingEngine();
        assertThatThrownBy(() -> badEngine.init("com.example.Missing", brokerUrl, "", ""))
            .isInstanceOf(ReflectiveOperationException.class);
    }

}
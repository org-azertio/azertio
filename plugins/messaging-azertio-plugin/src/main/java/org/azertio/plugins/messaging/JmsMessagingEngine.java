package org.azertio.plugins.messaging;

import jakarta.jms.Connection;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.Destination;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.MessageConsumer;
import jakarta.jms.MessageProducer;
import jakarta.jms.Session;
import jakarta.jms.TextMessage;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class JmsMessagingEngine implements MessagingEngine {

    private Connection connection;
    private Session session;
    private final Map<String, MessageConsumer> consumers = new ConcurrentHashMap<>();

    public void init(String connectionFactoryClass, String brokerUrl, String username, String password)
            throws ReflectiveOperationException, JMSException {
        ConnectionFactory factory = loadFactory(connectionFactoryClass, brokerUrl);
        connection = (username != null && !username.isEmpty())
            ? factory.createConnection(username, password)
            : factory.createConnection();
        connection.start();
        session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
    }

    @Override
    public void subscribe(String destination) {
        try {
            Destination dest = resolveDestination(destination);
            MessageConsumer consumer = session.createConsumer(dest);
            consumers.put(destination, consumer);
        } catch (JMSException e) {
            throw new RuntimeException("Failed to subscribe to destination '" + destination + "': " + e.getMessage(), e);
        }
    }

    @Override
    public void publish(String destination, String body) {
        try (MessageProducer producer = session.createProducer(resolveDestination(destination))) {
            TextMessage message = session.createTextMessage(body);
            producer.send(message);
        } catch (JMSException e) {
            throw new RuntimeException("Failed to publish to destination '" + destination + "': " + e.getMessage(), e);
        }
    }

    @Override
    public void publish(String destination, String key, String body) {
        try (MessageProducer producer = session.createProducer(resolveDestination(destination))) {
            TextMessage message = session.createTextMessage(body);
            message.setStringProperty("key", key);
            producer.send(message);
        } catch (JMSException e) {
            throw new RuntimeException("Failed to publish to destination '" + destination + "': " + e.getMessage(), e);
        }
    }

    @Override
    public String pollNext(String destination, int timeoutSeconds) {
        MessageConsumer consumer = consumers.get(destination);
        if (consumer == null) {
            throw new IllegalStateException(
                "No subscriber registered for destination '" + destination + "'. " +
                "Use messaging.subscribe before asserting."
            );
        }
        try {
            Message message = consumer.receive(timeoutSeconds * 1000L);
            if (message == null) {
                throw new AssertionError(
                    "No message received on destination '" + destination + "' within " + timeoutSeconds + " seconds"
                );
            }
            if (message instanceof TextMessage text) {
                return text.getText();
            }
            throw new AssertionError("Received a non-text JMS message on destination '" + destination + "'");
        } catch (JMSException e) {
            throw new RuntimeException("Error receiving message from '" + destination + "': " + e.getMessage(), e);
        }
    }

    @Override
    public void close() {
        try {
            for (MessageConsumer consumer : consumers.values()) {
                try { consumer.close(); } catch (JMSException ignored) { }
            }
            consumers.clear();
            if (session != null) session.close();
            if (connection != null) connection.close();
        } catch (JMSException e) {
            throw new RuntimeException("Error closing JMS connection: " + e.getMessage(), e);
        }
    }

    private static Class<?> findClass(String className) {
        try { return Class.forName(className); } catch (ClassNotFoundException ignored) { }
        ModuleLayer layer = JmsMessagingEngine.class.getModule().getLayer();
        if (layer == null) return null;
        for (Module module : layer.modules()) {
            ClassLoader loader = module.getClassLoader();
            if (loader == null) continue;
            try { return loader.loadClass(className); } catch (ClassNotFoundException ignored) { }
        }
        return null;
    }

    private Destination resolveDestination(String destination) throws JMSException {
        if (destination.startsWith("topic://")) {
            return session.createTopic(destination.substring("topic://".length()));
        } else if (destination.startsWith("queue://")) {
            return session.createQueue(destination.substring("queue://".length()));
        }
        // default to queue if no prefix
        return session.createQueue(destination);
    }

    @SuppressWarnings("unchecked")
    private static ConnectionFactory loadFactory(String className, String brokerUrl)
            throws ReflectiveOperationException {
        Class<?> clazz = findClass(className);
        if (clazz == null) {
            throw new ClassNotFoundException(
                "JMS ConnectionFactory class not found: " + className +
                ". Ensure the broker client JAR is declared with `with` in azertio.yaml."
            );
        }
        // Try (String url) constructor first — works with ActiveMQ Classic, Artemis
        try {
            return (ConnectionFactory) clazz.getConstructor(String.class).newInstance(brokerUrl);
        } catch (NoSuchMethodException e) {
            // Fall back to no-arg constructor (RabbitMQ, etc.)
            return (ConnectionFactory) clazz.getDeclaredConstructor().newInstance();
        }
    }

}
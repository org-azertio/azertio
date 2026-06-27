## Context

Azertio's plugin architecture follows a consistent pattern: a `StepProvider` + `ConfigProvider` pair backed by an engine interface with one or more implementations. The REST plugin is the closest analogue — it has a `RestEngine` interface, a `JdkHttpEngine` implementation, and a `RestStepProvider` that delegates all I/O to the engine.

The DB plugin is the architectural model for generic broker support: it uses the JDBC API (standard JDK interface), with the actual driver JAR provided by the user as a `with` dependency. The `messaging-azertio-plugin` follows the same pattern using **Jakarta Messaging (JMS)** — the standard Java API for messaging brokers.

## Goals / Non-Goals

**Goals:**
- Provide Gherkin steps to publish messages to JMS destinations (topics and queues)
- Provide steps to assert that a message was received on a destination, with content assertions (JSON field, full body, plain text)
- Support a subscribe-before-act-then-assert pattern to handle asynchronous delivery
- Support a configurable timeout when waiting for messages
- Work with any JMS-compliant broker: ActiveMQ Classic, ActiveMQ Artemis, RabbitMQ (via rabbitmq-jms-client), IBM MQ, HornetQ, etc.
- Follow the existing plugin conventions exactly (same pom structure, SPI, YAML definitions)

**Non-Goals:**
- Kafka support (Kafka does not implement JMS; a future `kafka-azertio-plugin` will handle this separately)
- Avro / Schema Registry support (v1.0 handles text and JSON only)
- Message ordering assertions
- Dead-letter queue assertions
- Selector-based filtering (deferred)

## Decisions

### 1. Jakarta Messaging (JMS) as the broker abstraction

Same pattern as the DB plugin and JDBC. `messaging-azertio-plugin` depends only on `jakarta.jms-api` (the API JAR, no implementation). Users add their broker's JMS provider JAR as a `with` dependency in `azertio.yaml`.

**Examples:**
- ActiveMQ Classic: `with org.apache.activemq:activemq-all`
- ActiveMQ Artemis: `with org.apache.activemq:artemis-jms-client-all`
- RabbitMQ: `with com.rabbitmq.jms:rabbitmq-jms`

**Rationale:** Zero broker coupling in the plugin itself. Any JMS-compliant broker works out-of-the-box without any Azertio-side code.

**Alternative considered:** Kafka-native implementation (`kafka-clients`). Rejected — Kafka does not implement JMS; it would lock the plugin to a single broker.

---

### 2. ConnectionFactory loaded by class name (reflection)

The config property `messaging.connectionFactoryClass` holds the fully qualified class name of the JMS `ConnectionFactory` implementation. The engine loads it at `init()` time using reflection, mirroring how older JDBC drivers are registered via `Class.forName()`.

The engine tries a `(String url)` constructor first (supported by ActiveMQ, Artemis), then falls back to a no-arg constructor.

```
messaging.connectionFactoryClass = org.apache.activemq.ActiveMQConnectionFactory
messaging.brokerUrl              = tcp://localhost:61616
```

**Alternative considered:** JNDI lookup. Rejected — adds operational complexity not justified in a testing tool.

---

### 3. Subscribe-before-assert step design

The step sequence is:
```
Given I subscribe to destination "order.created"    ← creates JMS consumer before the action
When  I make a POST request to "orders" ...         ← REST action (produces event)
Then  destination "order.created" should contain    ← consumer.receive(timeout)
      """json
      { "orderId": "${id}" }
      """
```

For **JMS Topics** (non-durable subscriptions), a subscriber only receives messages published after it was created. This naturally isolates each test scenario — no stale messages from previous runs.

For **JMS Queues**, messages accumulate until consumed. Test authors must ensure queues are clean before subscribing, or use dedicated test queues.

**Alternative considered:** No subscribe step, always seek to latest. Rejected — requires broker-specific concepts not available in JMS.

---

### 4. No background polling thread needed

JMS `MessageConsumer.receive(long timeout)` is a blocking call that returns the next available message or `null` after the timeout. No background thread is needed — the assert step blocks directly.

This is simpler and more reliable than the Kafka approach.

---

### 5. Message format: text and JSON for v1.0

Values are sent as JMS `TextMessage`. JSON assertion uses the same path/contains logic already present in core (via `ContentTypes`). Binary message support deferred.

---

### 6. Engine interface for future extensibility

`MessagingEngine` is an internal interface wrapping JMS semantics. `JmsMessagingEngine` is the sole v1.0 implementation. This allows adding a non-JMS engine (e.g., a future Kafka engine) without changing `MessagingStepProvider`.

## Risks / Trade-offs

- **RabbitMQ routing key model differs from JMS** → RabbitMQ's JMS client maps JMS Topics to AMQP exchanges. Users may need to configure exchanges explicitly. Documented in config.
- **Queue stale messages** → Test authors must design test queues or drain them in teardown. Not specific to this plugin.
- **Class not found if provider JAR missing** → `init()` throws with a descriptive message including the required `with` declaration.
- **JMS provider JPMS compatibility** → Some older JMS provider JARs may not be modular. Loaded as automatic modules via `with`, which works for unnamed-module JARs.

## Migration Plan

No migration needed — this is a new artifact. Plugin is opt-in.

## Open Questions

- Should `messaging.destination` support both `topic://name` and `queue://name` URI syntax, or use separate config properties? **Lean toward URI prefix syntax for clarity.**
- Should assertion steps fail immediately if the consumer was never subscribed, or wait and time out? **Lean toward: fail immediately with a clear error if no subscriber exists for the destination.**
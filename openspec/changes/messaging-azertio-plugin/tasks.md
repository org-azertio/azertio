## 1. Maven Module Setup

- [x] 1.1 Create `plugins/messaging-azertio-plugin/` directory with `pom.xml` (parent: `azertio-plugin-starter:1.2.0`, groupId: `org.azertio.plugins`, version: `1.0.0-SNAPSHOT`)
- [x] 1.2 Add `jakarta.jms-api` (Jakarta Messaging 3.x) compile dependency to `pom.xml`; add `activemq-all` (ActiveMQ Classic 6.x) as test-scope dependency for the embedded broker in tests
- [x] 1.3 Add `module-info.java` declaring the module and its exports
- [x] 1.4 Add the plugin to the `plugins` profile in the root `pom.xml`

## 2. Engine Interface and JMS Implementation

- [x] 2.1 Create `MessagingEngine` interface with methods: `subscribe(destination)`, `publish(destination, body)`, `publish(destination, key, body)`, `pollNext(destination, timeoutSeconds)`, `close()`
- [x] 2.2 Create `JmsMessagingEngine` implementing `MessagingEngine`; load `ConnectionFactory` by class name using `ClassFinder` (core utility); create and start the JMS `Connection`
- [x] 2.3 Implement `subscribe(destination)`: parse destination URI (`topic://name` or `queue://name`); create a JMS `MessageConsumer` for that destination and register it keyed by destination string
- [x] 2.4 Implement `pollNext(destination, timeoutSeconds)`: call `consumer.receive(timeoutMillis)` on the registered consumer; throw if no consumer exists for that destination; return message text or throw on timeout

## 3. Step Provider and Config

- [x] 3.1 `MessagingConfigProvider` scaffold created (extends `ConfigAdapter`, annotated `@Extension`)
- [x] 3.2 Fill in `config.yaml` with all properties under `messaging.systems.<alias>.*`: `connectionFactoryClass`, `brokerUrl`, `username`, `password`; plus `messaging.timeout` (default 10)
- [x] 3.3 Flesh out `MessagingStepProvider` (`@Extension`, `Scope.TRANSIENT`) with multi-system support: `init(Config)` iterates `messaging.systems.*` and builds one `JmsMessagingEngine` per alias; `@TearDown close()` closes all engines
- [x] 3.4 Implement `@StepExpression("messaging.use")` — selects active system by alias
- [x] 3.5 Implement `@StepExpression("messaging.subscribe")` — calls `engine.subscribe(destination)`
- [x] 3.6 Implement `@StepExpression("messaging.publish")` — publishes body docstring to destination; interpolates variables
- [x] 3.7 Implement `@StepExpression("messaging.publish.keyed")` — publishes with key property and body; interpolates both
- [x] 3.8 Implement `@StepExpression("messaging.assert.received")` — calls `pollNext`; asserts body contains the given JSON/text
- [x] 3.9 Implement `@StepExpression("messaging.assert.received.exact")` — calls `pollNext`; asserts exact body match
- [x] 3.10 Implement `@StepExpression("messaging.extract.field")` — calls `pollNext`; extracts JSON field into named variable via `ExecutionContext`

## 4. Step Definitions (YAML)

- [x] 4.1 Fill in `steps.yaml` with descriptions and roles for all steps
- [x] 4.2 Fill in `steps_en.yaml` with English natural-language expressions
- [x] 4.3 Fill in `steps_es.yaml` with Spanish expressions
- [x] 4.4 Fill in `steps_dsl.yaml` with compact DSL aliases

## 5. SPI Registration

- [x] 5.1 SPI registered via `provides ... with ...` in `module-info.java` (same mechanism as all other plugins)

## 6. Tests

- [x] 6.1 Add `activemq-all` (ActiveMQ 6.x) as test dependency to `pom.xml`
- [x] 6.2 Write integration test: publish a message and assert it is received on the same topic
- [x] 6.3 Write integration test: subscribe before publish, assert pre-existing messages on a topic are excluded
- [x] 6.4 Write integration test: timeout fires when no message is published
- [x] 6.5 Write integration test: message body is faithfully sent and received (interpolation contract)

## 7. Documentation

- [x] 7.1 `config.yaml` has complete descriptions for all properties
- [x] 7.2 Add `messaging-azertio-plugin` card to `www/index.html` plugins section
- [x] 7.3 Add example snippet to docs showing a REST → JMS end-to-end scenario
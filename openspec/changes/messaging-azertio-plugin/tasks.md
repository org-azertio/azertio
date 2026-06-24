## 1. Maven Module Setup

- [ ] 1.1 Create `plugins/messaging-azertio-plugin/` directory with `pom.xml` (parent: `azertio-plugin-starter`, groupId: `org.azertio.plugins`, version: `1.0.0-SNAPSHOT`)
- [ ] 1.2 Add `kafka-clients` dependency (latest stable) to `pom.xml`
- [ ] 1.3 Add `module-info.java` declaring the module and its exports
- [ ] 1.4 Add the plugin to `plugins/pom.xml` reactor and to the `plugins` profile in the root `pom.xml`

## 2. Engine Interface and Kafka Implementation

- [ ] 2.1 Create `MessagingEngine` interface with methods: `subscribe(topic)`, `publish(topic, body)`, `publish(topic, key, body)`, `pollNext(topic, timeoutSeconds)`, `clearConsumed(topic)`, `close()`
- [ ] 2.2 Create `KafkaMessagingEngine` implementing `MessagingEngine`; configure producer with `acks=all` and consumer with `assign()` + explicit offset seek (not `subscribe()`)
- [ ] 2.3 Implement background polling thread in `KafkaMessagingEngine` that buffers received messages per topic; start on `init()`, stop on `close()`
- [ ] 2.4 Implement `subscribe(topic)`: assigns partition 0, seeks to end offset, registers topic in buffer
- [ ] 2.5 Implement `pollNext(topic, timeoutSeconds)`: blocks until a message appears in the buffer or timeout expires; removes the message from the buffer (consume-once semantics)

## 3. Step Provider and Config

- [ ] 3.1 Create `MessagingConfigProvider` (extends `ConfigAdapter`, annotated `@Extension`)
- [ ] 3.2 Create `config.yaml` with properties: `messaging.bootstrapServers`, `messaging.timeout` (default 10), `messaging.enabled` (default true), `messaging.security.*` (optional TLS/SASL stubs)
- [ ] 3.3 Create `MessagingStepProvider` (`@Extension`, `Scope.TRANSIENT`) with `init(Config)` that builds the engine; `close()` that calls `engine.close()`
- [ ] 3.4 Implement `@StepExpression("messaging.subscribe")` — calls `engine.subscribe(topic)`
- [ ] 3.5 Implement `@StepExpression("messaging.publish")` — publishes body docstring to topic; interpolates variables
- [ ] 3.6 Implement `@StepExpression("messaging.publish.keyed")` — publishes with key and body; interpolates both
- [ ] 3.7 Implement `@StepExpression("messaging.assert.received")` — polls until matching message or timeout; asserts body contains the given JSON/text
- [ ] 3.8 Implement `@StepExpression("messaging.assert.received.exact")` — polls until exact body match or timeout
- [ ] 3.9 Implement `@StepExpression("messaging.extract.field")` — polls for message, extracts JSON field into named variable via `ExecutionContext`

## 4. Step Definitions (YAML)

- [ ] 4.1 Create `steps.yaml` with descriptions and roles for all steps (messaging.subscribe: `given`, messaging.publish: `when`, messaging.assert.*: `then`, messaging.extract.*: `then`)
- [ ] 4.2 Create `steps_en.yaml` with English natural-language expressions (e.g. `I subscribe to topic {topic:text}`, `I publish to topic {topic:text} the message:`)
- [ ] 4.3 Create `steps_es.yaml` with Spanish expressions
- [ ] 4.4 Create `steps_dsl.yaml` with compact DSL aliases (e.g. `subscribe {topic:text}`, `publish {topic:text}:`)

## 5. SPI Registration

- [ ] 5.1 Register `MessagingStepProvider` and `MessagingConfigProvider` in `META-INF/services/` (or via jexten annotation processing — confirm which mechanism the other plugins use)

## 6. Tests

- [ ] 6.1 Add `testcontainers-kafka` test dependency to `pom.xml`
- [ ] 6.2 Write integration test: publish a message and assert it is received on the same topic
- [ ] 6.3 Write integration test: subscribe before publish, assert pre-existing messages are excluded
- [ ] 6.4 Write integration test: timeout fires when no message is published
- [ ] 6.5 Write integration test: variable interpolation in published body

## 7. Documentation

- [ ] 7.1 Create `src/main/resources/config.yaml` help file with all config property descriptions
- [ ] 7.2 Add `messaging-azertio-plugin` card to `www/index.html` plugins section
- [ ] 7.3 Add example snippet to the README or docs site showing a REST → Kafka end-to-end scenario
## Context

Azertio's plugin architecture follows a consistent pattern: a `StepProvider` + `ConfigProvider` pair backed by an engine interface with one or more implementations. The REST plugin is the closest analogue — it has a `RestEngine` interface, a `JdkHttpEngine` implementation, and a `RestStepProvider` that delegates all I/O to the engine.

Message brokers introduce a fundamental difference from HTTP: **consumption is asynchronous and stateful**. A consumer must be subscribed *before* the action that produces the message, then poll for the message to appear. This ordering constraint shapes the step design and the engine lifecycle.

## Goals / Non-Goals

**Goals:**
- Provide Gherkin steps to publish messages to Kafka topics
- Provide steps to assert that a message was received on a topic, with content assertions (JSON field, full body, plain text)
- Support a subscribe-before-act-then-assert pattern to handle asynchronous delivery
- Support a configurable timeout when waiting for messages
- Kafka-first implementation for v1.0
- Follow the existing plugin conventions exactly (same pom structure, SPI, YAML definitions)

**Non-Goals:**
- RabbitMQ support in v1.0 (the engine interface will be broker-agnostic to allow it later)
- Avro / Schema Registry support (v1.0 handles JSON and plain text only)
- Message ordering assertions
- Dead-letter queue assertions
- Kafka Streams or consumer group offset management beyond test isolation

## Decisions

### 1. Engine interface instead of direct Kafka calls in StepProvider

Same pattern as `RestEngine`. `MessagingEngine` is a broker-agnostic interface. `KafkaMessagingEngine` is the v1.0 implementation.

**Rationale:** Allows a future `RabbitMqMessagingEngine` without touching `MessagingStepProvider`. Also makes unit testing easier.

**Alternative considered:** Directly embed Kafka calls in `MessagingStepProvider`. Rejected — locks the plugin to Kafka and makes testing harder.

---

### 2. Subscribe-before-assert step design

The step sequence is:
```
Given I subscribe to topic "order.created"   ← pre-subscribe (captures offset)
When  I make a POST request to "orders" ...  ← REST action (produces event)
Then  topic "order.created" should contain   ← polls until message or timeout
      """json
      { "orderId": "${id}" }
      """
```

The `messaging.subscribe` step records the current end offset of the topic partition(s) at subscribe time. The assert step polls only for messages **after** that offset, preventing stale messages from causing false positives.

**Alternative considered:** Always seek to LATEST on consumer start. Rejected — race condition between seek and the producing action.

**Alternative considered:** Seek to EARLIEST (read everything). Rejected — test runs contaminate each other.

---

### 3. Isolated consumer group per execution

Each test plan execution gets a consumer group ID of the form `azertio-<UUID>`. The group is used only for offset tracking internally; actual polling uses `assign()` + explicit offset seek (not `subscribe()`) to avoid group coordination delays.

**Rationale:** Consumer group rebalancing (triggered by `subscribe()`) can take seconds. Using `assign()` + seek is instant and avoids flakiness.

---

### 4. Background polling thread

`KafkaMessagingEngine` starts a background thread on `init()` that polls for new messages across all subscribed topics and buffers them in memory. The assert steps check the buffer, blocking up to the configured timeout.

**Rationale:** Kafka's `Consumer.poll()` is synchronous; without a background thread, there is a window between the REST call and the assert step where messages are not being consumed. The background thread eliminates this gap.

**Risk:** Thread lifecycle must be tied to the test execution. The engine's `close()` method (called by the framework at plan teardown) shuts it down.

---

### 5. Message format: JSON + plain text for v1.0

Keys and values are treated as UTF-8 strings. JSON assertion uses the same path/contains logic already present in the REST plugin (via `Assertion`). Avro / Protobuf deferred.

---

### 6. Producer is synchronous (fire-and-wait)

`messaging.publish` blocks until the broker acknowledges (acks=all). This gives a predictable "message is in Kafka" guarantee before the next step runs.

## Risks / Trade-offs

- **Flaky tests on slow brokers** → Mitigated by a configurable `messaging.timeout` (default 10 s). Users can increase it for slow CI environments.
- **Background thread resource leak if `close()` is not called** → The engine registers a JVM shutdown hook as a safety net; but proper teardown via the plugin lifecycle is the primary path.
- **Multiple topics, multiple partitions** → v1.0 subscribes to a single partition (partition 0) by default; full partition support deferred.
- **No broker = test skipped or failed?** → Connection failure at `init()` throws, failing the test plan. A `messaging.enabled` config flag can be used to skip when no broker is available.

## Migration Plan

No migration needed — this is a new artifact. Plugin is opt-in: users add it to `azertio.yaml` only when they need it.

## Open Questions

- Should `messaging.assert.received` consume the message (mark it "seen") so it cannot match a second assertion, or should each assert re-scan the buffer? **Lean toward: consume-once to avoid double-match bugs.**
- Should the plugin auto-detect broker type from the bootstrap address (Kafka port 9092 vs RabbitMQ port 5672) or always require explicit config? **For v1.0 with only Kafka, this is moot.**
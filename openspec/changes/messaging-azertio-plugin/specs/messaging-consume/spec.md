## ADDED Requirements

### Requirement: Subscribe to a topic before acting
The plugin SHALL provide a step to subscribe to a Kafka topic, recording the current end offset at subscribe time. Only messages produced *after* this point SHALL be visible to subsequent assertion steps in the same scenario.

#### Scenario: Subscribe records current offset
- **WHEN** `messaging.subscribe` is called on topic `"order.created"` before any producing action
- **THEN** the consumer's starting offset is set to the topic's current end offset, excluding pre-existing messages

#### Scenario: Subscribe does not consume messages itself
- **WHEN** `messaging.subscribe` is called
- **THEN** no messages are consumed or blocked; the step completes immediately

### Requirement: Assert a message was received on a topic
The plugin SHALL provide a step to assert that at least one message matching a given condition was received on a subscribed topic within the configured timeout.

#### Scenario: Assert JSON field in received message
- **WHEN** a message `{"orderId": 42, "status": "created"}` is received on topic `"order.created"`
- **AND** the step asserts that the message contains `{"orderId": 42}`
- **THEN** the assertion passes

#### Scenario: Assert full message body
- **WHEN** a message is received and the step asserts an exact body match
- **THEN** the assertion passes only if the body equals the expected value exactly

#### Scenario: No message received within timeout
- **WHEN** no matching message arrives within `messaging.timeout` seconds
- **THEN** the assertion step fails with a descriptive timeout error

### Requirement: Extract a field from a received message into a variable
The plugin SHALL provide a step to extract a JSON field from a received message and store it in a named variable for use in subsequent steps.

#### Scenario: Extract field from message
- **WHEN** a message `{"orderId": 99}` is received and the step extracts field `orderId` into variable `extractedId`
- **THEN** `${extractedId}` equals `"99"` in subsequent steps
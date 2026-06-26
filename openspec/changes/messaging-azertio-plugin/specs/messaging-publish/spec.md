## ADDED Requirements

### Requirement: Publish a message to a topic
The plugin SHALL provide a step to publish a text or JSON message to a named Kafka topic. The publish step MUST block until the broker acknowledges receipt before the next step executes.

#### Scenario: Publish JSON message to topic
- **WHEN** the step `messaging.publish` is executed with topic `"orders"` and a JSON body
- **THEN** the message is written to the `orders` topic and acknowledged by the broker before the step completes

#### Scenario: Publish message with a key
- **WHEN** the step `messaging.publish.keyed` is executed with a topic, a key, and a body
- **THEN** the message is written to the topic with the given key, ensuring partition routing

#### Scenario: Publish fails when broker is unavailable
- **WHEN** the broker is unreachable and `messaging.publish` is executed
- **THEN** the step fails with a descriptive error and the test plan is marked failed

### Requirement: Variable interpolation in published messages
The plugin SHALL interpolate `${variable}` placeholders in both the message key and body before publishing, consistent with how the REST plugin handles body interpolation.

#### Scenario: Interpolate variable in message body
- **WHEN** a variable `id` holds the value `"42"` and the message body contains `{"orderId": "${id}"}`
- **THEN** the published message body is `{"orderId": "42"}`
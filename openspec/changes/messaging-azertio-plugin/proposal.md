## Why

Azertio can test REST APIs and databases but has no way to verify the downstream effects of those actions on message brokers. Event-driven architectures are mainstream, and black-box testing them today requires bespoke tooling or manual inspection. A `messaging-azertio-plugin` closes this gap using the same Gherkin DSL already familiar to Azertio users.

## What Changes

- New Maven artifact `messaging-azertio-plugin` published to Maven Central under `org.azertio.plugins`
- Steps to publish messages to Kafka topics (and optionally RabbitMQ queues)
- Steps to consume and assert message content (JSON field assertions, full-body match, header checks)
- Steps to wait/poll for a message to appear with a configurable timeout
- Configuration for broker connection (bootstrap servers, credentials, consumer group)
- Integrates naturally with existing REST and DB plugins: a test can call a REST endpoint and then assert the resulting Kafka event

## Capabilities

### New Capabilities

- `messaging-publish`: Steps to produce messages to a topic or queue
- `messaging-consume`: Steps to consume messages and assert their content
- `messaging-poll`: Step to wait until a message matching a condition appears within a timeout

### Modified Capabilities

_(none — this is a new plugin with no changes to existing specs)_

## Impact

- New Maven module under `plugins/messaging-azertio-plugin`
- Parent: `azertio-plugin-starter` (same as all other plugins)
- Runtime dependencies: `kafka-clients` (Apache Kafka); RabbitMQ support via `amqp-client` (optional, declared with `provided` scope or loaded on demand)
- No changes to core, gherkin, rest, db, or webui plugins
- No breaking changes
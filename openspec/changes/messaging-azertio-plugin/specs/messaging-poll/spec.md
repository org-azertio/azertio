## ADDED Requirements

### Requirement: Configurable timeout for message polling
The plugin SHALL support a `messaging.timeout` configuration property (in seconds, default 10) that controls how long assertion steps wait for a matching message before failing.

#### Scenario: Message arrives within timeout
- **WHEN** a matching message is published within the configured timeout
- **THEN** the assertion step passes immediately upon receiving the message

#### Scenario: Timeout exceeded
- **WHEN** no matching message is published within `messaging.timeout` seconds
- **THEN** the assertion step fails with an error indicating which topic and condition timed out

### Requirement: Isolated message visibility per test execution
Each test plan execution SHALL use an independent consumer offset position so that messages from previous executions do not satisfy assertions in the current run.

#### Scenario: Stale messages do not match
- **WHEN** a topic already contains messages from a prior test run
- **AND** `messaging.subscribe` is called at the start of the scenario
- **THEN** those pre-existing messages are not visible to assertion steps in the current scenario

### Requirement: Clear consumed messages between scenarios
Messages consumed during one scenario SHALL NOT be visible to subsequent scenarios in the same test plan execution.

#### Scenario: Message consumed in scenario 1 is not re-matched in scenario 2
- **WHEN** scenario 1 asserts and consumes a message from topic `"events"`
- **AND** scenario 2 also asserts on topic `"events"` without a new producing action
- **THEN** scenario 2's assertion waits for a new message and times out if none arrives
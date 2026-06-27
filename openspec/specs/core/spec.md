# Core

Built-in steps and configuration available in every Azertio execution, regardless of which
plugins are active. The core provides variable assertions, benchmark mode, execution control,
and the runtime configuration model.

---

## Variable Assertions

Variables are stored during test execution by plugin steps (e.g. `rest.response.extracts.field`,
`db.store.query.result`) and can be asserted by type using the steps below.
Each assertion step takes the variable name and a typed condition expression.

### Requirement: Assert integer variable
The core SHALL assert that a named variable, interpreted as an integer, satisfies a numeric
condition expression (e.g. `= 5`, `> 0`, `<= 100`, `is not equal to 0`).

#### Scenario: Exact integer match
- **WHEN** a variable `count` holds `"3"` and the assertion is `= 3`
- **THEN** `assert.variable.integer` passes

#### Scenario: Integer comparison fails
- **WHEN** a variable `count` holds `"2"` and the assertion is `> 5`
- **THEN** the assertion fails with actual vs expected values

### Requirement: Assert decimal variable
The core SHALL assert that a named variable, interpreted as a decimal number, satisfies
a numeric condition expression (e.g. `= 19.99`, `> 0.0`, `< 1.0`).

#### Scenario: Exact decimal match
- **WHEN** a variable `price` holds `"19.99"` and the assertion is `= 19.99`
- **THEN** `assert.variable.decimal` passes

### Requirement: Assert date variable
The core SHALL assert that a named variable, interpreted as a date (`yyyy-MM-dd`), satisfies
a date condition expression (e.g. `= 2025-01-01`, `is before 2025-12-31`, `is after 2020-01-01`).

#### Scenario: Date is after a given date
- **WHEN** a variable `expiry` holds `"2026-06-01"` and the assertion is `is after 2025-01-01`
- **THEN** `assert.variable.date` passes

### Requirement: Assert time variable
The core SHALL assert that a named variable, interpreted as a time (`HH:mm:ss`), satisfies
a time condition expression (e.g. `= 12:00:00`, `is before 18:00:00`, `is after 08:00:00`).

#### Scenario: Time is before a given time
- **WHEN** a variable `openTime` holds `"09:00:00"` and the assertion is `is before 18:00:00`
- **THEN** `assert.variable.time` passes

### Requirement: Assert datetime variable
The core SHALL assert that a named variable, interpreted as a datetime (`yyyy-MM-dd'T'HH:mm:ss`),
satisfies a datetime condition expression (e.g. `is before 2025-12-31T23:59:59`).

#### Scenario: Datetime is before a given instant
- **WHEN** a variable `createdAt` holds `"2025-06-01T09:00:00"` and the assertion is `is before 2025-12-31T23:59:59`
- **THEN** `assert.variable.datetime` passes

### Requirement: Assert text variable
The core SHALL assert that a named variable, interpreted as a string, satisfies a text condition
expression (e.g. `= "active"`, `contains "error"`, `starts with "OK"`, `ends with "done"`).

#### Scenario: Exact text match
- **WHEN** a variable `status` holds `"active"` and the assertion is `= "active"`
- **THEN** `assert.variable.text` passes

#### Scenario: Contains match
- **WHEN** a variable `message` holds `"Request failed: timeout"` and the assertion is `contains "timeout"`
- **THEN** the assertion passes

---

## Benchmark Mode

Benchmark mode runs a single `when` step (the one immediately following `enable.benchmark.mode`)
repeatedly across parallel threads, then collects timing statistics for assertion.

### Requirement: Enable benchmark mode
The core SHALL enable benchmark mode for the next `when` step, running it a given total number
of times distributed across a given number of parallel threads.

#### Scenario: Step executed the specified number of times
- **WHEN** `enable.benchmark.mode` is called with 100 executions and 4 threads
- **AND** the next `when` step is a GET request
- **THEN** the GET is executed 100 times across 4 threads and statistics are collected

#### Scenario: Benchmark mode does not affect non-benchmark steps
- **WHEN** benchmark mode is enabled and the benchmark step completes
- **THEN** subsequent steps execute normally (once, not in benchmark mode)

### Requirement: Assert benchmark mean response time
The core SHALL assert that the mean response time in milliseconds of the last benchmark
satisfies an integer condition expression.

#### Scenario: Mean time under threshold
- **WHEN** the benchmark recorded a mean of 85 ms and the assertion is `is less than 200`
- **THEN** `assert.benchmark.statistics.mean` passes

### Requirement: Assert benchmark minimum response time
The core SHALL assert that the minimum response time (ms) of the last benchmark satisfies
an integer condition expression.

#### Scenario: Minimum time is positive
- **WHEN** the benchmark recorded a minimum of 12 ms and the assertion is `> 0`
- **THEN** `assert.benchmark.statistics.min` passes

### Requirement: Assert benchmark maximum response time
The core SHALL assert that the maximum response time (ms) of the last benchmark satisfies
an integer condition expression.

#### Scenario: Maximum time under hard limit
- **WHEN** the benchmark recorded a maximum of 450 ms and the assertion is `is less than 1000`
- **THEN** `assert.benchmark.statistics.max` passes

### Requirement: Assert benchmark P50 response time
The core SHALL assert that the 50th-percentile response time (ms) satisfies an integer condition.

#### Scenario: P50 under threshold
- **WHEN** the P50 is 120 ms and the assertion is `is less than 200`
- **THEN** `assert.benchmark.statistics.p50` passes

### Requirement: Assert benchmark P95 response time
The core SHALL assert that the 95th-percentile response time (ms) satisfies an integer condition.

#### Scenario: P95 under SLA threshold
- **WHEN** the P95 is 380 ms and the assertion is `is less than 500`
- **THEN** `assert.benchmark.statistics.p95` passes

### Requirement: Assert benchmark P99 response time
The core SHALL assert that the 99th-percentile response time (ms) satisfies an integer condition.

#### Scenario: P99 under hard limit
- **WHEN** the P99 is 890 ms and the assertion is `is less than 1000`
- **THEN** `assert.benchmark.statistics.p99` passes

### Requirement: Assert benchmark throughput
The core SHALL assert that the throughput in requests per second satisfies a decimal condition.

#### Scenario: Throughput exceeds minimum
- **WHEN** the benchmark achieved 42.5 req/s and the assertion is `> 10.0`
- **THEN** `assert.benchmark.statistics.throughput` passes

### Requirement: Assert benchmark error rate
The core SHALL assert that the error rate (0.0–1.0) of the last benchmark satisfies a decimal condition.

#### Scenario: Zero errors
- **WHEN** all benchmark executions succeeded and the assertion is `= 0.0`
- **THEN** `assert.benchmark.statistics.errorRate` passes

#### Scenario: Error rate under acceptable threshold
- **WHEN** the error rate is 0.005 and the assertion is `is less than 0.01`
- **THEN** the assertion passes

---

## Execution Control

### Requirement: Pause execution
The core SHALL pause test execution for a given integer number of seconds before proceeding
to the next step. Thread interruption SHALL be handled gracefully (interrupt flag restored).

#### Scenario: Execution paused for specified duration
- **WHEN** `wait.seconds` is called with `3`
- **THEN** execution pauses for approximately 3 seconds before the next step runs

#### Scenario: Thread interrupt handled gracefully
- **WHEN** the waiting thread is interrupted externally
- **THEN** the interrupt flag is restored and execution continues without throwing

---

## Configuration

### Resources

| Property | Type | Default | Description |
|---|---|---|---|
| `core.resourcePath` | text | `.` | Root directory for loading test resources (feature files, data files) |
| `core.resourceFilter` | text | `**/*` | Glob pattern to filter which resources are loaded |
| `core.environmentPath` | text | `.azertio` | Directory for environment state and local persistence |

### Formatting

| Property | Type | Default | Description |
|---|---|---|---|
| `core.timeZone` | text | system default | IANA time zone ID used when formatting timestamps in reports (e.g. `Europe/Madrid`) |

### Test Plan Structure

| Property | Type | Default | Description |
|---|---|---|---|
| `core.idTagPattern` | text | `ID-(\w+)` | Regex for tags used as scenario identifiers; if it contains a group, only the group value is used |
| `core.definitionTag` | text | `definition` | Tag that marks a node aggregator as a definition node |
| `core.implementationTag` | text | `implementation` | Tag that marks a node as an implementation node |
| `core.parallelExecutionTag` | text | `parallel` | Tag that marks a test case as eligible for parallel execution |

### Step Execution

| Property | Type | Default | Description |
|---|---|---|---|
| `core.stepExecutionTimeout` | integer (s) | `60` | Maximum seconds allowed for a single step; step is aborted if exceeded |

### Persistence

| Property | Type | Default | Description |
|---|---|---|---|
| `core.persistence.mode` | enum | `file` | Storage mode: `transient` (in-memory), `file` (local H2), `remote` (external DB) |
| `core.persistence.file` | text | `db/azertio.db` | File path for `file` mode |
| `core.persistence.db.url` | text | — | JDBC URL for `remote` mode |
| `core.persistence.db.username` | text | — | DB username for `remote` mode |
| `core.persistence.db.password` | text | — | DB password for `remote` mode |

### Artifact Resolution

| Property | Type | Default | Description |
|---|---|---|---|
| `core.artifacts.local.repository` | text | `.m2/repository` | Local filesystem cache for downloaded plugin artifacts |
| `core.artifacts.repository.url` | text | Maven Central | Remote repository URL for fetching plugins |
| `core.artifacts.repository.username` | text | — | Username for authenticated repositories |
| `core.artifacts.repository.password` | text | — | Password for authenticated repositories |
| `core.artifacts.repository.proxy.url` | text | — | Proxy URL for reaching the remote repository |
| `core.artifacts.repository.proxy.username` | text | — | Proxy authentication username |
| `core.artifacts.repository.proxy.password` | text | — | Proxy authentication password |

### Attachments (remote mode only)

| Property | Type | Default | Description |
|---|---|---|---|
| `core.attachments.server.url` | text | — | URL of the S3/Minio-compatible attachment server (required for `remote` mode) |
| `core.attachments.server.username` | text | — | Access key for the attachment server |
| `core.attachments.server.password` | text | — | Secret key for the attachment server |
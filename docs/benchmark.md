# Benchmark Mode

Benchmark mode lets you measure the performance of any test step by running it repeatedly across multiple parallel threads and then asserting on the collected statistics.

---

## How it works

1. **Enable** benchmark mode immediately before the step you want to measure, specifying the total number of executions and the number of parallel threads.
2. **Run** the target step. OpenBBT executes it the requested number of times using virtual threads, bounded to the configured parallelism. The step is responsible for marking its own start and finish times.
3. **Assert** on one or more statistics produced by the benchmark run.

The benchmarked step always reports `PASSED` regardless of individual iteration errors; error count is captured in the `errorRate` statistic instead.

---

## Steps

### `enable.benchmark.mode`

**Role:** `given`

Enables benchmark mode for the immediately following step.

| Parameter | Type | Description |
|-----------|------|-------------|
| `executions` | integer | Total number of times the next step will be executed |
| `threads` | integer | Maximum number of concurrent virtual threads |

**English:**
```gherkin
Given benchmark mode is enabled with 100 executions and 4 threads
```

**Spanish:**
```gherkin
Dado el modo benchmark está habilitado con 100 ejecuciones y 4 hilos
```

**DSL:**
```gherkin
* benchmark 100 x 4 threads
```

---

### `assert.benchmark.statistics.*`

All assertion steps have **role** `then` and accept an assertion expression that matches their type.

| Step ID | Metric | Assertion type |
|---------|--------|----------------|
| `assert.benchmark.statistics.mean` | Mean response time (ms) | integer |
| `assert.benchmark.statistics.min` | Minimum response time (ms) | integer |
| `assert.benchmark.statistics.max` | Maximum response time (ms) | integer |
| `assert.benchmark.statistics.p50` | P50 response time (ms) | integer |
| `assert.benchmark.statistics.p95` | P95 response time (ms) | integer |
| `assert.benchmark.statistics.p99` | P99 response time (ms) | integer |
| `assert.benchmark.statistics.throughput` | Throughput (requests/s) | decimal |
| `assert.benchmark.statistics.errorRate` | Error rate (0.0–1.0) | decimal |

Statistics from the last benchmark run are stored in the execution context and remain accessible to any subsequent `assert.benchmark.statistics.*` step in the same test case.

---

## Complete example

```gherkin
Scenario: API endpoint meets performance SLA

  Given benchmark mode is enabled with 200 executions and 8 threads
  When I make a GET request to "health"
  Then the benchmark mean response time (ms) is less than 100
  Then the benchmark P95 response time (ms) is less than 300
  Then the benchmark error rate is equal to 0.0
  Then the benchmark throughput (req/s) is greater than 50.0
```

DSL equivalent:

```gherkin
  * benchmark 200 x 8 threads
  * do HTTP GET "health"
  * assert benchmark:mean < 100
  * assert benchmark:p95 < 300
  * assert benchmark:errorRate = 0.0
  * assert benchmark:throughput > 50.0
```

---

## Writing benchmark-compatible steps

A step can be used in benchmark mode only if it is annotated with `@StatisticsProvider` **and** it calls `Benchmark.markStarted()` / `Benchmark.markFinished()` to record timing. Steps that lack the annotation are rejected at runtime with an error.

See [Creating Step Plugins](creating-step-plugins.md#benchmark-support) for implementation details.

---

## Stored statistics

After a benchmark run, the statistics are persisted in the `EXECUTION_NODE_STATS` table linked to the benchmarked step's execution node. The record is upserted, so re-running the same plan execution overwrites the previous values for that node.

| Column | Type | Description |
|--------|------|-------------|
| `NUM_EXECUTIONS` | integer | Configured total executions |
| `NUM_THREADS` | integer | Configured thread count |
| `MIN_MS` | integer | Minimum elapsed time in milliseconds |
| `MAX_MS` | integer | Maximum elapsed time in milliseconds |
| `MEAN_MS` | integer | Mean elapsed time in milliseconds |
| `P50_MS` | integer | 50th percentile elapsed time in milliseconds |
| `P95_MS` | integer | 95th percentile elapsed time in milliseconds |
| `P99_MS` | integer | 99th percentile elapsed time in milliseconds |
| `THROUGHPUT` | double | Executions per second (total executions / total time in seconds) |
| `ERROR_RATE` | double | Fraction of iterations that threw an exception (0.0–1.0) |
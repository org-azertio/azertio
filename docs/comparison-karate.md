# OpenBBT vs Karate — A Detailed Comparison

[Karate](https://github.com/karatelabs/karate) is one of the most popular Java-based API testing frameworks. Both OpenBBT and Karate use `.feature` files and support REST and database testing, but they differ significantly in architecture, philosophy, and extensibility. This document gives an honest side-by-side picture, highlighting the areas where each tool excels.

---

## Quick Overview

| | **OpenBBT** | **Karate** |
|---|---|---|
| First release | 2025 | 2017 |
| Language | Java 21 | Java |
| Build integration | Standalone CLI + Maven | Maven / Gradle |
| Test format | Gherkin / compact DSL / Markdown | Karate DSL (Gherkin-like) |
| Architecture | Plugin-based (JPMS modules) | Monolithic core |
| REST testing | ✅ via `rest` plugin | ✅ built-in |
| Database testing | ✅ via `db` plugin (any JDBC) | ⚠️ limited, needs Java glue |
| Performance testing | ✅ built-in benchmark mode | ⚠️ via Gatling integration |
| VS Code extension | ✅ dedicated extension | ❌ community-only |
| Custom protocols | ✅ write a plugin (pure Java) | ⚠️ requires JS/Java in feature files |
| Multilingual steps | ✅ EN / ES / compact DSL | ❌ single DSL |
| Definition / implementation | ✅ two-level scenario model | ❌ |
| Execution history | ✅ transient / file / remote DB | ❌ no built-in persistence |
| Runtime deps | ✅ declared in YAML, auto-downloaded | ❌ must be in Maven/Gradle POM |

---

## Philosophy

### Karate: all-in-one scripting engine

Karate embeds a JavaScript engine (GraalJS) inside `.feature` files. This gives a lot of power quickly — you can call Java, write loops, use conditional logic — but at the cost of blurring the line between tests and code:

```gherkin
# Karate: JavaScript lives inside the feature file
* def body = { title: 'My post', userId: 1 }
* def result = call read('create-post.feature')
* match result.id == '#number'
* def ids = karate.map(result, x => x.id)
```

Tests become mini-programs. Non-technical stakeholders cannot read or validate them, and the line between "test" and "glue code" disappears.

### OpenBBT: pure declarative steps, zero scripting

OpenBBT enforces a clean boundary: feature files contain only declarative steps, and all logic lives in typed Java step providers. Variables are passed between steps via a typed context — no scripting language, no embedded JS:

```gherkin
# OpenBBT: plain English, no programming constructs
When I make a POST request to "posts" with body:
  """json
  { "title": "My post", "userId": 1 }
  """
Then the HTTP status code is equal to 201
And I store the value of field 'id' from the response body into variable id
When I make a GET request to "posts/${id}"
Then the HTTP status code is equal to 200
```

Any business analyst can read this. The step implementations are reusable, testable Java — not entangled with the test file.

---

## Architecture

### Karate: fixed core

Karate ships as a single all-or-nothing dependency. Protocols and features are baked in; adding a custom transport requires writing Java code and wiring it through Karate's API. Changing the behavior of built-in steps is not possible without forking.

### OpenBBT: modular plugin system (JPMS)

Every capability in OpenBBT is a plugin: an isolated Java module loaded at runtime via the Java Platform Module System. Plugins are distributed as Maven artifacts and declared in `openbbt.yaml`:

```yaml
plugins:
  - gherkin
  - rest
  - db with com.mysql:mysql-connector-j
  - org.example:my-custom-protocol-plugin
```

Each plugin runs in its own module layer, which means:

- **No classpath pollution** — plugin dependencies cannot interfere with each other or with the core.
- **Runtime resolution** — plugins and their dependencies (including JDBC drivers) are downloaded from Maven Central on demand, without recompiling or editing a build file.
- **True replaceability** — if you don't like the built-in REST implementation, you can replace it with your own plugin without forking anything.

---

## REST API Testing

Both tools support REST testing well. The syntax is comparable in expressiveness.

**Karate:**
```gherkin
Given url 'https://jsonplaceholder.typicode.com/posts'
And request { title: 'foo', body: 'bar', userId: 1 }
When method post
Then status 201
And match response.title == 'foo'
```

**OpenBBT:**
```gherkin
When I make a POST request to "posts" with body:
  """json
  { "title": "foo", "body": "bar", "userId": 1 }
  """
Then the HTTP status code is equal to 201
And the response body contains:
  """json
  { "title": "foo" }
  """
```

The key difference is that in OpenBBT the base URL, timeout, and other settings are declared once in `openbbt.yaml`, not scattered across feature files. Feature files contain only the test intent, not the infrastructure configuration.

---

## Database Testing

### Karate

Karate has basic JDBC support via `karate-jdbc`, but it's not a first-class citizen:

- Adding a JDBC driver requires editing `pom.xml` or `build.gradle` and rebuilding.
- SQL execution is done through Java interop, mixing scripting and queries.
- Result assertions are manual JavaScript/Java comparisons.
- No native support for `NULL` sentinel values, CSV fixtures, or Excel fixtures.

```gherkin
# Karate: requires a Java helper class
* def db = Java.type('com.example.DbHelper').instance
* def rows = db.execute("SELECT * FROM users WHERE id = 1")
* match rows[0].name == 'Alice'
```

### OpenBBT

Database testing is a dedicated first-class plugin with a complete step vocabulary:

```gherkin
* use db "main"
* db query:
  """sql
  SELECT id, name FROM users WHERE status = 'active'
  """
* db query count > 0
* db table users has:
  | id | name  |
  | 1  | Alice |
* db table users is CSV "expected/users.csv"
* db has XLS "expected/all-data.xlsx"
```

The JDBC driver is declared as a runtime dependency in `openbbt.yaml` and downloaded automatically — no build file changes:

```yaml
plugins:
  - db with com.mysql:mysql-connector-j
  - db with org.postgresql:postgresql-42.7.3
```

You can run tests against multiple datasources in the same scenario, switch drivers without rebuilding, and assert against NULL values, CSV files, and Excel files out of the box.

---

## Performance / Benchmark Testing

### Karate

Karate integrates with [Gatling](https://gatling.io) for performance testing, which means:

- A separate Gatling dependency and configuration.
- Gatling scenarios are different files from functional test scenarios — no reuse.
- A separate reporting pipeline.
- Gatling is a full load testing tool, with more setup overhead than most functional teams need.

### OpenBBT: benchmark mode built in

OpenBBT has native benchmark support that works directly on any benchmarkable step, in the same `.feature` file as your functional tests:

```gherkin
Scenario: API meets performance SLA
  Given benchmark mode is enabled with 200 executions and 8 threads
  When I make a GET request to "posts/1"
  Then the benchmark mean response time (ms) is less than 100
  Then the benchmark P95 response time (ms) is less than 300
  Then the benchmark error rate is equal to 0.0
  Then the benchmark throughput (req/s) is greater than 50.0
```

Benchmark runs use virtual threads for high concurrency with low resource overhead. All statistics (min, max, mean, P50, P95, P99, throughput, error rate) are stored with the execution and visible in the VS Code extension. No separate tool, no separate file, no separate pipeline.

---

## VS Code Integration

### Karate

There is no official VS Code extension for Karate. Community extensions exist for syntax highlighting, but there is no native integration for running tests, browsing results, or inspecting execution history from within the IDE.

### OpenBBT

OpenBBT ships a dedicated VS Code extension that connects to the CLI via a JSON-RPC server. From the IDE you can:

- **Browse past executions** with date, duration, and overall pass/fail status.
- **Drill into the result tree**: test plan → suite → scenario → step, with individual timings and statuses.
- **Inspect attachments**: query result CSVs, JSON response bodies, and any other data produced by steps are stored with the execution and openable directly from the tree.
- **Re-run any past execution** against its original test plan and profile with a single button — no configuration needed.
- **View benchmark statistics** inline in the execution detail.

All of this without leaving VS Code or touching a terminal.

---

## Multilingual and DSL Support

### Karate

Karate has its own fixed DSL. Steps must be written in Karate syntax; there is no mechanism to write the same step in a different natural language, and the compact shorthand is tied to Karate keywords (`url`, `request`, `method`, `match`).

### OpenBBT

OpenBBT separates step logic from step language. Every plugin ships with multiple language files. The same step can be expressed in:

| Language | Expression |
|---|---|
| English | `When I make a GET request to "posts"` |
| Spanish | `Cuando realizo una petición GET a "posts"` |
| Compact DSL | `* do HTTP GET "posts"` |

The language is selected per feature file with a `# language:` header. Teams can write in the language that best fits their workflow, and adding a new language to an existing plugin requires only a YAML file — no Java changes.

---

## Extensibility: Writing Custom Steps

### Karate

Custom logic in Karate is added by calling Java from inside feature files, or by writing `*.feature` files that act as callable functions. Both approaches interweave code and tests:

```gherkin
* def MyUtil = Java.type('com.example.MyUtil')
* def result = MyUtil.compute(someValue)
```

There is no first-class plugin API. Custom step implementations cannot be packaged and distributed independently as Maven artifacts.

### OpenBBT

Custom plugins are standard Maven modules that implement a clean Java API:

```java
@Extension(name = "My Protocol", scope = Scope.TRANSIENT)
public class MyStepProvider implements StepProvider {

    @StepExpression(value = "my.step.connect", args = {"host:text", "port:integer"})
    public void connect(String host, int port) {
        // implementation
    }
}
```

The plugin is published to any Maven repository and declared in `openbbt.yaml` — users get it automatically without any code changes. The JPMS isolation ensures it cannot break other plugins.

---

## Configuration and Profiles

### Karate

Environment-specific configuration in Karate is handled via a `karate-config.js` JavaScript file. Profiles are JavaScript objects keyed by environment name. This again puts configuration into a programming language:

```javascript
// karate-config.js
function fn() {
  var env = karate.env || 'dev';
  var config = { baseUrl: 'http://localhost:8080' };
  if (env == 'staging') { config.baseUrl = 'https://staging.example.com'; }
  return config;
}
```

### OpenBBT

Configuration is pure YAML, with first-class profile support:

```yaml
configuration:
  rest:
    baseURL: '{{base-url}}/api'
    timeout: 10000

profiles:
  dev:
    base-url: http://localhost:8080
  staging:
    base-url: https://staging.example.com
  production:
    base-url: https://api.example.com
```

Switching profiles is a CLI flag: `openbbt run -p staging`. No JavaScript, no conditionals, no build-time changes.

---

## Test Suite Filtering

### Karate

Karate supports tags for filtering, but the tag system is basic — you can include or exclude tags, but complex boolean expressions are limited and the configuration varies depending on how you run tests (Maven Surefire, JUnit runner, CLI).

### OpenBBT

Suites in OpenBBT are named sets of boolean tag expressions defined in `openbbt.yaml`, making them reproducible and version-controlled:

```yaml
test-suites:
  - name: smoke
    tag-expression: "smoke"
  - name: regression
    tag-expression: "(regression or smoke) and not wip"
  - name: api-only
    tag-expression: "GET or POST or PUT or DELETE and not DB"
```

Running a specific suite is a single flag: `openbbt run -s regression`. Suites can be combined in the same run.

---

## Execution History and Persistence

### Karate

Karate produces JUnit XML and HTML reports at the end of each run. These reports are written to the `target/` directory and discarded with the next build. There is no persistent store of past executions: you cannot browse a historical run from two weeks ago, compare results across runs, or re-execute a specific past run by ID. If you need long-term result tracking, you must integrate an external tool (Allure, ReportPortal, a CI dashboard) and configure the export yourself.

### OpenBBT: three persistence modes

OpenBBT has a built-in persistence layer with three configurable modes, covering the full spectrum from ephemeral CI runs to team-wide shared history:

| Mode | Backend | Attachments | Use case |
|---|---|---|---|
| `transient` | Temp HSQLDB file (deleted on exit) | Temp directory | CI pipelines that only need pass/fail |
| `file` | HSQLDB file in `.openbbt/` | Local filesystem | Developer workstation, browsable in VS Code |
| `remote` | PostgreSQL | MinIO (S3-compatible) | Shared team history across CI and all developers |

Configuration is a single block in `openbbt.yaml`:

```yaml
configuration:
  core:
    persistence.mode: remote
    persistence.db.url: jdbc:postgresql://db-server:5432/openbbt
    persistence.db.username: openbbt
    persistence.db.password: '{{DB_PASSWORD}}'
    attachment.server.url: http://minio-server:9000
    attachment.server.username: minio-user
    attachment.server.password: '{{MINIO_PASSWORD}}'
```

In `remote` mode, every CI run writes its full execution tree — plan, suites, scenarios, steps, timings, and attachments — to the shared database. Every developer opens VS Code and can immediately browse any past execution: drill into individual steps, inspect response body attachments, view benchmark statistics, and re-run any execution with a single click. No Allure server, no ReportPortal, no external dashboard.

This also enables workflows impossible with report-file approaches: because every execution is a structured record in a relational database, it is straightforward to query trends (e.g. which scenarios have been flaky over the last 30 days), compare runs across branches, or build custom dashboards directly from the PostgreSQL data.

---

## Two-Level Scenarios: Definition / Implementation

### Karate

Karate has no concept of separating test intent from test implementation. A Karate feature file is always a single level: the steps you write are the steps that execute. There is no built-in mechanism for a business analyst to maintain an abstract, readable specification while an engineer maintains a separate, technically precise version of the same test.

Teams that want this separation in Karate typically resort to calling reusable `.feature` files as functions — but the caller file is itself a technical Karate script, not a business-readable specification.

### OpenBBT: definition / implementation

OpenBBT has first-class support for a two-level scenario model. A **definition** feature (tagged `@definition`) declares abstract, business-readable scenarios identified by `@ID-*` tags. An **implementation** feature (tagged `@implementation`) provides the concrete, executable steps for each scenario, matched by identifier.

```gherkin
# definition.feature — written and owned by the business
@definition
Feature: User Registration

@ID-REG-01
Scenario: A new user can register with valid data
  Given a valid registration form
  When the form is submitted
  Then the account is created
  And a welcome email is sent
```

```gherkin
# implementation.feature — written by the test engineer
@implementation
Feature: User Registration — REST

# gherkin.step-map: 1-1-1-1
@ID-REG-01
Scenario: A new user can register
  When I make a POST request to "users" with body:
    """json
    { "name": "Alice", "email": "alice@example.com" }
    """
  Then the HTTP status code is equal to 201
  And the response body contains:
    """json
    { "email": "alice@example.com" }
    """
  And the response body field "welcomeEmailSent" is equal to "true"
```

At plan-build time, the two files are merged: the definition structure becomes the visible test tree, while the implementation steps are what actually execute. The `gherkin.step-map` comment controls how many implementation steps replace each abstract definition step (including `0` for virtual steps that appear in the tree but run no code).

Implementation features can be in a **different natural language** from the definition — enabling a project where the business contract is in English and the technical implementation is in Spanish (or any other supported language), all in the same plan.

This is particularly valuable for:
- Regulatory or contractual traceability (the definition is the signed-off specification).
- Multilingual teams where business and engineering speak different languages.
- Projects where the same abstract test contract needs multiple concrete implementations (e.g. REST today, gRPC tomorrow).

---

## Summary: When to Choose Each

### Choose Karate if

- Your team is already heavily invested in the Karate ecosystem.
- You need WebSocket or gRPC support out of the box (not yet in OpenBBT).
- You need to call Java or JavaScript logic directly from feature files.
- You prefer a large, established community with abundant StackOverflow answers.

### Choose OpenBBT if

- You want feature files that non-developers can read, review, and write.
- You need to test multiple databases with different JDBC drivers — especially if you don't control the build file.
- You want performance benchmarking integrated into your existing functional test suite, without a separate tool.
- You work in VS Code and want a native IDE experience for test execution and result inspection.
- You need to support multiple natural languages in your test suite.
- You want to distribute custom test steps as Maven plugins, reusable across projects and teams.
- Your architecture values strong module isolation (JPMS) and separation of concerns.

---

> **Note:** OpenBBT is under active development. Some features available in Karate (GraphQL, gRPC, WebSocket testing) are planned for future plugins but not yet available. Check the [release page](https://github.com/org-myjtools/openbbt/releases) for the current plugin catalogue.
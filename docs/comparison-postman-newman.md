# OpenBBT vs Postman / Newman — A Detailed Comparison

[Postman](https://www.postman.com/) is the most widely used tool for API testing, and [Newman](https://github.com/postmanlabs/newman) is its CLI runner for CI pipelines. They cover the full lifecycle from exploration to automated testing. OpenBBT targets a narrower scope — automated black-box testing — but does so with a fundamentally different approach. This document compares the two honestly, focusing on teams that have outgrown manual Postman testing and need a robust, version-controlled test suite.

---

## Quick Overview


|                            | **OpenBBT**                 | **Postman / Newman**                    |
| -------------------------- | --------------------------- | --------------------------------------- |
| First release              | 2025                        | 2012 / 2014                             |
| Primary interface          | CLI + VS Code extension     | GUI (Postman app) + CLI (Newman)        |
| Test format                | Gherkin / compact DSL       | Proprietary JSON collections            |
| Test authoring             | Plain text in any editor    | Postman GUI or raw JSON                 |
| Git-friendly               | ✅ plain text, diffable     | ⚠️ JSON collections, noisy diffs      |
| BDD / readable scenarios   | ✅ Gherkin                  | ❌                                      |
| Test logic language        | Declarative steps (no code) | JavaScript (`pm.test`, `pm.expect`)     |
| Database testing           | ✅ first-class plugin       | ❌ not supported                        |
| Performance benchmarking   | ✅ built-in benchmark mode  | ❌ separate tool needed                 |
| VS Code execution history  | ✅ dedicated extension      | ❌                                      |
| Profile / environment mgmt | ✅ YAML profiles            | ⚠️ Postman environments (GUI or JSON) |
| Plugin / extensibility     | ✅ Maven plugin API         | ❌ JS scripts only                      |
| Definition / implementation | ✅ two-level scenario model | ❌ |
| Execution history           | ✅ transient / file / remote DB | ❌ no persistence between runs     |
| Cost                       | Free / open source          | Freemium (team features are paid)       |
| CI integration             | `openbbt run`               | `newman run`                            |

---

## Philosophy

### Postman: API platform for exploration and collaboration

Postman is built around the concept of an API client: you explore endpoints interactively, build up a collection of requests, and add JavaScript assertions to turn those requests into tests. The GUI is the primary interface; Newman makes those collections runnable in CI.

This model is excellent for exploration and for teams where the primary persona is a QA engineer who prefers a visual tool. It struggles, however, when:

- Tests need to live in git alongside code and be reviewed like code.
- Tests need to cover more than HTTP (databases, benchmarks).
- Business stakeholders need to read or contribute to test scenarios.
- Teams need to run tests without a Postman account or internet connectivity.

### OpenBBT: automated black-box testing as code

OpenBBT starts from the opposite direction: tests are plain text files, written in a domain-specific language, version-controlled alongside the system under test, and run entirely from the CLI. There is no GUI for authoring — the IDE is any text editor (VS Code recommended). The recommended VS Code extension is also for *inspecting results*, not only for writing tests.

---

## Test Format and Version Control

This is the most consequential difference for teams that treat tests as code.

### Postman: JSON collections

A Postman collection is a deeply nested JSON file. The format is proprietary and not designed for human reading or editing:

```json
{
  "item": [{
    "name": "Create a post",
    "request": {
      "method": "POST",
      "header": [{"key": "Content-Type", "value": "application/json"}],
      "body": {
        "mode": "raw",
        "raw": "{\"title\": \"My post\", \"userId\": 1}"
      },
      "url": { "raw": "{{baseUrl}}/posts", "host": ["{{baseUrl}}"], "path": ["posts"] }
    },
    "event": [{
      "listen": "test",
      "script": {
        "exec": [
          "pm.test('Status is 201', () => pm.response.to.have.status(201));",
          "pm.environment.set('postId', pm.response.json().id);"
        ]
      }
    }]
  }]
}
```

A `git diff` on this file when a single assertion changes is nearly unreadable. Merge conflicts between two engineers editing the same collection simultaneously are painful. Collections exported from the Postman app often contain generated IDs and timestamps that produce meaningless diffs.

### OpenBBT: plain text feature files

Every test is a `.feature` file — plain text, minimal syntax, fully diffable:

```gherkin
@ID-3 @POST
Scenario: Create a post
  When I make a POST request to "posts" with body:
    """json
    { "title": "My post", "userId": 1 }
    """
  Then the HTTP status code is equal to 201
  And I store the value of field 'id' from the response body into variable id
```

A `git diff` on this file is immediately understandable. Pull request reviews are meaningful. Branching strategies, conflict resolution, and history navigation all work as expected.

---

## Test Logic: JavaScript vs Declarative Steps

### Postman: JavaScript everywhere

Postman's test assertions and setup logic are JavaScript, written in a sandboxed environment (`pm` object). Even simple assertions require JS:

```javascript
// Pre-request script
const timestamp = new Date().toISOString();
pm.environment.set('timestamp', timestamp);

// Test script
pm.test('Status is 201', () => {
    pm.response.to.have.status(201);
});
pm.test('Response has id', () => {
    const body = pm.response.json();
    pm.expect(body.id).to.be.a('number');
    pm.environment.set('postId', body.id);
});
pm.test('Title matches', () => {
    pm.expect(pm.response.json().title).to.eql('My post');
});
```

This is code. Non-developer QA engineers, business analysts, and product managers cannot read it, let alone write it. As collections grow, they effectively become JavaScript codebases maintained inside a GUI tool — without linting, without IDE support, without unit testing.

### OpenBBT: declarative steps, no scripting

Every action in OpenBBT is a named step. There is no scripting language. Business analysts can read and validate every line:

```gherkin
When I make a POST request to "posts" with body:
  """json
  { "title": "My post", "userId": 1 }
  """
Then the HTTP status code is equal to 201
And the response body contains:
  """json
  { "title": "My post" }
  """
And I store the value of field 'id' from the response body into variable id
```

Steps are implemented once in Java (inside a plugin) and reused across all projects that declare the plugin. The test file contains only intent — zero logic.

---

## Database Testing

### Postman

Postman has no database support. It is an HTTP client. If a test scenario requires verifying that an API call correctly persisted data to a database, the only option is to query the database through another API endpoint — if one exists. There is no way to run SQL, assert table contents, or load database fixtures from a Postman collection.

### OpenBBT

Database testing is a first-class plugin:

```gherkin
Scenario: Creating a post persists it in the database
  When I make a POST request to "posts" with body:
    """json
    { "title": "My post", "userId": 1 }
    """
  Then the HTTP status code is equal to 201
  And I store the value of field 'id' from the response body into variable id
  * use db "main"
  * db query:
    """sql
    SELECT title FROM posts WHERE id = '${id}'
    """
  * db query count = 1
```

Any JDBC-compatible database works — MySQL, PostgreSQL, Oracle, SQL Server, H2, SQLite — declared as a runtime dependency without touching any build file:

```yaml
plugins:
  - rest
  - db with org.postgresql:postgresql-42.7.3
```

---

## Performance / Benchmark Testing

### Postman

Postman has a "Performance" tab (available on paid plans) that runs a collection under load and reports response times and error rates. Newman itself has no performance mode — it runs requests sequentially.

For serious performance testing against a CI gate, teams typically export Postman collections to k6 or use Gatling independently, which means maintaining the test logic in two places.

### OpenBBT: benchmark mode built in

Benchmark testing is integrated directly into the functional test suite. The same step used for functional testing is benchmarked with a single `Given`:

```gherkin
Scenario: POST /posts meets latency SLA
  Given benchmark mode is enabled with 500 executions and 16 threads
  When I make a POST request to "posts" with body:
    """json
    { "title": "My post", "userId": 1 }
    """
  Then the benchmark P95 response time (ms) is less than 200
  Then the benchmark error rate is equal to 0.0
  Then the benchmark throughput (req/s) is greater than 100.0
```

Statistics (min, max, mean, P50, P95, P99, throughput, error rate) are stored with the execution and visible in VS Code. Benchmark assertions fail the CI build if SLAs are breached. No extra tool, no extra plan, no extra pipeline stage.

---

## Environment and Profile Management

### Postman

Environments in Postman are key-value stores managed through the GUI and exported as JSON files. Switching environments requires either:

- Clicking a dropdown in the Postman app.
- Passing `--environment env.json` to Newman with a separate exported file.

Environments are flat maps — there is no hierarchy or composition. Sensitive values (passwords, tokens) often end up committed to the repository inside the environment JSON file, or managed out-of-band and injected via CI variables.

### OpenBBT

Profiles are declared in `openbbt.yaml` alongside the test configuration, as structured YAML with template substitution:

```yaml
configuration:
  rest:
    baseURL: '{{base-url}}/api'
  db:
    datasources:
      main:
        url: '{{db-url}}'
        username: '{{db-user}}'
        password: '{{db-password}}'

profiles:
  dev:
    base-url: http://localhost:8080
    db-url: jdbc:h2:mem:testdb
    db-user: sa
    db-password: ''
  staging:
    base-url: https://staging.example.com
    db-url: jdbc:postgresql://staging-db:5432/app
    db-user: testuser
    db-password: '{{DB_PASSWORD}}'   # injected from CI secret
```

Switching profiles: `openbbt run -p staging`. Sensitive values can be left as placeholders and overridden at runtime with `-D key=value`, keeping secrets out of the repository entirely.

---

## Execution History and Re-runs

### Postman / Newman

Newman produces a test report (JUnit XML, JSON, or HTML) at the end of each run. There is no persistent store of executions. To compare two runs, you must save the report files externally. Re-running an exact past execution — same requests, same environment snapshot — is not possible without manual reconstruction.

### OpenBBT

OpenBBT has a built-in persistence layer with three modes that cover the full spectrum from ephemeral CI to team-wide shared history:

| Mode | Backend | Attachments | Use case |
|---|---|---|---|
| `transient` | Temp HSQLDB (deleted on exit) | Temp directory | CI pipelines that only need pass/fail |
| `file` | HSQLDB file in `.openbbt/` | Local filesystem | Developer workstation, full history in VS Code |
| `remote` | PostgreSQL | MinIO (S3-compatible) | Shared history across CI and all developers |

In **`file` mode** (the default for server mode), every execution is persisted locally. From VS Code you can browse all past runs, drill into the full result tree (suite → scenario → step), view step-level attachments (response bodies, CSV query results), and re-run any past execution with one click.

In **`remote` mode**, CI writes to a shared PostgreSQL database and MinIO object store. Every developer's VS Code connects to the same backend and can immediately browse, inspect, and re-run any CI execution — including runs from other team members or other branches. Configuration is a single block in `openbbt.yaml`:

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

Because the execution store is a relational database, the data is also directly queryable for custom reporting, flakiness tracking, or SLA trend analysis — without any additional tooling.

From the CLI:

```bash
# Re-run a specific past execution by ID
openbbt run --rerun <execution-id>
```

---

## CI / CD Integration

### Newman

Newman is an npm package. Adding it to CI requires Node.js in the build environment, even for a Java project:

```bash
npm install -g newman
newman run collection.json \
  --environment staging-env.json \
  --reporters cli,junit \
  --reporter-junit-export results.xml
```

The collection and environment files must be either committed to the repository (with the diffs and merge conflict problems described earlier) or retrieved from the Postman API (requires an API key and internet access).

### OpenBBT

OpenBBT is a self-contained Java distribution. No Node.js, no npm, no Postman API key:

```bash
# Download and extract the distribution once
export PATH="$OPENBBT_HOME/bin:$PATH"

# Run in CI
openbbt install
openbbt run -s regression -p staging
```

The `openbbt.yaml` and `.feature` files are committed to the repository like any other source file. The run is fully reproducible and offline (after the initial plugin download).

---

## Cost and Licensing


|                    | **OpenBBT**       | **Postman**                                   |
| ------------------ | ----------------- | --------------------------------------------- |
| License            | MIT (open source) | Proprietary (freemium)                        |
| Free tier          | Unlimited         | Limited collaboration, limited mock calls     |
| Team collaboration | Via git           | Paid workspace features                       |
| Offline use        | ✅ fully offline  | ⚠️ some features require cloud connectivity |
| Self-hosting       | N/A (CLI tool)    | ⚠️ enterprise plan required                 |

Postman's free tier is generous for individual use, but teams that need shared workspaces, private APIs, or advanced monitoring quickly hit the paid tier. OpenBBT has no paid tier — it is MIT-licensed and entirely self-hosted.

---

## Two-Level Scenarios: Definition / Implementation

### Postman

Postman has no concept of separating test intent from test execution. A collection request is always both the specification and the implementation: the URL, headers, body, and JavaScript assertions are all one artifact. There is no way to write a business-readable description of what the test does separately from how it does it.

Some teams add a "description" field to requests, but this is free text with no structural meaning — it is not executable, not validated, and not visible in CI reports.

### OpenBBT: definition / implementation

OpenBBT introduces a two-level scenario model unique among testing tools. A **definition** feature (tagged `@definition`) is a plain-English, business-readable specification of test intent. An **implementation** feature (tagged `@implementation`) is the concrete execution, matched to the definition by scenario identifier.

```gherkin
# definition.feature — readable by anyone
@definition
Feature: Payment Processing

@ID-PAY-01
Scenario: A valid payment is accepted
  Given a customer with a valid credit card
  When a payment of €49.99 is submitted
  Then the payment is approved
  And the customer receives a confirmation

@ID-PAY-02
Scenario: An expired card is rejected
  Given a customer with an expired credit card
  When a payment of €49.99 is submitted
  Then the payment is declined
  And the customer receives an error message
```

```gherkin
# implementation.feature — the technical test
@implementation
Feature: Payment Processing — REST

# gherkin.step-map: 1-1-1-1
@ID-PAY-01
Scenario: A valid payment is accepted
  When I make a POST request to "payments" with body:
    """json
    { "amount": 49.99, "currency": "EUR", "card": "4111111111111111", "expiry": "12/28" }
    """
  Then the HTTP status code is equal to 200
  And the response body contains:
    """json
    { "status": "APPROVED" }
    """
  And the response body field "confirmationId" is not empty

# gherkin.step-map: 1-1-1-1
@ID-PAY-02
Scenario: An expired card is rejected
  When I make a POST request to "payments" with body:
    """json
    { "amount": 49.99, "currency": "EUR", "card": "4111111111111111", "expiry": "01/20" }
    """
  Then the HTTP status code is equal to 422
  And the response body contains:
    """json
    { "error": "CARD_EXPIRED" }
    """
  And the response body field "message" contains "expired"
```

Unlike Postman's request descriptions — which are free text that no tool validates or enforces — OpenBBT's definition features are structural: they drive the test plan, appear in the result tree, and provide the vocabulary that stakeholders sign off on. The implementation is hidden from business review but fully visible to engineers in the execution detail.

This enables a workflow impossible in Postman: a product owner reviews and approves the definition feature files in a pull request, without ever seeing the technical implementation details. The CI pipeline runs the implementation and reports results against the business-defined structure.

---

## Summary: When to Choose Each

### Choose Postman / Newman if

- Your primary use case is **interactive API exploration** — clicking through endpoints, inspecting responses, building up requests gradually.
- Your team needs **API documentation, mock servers, or contract testing** features built into the same tool.
- You need **API monitoring** with cloud-hosted scheduled runs.
- Your QA team has no familiarity with text-based tooling and a GUI is essential.

### Choose OpenBBT if

- You want tests that live in git, are reviewed in pull requests, and are as readable as documentation.
- You need to test more than HTTP — **database state, query results, cross-system assertions**.
- You want **benchmark SLA gates** in your CI pipeline without adding a separate load testing tool.
- Non-developer stakeholders need to read, validate, or contribute to test scenarios.
- You want a **fully offline, open-source** tool with no account, no API key, and no usage limits.
- Your team is graduating from "manual Postman exploration" to "automated, version-controlled test suite" and wants a clean architecture from the start.

---

> **Note:** Postman and OpenBBT are not mutually exclusive. Many teams use Postman for initial API exploration and request prototyping, then translate their validated scenarios into OpenBBT `.feature` files for the automated regression suite. Postman excels at the discovery phase; OpenBBT excels at the automation phase.

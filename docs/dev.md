# Azertio: API and Database Testing Without the Glue Code

If you have ever maintained a Cucumber + RestAssured test suite, you know the feeling. The feature files look clean. But underneath there are dozens of step definition classes, `ScenarioContext` maps to pass state between steps, `@Before` hooks to wire up the HTTP client, and a `pom.xml` that has grown to include JDBC drivers, connection pools, and fixtures loaders just to back up a few database assertions.

The tests work. But they are a project in themselves.

**Azertio** is a new open-source testing tool that takes a different approach: instead of writing glue code, you declare plugins. Instead of managing a build file, you configure a YAML file. Instead of parsing JUnit XML reports, you browse a live execution history in your IDE.

Let me walk you through what it does and how it is designed.

---

## The Problem: Tests That Require Too Much Code

BDD tools like Cucumber are built around a promise: business stakeholders write the test scenarios, developers implement the steps. In practice this rarely holds. Every new step a tester wants to use requires a developer to:

1. Write a Java method annotated with `@Given`, `@When`, or `@Then`
2. Wire it to a Gherkin expression via a regex
3. Manage shared state between steps
4. Register configuration in a Spring context or a custom hook

The result is that feature files — the part everyone can read — are tightly coupled to a growing body of Java infrastructure that almost nobody reads. And this infrastructure is per-project: if three teams test REST APIs, each team writes their own `iMakeGetRequest` and `statusCodeIs` methods.

Azertio's premise is simple: **the most common test steps should already exist as reusable, versioned plugins, and you should not need to write any Java to use them.**

---

## How It Works

An Azertio project consists of two things: an `azertio.yaml` configuration file and your `.feature` files. That's it — no `pom.xml`, no step definition classes, no build tool.

```yaml
# azertio.yaml
testProject:
  organization: Acme Corp
  name: Payment Service Tests
  test-suites:
    - name: smoke
      tag-expression: "smoke"
    - name: regression
      tag-expression: "regression or smoke"

plugins:
  - gherkin
  - rest
  - db with org.postgresql:postgresql-42.7.3

configuration:
  rest:
    baseURL: "{{base-url}}/api"
    timeout: 10000

profiles:
  dev:
    base-url: http://localhost:8080
  staging:
    base-url: https://staging.example.com
```

Then you install the plugins (downloaded from Maven Central and cached locally):

```bash
azertio install
```

And run your tests:

```bash
azertio run -s smoke -p staging
```

Your feature files can use REST and database steps immediately, with no backing code:

```gherkin
Scenario: Creating a payment persists it in the database
  When I make a POST request to "payments" with body:
    """json
    { "amount": 49.99, "currency": "EUR", "card": "4111111111111111" }
    """
  Then the HTTP status code is equal to 201
  And I store the value of field "id" from the response body into variable paymentId
  * use db "main"
  * db query:
    """sql
    SELECT status FROM payments WHERE id = '${paymentId}'
    """
  * db query count = 1
```

Variable interpolation (`${paymentId}`) is first-class. No custom parameter types, no manual string replacement.

---

## Architecture: Plugins All the Way Down

Every capability in Azertio is a plugin — including the Gherkin parser, the REST steps, and the database steps. Plugins are standard Maven artifacts loaded at runtime via the **Java Platform Module System (JPMS)**.

This has concrete consequences:

**No classpath pollution.** Each plugin runs in its own module layer. A plugin's dependencies cannot interfere with the core runtime or with other plugins. You can have two plugins that depend on different versions of the same library without conflict.

**Runtime dependency declaration.** Need to test a MySQL database? Add `db with com.mysql:mysql-connector-j` to `azertio.yaml`. The JDBC driver is downloaded from Maven Central and loaded into the plugin's module layer at runtime. No `pom.xml` edit required, because there is no `pom.xml`.

**True extensibility.** Writing a custom step plugin is straightforward: implement `StepProvider`, annotate methods with `@StepExpression`, and publish the artifact to any Maven repository. Teams that consume it just add one line to `azertio.yaml`.

```java
@Extension(name = "My Protocol", scope = Scope.TRANSIENT)
public class MyStepProvider implements StepProvider {

    @StepExpression(value = "my.connect", args = {"host:text", "port:integer"})
    public void connect(String host, int port) {
        // your implementation
    }
}
```

The consuming project gets it automatically on the next `azertio install` — no code changes, no dependency management.

---

## Benchmark Mode: Performance Testing Without a Separate Tool

One problem with specialized testing tools is proliferation. You have one tool for functional tests (Cucumber), one for performance (Gatling or k6), and you end up maintaining the same test logic in two places.

Azertio has a built-in benchmark mode that works on any compatible step in the same `.feature` file:

```gherkin
Scenario: Functional — retrieve payment
  When I make a GET request to "payments/1"
  Then the HTTP status code is equal to 200

Scenario: Performance — retrieve payment meets SLA
  Given benchmark mode is enabled with 500 executions and 16 threads
  When I make a GET request to "payments/1"
  Then the benchmark P95 response time (ms) is less than 150
  Then the benchmark error rate is equal to 0.0
  Then the benchmark throughput (req/s) is greater than 200.0
```

Benchmark runs use virtual threads for high concurrency with low resource overhead. Statistics (min, max, mean, P50, P95, P99, throughput, error rate) are stored with the execution and visible in the VS Code extension alongside functional results. If the P95 threshold is breached, the CI build fails — no Gatling server, no separate pipeline stage.

---

## The Definition / Implementation Model

One of Azertio's most distinctive features is a two-level scenario model that has no direct equivalent in other tools.

In most BDD projects, feature files serve two masters at once: business stakeholders who need to validate that tests reflect real requirements, and engineers who need steps precise enough to execute. These two needs pull in opposite directions, and the result is usually a compromise that satisfies neither.

Azertio solves this with a formal separation. A **definition** feature (tagged `@definition`) contains the abstract, business-readable test intent:

```gherkin
@definition
Feature: User Registration

@ID-REG-01
Scenario: A new user can register with valid data
  Given a valid registration form
  When the form is submitted
  Then the account is created
  And a welcome email is sent
```

An **implementation** feature (tagged `@implementation`) contains the concrete, executable steps, matched to the definition by identifier:

```gherkin
@implementation
Feature: User Registration — REST

# gherkin.step-map: 1-1-1-1
@ID-REG-01
Scenario: A new user can register with valid data
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

At plan-build time the two files are merged. The result tree shows the definition structure — business-readable, approvable in a pull request — while executing the implementation steps underneath each abstract step. Stakeholders see the definition; the framework runs the implementation.

The `gherkin.step-map` comment controls how many implementation steps correspond to each abstract definition step, including `0` for steps that should appear in the result tree but not execute.

This model is particularly useful for:
- **Regulatory traceability** — the definition is the signed-off specification; the implementation is the audit trail.
- **Multilingual teams** — definition in the business language, implementation in the team's preferred language.
- **Protocol evolution** — one definition, multiple implementations (REST today, gRPC tomorrow).

---

## Execution History and VS Code Integration

A persistent frustration with most testing tools is that execution history vanishes. JUnit XML reports go into `target/`, CI artifacts expire after 30 days, and there is no way to compare last Thursday's run with today's without a separate Allure or ReportPortal setup.

Azertio has a built-in persistence layer with three modes:

| Mode | Backend | Use case |
|---|---|---|
| `transient` | Temp HSQLDB (deleted on exit) | CI pipelines that only need pass/fail |
| `file` | HSQLDB file in `.azertio/` | Developer workstation |
| `remote` | PostgreSQL + MinIO | Shared team history |

In `remote` mode, every CI run writes the full execution tree — plan, suites, scenarios, steps, timings, and binary attachments — to a shared PostgreSQL database. Every developer's VS Code extension connects to the same backend and can browse, inspect, and re-run any past execution from any branch or CI run.

The VS Code extension connects to the CLI via a JSON-RPC server and provides:
- **Execution history** with date, duration, and pass/fail status
- **Result tree navigation** from suite down to individual steps with timings
- **Attachment inspection** — response bodies, CSV query results, any data produced by steps
- **One-click re-run** of any past execution against its original plan and profile
- **Inline benchmark statistics** alongside functional results

No Allure server. No external dashboard. No configuration beyond the persistence block in `azertio.yaml`.

---

## Comparison: How Azertio Fits Against Other Tools

### vs. Cucumber + RestAssured

Cucumber requires glue code for every step — a Java method annotated with `@Given`/`@When`/`@Then`. RestAssured provides the HTTP layer, but state management between steps, configuration wiring, and especially database assertions are all your problem.

Azertio eliminates glue code entirely. The `rest` and `db` plugins cover the majority of API and database testing scenarios without writing any Java. When you do need custom steps, you write a plugin once and reuse it across all projects.

The tradeoff: if your tests are tightly coupled to a Spring Boot application context (accessing internal services, using transactional rollback between scenarios), Cucumber + RestAssured remains the right tool. Azertio is explicitly a black-box tool and does not access application internals.

### vs. Karate

Karate embeds a JavaScript engine inside `.feature` files. You can call Java, write loops, and use conditional logic directly in feature files — which is powerful, but it means tests become mini-programs that non-technical stakeholders cannot read or validate.

Azertio enforces a clean boundary: feature files contain only declarative steps, all logic lives in typed Java step providers. The configuration model is also fundamentally different — Karate uses a `karate-config.js` JavaScript file for environments, while Azertio uses pure YAML profiles. Azertio also has first-class features that Karate lacks: the definition/implementation model, built-in persistence, and a dedicated VS Code extension.

The tradeoff: Karate has a larger ecosystem, more community resources, and built-in support for protocols (gRPC, WebSocket) not yet available as Azertio plugins.

### vs. Postman / Newman

Postman is an excellent tool for interactive API exploration. Newman makes collections runnable in CI. The problems emerge when teams graduate to treating tests as code: Postman collections are proprietary JSON that produces noisy diffs, assertions are JavaScript scattered across collection items, and there is no database testing, no benchmark mode, and no persistent execution history.

Azertio's `.feature` files are plain text, fully diffable, reviewable in pull requests, and readable by business stakeholders. The two tools are not mutually exclusive — many teams use Postman for initial exploration and translate validated scenarios into Azertio for the automated regression suite.

---

## Quick Start

```bash
# 1. Download the distribution from GitHub releases and add bin/ to PATH

# 2. Create azertio.yaml in your project
cat > azertio.yaml <<EOF
testProject:
  name: My Project
  test-suites:
    - name: smoke
      tag-expression: "smoke"
plugins:
  - gherkin
  - rest
configuration:
  rest:
    baseURL: "https://jsonplaceholder.typicode.com"
EOF

# 3. Write a feature file
mkdir -p features && cat > features/posts.feature <<EOF
@smoke
Feature: Posts API

  Scenario: Retrieve a post
    When I make a GET request to "posts/1"
    Then the HTTP status code is equal to 200
    And the response body contains:
      """json
      { "id": 1 }
      """
EOF

# 4. Install plugins
azertio install

# 5. Run
azertio run -s smoke
```

---

## Where Things Stand

Azertio is in early alpha. The core runtime, the REST plugin, and the database plugin are functional; the VS Code extension is published. Protocol plugins for gRPC, GraphQL, and WebSocket are on the roadmap but not yet available.

If you are building a new test suite for API and database testing and want clean feature files, no glue code, and execution history out of the box — it is worth a look.

- **GitHub:** [github.com/org-azertio/azertio](https://github.com/org-azertio/azertio)
- **Getting started:** [docs/getting-started.md](https://github.com/org-azertio/azertio/blob/main/docs/getting-started.md)
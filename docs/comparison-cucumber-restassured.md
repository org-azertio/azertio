# OpenBBT vs Cucumber + RestAssured — A Detailed Comparison

Cucumber + RestAssured is arguably the most common Java BDD stack for API testing: Cucumber provides the Gherkin layer and test execution, while RestAssured handles HTTP interactions through Java glue code. The combination is powerful but comes with significant ceremony. This document compares it against OpenBBT, focusing on the areas where each approach shines.

---

## Quick Overview

| | **OpenBBT** | **Cucumber + RestAssured** |
|---|---|---|
| First release | 2025 | 2008 / 2010 |
| Language | Java 21 | Java |
| Build integration | Standalone CLI + Maven | Maven / Gradle (test scope) |
| Test format | Gherkin / compact DSL / Markdown | Gherkin |
| Architecture | Plugin-based (JPMS modules) | Library-based, glue code required |
| Glue code required | ❌ zero | ✅ every step needs a Java method |
| REST testing | ✅ via `rest` plugin | ✅ via RestAssured step definitions |
| Database testing | ✅ via `db` plugin (any JDBC) | ⚠️ manual (Spring JDBC, jOOQ, etc.) |
| Performance testing | ✅ built-in benchmark mode | ❌ separate tool (JMeter, Gatling…) |
| VS Code extension | ✅ dedicated extension | ❌ generic Cucumber highlighting only |
| Custom steps | ✅ write a plugin, distribute as artifact | ✅ write Java step defs in the project |
| Multilingual steps | ✅ EN / ES / compact DSL | ⚠️ manual per-project translation |
| Runtime deps | ✅ declared in YAML, auto-downloaded | ❌ must be in Maven/Gradle POM |
| Step reuse across projects | ✅ publish plugin to Maven | ⚠️ copy-paste or shared library |

---

## Philosophy

### Cucumber + RestAssured: BDD shell around code

Cucumber was designed to bridge business analysts and developers: business writes the Gherkin scenarios, developers write the step definitions. In practice, every Gherkin line must be backed by a Java method annotated with `@Given`, `@When`, or `@Then`. RestAssured provides a fluent Java API to make HTTP calls inside those methods.

The result is that for every new step a team wants to use, a developer must:

1. Write a new Java method.
2. Wire it to a Gherkin expression via a regex or Cucumber Expression.
3. Manage shared state between steps (usually with instance variables, `ScenarioContext` maps, or ThreadLocal).
4. Register any new configuration in the Spring context or a custom hook.

Feature files are readable, but the code that backs them is spread across dozens of step definition classes. Maintenance grows proportionally with the number of steps.

```java
// Step definition class — one per domain area, multiplied across projects
public class PostsStepDefs {

    private RequestSpecification request;
    private Response response;

    @Before
    public void setUp() {
        request = RestAssured.given()
            .baseUri(System.getenv("BASE_URL"))
            .contentType(ContentType.JSON);
    }

    @When("I make a GET request to {string}")
    public void iMakeGetRequest(String path) {
        response = request.get(path);
    }

    @Then("the HTTP status code is equal to {int}")
    public void statusCodeIs(int expected) {
        response.then().statusCode(expected);
    }

    @Then("the response body contains:")
    public void responseBodyContains(String body) {
        // custom JSON subset comparison…
        response.then().body(matchesJson(body));
    }
}
```

This is code any Java developer can write — but it must be written, tested, and maintained for every project.

### OpenBBT: steps are plugins, not glue code

In OpenBBT there is no glue code. Steps are provided by plugins — Maven artifacts that are declared in `openbbt.yaml` and downloaded automatically. Feature files call steps by name; the framework resolves them:

```gherkin
When I make a GET request to "posts"
Then the HTTP status code is equal to 200
And the response body contains:
  """json
  [{"userId": 1, "id": 1}]
  """
```

No Java class backs this test. The `rest` plugin provides all REST steps. A team with zero Java experience can write and run complete API test suites.

---

## The Glue Code Problem

The most fundamental difference between the two approaches is glue code — and its consequences are far-reaching.

### In Cucumber + RestAssured

Each Gherkin step is a regex or Cucumber Expression mapped to a Java method. Everything the test does flows through those methods. This creates several friction points:

**Duplication across projects.** Step definitions are written per-project. If three teams test REST APIs, each team writes their own `iMakeGetRequest`, `statusCodeIs`, `responseBodyContains`. Sharing them requires extracting a shared library — a non-trivial effort with its own versioning, publication, and dependency management.

**State management complexity.** RestAssured is stateless; step definitions are not. The `response` object from a `@When` step must be available to the `@Then` step. Teams solve this with instance variables (Cucumber creates a new step definition instance per scenario, which helps), injection frameworks (PicoContainer, Spring), or manual `ScenarioContext` maps:

```java
// Common pattern: pass state through a shared context object
public class ScenarioContext {
    private final Map<String, Object> data = new HashMap<>();
    public void set(String key, Object value) { data.put(key, value); }
    public Object get(String key) { return data.get(key); }
}
```

**Expression ambiguity.** Two step definitions with overlapping regex patterns cause runtime `AmbiguousStepDefinitionException`. As the codebase grows, keeping expressions non-overlapping becomes a discipline problem.

**Step discovery at runtime.** Cucumber scans the classpath for step definitions using reflection. With many modules, getting the scan path right — and keeping it fast — requires explicit configuration.

### In OpenBBT

Steps are registered by plugins via the Java module system. There is no scanning, no reflection across the classpath, no ambiguity. Each step has a unique dot-separated ID (e.g. `rest.request.GET`). State between steps is stored in a typed `ExecutionContext` — no maps, no ThreadLocals, no injection framework needed.

---

## REST API Testing

Both stacks can test REST APIs. The feature-file syntax is very similar because both use Gherkin. The difference is what happens underneath.

**Cucumber + RestAssured feature file:**
```gherkin
Scenario: Create a post and retrieve it
  Given the base URL is "https://jsonplaceholder.typicode.com"
  When I POST to "/posts" with body:
    """
    { "title": "My post", "userId": 1 }
    """
  Then the status code is 201
  And I store the response field "id" as "postId"
  When I GET "/posts/{postId}"
  Then the status code is 200
```

This feature file looks clean, but behind it is a step definition class that implements every one of those steps, including the variable substitution for `{postId}` — typically done with a custom Cucumber parameter type or manual string replacement.

**OpenBBT feature file (identical in structure):**
```gherkin
Scenario: Create a post and retrieve it
  When I make a POST request to "posts" with body:
    """json
    { "title": "My post", "userId": 1 }
    """
  Then the HTTP status code is equal to 201
  And I store the value of field 'id' from the response body into variable id
  When I make a GET request to "posts/${id}"
  Then the HTTP status code is equal to 200
```

Variable interpolation (`${id}`) is a first-class feature of `ExecutionContext`. No custom parameter type, no Java code needed.

---

## Database Testing

### Cucumber + RestAssured

There is no standard database step library for Cucumber. Each team builds its own:

1. Add a JDBC driver to `pom.xml`.
2. Add a connection pool library (HikariCP, etc.) to `pom.xml`.
3. Write a `DatabaseHelper` or `DataSource` Spring bean.
4. Write step definitions that use it.
5. Handle `NULL` values, type coercions, and teardown manually.

Typical boilerplate for a single database assertion step:

```java
@Then("the table {word} contains {int} rows")
public void tableRowCount(String table, int expected) {
    int actual = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM " + table, Integer.class
    );
    assertThat(actual).isEqualTo(expected);
}
```

A full DB step vocabulary (query execution, table assertions, CSV/Excel fixtures, teardown hooks) takes days to build properly and must be replicated for each project.

### OpenBBT

The `db` plugin provides a complete, production-ready database step vocabulary:

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
* db table orders is CSV "expected/orders.csv"
* db has XLS "expected/full-dataset.xlsx"
```

The JDBC driver is declared as a runtime dependency and downloaded automatically:

```yaml
plugins:
  - db with com.mysql:mysql-connector-j
```

No `pom.xml` changes. No connection pool setup. No helper classes. The plugin handles connection management, type coercion, NULL sentinels, CSV and Excel fixture loading, and teardown hooks — ready to use in minutes.

---

## Configuration and Environment Management

### Cucumber + RestAssured

Configuration in this stack typically flows through one of:

- **System properties / environment variables** read in `@Before` hooks.
- **Spring Boot `application.yml`** profiles activated via Maven (`-Dspring.profiles.active=staging`).
- **A custom `ConfigurationLoader`** class that reads a properties file.

None of these is standard — teams implement them differently, and new team members must understand the local convention before they can run tests.

```java
@Before
public void setUp() {
    String env = System.getProperty("env", "dev");
    String baseUrl = switch (env) {
        case "staging" -> "https://staging.example.com";
        case "prod"    -> "https://api.example.com";
        default        -> "http://localhost:8080";
    };
    RestAssured.baseURI = baseUrl;
}
```

### OpenBBT

Configuration is always `openbbt.yaml`, with first-class profile support. No Java code, no Spring, no system property conventions to learn:

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

Activating a profile: `openbbt run -p staging`. Any team member can read and modify this without knowing Java.

---

## Performance Testing

### Cucumber + RestAssured

There is no performance testing built into this stack. Teams that need load or benchmark testing add a completely separate tool — JMeter, Gatling, k6 — with its own scripting language, configuration, and CI pipeline. Functional test scenarios cannot be reused as performance scenarios without rewriting them in the target tool's language.

### OpenBBT: benchmark mode in the same feature file

OpenBBT integrates benchmark testing directly. The same step used for functional testing can be run in benchmark mode in the same `.feature` file:

```gherkin
Scenario: Functional — create a post
  When I make a POST request to "posts" with body:
    """json
    { "title": "My post", "userId": 1 }
    """
  Then the HTTP status code is equal to 201

Scenario: Performance — create post meets SLA
  Given benchmark mode is enabled with 500 executions and 16 threads
  When I make a POST request to "posts" with body:
    """json
    { "title": "My post", "userId": 1 }
    """
  Then the benchmark P95 response time (ms) is less than 200
  Then the benchmark error rate is equal to 0.0
  Then the benchmark throughput (req/s) is greater than 100.0
```

No separate tool, no separate repository, no separate CI job. Benchmarks are tracked per-execution and visible in the VS Code extension alongside functional results.

---

## Step Reuse Across Projects

### Cucumber + RestAssured

Steps are Java classes in the project. Sharing them across projects requires:

1. Extracting the step definitions into a separate Maven module.
2. Publishing it to a repository (Maven Central, a private Nexus/Artifactory, GitHub Packages).
3. Managing the version dependency in each consuming project's `pom.xml`.
4. Handling any transitive dependency conflicts.

This is standard Java library management, but it is non-trivial for teams whose primary skill is testing rather than library publishing.

### OpenBBT

Custom plugins are Maven artifacts by design. Publishing a plugin and consuming it in another project is the native workflow:

```yaml
# In any project that needs the custom step
plugins:
  - org.myteam:my-custom-steps-plugin:1.2.0
```

The plugin is downloaded from Maven Central (or any configured repository) at `openbbt install` time. The consuming project does not need to add anything to a `pom.xml` — it has no `pom.xml` at all.

---

## IDE and Tooling

### Cucumber + RestAssured

IDE support for Cucumber is mature: both IntelliJ and VS Code have plugins that link Gherkin steps to their Java implementations, highlight undefined steps, and run individual scenarios. However, this support is about editing assistance, not test execution management. Browsing past results, inspecting step-level timings, viewing response body attachments, or re-running a specific historical execution requires external tools (Allure, Extent Reports, a CI dashboard).

### OpenBBT

The VS Code extension provides a complete execution management UI, not just editing assistance:

- **Execution history**: every run is stored locally with date, duration, and overall status.
- **Result tree**: drill from test plan → suite → scenario → step, with individual pass/fail and timing.
- **Attachments**: query result CSVs, JSON response bodies, and other step outputs are stored with the execution and openable inline.
- **One-click re-run**: replay any past execution against its original test plan and profile.
- **Benchmark statistics**: visible per-step alongside functional results.

No Allure setup. No report server. No external dashboard.

---

## Test Execution Model

### Cucumber + RestAssured

Tests are run as part of a Maven or Gradle build. This means:

- The project must be compilable (`mvn test` fails if there are compile errors in unrelated code).
- Running a subset of scenarios requires Maven Surefire / Failsafe configuration or command-line tag filters.
- Execution history is not stored by default — you get a JUnit XML report that is discarded after the build.
- Re-running a past execution is not possible without re-running the full build with the same inputs.

### OpenBBT

OpenBBT is a standalone CLI. Tests do not need a Java project; only an `openbbt.yaml` and `.feature` files are required:

```bash
# Install plugins (once)
openbbt install

# Run a suite
openbbt run -s smoke -p staging

# Re-run a past execution by ID
openbbt run --rerun <execution-id>
```

Every execution is persisted to a local HSQLDB database. Past executions can be inspected, compared, and re-run from the CLI or the VS Code extension at any time.

---

## Summary: When to Choose Each

### Choose Cucumber + RestAssured if

- Your team already has Cucumber expertise and a large library of step definitions.
- You need deep Spring Boot integration (test slices, application context reuse, transaction rollback).
- Your tests are tightly coupled to the application source code (integration tests, not black-box).
- You need step-level Java access to internal application components (repositories, services).
- Your CI pipeline is Maven-centric and you want tests to run as part of the build lifecycle.

### Choose OpenBBT if

- You want feature files that non-developers can read, write, and maintain without a developer involved for every new step.
- You are testing external systems (APIs, databases) from the outside — true black-box testing.
- You want database testing without writing connection helpers, step definitions, and fixture loaders from scratch.
- You need performance benchmarks without adding a separate tool and pipeline.
- You work in VS Code and want execution history, result inspection, and re-run built into the editor.
- You want to share test step implementations across projects without managing a shared Java library.
- Your test project has no build file and you want to keep it that way.

---

> **Note:** OpenBBT is under active development. Spring context integration and transactional rollback between scenarios — common in Cucumber integration test suites — are not part of OpenBBT's scope by design: OpenBBT is a black-box tool and does not access application internals. For white-box integration testing within a Spring Boot application, Cucumber + RestAssured (or Spring's own `MockMvc` test support) remains the appropriate choice.
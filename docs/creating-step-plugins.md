# Creating Step Plugins for Azertio

This guide explains how to build a step plugin for Azertio — a self-contained module that contributes new test steps to the framework. The `rest-azertio-plugin` is used as the reference example throughout.

---

## Overview

A step plugin consists of up to three contributors, each registered via the Java module system:

| Contributor | Interface | Purpose |
|---|---|---|
| Step provider | `StepProvider` | Implements the step logic |
| Config provider | `ConfigProvider` | Declares configuration keys |
| Message provider | `MessageProvider` | Maps step IDs to natural-language expressions |

All three are discovered automatically by Azertio through the `jexten` extension framework.

---

## Project Setup

### Maven POM

Inherit from `azertio-plugin-starter` to get the compiler annotation processor, the jexten bundle assembler, and the doc generation plugin pre-configured:

```xml
<parent>
    <groupId>org.azertio</groupId>
    <artifactId>azertio-plugin-starter</artifactId>
    <version>1.0.0-alpha1</version>
</parent>

<groupId>org.azertio.plugins</groupId>
<artifactId>my-azertio-plugin</artifactId>
<version>1.0.0-alpha1</version>
```

The starter POM automatically:
- Runs the `jexten-processor` annotation processor to generate the extension manifest
- Runs `jexten-maven-plugin` to assemble the plugin bundle (excluding `azertio-core`, which is provided by the host)
- Runs `azertio-docgen-maven-plugin` to generate step and config reference docs from YAML

### module-info.java

Declare the module, its dependencies, and the services it provides:

```java
import com.example.MyConfigProvider;
import com.example.MyMessageProvider;
import com.example.MyStepProvider;

module org.azertio.plugins.my {

    requires org.myjtools.jexten;
    requires org.azertio.core;
    requires org.myjtools.imconfig;

    provides org.azertio.core.contributors.StepProvider  with MyStepProvider;
    provides org.azertio.core.contributors.ConfigProvider with MyConfigProvider;
    provides org.azertio.core.messages.MessageProvider    with MyMessageProvider;

    exports org.azertio.plugins.my;
    opens   org.azertio.plugins.my to org.myjtools.jexten;
}
```

The `opens` declaration is required so that jexten can inject fields at runtime.

---

## Step Provider

`StepProvider` is the core contributor. Each public method annotated with `@StepExpression` becomes a test step.

```java
@Extension(
    name = "My steps provider",
    scope = Scope.TRANSIENT,        // new instance per test plan execution
    extensionPointVersion = "1.0"
)
public class MyStepProvider implements StepProvider {

    @Inject
    ResourceFinder resourceFinder;  // reads files relative to the test resource path

    @Inject
    ContentTypes contentTypes;      // resolves ContentType by MIME type

    @Override
    public void init(Config config) {
        // Called once per execution with the merged configuration
        String baseUrl = config.getString("my.baseUrl").orElse("");
    }

    @StepExpression(value = "my.step.doSomething", args = {"param:text"})
    public void doSomething(String param) {
        // step implementation
    }
}
```

### `@Extension` attributes

| Attribute | Purpose |
|---|---|
| `name` | Human-readable name shown in tooling |
| `scope = Scope.TRANSIENT` | A fresh instance is created for each test plan execution (recommended for stateful providers) |
| `extensionPointVersion` | Must match the version declared by the extension point |

### `@StepExpression` attributes

| Attribute | Purpose |
|---|---|
| `value` | A dot-separated ID that uniquely identifies this step across all plugins (e.g. `rest.request.GET`) |
| `args` | Inline parameter declarations: `"name:type"`. Recognized types include `text`, `id`, `integer` |

Steps can accept the following special parameter types that Azertio resolves automatically:

| Parameter type | Java type | Description |
|---|---|---|
| Inline parameter | `String` | A value specified inline in the step expression |
| Document | `Document` | A multi-line body attached to the step (e.g. a JSON block) |
| Assertion | `Assertion` | An assertion expression (e.g. `is equal to 200`) |

### REST plugin example

```java
@StepExpression(value = "rest.request.GET", args = {"endpoint:text"})
public void get(String endpoint) {
    restEngine.requestGET(interpolate(endpoint));
}

@StepExpression(value = "rest.request.POST.body", args = {"endpoint:text"})
public void postWithBody(String endpoint, Document body) {
    restEngine.requestPOST(interpolate(endpoint), interpolate(body.content()));
}

@StepExpression("rest.response.statusCode")
public void checkStatusCode(Assertion assertion) {
    Assertion.assertThat(restEngine.responseHttpCode(), assertion);
}
```

### Variable interpolation

Use `ExecutionContext.current().interpolateString(text)` to expand `${variableName}` references in step arguments. This allows steps to share values across a scenario:

```java
protected String interpolate(String text) {
    return ExecutionContext.current().interpolateString(text);
}
```

To store a value into a variable for use in subsequent steps, call:

```java
ExecutionContext.current().setVariable(variableName, value);
```

### Injected helpers

| Field type | Description |
|---|---|
| `ResourceFinder` | Reads files from the configured test resource path (`core.resourcePath`) |
| `ContentTypes` | Looks up a `ContentType` implementation by MIME type string |

---

## Config Provider

`ConfigProvider` declares the configuration keys that your plugin understands. Extend `ConfigAdapter` and point it to a YAML resource file:

```java
@Extension
public class MyConfigProvider extends ConfigAdapter implements ConfigProvider {

    @Override
    protected String resource() {
        return "config.yaml";   // path inside the plugin JAR resources
    }
}
```

### config.yaml format

Each top-level key is a configuration property:

```yaml
my.baseUrl:
  description: |
    The base URL for all requests made by this plugin.
  type: text

my.timeout:
  description: |
    Request timeout in milliseconds.
  type: integer
  defaultValue: 10000
```

Supported types: `text`, `integer`, `boolean`.

The configuration is merged with user-supplied values and passed to `StepProvider.init(Config config)`.

---

## Message Provider

`MessageProvider` maps step IDs to natural-language expressions in one or more languages. Extend `StepDocMessageAdapter`:

```java
@Extension
public class MyMessageProvider extends StepDocMessageAdapter implements MessageProvider {

    public MyMessageProvider() {
        super("steps.yaml");    // canonical step doc (used for doc generation)
    }

    @Override
    protected Map<String, String> languageResources() {
        var map = new LinkedHashMap<String, String>();
        map.put("dsl", "steps_dsl.yaml");
        map.put("en",  "steps_en.yaml");
        map.put("es",  "steps_es.yaml");
        return map;
    }

    @Override
    public boolean providerFor(String category) {
        // Return true for the step provider class name this message provider targets
        return MyStepProvider.class.getSimpleName().equals(category);
    }
}
```

### steps.yaml — canonical step doc

This file documents each step and is consumed by the `azertio-docgen-maven-plugin` to generate Markdown reference docs. It is also the source of truth for parameter metadata:

```yaml
'my.step.doSomething':
  role: when               # when | then | given
  description: |
    Does something with the given parameter.
  parameters:
    - name: param
      type: text
      description: The value to use
  additional-data: |       # optional: description of an attached Document body
    The body content, provided as a multi-line text input.
```

### steps_en.yaml — language file

Language files map each step ID to the expression that test authors write:

```yaml
'my.step.doSomething':
  expression: 'I do something with {param:text}'
  example: |
    When I do something with "hello"
  scenarios:
    - title: Example usage
      gherkin: |
        When I do something with "hello"
        Then the result is equal to "world"
```

Parameter placeholders in expressions use `{name:type}` syntax. For assertion parameters use `{{integer-assertion}}` or `{{text-assertion}}`.

Multiple language files can be provided; Azertio selects the file matching the language configured for the test suite.

---

## Benchmark Support

Steps that can be executed in [benchmark mode](benchmark.md) must do two things:

1. Annotate the method with `@StatisticsProvider`.
2. Manually record the start and finish of each iteration via the `Benchmark` API.

Without `@StatisticsProvider`, the framework rejects the step at runtime when benchmark mode is active.

### Implementation

```java
import org.azertio.core.backend.Benchmark;
import org.azertio.core.backend.ExecutionContext;
import org.azertio.core.contributors.StatisticsProvider;

@StatisticsProvider
@StepExpression(value = "my.step.measure", args = {"endpoint:text"})
public void measureEndpoint(String endpoint) {
    Benchmark benchmark = ExecutionContext.current().benchmark();
    if (benchmark == null) return;   // not in benchmark mode — should not happen, but defensive

    int n = benchmark.markStarted();
    boolean error = false;
    try {
        // the actual work being benchmarked
        httpClient.get(endpoint);
    } catch (Exception e) {
        error = true;
        throw e;
    } finally {
        benchmark.markFinished(n, error);
    }
}
```

### Rules

- `markStarted()` returns an execution number that must be passed to `markFinished()`. Both calls must happen in the same virtual thread.
- Pass `error = true` to `markFinished()` if the iteration failed; this counts towards `errorRate` but does not affect the timing statistics.
- The step does not need to handle the case where benchmark mode is not active — the framework only calls it in benchmark mode when `@StatisticsProvider` is present. Defensive guards (`if (benchmark == null)`) are optional but harmless.
- Timing is measured by the framework clock (`Clock` injected into `Benchmark`), not by wall time directly. Tests can inject a controllable clock for unit testing.

---

## ContentType Integration

If your steps need to compare structured content (JSON, XML, YAML, text), use the injected `ContentTypes` registry:

```java
@Inject
ContentTypes contentTypes;

// Look up a comparator by MIME type (or shorthand like "json", "xml")
contentTypes.get(mimeType).ifPresent(comparator ->
    comparator.assertContentEquals(expected, actual, ComparisonMode.STRICT)
);
```

Available comparison modes:

| Mode | Meaning |
|---|---|
| `STRICT` | All fields/elements must match exactly |
| `ANY_ORDER` | Same elements, order-independent (arrays/children may be reordered) |
| `LOOSE` | Expected is a subset of actual; extra fields in actual are allowed |

You can also extract values from structured content for variable storage:

```java
String value = comparator.extractValue(responseBody, "$.user.id");
ExecutionContext.current().setVariable("userId", value);
```

---

## Testing

Use `AzertioExtension` with JUnit 5 to run full end-to-end tests against real feature files. WireMock is the recommended tool for mocking HTTP endpoints in REST plugin tests.

```java
@ExtendWith(AzertioExtension.class)
class TestMySteps {

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
        .options(wireMockConfig().dynamicPort())
        .build();

    @BeforeEach
    void stubEndpoints() {
        wireMock.stubFor(get("/users")
            .willReturn(ok()
                .withHeader("Content-Type", "application/json")
                .withBody("[]")));
    }

    @Test
    @FeatureDir("get-200")          // loads feature files from src/test/resources/get-200/
    void get200_passes(JUnitAzertioPlan plan) {
        plan.withConfig("my.baseURL", "http://localhost:" + wireMock.getPort())
            .execute()
            .assertAllPassed();
    }
}
```

Each `@FeatureDir` test loads all `.feature` files from the named subdirectory under `src/test/resources/` and runs them as a single test plan execution. Use `assertAllPassed()` or `assertAllFailed()` to verify outcomes.

### Test module-info.java

The test module needs access to WireMock (which is non-modular). Add `--add-reads` in both the compiler and surefire configurations, as shown in the REST plugin's POM.

---

## File Layout Summary

```
my-azertio-plugin/
├── pom.xml                                    (inherits azertio-plugin-starter)
└── src/
    ├── main/
    │   ├── java/
    │   │   ├── module-info.java
    │   │   └── com/example/
    │   │       ├── MyStepProvider.java
    │   │       ├── MyConfigProvider.java
    │   │       └── MyMessageProvider.java
    │   └── resources/
    │       ├── config.yaml                    (configuration key definitions)
    │       ├── steps.yaml                     (canonical step doc for docgen)
    │       ├── steps_en.yaml                  (English expressions)
    │       ├── steps_es.yaml                  (Spanish expressions, optional)
    │       └── steps_dsl.yaml                 (DSL shorthand, optional)
    └── test/
        ├── java/
        │   ├── module-info.java
        │   └── com/example/test/
        │       └── TestMySteps.java
        └── resources/
            ├── get-200/
            │   └── scenario.feature
            └── post-201/
                └── scenario.feature
```
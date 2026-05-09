# OpenBBT - Open Blackbox Testing Framework

OpenBBT is a comprehensive Java-based testing framework designed to support behavior-driven development (BDD) and blackbox testing. It provides a pluggable architecture for defining, executing, and managing test plans with support for multiple test definition formats.

## Features

- **BDD Support**: Native support for Gherkin feature files through the plugin system
- **Pluggable Architecture**: Extensible through the JExten plugin framework
- **Expression Matching**: Sophisticated natural language step matching with data type support
- **Assertion DSL**: Locale-aware assertions with natural language patterns
- **Persistent Storage**: H2-backed repository for managing large test plans
- **Modular Design**: Java 11+ JPMS modules for clean separation of concerns
- **Benchmark Mode**: Run any step N times across T parallel threads and assert on min/max/mean/P95/P99/throughput/errorRate

## Project Structure

```
openbbt/
├── openbbt-core/                 # Core framework
│   └── src/main/java/
│       └── org.myjtools.openbbt.core/
│           ├── backend/          # Step execution engine
│           ├── step/             # Step annotations (@Step, @SetUp, @TearDown)
│           ├── expressions/      # Expression parsing and matching
│           ├── datatypes/        # Data type adapters
│           ├── assertions/       # Assertion framework
│           ├── testPlan/             # Plan node model
│           ├── persistence/      # Repository layer (H2)
│           └── config/           # Configuration management
│
└── gherkin-openbbt-plugin/       # Gherkin support plugin
	└── src/main/java/
		└── org.myjtools.openbbt.plugins.gherkin/
```

## Getting Started

See the **[Getting Started guide](docs/getting-started.md)** for step-by-step instructions on installation, PATH setup, plugin installation, and running a test testPlan.

## Requirements

- Java 21 or higher
- Maven 3.x

## Building

```bash
mvn install
```

## Core Concepts

### Step Definition

Define test steps using annotations:

```java
public class MySteps implements StepContributor {

	@Step("I click on the {button} button")
	public void clickButton(String button) {
		// implementation
	}

	@SetUp
	public void setup() {
		// runs before tests
	}

	@TearDown
	public void teardown() {
		// runs after tests
	}
}
```

### Data Types

Built-in support for common data types:

- `Number` - Numeric values
- `Duration` - Time durations
- `Period` - Time periods
- `Regex` - Regular expressions

### Assertions

Natural language assertions with locale support:

```properties
# assertions_en.properties
assertion.string.equals=is (equal to) _
assertion.string.contains=contains _
```

### Plan Model

Test plans are organized as a tree structure:

- **Aggregators**: Group related tests
- **Test Cases**: Individual test scenarios
- **Steps**: Executable test steps

A testPlan is identified by the combination of organization, testProject, and a testPlan ID (hash derived from configuration and resources).

## Gherkin Plugin

The Gherkin plugin enables writing tests using standard Gherkin syntax:

```gherkin
Feature: User Authentication

  Scenario: Successful login
	Given I am on the login page
	When I enter valid credentials
	Then I should see the dashboard
```

### Supported Gherkin Features

- Feature files with multiple scenarios
- Scenario Outlines with Examples
- Background scenarios
- Tags for filtering and organization

## Configuration

### Global Configuration (`global-config.yaml`)

```yaml
id-tag-pattern: "ID-(\\w+)"
definition-tag: "definition"
implementation-tag: "implementation"
```

### Project Configuration (`openbbt.yaml`)

```yaml
organization: My Organization
testProject: My Project
test-suites:
  - suite-a
  - suite-b
```

## Dependencies

| Library | Purpose |
|---------|---------|
| Caffeine | High-performance caching |
| H2 | Embedded database |
| Hamcrest | Assertion library |
| JExten | Plugin framework |
| ULID Creator | Unique identifier generation |

## License

This testProject is licensed under the MIT License - see the [LICENSE](openbbt-core/LICENSE) file for details.

## Contributing

Contributions are welcome. Please open an issue to discuss proposed changes before submitting a pull request.
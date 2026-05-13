# Gherkin Azertio Plugin

A plugin for [Azertio](https://github.com/myjtools/azertio) that enables writing test plans
using [Gherkin](https://cucumber.io/docs/gherkin/) syntax. It parses `.feature` files and
transforms them into the PlanNode tree structure that the Azertio engine uses to organise and
execute tests.

## How It Works

The plugin is discovered at runtime through the Java SPI mechanism (via the
[jexten](https://github.com/myjtools/jexten) extension framework). It implements two
extension points:

| Extension Point  | Implementation            | Role                                              |
|------------------|---------------------------|---------------------------------------------------|
| `SuiteAssembler` | `GherkinSuiteAssembler`   | Discovers `.feature` files and builds the testPlan tree |
| `ConfigProvider`  | `GherkinConfig`           | Provides default configuration from `gherkin-config.yaml` |

### Parsing Pipeline

1. **Discovery** — all `*.feature` resources in the testProject are located via `ResourceFinder`.
2. **Parsing** — each file is parsed by `gherkin-parser` into a Gherkin AST.
3. **Assembly** — `FeaturePlanAssembler` converts each AST into a `PlanNode` subtree:
   - `Feature` → `TEST_AGGREGATOR`
   - `Scenario` → `TEST_CASE`
   - `Scenario Outline` → `TEST_AGGREGATOR` (children expanded from the `Examples` table)
   - `Background` → `STEP_AGGREGATOR` (prepended to every scenario in the feature)
   - `Step` → `STEP`
4. **Redefining** (multi-feature only) — definition and implementation features are merged
   (see below).
5. **Filtering** — scenarios that do not match the suite's tag expression are excluded.

## Key Concepts

### Features, Scenarios, and Steps

Standard Gherkin elements are supported:

```gherkin
@Test1
Feature: Test 1 - Simple Scenario
  This is a simple scenario feature.

@ID-Test1_Scenario1
Scenario: Test Scenario
  Given a number with value 8.02 and another number with value 9
  When both numbers are multiplied
  Then the result is equals to 72.18
```

### Scenario Outlines

Scenario outlines are expanded into individual test cases, one per row in the `Examples`
table. Placeholders (`<a>`, `<b>`, …) in step names are substituted with the row values.

```gherkin
@ID-ScenarioOutline1
Scenario Outline: Test Scenario Outline
  Given a number with value <a> and another number with value <b>
  When both numbers are multiplied
  Then the result is equals to <c>
  Examples:
    | a   | b | c    |
    | 1.0 | 2 | 2.0  |
    | 2.0 | 3 | 6.0  |
```

### Backgrounds

A `Background` section is converted into a `STEP_AGGREGATOR` and attached as the first child
of every scenario in the feature.

### Definition / Implementation Redefining

This mechanism allows separating *what* a test does (definition) from *how* it is executed
(implementation), optionally in different languages.

- A **definition** feature (tagged `@definition`) declares abstract scenarios with
  high-level steps and identifiers (e.g. `@ID-1`).
- An **implementation** feature (tagged `@implementation`) provides concrete steps for each
  scenario, matched by the same identifier.

The `gherkin.step-map` property (set via a Gherkin comment, e.g. `# gherkin.step-map: 2-1-2-0`)
controls how implementation steps map onto definition steps. The format is a dash-separated list
of integers where each number indicates how many implementation steps replace the corresponding
definition step. A value of `0` makes the definition step virtual (no concrete execution).

**Definition feature:**

```gherkin
@definition
Feature: Calculator - Definition

@ID-1
Scenario: Multiply two numbers
  Given two numbers
  When they are multiplied
  Then the result is the product
```

**Implementation feature (Spanish):**

```gherkin
@implementation
Característica: Calculator [Spanish]

# gherkin.step-map: 2-1-2-0
@ID-1
Escenario: Multiplicar dos números
  Dado un número con valor 6,1 y otro número con valor 3
  Y un número con valor 6,1 y otro número con valor 3
  Cuando se multiplican ambos números
  Entonces el resultado es 18,3
  Y el resultado es 18,3
```

For scenario outlines, the implementation feature provides the concrete steps while the
examples table is taken from the definition feature.

## Configuration

Configuration is loaded from `gherkin-config.yaml` (bundled with the plugin) and can be
overridden by the testProject's own configuration. All keys are prefixed with `gherkin.`:

| Key                         | Type | Default       | Description                                                                                              |
|-----------------------------|------|---------------|----------------------------------------------------------------------------------------------------------|
| `gherkin.idTagPattern`      | text | `ID-(\w+)`    | Regex for tags used as identifiers. If it contains a capture group, only the group value is used.         |
| `gherkin.definitionTag`     | text | `definition`  | Tag that marks a feature as a definition.                                                                |
| `gherkin.implementationTag` | text | `implementation` | Tag that marks a feature as an implementation.                                                        |

### Properties from Comments

Gherkin comments matching the pattern `# key: value` are extracted as testPlan node properties.
This is used internally (e.g. `gherkin.step-map`) and can be leveraged for custom properties.

## Module Structure

```
src/main/java/org/azertio/plugins/gherkin/
├── GherkinConstants.java       — Property keys/values for Gherkin node types
├── GherkinConfig.java          — ConfigProvider loading gherkin-config.yaml
├── GherkinSuiteAssembler.java  — SuiteAssembler entry point (SPI)
└── FeaturePlanAssembler.java   — Converts a single Feature AST into PlanNodes
```

## Build and Test

```bash
mvn clean test
```

The test suite includes tests for:
- Simple scenarios, scenario outlines, backgrounds, and step arguments
- Single-feature and multi-feature assembly
- Definition/implementation redefining (including cross-language scenarios)
- Memory usage under load

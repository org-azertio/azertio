# Definition / Implementation — Two-Level Test Scenarios

OpenBBT supports a two-level scenario model that separates *what* a test does from *how* it does it. The **definition** layer describes the test intent in high-level, business-readable language. The **implementation** layer provides the concrete, executable steps that fulfil that intent — potentially in a different natural language, with a different level of technical detail.

This mechanism is unique to OpenBBT and has no direct equivalent in other testing frameworks.

---

## The Problem it Solves

In typical BDD projects, feature files serve two masters simultaneously:

- **Business stakeholders** need to read and validate that the tests reflect the real requirements.
- **Testers and developers** need steps that are precise enough to be executed by the framework.

These two needs often pull in opposite directions. Business-facing language is abstract and concise (`Given a registered user`), while executable steps tend to be technical and verbose (`Given I send a POST to "/users" with body {"name":"Alice","email":"alice@example.com"}`). The result is feature files that are too technical for business review, or steps so abstract they hide what actually happens — neither extreme is ideal.

OpenBBT solves this by letting you maintain both levels as separate files that are merged at plan-build time.

---

## Core Concepts

### Definition feature

A feature file tagged `@definition` declares the **abstract structure** of the test: the scenarios, their identifiers, and their high-level steps. These steps describe intent — they are not directly executable and do not need to match any step provider expression.

```gherkin
# language: en
@definition
Feature: User Registration

@ID-REG-01
Scenario: A new user can register with valid data
  Given a valid registration form
  When the form is submitted
  Then the account is created
  And a welcome email is sent

@ID-REG-02
Scenario: Registration fails with a duplicate email
  Given a registration form with an already-registered email
  When the form is submitted
  Then the account is not created
  And an error message is shown
```

This file is the contract between the business and the test team. It can be reviewed, challenged, and signed off by product owners without any technical knowledge.

> **Note:** Background steps in definition features are intentionally **ignored**. The definition is a contract, not an execution plan; its background has no meaning at the implementation level.

### Implementation feature

A feature file tagged `@implementation` provides the **concrete steps** for each scenario, matched by identifier. Implementation steps must match actual step provider expressions and will be executed by the framework.

```gherkin
# language: en
@implementation
Feature: User Registration — REST Implementation

Background:
  Given the base URL is "https://api.example.com"

@ID-REG-01
# gherkin.step-map: 1-1-1-1
Scenario: A new user can register with valid data
  When I make a POST request to "users" with body:
    """json
    { "name": "Alice", "email": "alice@example.com", "password": "s3cr3t" }
    """
  Then the HTTP status code is equal to 201
  And the response body contains:
    """json
    { "email": "alice@example.com" }
    """
  And the response body field "welcomeEmailSent" is equal to "true"

@ID-REG-02
# gherkin.step-map: 1-1-1-1
Scenario: Registration fails with a duplicate email
  When I make a POST request to "users" with body:
    """json
    { "name": "Bob", "email": "alice@example.com", "password": "s3cr3t" }
    """
  Then the HTTP status code is equal to 409
  And the response body contains:
    """json
    { "error": "EMAIL_ALREADY_EXISTS" }
    """
  And the response body field "message" contains "already registered"
```

The implementation Background *is* executed and is taken from the implementation feature.

---

## How Matching Works

Scenarios are matched between definition and implementation by their **identifier tag** — the tag extracted by the pattern configured in `core.idTagPattern` (default: `ID-(\w+)`). A definition scenario with `@ID-REG-01` is matched to the implementation scenario that also carries `@ID-REG-01`.

If a definition scenario has no identifier, it is silently dropped from the plan. If a definition scenario has an identifier but no matching implementation exists, it is also dropped (no failure — the scenario simply does not appear in the executed plan).

---

## The Step Map

The `gherkin.step-map` property controls how implementation steps are distributed across the abstract definition steps. It is written as a Gherkin comment on the implementation scenario:

```gherkin
# gherkin.step-map: 2-1-2-0
@ID-1
Scenario: ...
```

The format is a dash-separated list of integers, one per definition step, indicating how many implementation steps replace each definition step:

| Definition step | Step-map value | Result |
|---|---|---|
| `Given two numbers` | `2` | Replaced by 2 implementation steps |
| `When they are multiplied` | `1` | Replaced by 1 implementation step |
| `Then the result is correct` | `2` | Replaced by 2 implementation steps |
| `And the world is wonderful` | `0` | Becomes a **virtual step** (not executed) |

A value of `0` marks the definition step as a **virtual step**: it appears in the result tree as a structural label but runs no code. This is useful when an abstract definition step has no direct technical equivalent — for example, a step that represents a business rule that is verified indirectly by other steps.

### Default: one-to-one mapping

If `gherkin.step-map` is omitted, the framework assumes a 1-to-1 mapping: each definition step is replaced by exactly one implementation step. The total number of implementation steps must equal the number of definition steps in this case.

---

## Cross-Language Scenarios

One of the most powerful applications of this mechanism is **multilingual test suites**. The definition is written in the business language of the project (English, Spanish, etc.), while each implementation can use the natural language that best fits the team executing it — or the compact DSL for brevity.

**Definition (English — business language):**

```gherkin
# language: en
@definition
Feature: Inventory Management

@ID-INV-01
Scenario: Stock is reduced after a sale
  Given a product with 10 units in stock
  When a sale of 3 units is recorded
  Then the remaining stock is 7 units
```

**Implementation (Spanish — test team language):**

```gherkin
# language: es
@implementation
Característica: Gestión de Inventario

# gherkin.step-map: 2-1-1
@ID-INV-01
Escenario: El stock disminuye tras una venta
  Dado que uso el origen de datos "inventory"
  Y la tabla products contiene las filas:
    | sku   | stock |
    | P-001 | 10    |
  Cuando realizo una petición POST a "sales" con cuerpo:
    """json
    { "sku": "P-001", "quantity": 3 }
    """
  Entonces el número de filas de la consulta es mayor que 0
```

The final executed plan shows the **definition structure** (English, business-readable) in the result tree, while executing the **implementation steps** (Spanish, technically precise). Stakeholders inspect the definition; the framework runs the implementation.

---

## Scenario Outlines

For `Scenario Outline`, the mechanism works slightly differently:

- The **implementation** provides the concrete steps (with placeholders like `<a>`, `<b>`).
- The **Examples table** is always taken from the **definition** feature, regardless of any Examples table present in the implementation (which is ignored).

This means business stakeholders control the data sets; the test team controls the execution steps.

**Definition:**

```gherkin
@definition
Feature: Pricing Rules

@ID-PRICE-01
Scenario Outline: Discount is applied correctly
  Given a product priced at <price>
  When a discount of <discount>% is applied
  Then the final price is <expected>
  Examples:
    | price | discount | expected |
    | 100   | 10       | 90       |
    | 200   | 25       | 150      |
    | 50    | 50       | 25       |
```

**Implementation:**

```gherkin
@implementation
Feature: Pricing Rules — REST

# gherkin.step-map: 1-1-1
@ID-PRICE-01
Scenario Outline: Discount is applied correctly
  When I make a POST request to "pricing/calculate" with body:
    """json
    { "price": <price>, "discountPercent": <discount> }
    """
  Then the HTTP status code is equal to 200
  Then the response body field "finalPrice" is equal to "<expected>"
  Examples:
    | price | discount | expected |
    | 999   | 99       | 9.99     |
```

The implementation Examples table is ignored. The plan is expanded using the definition's three rows.

---

## Configuration

The tags used for definition and implementation are configurable in `openbbt.yaml`:

```yaml
configuration:
  core:
    definitionTag: definition          # default: "definition"
    implementationTag: implementation  # default: "implementation"
    idTagPattern: 'ID-(\w+)'           # default: "ID-(\\w+)"
```

If you want to use different tag names — for example `@contract` and `@concrete` — just change these values.

---

## File Layout

A typical project using this mechanism looks like:

```
tests/
├── openbbt.yaml
├── features/
│   ├── definition/
│   │   ├── user-registration.feature    (@definition)
│   │   ├── inventory.feature            (@definition)
│   │   └── pricing.feature              (@definition)
│   └── implementation/
│       ├── user-registration.feature    (@implementation)
│       ├── inventory.feature            (@implementation)
│       └── pricing.feature              (@implementation)
```

Both directories must be under the path configured in `core.resourceFilter` (default: `**/*.feature`).

---

## What the Result Tree Shows

After the plan is built, definition/implementation tags are stripped from all nodes. The result tree shows:

- The **definition scenario structure** as the top-level view.
- Under each definition step, the **implementation steps** that executed it (as children).
- Virtual steps (step-map value `0`) appear as non-executed structural labels.
- The implementation **Background** steps appear at the top of each scenario under a `<definition>` label.

This gives stakeholders a clean, business-readable view of results, while engineers can drill into each definition step to see exactly what concrete actions were taken.

---

## When to Use This

| Situation | Recommendation |
|---|---|
| Single team, single language, technical audience | Standard single-level scenarios — no need for definition/implementation |
| Business stakeholders actively review test scenarios | Use definition/implementation to keep feature files business-readable |
| Same test contract, multiple protocol implementations | One definition, multiple implementation files (e.g. REST vs gRPC) |
| Multilingual teams | Definition in the project language, implementation in the team language |
| Regulatory or contractual test traceability | Definition as the signed-off specification; implementation as the audit trail |
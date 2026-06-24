# When Cucumber Grows Too Big: Pain Points, Lessons Learned, and Alternatives

## What Is Cucumber, and Why Do Teams Love It?

If you have spent any time in behavior-driven development (BDD), you have almost certainly encountered Cucumber. First released in 2008, it has become the de facto standard for writing executable specifications in plain language.

The core idea is elegant: tests are written in **Gherkin**, a structured natural language format built around three keywords — `Given`, `When`, and `Then`. A product owner, a tester, and a developer can all read the same file and agree on what the system is supposed to do.

```gherkin
Feature: User login

  Scenario: Successful login with valid credentials
    Given the user "alice" exists with password "secret"
    When she submits the login form
    Then she is redirected to the dashboard
    And a session cookie is set
```

Under the hood, each step is matched by a regular expression or a cucumber expression to a **step definition** — a method in Java, Ruby, JavaScript, or whatever language your project uses. Cucumber finds the right method, runs it, and aggregates the results into a report.

The promise is compelling: **business-readable tests that are also executable**. The gap between what stakeholders describe and what testers automate, closed forever. In small projects and well-contained modules, it genuinely works.

---

## Where Things Start to Break Down

I spent several years working on large backend systems where Cucumber was adopted as the standard integration testing tool. Early on, things were manageable. Over time, a set of recurring problems emerged that no amount of team discipline could fully solve.

### 1. The Glue Code Explosion

Every Gherkin step needs a step definition. In a project with hundreds of scenarios covering REST APIs, databases, message queues, and background jobs, this means hundreds — sometimes thousands — of step definition methods spread across dozens of classes.

The immediate problem is **discoverability**. When a new developer writes a step, how do they know whether it already exists? The IDE can sometimes help, but step matching relies on regular expressions that are not always obvious to navigate. You end up with:

- Duplicate step definitions that do subtly different things
- Slightly different phrasing that bypasses an existing step and creates a new one
- Inconsistent abstractions because different people solved the same problem independently

The deeper problem is **coupling**. Step definitions are not unit-tested; they are integration plumbing. When a REST client is refactored, you find that fifteen step definitions directly instantiate it. When a database fixture format changes, you discover that nobody documented which step methods touch which tables.

### 2. Shared State and Context Passing

Cucumber scenarios are supposed to be independent, but step definitions need to share state: the HTTP response from the `When` step needs to be inspectable in the `Then` step. The standard solution is a **World object** (or `@ScenarioScoped` beans in Java) — a bag of shared state injected into step definition classes.

This works until you have fifteen step definition classes all mutating the same World, nobody owns the contract for what each field means, and a flaky test appears because some scenario left dirty state that wasn't cleaned up. Debugging it means reading glue code, not feature files — which defeats half the purpose of BDD.

### 3. The Feature File Drift Problem

In a healthy BDD process, feature files are living documents co-authored by business and technical people. In practice, after the initial sprint, product owners stop reading them. They become developer-only artifacts, written with the same mindset as JUnit tests: exhaustive, technical, and opaque to anyone outside the team.

You end up with scenarios like:

```gherkin
Given the user entity with id 42 exists in schema "core" table "users" with status "ACTIVE"
When the endpoint POST /api/v2/auth/session is called with payload from fixture "auth_fixtures/alice_valid.json"
Then the response body path "$.data.token" matches regex "[A-Za-z0-9-_]{40}"
```

This is not BDD. It is JUnit with extra ceremony.

### 4. Step Definition Scope Creep

In Cucumber, step definitions are global. There is no namespacing, no module boundary, no way to say "these steps belong to the payments domain." As the test suite grows, you inherit every step ever written, and step expressions start colliding.

Teams work around this with naming conventions, careful phrasing, and tribal knowledge. That is technical debt in disguise.

### 5. Maintenance Cost Compounds Over Time

Every refactor of production code ripples through glue code. A renamed endpoint, a changed response schema, a migrated database table — each one can silently break dozens of step definitions, or worse, fail to break them because a step is now asserting against stale data that happens to still match.

The test suite that was supposed to give you confidence becomes a maintenance burden that slows releases down. At some point the question stops being "how do we fix the tests?" and becomes "is this the right tool for this job?"

---

## The Specific Pain That Made Me Reconsider

The breaking point for me was the combination of two things happening simultaneously.

First, **onboarding friction**. A new team member joining the project needed days to understand the glue code before they could write a single new test. The feature files were not self-explanatory; they were a surface sitting on top of an iceberg of implementation. That is the opposite of what BDD promises.

Second, **the semantic gap for API testing**. Our integration tests were almost entirely black-box: send an HTTP request, assert on the response, check the database state. For this use case, Cucumber adds a translation layer — Gherkin step → step definition → HTTP client call — that provides no value. The "business readable" framing makes no sense for `Then the response status is 200`. Nobody is showing those files to a product owner.

We were paying the full cost of Cucumber's glue code model while getting almost none of its BDD benefits.

---

## Alternatives Worth Considering

Depending on what is actually causing your pain, different tools address different problems.

**[Karate](https://karatelabs.github.io/karate/)** is the closest direct alternative for API testing. It uses a Gherkin-like syntax but eliminates step definitions entirely — steps are interpreted directly by the framework. You get zero glue code for REST and GraphQL testing, plus built-in mocking and performance testing. If your Cucumber usage is primarily API testing, Karate is worth a serious look.

**[REST-assured](https://rest-assured.io/)** (with JUnit or TestNG) takes the opposite position: abandon the DSL entirely and write your tests as code. You lose the business-readable layer, but you gain the full power of a proper programming language — real abstractions, composable helpers, IDE support, type safety. For teams that have already given up on non-developer readers, this is often the pragmatic choice.

**[Playwright](https://playwright.dev/) / [Cypress](https://www.cypress.io/)** are not Cucumber replacements, but if your integration tests are UI-heavy, their built-in test organization and recording capabilities may do more for you than Cucumber's BDD layer.

**[SpecFlow](https://specflow.org/)** (for .NET) and **[Behave](https://behave.readthedocs.io/)** (for Python) are Cucumber-family tools that sometimes have better ecosystem integration for their respective stacks, though they share the same architectural tradeoffs.

The rule of thumb I arrived at: **Cucumber earns its keep when the scenario files are genuinely read and validated by non-developers on a regular basis.** If that is not happening — and it often is not — you are paying glue-code tax for a benefit you are not receiving.

---

## A Tool Built From These Lessons

After working through these problems repeatedly, I designed **[Azertio](http://azertio.org)** as an attempt to take what Cucumber gets right (human-readable Gherkin, structured scenario files) and eliminate what causes the most pain in large projects.

The central bet: for black-box testing of REST APIs and databases, **there should be no glue code at all**. Steps are provided by plugins loaded at runtime — `rest`, `db`, and others — and every step in those plugins is immediately available in any feature file without any wiring. You declare which plugins you use in a YAML config file and write tests immediately.

```gherkin
Scenario: Creating an order reduces stock
  Given db table stock has:
    | sku   | units |
    | P-001 | 10    |
  When I make a POST request to "orders" with body:
    """json
    { "sku": "P-001", "quantity": 3 }
    """
  Then the HTTP status code is equal to 201
  And db table stock row where sku = "P-001" has units = 7
```

No step definitions. No World objects. No regex to maintain.

It also directly addresses the definition/implementation split that Cucumber struggles with: you can write a **definition** feature (business-readable, owned by the product team) and a separate **implementation** feature (technical, owned by testers), linked by a tag. The execution report shows the business structure; the implementation details stay out of the way.

The project is open source, still early, and genuinely shaped by the frustrations described in this article. If any of this resonates with your own Cucumber experience, I would be glad to hear your feedback at [azertio.org](http://azertio.org).

---

*Have you hit any of these Cucumber pain points in your own projects? Which solutions worked for you? Let me know in the comments.*
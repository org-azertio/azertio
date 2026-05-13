# Azertio — Copilot Instructions

## Project Identity
- **Org**: `org.azertio` | **Version**: `1.0.0-alpha1` | **License**: MIT
- **Stack**: Java 21 + Maven 3.x | JPMS modules throughout | Parent POM: `myjtools-parent:1.4.0`

## Branch Strategy
- `main` (stable) ← `develop` (integration) ← `feature/*` / `fix/*`
- **PRs target `develop`, NOT `main`**

## Build Commands
```bash
mvn verify                   # full build + tests
mvn verify -P plugins        # include plugin modules
mvn -B verify                # CI mode
```

## Module Map
| Module | Role |
|--------|------|
| `azertio-core` | BDD runtime, step execution, expression matching, assertions |
| `azertio-persistence` | jOOQ + Flyway + HSQLDB/PostgreSQL |
| `azertio-cli` | PicoCLI — subcommands: browse, init, install, version, purge, plan, serve, show-config, tui, lsp |
| `azertio-lsp` | Eclipse LSP4J language server |
| `azertio-tui` | Lanterna terminal UI |
| `azertio-vscode` | TypeScript VS Code extension (npm, separate from Maven) |
| `azertio-plugin-starter` | Template POM for plugin development |
| `azertio-it` | Integration tests (TestContainers) |
| `azertio-docgen-maven-plugin` | Doc generation from code |
| `plugins/gherkin-azertio-plugin` | Gherkin feature file support (`-P plugins`) |
| `plugins/rest-azertio-plugin` | REST-assured HTTP testing steps |
| `plugins/markdown-plan-azertio-plugin` | Markdown-based test plan definitions |

## Architecture Patterns
- **Plugins**: JExten + JPMS `ModuleLayerProvider`
- **SPI**: `SuiteAssembler`, `StepProvider`, `ConfigProvider` via `provides ... with ...` in `module-info.java`
- **Model**: `TestPlanNode` tree — PLAN > SUITE > CASE > STEP
- **Caching**: hash-based plan cache (resource + config hashes match → reuse)
- **Lazy init**: `Lazy<T>` wrapper with `Lazy.of(supplier)`
- **Extension points**: interfaces annotated `@ExtensionPoint`

## Persistence Modes (`core.persistence.mode`)
- `transient` — temp HSQLDB (default dev)
- `file` — persistent HSQLDB
- `remote` — PostgreSQL

## Code Style

### Formatting
- 4-space indent, no tabs. K&R braces (opening brace on same line).
- Explicit imports only, no wildcards. Order: `java.*` → external libs → project-internal.

### Java 21 Features
- Sealed classes for closed hierarchies.
- Pattern matching with switch + record deconstruction — used heavily.
- `Optional<T>` over null for all optional return values.
- `var` conservatively (obvious types only). No text blocks.

### Naming
| Element | Convention | Example |
|---------|-----------|---------|
| Classes | PascalCase + role suffix | `InstallCommand`, `JooqPlanRepository` |
| Methods | camelCase, verb-first | `getNodeChildren()`, `buildTestPlan()` |
| Static factories | `of()` | `Log.of()`, `Lazy.of(supplier)` |
| Constants | UPPER_SNAKE_CASE | `TABLE_PLAN_NODE` |

### Logging
```java
private static final Log log = Log.of();          // NOT LoggerFactory directly
private static final Log log = Log.of("rest");    // subcategory
log.warn("No factory found for {}", type.getSimpleName());
```

### Exceptions
```java
throw new AzertioException("Node {} not found in plan {}", nodeId, planId);
```
- Prefer unchecked exceptions.
- `@Serial private static final long serialVersionUID = 1L;` in serializable exceptions.

### Lombok — conservative
- Allowed: `@Getter`, `@Setter`, `@NoArgsConstructor`, `@EqualsAndHashCode`, `@ToString`
- **Never** `@Data`
- `requires static lombok;` in `module-info.java`

### Patterns
```java
// Fluent builder (setters return this)
new TestPlanNode().nodeType(NodeType.TEST_PLAN).name("x").addTags(Set.of("smoke"));

// Static factory
public static <T> Lazy<T> of(Supplier<T> supplier) { return new Lazy<>(supplier); }

// Template method hook
protected abstract void fillSuppliers();
```

### JPMS Module Template
```java
module org.azertio.mymodule {
    requires org.azertio.core;
    requires org.myjtools.jexten;
    requires static lombok;
    exports org.azertio.mymodule;
    opens org.azertio.mymodule to org.myjtools.jexten;
    uses SomeExtensionPoint;
    provides SomeExtensionPoint with MyImpl;
}
```

### Package Structure
```
org.azertio.<module>
├── (root)         — public API
├── .contributors  — SPI interfaces / extension points
├── .backend       — engine internals
├── .util          — Log, Lazy, Pair, Either…
└── .test          — test utilities (test source tree)
```

### Tests
- Class suffix `Test` (not `Tests`). Methods: descriptive, no `test` prefix.
- **AssertJ only** — never raw JUnit asserts (`assertThat(x).isEqualTo(y)`).
- Fluent builders for test fixtures, not constructor overloads.
- `@BeforeEach` / `@AfterEach` for setup/teardown.

### Javadoc
- Public API: full Javadoc with `@param`, `@return`, `@throws`.
- Internal/private: minimal or none — code structure explains intent.
- Inline comments: rare, explain *why* not *what*.

## Key Data Notes
- `number` type was renamed to `integer` (commit b92f695); integer assertions use `{{value}}` (double braces).
- Full style reference: `memory/codestyle.md`
- Full architecture reference: `memory/MEMORY.md`

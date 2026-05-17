# Azertio — VS Code Extension

Browse test plans, inspect executions, and run open black-box tests directly from the editor.

> **Requires** the [Azertio CLI](https://github.com/org-azertio/azertio) (`azertio`) on your PATH and Java 21+.

---

## Features

### Test Plan view
Browse the test plan defined in `azertio.yaml` — suites, scenarios, and tags — as a tree. Run the full plan or a specific suite with one click.

### Executions view
Inspect past execution results: pass/fail status per scenario, execution timestamps, and attached artifacts. Re-run any past execution or generate HTML/PDF reports directly from the view.

### Contributors view
See which plugins are currently installed and active in the workspace.

### Help view
Browse step documentation and configuration reference for each installed plugin, without leaving the editor.

### Gherkin language support
Syntax highlighting for `.feature` files, including embedded JSON, XML, YAML, SQL, and more.

### YAML diagnostics
Real-time validation of `azertio.yaml` via the Azertio Language Server — invalid config values are reported as diagnostics inline.

### AI step completions *(experimental)*
Inline step suggestions in `.feature` files powered by any language model available in VS Code (e.g. GitHub Copilot).

---

## Requirements

- [Azertio CLI](https://github.com/org-azertio/azertio) installed and available on `PATH`
- Java 21 or higher

---

## Extension Settings

| Setting | Default | Description |
|---|---|---|
| `azertio.executablePath` | `azertio` | Path to the Azertio CLI executable. |
| `azertio.ai.enabled` | `false` | Enable AI-powered inline step completions in `.feature` files. |
| `azertio.ai.model` | *(empty)* | Language model family to use (e.g. `gpt-4o`). Leave empty to use the first available model. |

---

## Getting Started

1. Install the Azertio CLI and add it to your PATH.
2. Open a folder containing an `azertio.yaml` file — the extension activates automatically.
3. Run `Azertio: Install plugins` from the Command Palette to download the configured plugins.
4. Use the **Test Plan** view to run your first test suite.

See the [full documentation](https://github.com/org-azertio/azertio) for configuration options, available plugins, and examples.

---

## License

[MIT](LICENSE)
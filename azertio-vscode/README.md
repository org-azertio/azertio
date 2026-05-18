# Azertio — VS Code Extension

Browse test plans, inspect executions, run open black-box tests, and write feature files faster with AI — all without leaving the editor.

> **Requires** the [Azertio CLI](https://github.com/org-azertio/azertio) (`azertio`) on your PATH and Java 21+.

---

## AI Features

The extension integrates with GitHub Copilot, Claude, GPT-4o, and any other language model available in VS Code to accelerate feature authoring.

### Inline step completions
As you type a step line in a `.feature` file, the extension suggests the next step drawn from the **actual step index** of your project — not generic templates. Enable it in Settings with `azertio.ai.enabled`.

### Generate feature from description
Run **Azertio: Generate Feature with AI** (or click the CodeLens that appears at the top of an empty `.feature` file) and describe what you want to test in plain language. Azertio generates a complete, ready-to-run Gherkin feature file — including correct structural keywords for your chosen locale.

### Generate feature from OpenAPI / Swagger
Run **Azertio: Generate Feature from Swagger**, enter an OpenAPI/Swagger URL, and Azertio reads the endpoints and generates a full feature file covering the API surface automatically.

> Works with GitHub Copilot, Claude, GPT-4o, and any language model registered in VS Code.

---

## Features

### Test Plan view
Browse the test plan defined in `azertio.yaml` — suites, scenarios, and tags — as a tree. Run the full plan or a specific suite with one click.

### Executions view
Inspect past execution results: pass/fail status per scenario, execution timestamps, and attached artifacts. Re-run any past execution or generate HTML/PDF reports directly from the view.

### Contributors view
See which plugins are currently installed and active in the workspace.

### Help view
Browse step documentation and configuration reference for each installed plugin, without leaving the editor. Includes an in-editor User Guide.

### Gherkin language support
Syntax highlighting for `.feature` files, including embedded JSON, XML, YAML, SQL, and more.

### YAML diagnostics
Real-time validation of `azertio.yaml` via the Azertio Language Server — invalid config values are reported as diagnostics inline.

---

## Requirements

- [Azertio CLI](https://github.com/org-azertio/azertio) installed and available on `PATH`
- Java 21 or higher
- For AI features: GitHub Copilot or another VS Code language model provider

---

## Extension Settings

| Setting | Default | Description |
|---|---|---|
| `azertio.executablePath` | `azertio` | Path to the Azertio CLI executable. |
| `azertio.ai.enabled` | `false` | Enable AI-powered inline step completions in `.feature` files. |
| `azertio.ai.model` | *(empty)* | Language model family to use (e.g. `gpt-4o`, `claude-sonnet-4-5`). Leave empty to use the first available model. |

---

## Getting Started

1. Install the Azertio CLI and add it to your PATH.
2. Open a folder containing an `azertio.yaml` file — the extension activates automatically.
3. Run `Azertio: Install plugins` from the Command Palette to download the configured plugins.
4. Use the **Test Plan** view to run your first test suite.
5. To try AI features, install GitHub Copilot (or another LM provider) and enable `azertio.ai.enabled` in Settings.

See the [full documentation](https://github.com/org-azertio/azertio) for configuration options, available plugins, and examples.

---

## License

[MIT](LICENSE)
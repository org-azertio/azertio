# Azertio — User Guide

## Requirements

- **Azertio CLI** installed and available on `PATH` (or set `azertio.executablePath` in settings).
- **Java 21** or higher.
- An `azertio.yaml` file in the root of your workspace — the extension activates automatically when it finds one.

---

## Getting started

1. Install the Azertio CLI and add it to your `PATH`.
2. Open a folder containing an `azertio.yaml` file.
3. Run **Azertio: Install plugins** from the Command Palette (`Ctrl+Shift+P`) to download the plugins declared in `azertio.yaml`.
4. Open the Azertio sidebar (activity bar icon) to see the Test Plan, Executions, Contributors and Help views.

---

## Views

### Test Plan
Displays the suites and scenarios defined in `azertio.yaml` as a tree.

- Click **Run** (▶▶) in the view toolbar to execute the full plan.
- Right-click a suite to run only that suite.
- Use the **Refresh** button after changing `azertio.yaml`.

### Executions
Shows the history of past executions with pass/fail status per scenario.

- Click an execution to expand its scenarios.
- Use **Re-run** to repeat a past execution.
- Use **Generate Reports** to produce HTML/PDF artifacts.
- Use **Delete** to remove individual executions or empty plans.

### Contributors
Lists the plugins currently installed and active in the workspace (populated when `azertio serve` is running).

### Help
Displays step documentation and configuration reference for each installed plugin.

- Click any entry to open its documentation in Markdown preview.
- Use the **Refresh** button to reload after installing new plugins.

---

## Gherkin language support

The extension provides:

- **Syntax highlighting** for `.feature` files, including embedded languages inside doc strings (JSON, XML, YAML, SQL, TypeScript, …).
- **Formatter** — the file is automatically formatted on save.
- **Real-time diagnostics** — invalid `azertio.yaml` values are underlined and reported in the Problems panel.

---

## AI features

AI features require:
- **GitHub Copilot Chat** (or another VS Code language model provider) installed and signed in.
- The setting `azertio.ai.enabled` set to `true`, or toggled via **Azertio: Toggle AI Completions**.

### Inline step completions

While editing a `.feature` file, place the cursor at the end of a step line (starting with `Given`, `When`, `Then`, `And`, `But`, or `*`) and wait ~1 second. A grey ghost-text suggestion will appear; press `Tab` to accept it or `Escape` to dismiss.

The completion uses the project's available steps (fetched from `azertio serve`) as context, so suggestions are specific to the plugins installed in your project.

### Generate Feature with AI

Opens an interactive wizard to generate a complete feature file from scratch:

1. Open (or create) an empty `.feature` file.
2. Click **✨ Generate feature with AI** in the CodeLens shown at the top of the file, or run **Azertio: Generate Feature with AI** from the Command Palette.
3. Select the target language (`en`, `es`, `dsl`, …).
4. Describe what the feature should test (e.g. *"query pets by species"*).
5. The model generates a complete `Feature` with 2–3 `Scenario` blocks using only the steps available in your project.

### Generate Feature from Swagger

Generates a feature file from an OpenAPI/Swagger JSON specification:

1. Open (or create) an empty `.feature` file.
2. Click **☁ Generate feature from Swagger** in the CodeLens, or run **Azertio: Generate Feature from Swagger** from the Command Palette.
3. Enter the URL or local path to the OpenAPI JSON spec (e.g. `http://localhost:8080/v3/api-docs`).
4. Select the target language.
5. The model generates scenarios covering the discovered API endpoints, using the project's available steps.

---

## Settings

| Setting | Default | Description |
|---|---|---|
| `azertio.executablePath` | `azertio` | Path to the Azertio CLI executable. |
| `azertio.ai.enabled` | `false` | Enable AI-powered inline step completions and generation commands. |
| `azertio.ai.model` | *(empty)* | Language model family to use (e.g. `gpt-4o`, `claude-sonnet-4.6`). Leave empty to use the first available model. |

---

## Commands

| Command | Description |
|---|---|
| `Azertio: Install plugins` | Download and install the plugins declared in `azertio.yaml`. |
| `Azertio: Toggle AI Completions` | Enable or disable AI inline completions. |
| `Azertio: Generate Feature with AI` | Generate a feature file from a description. |
| `Azertio: Generate Feature from Swagger` | Generate a feature file from an OpenAPI spec. |
| `Azertio: Show Logs` | Open the Azertio output channel. |
| `Azertio: Refresh` | Refresh the Test Plan view. |
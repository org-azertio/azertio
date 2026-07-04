# Changelog

## [1.2.0] - 2026-07-04

### Added
- **Live execution follow** — the execution detail view now auto-expands and auto-scrolls to the currently running node as the test plan executes, instead of requiring manual clicks to track progress. Scrolling is "sticky": it stops following as soon as you scroll away manually, and resumes once you scroll back near the bottom.

### Fixed
- **Executions view stuck empty** — projects without an `organization` field in `azertio.yaml` showed no plans/executions under the project node, and the extension kept polling the server every second for up to 5 minutes per execution. The placeholder label used for display (`Unknown Organization`) was incorrectly also sent as the server query filter, which never matched.

## [1.1.0] - 2026-05-18

### Added
- **AI inline step completions** — suggests Gherkin step completions as you type in `.feature` files, powered by any language model available in VS Code (e.g. GitHub Copilot Chat). Enable with the `azertio.ai.enabled` setting or the *Azertio: Toggle AI Completions* command.
- **Generate Feature with AI** — generates a complete Gherkin feature file from a plain-text description of what the feature should test. Accessible from the Command Palette or the CodeLens shown on empty `.feature` files.
- **Generate Feature from Swagger** — generates a Gherkin feature file from an OpenAPI/Swagger JSON spec (URL or local path). Groups endpoints into scenarios using the project's available steps.
- **Multi-language generation** — both generators ask for the target language and produce feature files with the correct Gherkin structural keywords (`Característica`, `Escenario`, `Dado que`…) and a `# language:` header when needed.
- **User Guide** — a built-in user manual is now available directly from the Help view in the Azertio sidebar.

### Changed
- The `azertio.ai.model` setting now accepts any Copilot model family name (e.g. `claude-sonnet-4.6`, `gpt-4o`). Leave empty to use the first available model.

## [1.0.0] - 2026-05-17

### Added
- Initial release.
- Test Plan view: browse suites, scenarios, and tags; run the full plan or a specific suite.
- Executions view: inspect past results, re-run executions, generate HTML/PDF reports.
- Contributors view: see installed and active plugins.
- Help view: browse step documentation and configuration reference for each plugin.
- Gherkin syntax highlighting for `.feature` files with embedded language support (JSON, XML, YAML, SQL, …).
- Real-time `azertio.yaml` validation via the Azertio Language Server.
- Gherkin formatter on save.
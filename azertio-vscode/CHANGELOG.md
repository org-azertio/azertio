# Changelog

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
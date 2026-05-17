# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0] - 2026-05-17

### Added
- `init` CLI command to initialize the local environment (idempotent)
- `report` CLI command to generate reports for a given execution
- Colorized CLI output with execution summary
- `core.timeZone` configuration property
- Configuration validation before executing a test plan
- HelpProvider SPI for plugins to expose step and config documentation
- VS Code help pages integration via HelpProvider SPI
- VS Code: Refresh button in Executions view
- VS Code: Generate Reports inline button in Executions view
- H2 AUTO_SERVER persistence backend replacing HSQLDB, enabling multi-process DB visibility
- SonarCloud integration with coverage reporting
- Petclinic example project

### Fixed
- LSP: stale configuration no longer persists across serve mode restarts
- LSP: config values are validated and reported as diagnostics
- VS Code: Contributors view now populates on startup without manual refresh
- VS Code: reuse a single LSP output channel instead of creating one per restart
- Plan deduplication stabilized by fixing hash source consistency
- XXE protection across XML content processing
- Plugins are installed at serve startup so all extensions appear in Contributors view
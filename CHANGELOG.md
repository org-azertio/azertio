# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.2.0] - 2026-06-24

### Added
- `man` CLI command to browse available steps and configuration topics from the terminal (`azertio man [TOPIC] [--json]`)
- `wait.seconds` built-in step: pause execution for a given number of seconds (EN: `I wait N second(s)`, ES: `espero N segundo(s)`, DSL: `wait Ns`)

### Fixed
- Plugin `init()` now runs with the plugin classloader as Thread Context ClassLoader, fixing resource-loading failures in plugins that rely on TCCL during initialization

### Dependencies
- `logback-classic` 1.5.25 → 1.5.34
- `postgresql` 42.7.7 → 42.7.11
- `gson` 2.11.0 → 2.14.0
- `okio-jvm` 3.6.0 → 3.17.0
- `flyway-database-postgresql` bump
- `jackson-annotations` bump
- `caffeine` bump
- `maven-plugin-api` 3.9.9 → 3.9.16
- `central-publishing-maven-plugin` 0.6.0 → 0.11.0


## [1.1.0] - 2026-06-19

### Added
- Declared step outputs are printed at the end of `exec` command
- `since` version field in step definitions for documentation
- Static plugin documentation pages published at predictable URLs
- Configuration properties can be used as interpolatable variables in step expressions


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
# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0] - 2026-05-17

### Added
- Table-based steps for setting request headers
- Table-based steps for asserting response headers
- Authorization steps: Basic, Bearer token, API Key (header and query param), OAuth2
- Persistent headers support across multiple requests
- Response header extraction into variables
- Step and config help provider for VS Code integration

### Fixed
- Bearer token literals now quoted to match text type pattern
- NPE when using a variable in Bearer token step
- Typo in RestStepProvider causing import failure
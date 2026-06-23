# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.1] - 2026-06-23

### Fixed
- Feature files with duplicate comment-based metadata keys (e.g. two `# Target:` lines in the same file)
  no longer cause a parse error. The last value wins when keys are repeated.

## [1.0.0] - 2026-05-17

First release.
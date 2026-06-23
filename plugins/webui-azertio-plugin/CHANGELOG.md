# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [1.0.0] - TBD

### Added
- `webui.go` step: navigate to a URL (absolute or relative to `webui.baseURL`)
- `webui.click` step: click an element by Playwright locator
- `webui.fill` step: type text into an input field
- `webui.select` step: select an option from a dropdown
- `webui.check` step: check a checkbox or radio button
- `webui.uncheck` step: uncheck a checkbox
- `webui.assert.visible` step: assert an element is visible
- `webui.assert.hidden` step: assert an element is absent or hidden
- `webui.assert.text` step: assert an element's inner text against a condition
- `webui.assert.url` step: assert the current page URL against a condition
- `webui.assert.title` step: assert the current page title against a condition
- `webui.extract.text` step: store an element's inner text into a scenario variable
- Configuration: `webui.browser`, `webui.headless`, `webui.baseURL`, `webui.timeout`
- English and Spanish step expressions
- Browser lifecycle managed automatically: browser opens on init, closes on teardown
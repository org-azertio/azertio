# Web UI Azertio Plugin

A plugin for [Azertio](https://azertio.org) that provides browser automation steps
powered by [Playwright](https://playwright.dev/java/). It lets you write end-to-end
web UI tests using Gherkin scenarios with no boilerplate.

## Steps

### Navigation

| Step key | DSL expression |
|---|---|
| `webui.go` | `navigate to {url}` |

### Interactions

| Step key | DSL expression |
|---|---|
| `webui.click` | `click {locator}` |
| `webui.fill` | `fill {locator} with {value}` |
| `webui.select` | `select {value} in {locator}` |
| `webui.check` | `check {locator}` |
| `webui.uncheck` | `uncheck {locator}` |

### Assertions

| Step key | DSL expression |
|---|---|
| `webui.assert.visible` | `assert {locator} is visible` |
| `webui.assert.hidden` | `assert {locator} is hidden` |
| `webui.assert.text` | `assert {locator} text <assertion>` |
| `webui.assert.url` | `assert page URL <assertion>` |
| `webui.assert.title` | `assert page title <assertion>` |

### Extraction

| Step key | DSL expression |
|---|---|
| `webui.extract.text` | `var {variable} = text of {locator}` |

Locators follow [Playwright's locator syntax](https://playwright.dev/java/docs/locators):
CSS selectors, `role=`, `text=`, `data-testid=`, etc.

### Example scenario

```gherkin
Scenario: Login and reach the dashboard
  * navigate to "/login"
  * fill "#username" with "alice"
  * fill "#password" with "s3cr3t"
  * click "button[type=submit]"
  * assert page URL contains "/dashboard"
  * assert "h1" text = "Dashboard"
  * assert ".welcome-message" is visible
```

## Configuration

| Key | Type | Default | Description |
|---|---|---|---|
| `webui.browser` | text | `chrome` | Browser to use: `chrome`, `firefox`, `edge` |
| `webui.headless` | boolean | `true` | Run without a visible window |
| `webui.baseURL` | text | — | Base URL prepended to relative navigation paths |
| `webui.timeout` | integer | `10000` | Default timeout in milliseconds |

## Build

```bash
mvn clean test
```
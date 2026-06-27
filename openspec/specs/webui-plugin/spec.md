# Web UI Plugin

Provides steps to drive a real browser for black-box UI testing.
Backed by Selenium WebDriver 4.x. A single browser instance is created per scenario
execution and closed at teardown via `@TearDown`.

> **Locator format**: All `locator` parameters are **CSS selectors** (e.g. `#submit`,
> `.btn-primary`, `input[name="email"]`). The step YAML descriptions mention "Playwright
> locator expression" but the engine resolves locators exclusively via `By.cssSelector()`.
> XPath, role-based, and text-based locators are not supported in the current implementation.

---

## Navigation

### Requirement: Navigate to a URL
The plugin SHALL navigate the browser to the given URL. If the URL is relative (no `://`),
it SHALL be prefixed with `webui.baseURL` before navigation.

#### Scenario: Absolute URL navigated directly
- **WHEN** `webui.go` is called with `"https://example.com/login"`
- **THEN** the browser navigates to `https://example.com/login` regardless of `webui.baseURL`

#### Scenario: Relative URL resolved against baseURL
- **WHEN** `webui.baseURL` is `"https://app.example.com"` and `webui.go` is called with `"/login"`
- **THEN** the browser navigates to `"https://app.example.com/login"`

#### Scenario: Variable interpolation in URL
- **WHEN** a variable `page` holds `"dashboard"` and `webui.go` is called with `"/${page}"`
- **THEN** the browser navigates to the resolved URL

---

## Interactions

### Requirement: Click an element
The plugin SHALL locate the element by CSS selector and click it.

#### Scenario: Button clicked by CSS selector
- **WHEN** `webui.click` is called with `"#submit-btn"`
- **THEN** the element with id `submit-btn` is clicked

#### Scenario: Element not found fails the step
- **WHEN** `webui.click` is called with a selector that matches no element
- **THEN** the step fails with a descriptive error

### Requirement: Fill a text input
The plugin SHALL clear the input field identified by the CSS selector and type the given value into it.

#### Scenario: Input cleared and filled
- **WHEN** `webui.fill` is called with selector `"input[name='email']"` and value `"user@example.com"`
- **THEN** the field is cleared and then populated with `"user@example.com"`

#### Scenario: Variable interpolated in value
- **WHEN** a variable `email` holds `"test@example.com"` and fill value is `"${email}"`
- **THEN** the field is filled with `"test@example.com"`

### Requirement: Select a dropdown option
The plugin SHALL select the option in a `<select>` element whose **visible text label** matches the given value.

> **Note**: The YAML documentation states the match can be by label, value attribute, or index.
> The implementation uses `Select.selectByVisibleText()` only — value attribute and index matching
> are not supported.

#### Scenario: Option selected by visible text
- **WHEN** `webui.select` is called with value `"Spain"` and the dropdown contains `<option>Spain</option>`
- **THEN** the `Spain` option is selected

#### Scenario: Non-existent option text fails
- **WHEN** the given text does not match any option's visible label
- **THEN** the step fails

### Requirement: Check a checkbox or radio button
The plugin SHALL check the element if it is not already checked. If it is already checked, no action is taken.

#### Scenario: Unchecked checkbox becomes checked
- **WHEN** `webui.check` is called on an unchecked checkbox
- **THEN** the checkbox is checked

#### Scenario: Already-checked checkbox unchanged
- **WHEN** `webui.check` is called on an already-checked checkbox
- **THEN** no action is taken and no error is raised

### Requirement: Uncheck a checkbox
The plugin SHALL uncheck the element if it is currently checked. If it is already unchecked, no action is taken.

#### Scenario: Checked checkbox becomes unchecked
- **WHEN** `webui.uncheck` is called on a checked checkbox
- **THEN** the checkbox is unchecked

---

## Waits

### Requirement: Wait until element is visible
The plugin SHALL block until the element identified by the CSS selector is present in the DOM
and visible (non-zero dimensions, not `display:none`, not `visibility:hidden`, not `opacity:0`).
The step SHALL fail if the element does not become visible within `webui.timeout`.

#### Scenario: Element becomes visible within timeout
- **WHEN** `webui.wait.visible` is called and the element appears within the timeout
- **THEN** the step completes and execution continues

#### Scenario: Timeout exceeded fails the step
- **WHEN** the element never becomes visible within `webui.timeout` milliseconds
- **THEN** the step fails

### Requirement: Wait until element is hidden
The plugin SHALL block until the element identified by the CSS selector is hidden or absent from the DOM.
The step SHALL fail if the element is still visible after `webui.timeout`.

#### Scenario: Element hidden within timeout
- **WHEN** `webui.wait.hidden` is called and the element disappears within the timeout
- **THEN** the step completes and execution continues

#### Scenario: Timeout exceeded fails the step
- **WHEN** the element remains visible after `webui.timeout` milliseconds
- **THEN** the step fails

---

## Assertions

### Requirement: Assert element is visible
The plugin SHALL assert that the element identified by the CSS selector is present in the DOM
and visible. Visibility is determined by CSS computed style (display, visibility, opacity) and
element dimensions, evaluated via JavaScript.

#### Scenario: Visible element passes assertion
- **WHEN** `webui.assert.visible` is called and the element is rendered and visible
- **THEN** the assertion passes

#### Scenario: Hidden or absent element fails assertion
- **WHEN** the element is not in the DOM or is hidden
- **THEN** the assertion fails with a message identifying the locator

### Requirement: Assert element is hidden
The plugin SHALL assert that the element identified by the CSS selector is either absent from
the DOM or not visible.

#### Scenario: Hidden element passes assertion
- **WHEN** `webui.assert.hidden` is called and the element is `display:none`
- **THEN** the assertion passes

#### Scenario: Absent element passes assertion
- **WHEN** no element matches the selector
- **THEN** `webui.assert.hidden` passes (absence implies hidden)

### Requirement: Assert element text
The plugin SHALL assert that the inner text of the element identified by the CSS selector
satisfies a text condition expression (same `Assertion` type as core variable assertions,
e.g. `= "Welcome"`, `contains "Error"`, `starts with "Hello"`).

#### Scenario: Exact text match
- **WHEN** the element's inner text is `"Welcome, Ana"` and the assertion is `= "Welcome, Ana"`
- **THEN** the assertion passes

#### Scenario: Contains match
- **WHEN** the element's inner text is `"Error: invalid input"` and the assertion is `contains "invalid"`
- **THEN** the assertion passes

### Requirement: Assert current page URL
The plugin SHALL assert that the current browser URL satisfies a text condition expression.

#### Scenario: URL ends with expected path
- **WHEN** the browser is at `https://app.example.com/dashboard` and the assertion is `contains "dashboard"`
- **THEN** the assertion passes

### Requirement: Assert current page title
The plugin SHALL assert that the current browser page title satisfies a text condition expression.

#### Scenario: Title matches exactly
- **WHEN** the page title is `"Dashboard – MyApp"` and the assertion is `= "Dashboard – MyApp"`
- **THEN** the assertion passes

---

## Variable Extraction

### Requirement: Extract element text into variable
The plugin SHALL read the inner text of the element identified by the CSS selector and store it
in a named scenario variable for use in subsequent steps.

#### Scenario: Text extracted and available as variable
- **WHEN** an element with text `"Order #42"` is matched and `webui.extract.text` stores into `orderId`
- **THEN** `${orderId}` equals `"Order #42"` in subsequent steps

---

## Screenshot Capture

### Requirement: Automatic screenshot after each interaction
When `webui.screenshots` is `true`, the plugin SHALL capture a PNG screenshot after every
interaction step (`webui.go`, `webui.click`, `webui.fill`, `webui.select`, `webui.check`,
`webui.uncheck`, `webui.wait.visible`, `webui.wait.hidden`) and attach it to the execution result.
Screenshot capture SHALL be skipped in benchmark mode.

#### Scenario: Screenshot attached after click
- **WHEN** `webui.screenshots` is `true` and `webui.click` is executed
- **THEN** a PNG attachment is added to the step result

#### Scenario: No screenshot in benchmark mode
- **WHEN** benchmark mode is active and `webui.screenshots` is `true`
- **THEN** no screenshot is captured

---

## Browser Lifecycle

### Requirement: Browser closed at scenario teardown
The plugin SHALL close the browser (via `WebDriver.quit()`) at the end of every scenario,
regardless of whether the scenario passed or failed. This includes releasing the WebDriver process.

#### Scenario: Browser closed after passing scenario
- **WHEN** a scenario completes successfully
- **THEN** the browser window and WebDriver process are terminated

#### Scenario: Browser closed after failing scenario
- **WHEN** a scenario fails at any step
- **THEN** the browser window and WebDriver process are still terminated

### Requirement: Browser driver loaded from module layer
The WebDriver implementation class is resolved from the JPMS module layer at runtime.
Users add the driver JAR via `with <groupId>:<artifactId>` in the plugin declaration.
If the driver class is not found, the step fails with a descriptive error including the
`with` declaration needed.

#### Scenario: Missing driver fails with helpful message
- **WHEN** `webui.browser` is `"firefox"` but `selenium-firefox-driver` is not declared
- **THEN** the plugin init fails with an error message referencing the required `with` declaration

---

## Configuration

| Property | Type | Default | Description |
|---|---|---|---|
| `webui.browser` | text | `chrome` | Browser to use: `chrome`, `firefox`, `edge` / `msedge` |
| `webui.headless` | boolean | `true` | Run browser without a visible window |
| `webui.baseURL` | text | — | Base URL prepended to relative URLs in `webui.go` |
| `webui.timeout` | integer (ms) | `10000` | Timeout for element waits and page loads |
| `webui.screenshots` | boolean | `false` | Capture a screenshot after each interaction step |
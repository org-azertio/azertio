package org.azertio.plugins.webui;

import org.myjtools.imconfig.Config;
import org.myjtools.jexten.Extension;
import org.myjtools.jexten.Scope;
import org.azertio.core.Assertion;
import org.azertio.core.backend.ExecutionContext;
import org.azertio.core.contributors.StepExpression;
import org.azertio.core.contributors.StepProvider;
import org.azertio.core.contributors.TearDown;

@Extension(
    name = "Web UI steps provider",
    scope = Scope.TRANSIENT,
    extensionPointVersion = "1.0"
)
public class WebUiStepProvider implements StepProvider {

    private WebUiEngine engine;
    private boolean screenshots;

    @Override
    public void init(Config config) {
        String browser = config.getString("webui.browser").orElse("chrome");
        boolean headless = config.get("webui.headless", Boolean::parseBoolean).orElse(true);
        long timeout = config.getLong("webui.timeout").orElse(10000L);
        String baseUrl = config.getString("webui.baseURL").orElse("");
        this.screenshots = config.get("webui.screenshots", Boolean::parseBoolean).orElse(false);
        this.engine = new WebDriverEngine(browser, headless, timeout, baseUrl);
    }

    @TearDown
    public void close() {
        if (engine != null) {
            engine.close();
        }
    }

    // --- navigation ---

    @StepExpression(value = "webui.go", args = {"url:text"})
    public void navigateTo(String url) {
        engine.navigate(interpolate(url));
        captureScreenshot();
    }

    // --- interactions ---

    @StepExpression(value = "webui.click", args = {"locator:text"})
    public void click(String locator) {
        engine.click(interpolate(locator));
        captureScreenshot();
    }

    @StepExpression(value = "webui.fill", args = {"locator:text", "value:text"})
    public void fill(String locator, String value) {
        engine.fill(interpolate(locator), interpolate(value));
        captureScreenshot();
    }

    @StepExpression(value = "webui.select", args = {"value:text", "locator:text"})
    public void selectOption(String value, String locator) {
        engine.selectOption(interpolate(locator), interpolate(value));
        captureScreenshot();
    }

    @StepExpression(value = "webui.check", args = {"locator:text"})
    public void check(String locator) {
        engine.check(interpolate(locator));
        captureScreenshot();
    }

    @StepExpression(value = "webui.uncheck", args = {"locator:text"})
    public void uncheck(String locator) {
        engine.uncheck(interpolate(locator));
        captureScreenshot();
    }

    // --- waits ---

    @StepExpression(value = "webui.wait.visible", args = {"locator:text"})
    public void waitForVisible(String locator) {
        engine.waitForVisible(interpolate(locator));
        captureScreenshot();
    }

    @StepExpression(value = "webui.wait.hidden", args = {"locator:text"})
    public void waitForHidden(String locator) {
        engine.waitForHidden(interpolate(locator));
        captureScreenshot();
    }

    // --- assertions ---

    @StepExpression(value = "webui.assert.visible", args = {"locator:text"})
    public void assertVisible(String locator) {
        String loc = interpolate(locator);
        if (!engine.isVisible(loc)) {
            throw new AssertionError("Expected element '" + loc + "' to be visible, but it was not");
        }
    }

    @StepExpression(value = "webui.assert.hidden", args = {"locator:text"})
    public void assertHidden(String locator) {
        String loc = interpolate(locator);
        if (!engine.isHidden(loc)) {
            throw new AssertionError("Expected element '" + loc + "' to be hidden, but it was visible");
        }
    }

    @StepExpression(value = "webui.assert.text", args = {"locator:text"})
    public void assertText(String locator, Assertion assertion) {
        String actual = engine.getText(interpolate(locator));
        Assertion.assertThat(actual, assertion);
    }

    @StepExpression("webui.assert.url")
    public void assertUrl(Assertion assertion) {
        Assertion.assertThat(engine.getUrl(), assertion);
    }

    @StepExpression("webui.assert.title")
    public void assertTitle(Assertion assertion) {
        Assertion.assertThat(engine.getTitle(), assertion);
    }

    // --- extraction ---

    @StepExpression(value = "webui.extract.text", args = {"variable:id", "locator:text"})
    public void extractText(String variable, String locator) {
        String text = engine.getText(interpolate(locator));
        ExecutionContext.current().setVariable(variable, text);
    }

    private void captureScreenshot() {
        if (screenshots && !ExecutionContext.current().isBenchmarkMode()) {
            ExecutionContext.current().storeAttachment(engine.screenshot(), "image/png");
        }
    }

    protected String interpolate(String text) {
        return ExecutionContext.current().interpolateString(text);
    }

}
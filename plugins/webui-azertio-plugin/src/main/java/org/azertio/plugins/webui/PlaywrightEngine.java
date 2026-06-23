package org.azertio.plugins.webui;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.WaitForSelectorState;

public class PlaywrightEngine implements WebUiEngine {

    private final Playwright playwright;
    private final Browser browser;
    private final BrowserContext context;
    private final Page page;

    public PlaywrightEngine(String browserName, boolean headless, long timeout, String baseUrl) {
        this.playwright = Playwright.create();
        BrowserType browserType = switch (browserName.toLowerCase()) {
            case "firefox" -> playwright.firefox();
            case "edge", "msedge" -> playwright.chromium();
            default -> playwright.chromium();
        };
        var launchOptions = new BrowserType.LaunchOptions()
            .setHeadless(headless);
        if ("edge".equalsIgnoreCase(browserName) || "msedge".equalsIgnoreCase(browserName)) {
            launchOptions.setChannel("msedge");
        } else if ("chrome".equalsIgnoreCase(browserName)) {
            launchOptions.setChannel("chrome");
        }
        this.browser = browserType.launch(launchOptions);
        var contextOptions = new Browser.NewContextOptions()
            .setBaseURL(baseUrl.isBlank() ? null : baseUrl);
        this.context = browser.newContext(contextOptions);
        this.context.setDefaultTimeout(timeout);
        this.page = context.newPage();
    }

    @Override
    public void navigate(String url) {
        page.navigate(url);
    }

    @Override
    public void click(String locator) {
        page.locator(locator).click();
    }

    @Override
    public void fill(String locator, String value) {
        page.locator(locator).fill(value);
    }

    @Override
    public void selectOption(String locator, String value) {
        page.locator(locator).selectOption(value);
    }

    @Override
    public void check(String locator) {
        page.locator(locator).check();
    }

    @Override
    public void uncheck(String locator) {
        page.locator(locator).uncheck();
    }

    @Override
    public boolean isVisible(String locator) {
        return page.locator(locator).isVisible();
    }

    @Override
    public boolean isHidden(String locator) {
        return page.locator(locator).isHidden();
    }

    @Override
    public void waitForVisible(String locator) {
        page.locator(locator).waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));
    }

    @Override
    public void waitForHidden(String locator) {
        page.locator(locator).waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.HIDDEN));
    }

    @Override
    public String getText(String locator) {
        return page.locator(locator).innerText();
    }

    @Override
    public String getTitle() {
        return page.title();
    }

    @Override
    public String getUrl() {
        return page.url();
    }

    @Override
    public byte[] screenshot() {
        return page.screenshot();
    }

    @Override
    public void close() {
        context.close();
        browser.close();
        playwright.close();
    }

}
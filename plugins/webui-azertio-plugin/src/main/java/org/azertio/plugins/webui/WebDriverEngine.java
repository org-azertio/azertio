package org.azertio.plugins.webui;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.Map;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

public class WebDriverEngine implements WebUiEngine {

    static {
        // Suppress Selenium's CDP version mismatch warnings at the root handler level.
        // Logger.setLevel() alone doesn't work in jexten's JPMS module layer because the
        // CdpVersionFinder logger may be initialized before WebDriverEngine is loaded.
        Logger root = Logger.getLogger("");
        for (Handler handler : root.getHandlers()) {
            java.util.logging.Filter prev = handler.getFilter();
            handler.setFilter(record -> {
                if (record.getLevel().intValue() < Level.SEVERE.intValue()) {
                    String name = record.getLoggerName();
                    if (name != null && (
                        name.startsWith("org.openqa.selenium.devtools") ||
                        name.equals("org.openqa.selenium.chromium.ChromiumDriver")
                    )) return false;
                }
                return prev == null || prev.isLoggable(record);
            });
        }
    }

    private static final Map<String, String> DRIVER_CLASSES = Map.of(
        "chrome",  "org.openqa.selenium.chrome.ChromeDriver",
        "firefox", "org.openqa.selenium.firefox.FirefoxDriver",
        "edge",    "org.openqa.selenium.edge.EdgeDriver",
        "msedge",  "org.openqa.selenium.edge.EdgeDriver"
    );

    private static final Map<String, String> OPTIONS_CLASSES = Map.of(
        "chrome",  "org.openqa.selenium.chrome.ChromeOptions",
        "firefox", "org.openqa.selenium.firefox.FirefoxOptions",
        "edge",    "org.openqa.selenium.edge.EdgeOptions",
        "msedge",  "org.openqa.selenium.edge.EdgeOptions"
    );

    private static final Map<String, String> HEADLESS_ARGS = Map.of(
        "chrome",  "--headless=new",
        "firefox", "-headless",
        "edge",    "--headless=new",
        "msedge",  "--headless=new"
    );

    private final WebDriver driver;
    private final WebDriverWait wait;
    private final String baseUrl;

    public WebDriverEngine(String browserName, boolean headless, long timeoutMs, String baseUrl) {
        String browser = browserName.toLowerCase();
        this.driver = createDriver(browser, headless);
        this.wait = new WebDriverWait(driver, Duration.ofMillis(timeoutMs));
        this.baseUrl = (baseUrl == null) ? "" : baseUrl.stripTrailing();
    }

    private WebDriver createDriver(String browser, boolean headless) {
        String driverClassName   = DRIVER_CLASSES.getOrDefault(browser,  DRIVER_CLASSES.get("chrome"));
        String optionsClassName  = OPTIONS_CLASSES.getOrDefault(browser, OPTIONS_CLASSES.get("chrome"));
        String headlessArg       = HEADLESS_ARGS.getOrDefault(browser,   HEADLESS_ARGS.get("chrome"));

        Class<?> driverClass = findDriverClass(driverClassName);
        if (driverClass == null) {
            throw new IllegalStateException(
                "WebDriver class '" + driverClassName + "' not found. " +
                "Add 'with org.seleniumhq.selenium:selenium-" + browser + "-driver' " +
                "to the plugin declaration in azertio.yaml"
            );
        }
        return instantiateDriver(driverClass, optionsClassName, headless ? headlessArg : null);
    }

    // Scans the current module layer — finds driver classes loaded via 'with' at runtime
    private Class<?> findDriverClass(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException ignored) {}

        ModuleLayer layer = WebDriverEngine.class.getModule().getLayer();
        if (layer == null) return null;
        for (Module module : layer.modules()) {
            ClassLoader loader = module.getClassLoader();
            if (loader == null) continue;
            try {
                return loader.loadClass(className);
            } catch (ClassNotFoundException ignored) {}
        }
        return null;
    }

    private WebDriver instantiateDriver(Class<?> driverClass, String optionsClassName, String headlessArg) {
        try {
            if (headlessArg != null) {
                Class<?> optionsClass = driverClass.getClassLoader().loadClass(optionsClassName);
                Object options = optionsClass.getDeclaredConstructor().newInstance();
                Method addArguments = optionsClass.getMethod("addArguments", String[].class);
                addArguments.invoke(options, (Object) new String[]{headlessArg});
                for (Constructor<?> ctor : driverClass.getConstructors()) {
                    if (ctor.getParameterCount() == 1 && ctor.getParameterTypes()[0].isInstance(options)) {
                        return (WebDriver) ctor.newInstance(options);
                    }
                }
            }
            return (WebDriver) driverClass.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Cannot instantiate WebDriver: " + e.getMessage(), e);
        }
    }

    @Override
    public void navigate(String url) {
        String target = (!baseUrl.isEmpty() && !url.contains("://")) ? baseUrl + url : url;
        driver.get(target);
    }

    @Override
    public void click(String locator) {
        driver.findElement(By.cssSelector(locator)).click();
    }

    @Override
    public void fill(String locator, String value) {
        WebElement element = driver.findElement(By.cssSelector(locator));
        element.clear();
        element.sendKeys(value);
    }

    @Override
    public void selectOption(String locator, String value) {
        new Select(driver.findElement(By.cssSelector(locator))).selectByVisibleText(value);
    }

    @Override
    public void check(String locator) {
        WebElement element = driver.findElement(By.cssSelector(locator));
        if (!element.isSelected()) element.click();
    }

    @Override
    public void uncheck(String locator) {
        WebElement element = driver.findElement(By.cssSelector(locator));
        if (element.isSelected()) element.click();
    }

    @Override
    public boolean isVisible(String locator) {
        try {
            return isElementDisplayed(driver.findElement(By.cssSelector(locator)));
        } catch (NoSuchElementException e) {
            return false;
        }
    }

    @Override
    public boolean isHidden(String locator) {
        try {
            return !isElementDisplayed(driver.findElement(By.cssSelector(locator)));
        } catch (NoSuchElementException e) {
            return true;
        }
    }

    @Override
    public void waitForVisible(String locator) {
        wait.until(d -> {
            try {
                return isElementDisplayed(d.findElement(By.cssSelector(locator)));
            } catch (NoSuchElementException e) {
                return false;
            }
        });
    }

    @Override
    public void waitForHidden(String locator) {
        wait.until(d -> {
            try {
                return !isElementDisplayed(d.findElement(By.cssSelector(locator)));
            } catch (NoSuchElementException e) {
                return true;
            }
        });
    }

    // Replaces element.isDisplayed() which in Selenium 4.45+ loads isDisplayed.js via Read.class
    // (in selenium-api), but the JS resource lives in selenium-remote-driver — fails in JPMS layers.
    private boolean isElementDisplayed(WebElement element) {
        Object result = ((JavascriptExecutor) driver).executeScript(
            "var el = arguments[0]; if (!el) return false;" +
            "var s = window.getComputedStyle(el);" +
            "return s.display !== 'none' && s.visibility !== 'hidden' && s.opacity !== '0'" +
            " && (el.offsetWidth > 0 || el.offsetHeight > 0);",
            element
        );
        return Boolean.TRUE.equals(result);
    }

    @Override
    public String getText(String locator) {
        return driver.findElement(By.cssSelector(locator)).getText();
    }

    @Override
    public String getTitle() {
        return driver.getTitle();
    }

    @Override
    public String getUrl() {
        return driver.getCurrentUrl();
    }

    @Override
    public byte[] screenshot() {
        return ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
    }

    @Override
    public void close() {
        driver.quit();
    }
}
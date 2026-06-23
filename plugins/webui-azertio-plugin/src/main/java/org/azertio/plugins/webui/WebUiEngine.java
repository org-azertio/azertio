package org.azertio.plugins.webui;

public interface WebUiEngine {

    void navigate(String url);
    void click(String locator);
    void fill(String locator, String value);
    void selectOption(String locator, String value);
    void check(String locator);
    void uncheck(String locator);
    boolean isVisible(String locator);
    boolean isHidden(String locator);
    void waitForVisible(String locator);
    void waitForHidden(String locator);
    String getText(String locator);
    String getTitle();
    String getUrl();
    byte[] screenshot();
    void close();

}
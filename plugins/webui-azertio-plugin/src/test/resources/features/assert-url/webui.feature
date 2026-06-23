Feature: Assert page URL

  Scenario: Page URL contains expected path
    * I navigate to the web page "/basic"
    * the web page URL contains "/basic"

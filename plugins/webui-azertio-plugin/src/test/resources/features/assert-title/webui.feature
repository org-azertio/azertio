Feature: Assert page title

  Scenario: Page title matches expected value
    * I navigate to the web page "/basic"
    * the web page title is equal to "Basic Page"

Feature: Navigate to URL

  Scenario: Navigate to a page and verify URL and title
    * I navigate to the web page "/basic"
    * the web page URL contains "/basic"
    * the web page title is equal to "Basic Page"

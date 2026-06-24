Feature: Assert element is hidden

  Scenario: Hidden element passes the assertion
    * I navigate to the web page "/basic"
    * the UI element "#hidden-element" is hidden

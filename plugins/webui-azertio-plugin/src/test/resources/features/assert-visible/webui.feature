Feature: Assert element is visible

  Scenario: Visible element passes the assertion
    * I navigate to the web page "/basic"
    * the UI element "#visible-element" is visible

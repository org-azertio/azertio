Feature: Wait for element to be visible

  Scenario: Wait until a dynamically loaded element becomes visible
    * I navigate to the web page "/wait-visible"
    * I click the UI element "#load-btn"
    * I wait for the UI element "#content" to be visible
    * the UI element "#content" is visible

Feature: Wait for element to be hidden

  Scenario: Wait until a spinner disappears
    * I navigate to the web page "/wait-hidden"
    * the UI element "#spinner" is visible
    * I wait for the UI element "#spinner" to be hidden
    * the UI element "#spinner" is hidden

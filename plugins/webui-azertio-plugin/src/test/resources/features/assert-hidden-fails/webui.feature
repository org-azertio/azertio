Feature: Assert hidden fails when element is visible

  Scenario: Visible element fails the hidden assertion
    * I navigate to the web page "/basic"
    * the UI element "#visible-element" is hidden

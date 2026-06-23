Feature: Assert visible fails when element is hidden

  Scenario: Hidden element fails the visible assertion
    * I navigate to the web page "/basic"
    * the UI element "#hidden-element" is visible

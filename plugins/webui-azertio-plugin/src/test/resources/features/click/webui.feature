Feature: Click an element

  Scenario: Click a button and verify the result appears
    * I navigate to the web page "/form"
    * the UI element "#result" is hidden
    * I click the UI element "#submit-btn"
    * the UI element "#result" is visible
    * the text of the UI element "#result" is equal to "Submitted"

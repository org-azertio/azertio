Feature: Fill an input field

  Scenario: Fill an input and verify the display updates
    * I navigate to the web page "/form"
    * I fill the UI element "#name" with "Alice"
    * the text of the UI element "#name-display" is equal to "Alice"

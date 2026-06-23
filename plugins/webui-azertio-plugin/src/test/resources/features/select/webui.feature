Feature: Select a dropdown option

  Scenario: Select an option and verify the display
    * I navigate to the web page "/form"
    * I select "France" in the UI element "#country"
    * the text of the UI element "#selected" is equal to "France"

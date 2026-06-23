Feature: Extract element text into a variable

  Scenario: Store element text and use it in navigation
    * I navigate to the web page "/basic"
    * the text of the UI element "#code" is stored in the variable code
    * I navigate to the web page "/basic?id=${code}"
    * the web page URL contains "ABC-123"

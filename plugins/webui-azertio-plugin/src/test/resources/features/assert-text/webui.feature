Feature: Assert element text

  Scenario: Element text matches expected value
    * I navigate to the web page "/basic"
    * the text of the UI element "h1" is equal to "Hello World"

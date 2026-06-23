Feature: Assert element text fails on mismatch

  Scenario: Text mismatch causes a FAILED result
    * I navigate to the web page "/basic"
    * the text of the UI element "h1" is equal to "Wrong Text"

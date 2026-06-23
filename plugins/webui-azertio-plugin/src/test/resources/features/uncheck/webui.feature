Feature: Uncheck a checkbox

  Scenario: Check and then uncheck a checkbox
    * I navigate to the web page "/form"
    * I check the UI element "#agree"
    * the text of the UI element "#agree-status" is equal to "Checked"
    * I uncheck the UI element "#agree"
    * the text of the UI element "#agree-status" is equal to "Unchecked"

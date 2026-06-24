Feature: Check a checkbox

  Scenario: Check a checkbox and verify the status
    * I navigate to the web page "/form"
    * I check the UI element "#agree"
    * the text of the UI element "#agree-status" is equal to "Checked"

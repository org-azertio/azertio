# language: dsl
#
# Target: https://the-internet.herokuapp.com/dropdown
#
# Demonstrates: navigate, select, extract text, assert text
Feature: Dropdown

  Scenario: Select an option and verify it is selected
    * navigate to web page "https://the-internet.herokuapp.com/dropdown"
    * select "Option 1" in UI "#dropdown"
    * var selectedOption = text of UI "#dropdown option:checked"
    * assert UI "#dropdown" text has "Option 1"

  Scenario: Changing the selection updates the displayed option
    * navigate to web page "https://the-internet.herokuapp.com/dropdown"
    * select "Option 1" in UI "#dropdown"
    * assert UI "#dropdown" text has "Option 1"
    * select "Option 2" in UI "#dropdown"
    * assert UI "#dropdown" text has "Option 2"

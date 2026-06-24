# language: dsl
#
# Target: https://the-internet.herokuapp.com/checkboxes
# Initial state: checkbox 1 unchecked, checkbox 2 checked
#
# Demonstrates: navigate, check, uncheck, assert visible
Feature: Checkboxes

  Scenario: Check and uncheck checkboxes
    * navigate to web page "https://the-internet.herokuapp.com/checkboxes"
    * assert UI "form#checkboxes input:first-of-type" is visible
    * assert UI "form#checkboxes input:last-of-type" is visible
    * check UI "form#checkboxes input:first-of-type"
    * uncheck UI "form#checkboxes input:last-of-type"

  Scenario: All checkboxes end up checked
    * navigate to web page "https://the-internet.herokuapp.com/checkboxes"
    * check UI "form#checkboxes input:first-of-type"
    * check UI "form#checkboxes input:last-of-type"

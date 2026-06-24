# language: dsl
#
# Target: https://the-internet.herokuapp.com/dynamic_loading/1
# The result element exists in the DOM but is hidden; it becomes visible after
# clicking Start and waiting for the fake loading delay to complete.
#
# Target: https://the-internet.herokuapp.com/dynamic_loading/2
# The result element does not exist in the DOM at all; it is injected after
# the loading delay completes.
#
# Demonstrates: navigate, assert hidden, click, wait hidden, wait visible, assert text
Feature: Dynamic loading

  Scenario: Wait for a hidden element to appear (element exists in DOM)
    * navigate to web page "https://the-internet.herokuapp.com/dynamic_loading/1"
    * assert UI "#finish" is hidden
    * click UI "#start button"
    * wait for UI "#loading" to be hidden
    * assert UI "#finish" is visible
    * assert UI "#finish h4" text = "Hello World!"

  Scenario: Wait for an injected element to appear (element added to DOM)
    * navigate to web page "https://the-internet.herokuapp.com/dynamic_loading/2"
    * assert UI "#finish" is hidden
    * click UI "#start button"
    * wait for UI "#finish" to be visible
    * assert UI "#finish h4" text = "Hello World!"

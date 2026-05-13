Feature: Test - Scenario with arguments

Scenario: Test Scenario with document
  Given a number with value 8.02 and another number with value 9
  When both numbers are multiplied
  Then the matchResult is equals to:
  ```json
  {
    "matchResult": 72.18
  }
  ```

Scenario: Test Scenario with data table
    Given a number with value 8.02 and another number with value 9
    When both numbers are multiplied
    Then the matchResult is equals to:
    | name   | value |
    | matchResult | 72.18 |

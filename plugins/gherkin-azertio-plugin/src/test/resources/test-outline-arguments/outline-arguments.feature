# language: en
Feature: Scenario outline arguments

Scenario Outline: Substitute arguments outside the step text
  When I submit this payload:
    """json
    {
      "name": "<name>",
      "age": <age>
    }
    """
  Then the users table contains:
    | name   | age   |
    | <name> | <age> |

Examples:
  | name  | age |
  | Alice | 31  |
  | Bob   | 42  |

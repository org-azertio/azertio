Feature: Table is assertion fails

  Scenario: Table row count does not match
    Given I use datasource "test"
    Then the table users is exactly:
      | id | name  |
      | 1  | Alice |
      | 2  | Bob   |
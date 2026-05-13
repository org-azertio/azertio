Feature: Table is assertion passes

  Scenario: Table matches exactly the expected rows
    Given I use datasource "test"
    Then the table users is exactly:
      | id | name  |
      | 1  | Alice |
      | 2  | Bob   |
      | 3  | Carol |
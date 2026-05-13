Feature: Table contains assertion passes

  Scenario: Table contains a subset of expected rows
    Given I use datasource "test"
    Then the table users contains the rows:
      | id | name  |
      | 1  | Alice |
      | 2  | Bob   |
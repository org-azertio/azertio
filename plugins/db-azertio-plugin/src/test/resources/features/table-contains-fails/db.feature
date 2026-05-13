Feature: Table contains assertion fails

  Scenario: Table does not contain the expected row
    Given I use datasource "test"
    Then the table users contains the rows:
      | id | name   |
      | 99 | Nobody |
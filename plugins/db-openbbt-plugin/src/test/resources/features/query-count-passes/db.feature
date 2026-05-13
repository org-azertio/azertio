Feature: Execute SQL query count passes

  Scenario: Count all rows from SELECT result
    Given I use datasource "test"
    And I execute the SQL query:
      """sql
      SELECT * FROM users
      """
    Then the SQL result row count is equal to 3

  Scenario: Count filtered rows from SELECT result
    Given I use datasource "test"
    And I execute the SQL query:
      """sql
      SELECT * FROM users WHERE id = 1
      """
    Then the SQL result row count is equal to 1
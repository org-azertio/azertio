Feature: Execute SQL query count fails

  Scenario: Count does not match expected value
    Given I use datasource "test"
    And I execute the SQL query:
      """sql
      SELECT * FROM users
      """
    Then the SQL result row count is equal to 99
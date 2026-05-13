Feature: Execute SQL DML count passes

  Scenario: Affected rows count from INSERT
    Given I use datasource "test"
    And at teardown execute in datasource "test" the SQL statement:
      """sql
      DELETE FROM users WHERE id = 99
      """
    And I execute the SQL query:
      """sql
      INSERT INTO users VALUES (99, 'Temp')
      """
    Then the SQL result row count is equal to 1
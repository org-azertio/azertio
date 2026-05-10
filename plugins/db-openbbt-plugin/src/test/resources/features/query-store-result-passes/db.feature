Feature: Store SQL query result in variable passes

  Scenario: Store SELECT result and use in subsequent query via interpolation
    Given I use datasource "test"
    And I execute the SQL query:
      """sql
      SELECT name FROM users WHERE id = 1
      """
    And store db query result in variable "userName"
    And I execute the SQL query:
      """sql
      SELECT * FROM users WHERE name = '${userName}'
      """
    Then the SQL result row count is equal to 1

  Scenario: Store DML affected rows and use in subsequent query
    Given I use datasource "test"
    And at teardown execute in datasource "test" the SQL statement:
      """sql
      DELETE FROM users WHERE id = 99
      """
    And I execute the SQL query:
      """sql
      INSERT INTO users VALUES (99, 'Temp')
      """
    And store db query result in variable "affected"
    And I execute the SQL query:
      """sql
      SELECT * FROM users WHERE id = ${affected}
      """
    Then the SQL result row count is equal to 1
Feature: Teardown SQL executes after scenario

  Scenario: Insert row and register teardown to delete it
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

  Scenario: Verify teardown ran and row was deleted
    Given I use datasource "test"
    Then the count of rows of table users is equal to 3
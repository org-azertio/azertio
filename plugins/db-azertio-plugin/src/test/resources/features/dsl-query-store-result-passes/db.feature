# language: dsl
Feature: DSL store SQL query result in variable passes

  Scenario: Store SELECT result and use in subsequent query via interpolation
    * use db "test"
    * db query:
      """sql
      SELECT name FROM users WHERE id = 1
      """
    * db query result -> "userName"
    * db query:
      """sql
      SELECT * FROM users WHERE name = '${userName}'
      """
    * db query count = 1

  Scenario: Store DML affected rows and use in subsequent query
    * use db "test"
    * db teardown "test" query:
      """sql
      DELETE FROM users WHERE id = 99
      """
    * db query:
      """sql
      INSERT INTO users VALUES (99, 'Temp')
      """
    * db query result -> "affected"
    * db query:
      """sql
      SELECT * FROM users WHERE id = ${affected}
      """
    * db query count = 1
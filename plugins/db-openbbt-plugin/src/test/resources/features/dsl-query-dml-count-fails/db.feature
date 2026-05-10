# language: dsl
Feature: DSL execute SQL DML count fails

  Scenario: Affected rows count does not match expected value
    * use db "test"
    * db teardown "test" query:
      """sql
      DELETE FROM users WHERE id = 99
      """
    * db query:
      """sql
      INSERT INTO users VALUES (99, 'Temp')
      """
    * db query count = 99
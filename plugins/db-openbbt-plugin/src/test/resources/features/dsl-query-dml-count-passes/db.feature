# language: dsl
Feature: DSL execute SQL DML count passes

  Scenario: Affected rows count from INSERT
    * use db "test"
    * db teardown "test" query:
      """sql
      DELETE FROM users WHERE id = 99
      """
    * db query:
      """sql
      INSERT INTO users VALUES (99, 'Temp')
      """
    * db query count = 1
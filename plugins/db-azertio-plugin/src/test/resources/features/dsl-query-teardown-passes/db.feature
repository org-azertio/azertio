# language: dsl
Feature: DSL teardown SQL executes after scenario

  Scenario: Insert row and register teardown to delete it
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

  Scenario: Verify teardown ran and row was deleted
    * use db "test"
    * count db table users = 3
# language: dsl
Feature: DSL execute SQL query count fails

  Scenario: Count does not match expected value
    * use db "test"
    * db query:
      """sql
      SELECT * FROM users
      """
    * db query count = 99
# language: dsl
Feature: DSL execute SQL query count passes

  Scenario: Count all rows from SELECT result
    * use db "test"
    * db query:
      """sql
      SELECT * FROM users
      """
    * db query count = 3

  Scenario: Count filtered rows from SELECT result
    * use db "test"
    * db query:
      """sql
      SELECT * FROM users WHERE id = 1
      """
    * db query count = 1
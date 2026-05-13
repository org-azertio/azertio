# language: dsl
Feature: DSL table is assertion passes

  Scenario: Table matches exactly the expected rows
    * use db "test"
    * db table users is:
      | id | name  |
      | 1  | Alice |
      | 2  | Bob   |
      | 3  | Carol |
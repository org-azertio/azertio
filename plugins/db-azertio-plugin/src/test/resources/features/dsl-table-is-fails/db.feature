# language: dsl
Feature: DSL table is assertion fails

  Scenario: Table row count does not match
    * use db "test"
    * db table users is:
      | id | name  |
      | 1  | Alice |
      | 2  | Bob   |
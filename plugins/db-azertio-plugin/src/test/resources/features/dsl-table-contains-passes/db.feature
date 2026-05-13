# language: dsl
Feature: DSL table contains assertion passes

  Scenario: Table contains a subset of expected rows
    * use db "test"
    * db table users has:
      | id | name  |
      | 1  | Alice |
      | 2  | Bob   |
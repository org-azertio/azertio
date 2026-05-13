# language: dsl
Feature: DSL table contains assertion fails

  Scenario: Table does not contain the expected row
    * use db "test"
    * db table users has:
      | id | name   |
      | 99 | Nobody |
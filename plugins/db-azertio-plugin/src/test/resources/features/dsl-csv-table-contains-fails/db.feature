# language: dsl
Feature: DSL CSV table contains assertion fails

  Scenario: Table does not contain the expected row from CSV file
    * use db "test"
    * db table users has CSV "users.csv"
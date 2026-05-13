# language: dsl
Feature: DSL CSV table is assertion fails

  Scenario: Table row count does not match CSV file
    * use db "test"
    * db table users is CSV "users.csv"
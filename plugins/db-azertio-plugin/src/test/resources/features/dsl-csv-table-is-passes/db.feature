# language: dsl
Feature: DSL CSV table is assertion passes

  Scenario: Table matches exactly the rows from CSV file
    * use db "test"
    * db table users is CSV "users.csv"
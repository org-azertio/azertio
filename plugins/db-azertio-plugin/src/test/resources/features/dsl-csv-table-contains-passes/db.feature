# language: dsl
Feature: DSL CSV table contains assertion passes

  Scenario: Table contains a subset of rows from CSV file
    * use db "test"
    * db table users has CSV "users.csv"
# language: dsl
Feature: DSL XLS contains assertion passes

  Scenario: Database contains the rows from Excel file
    * use db "test"
    * db has XLS "data.xlsx"
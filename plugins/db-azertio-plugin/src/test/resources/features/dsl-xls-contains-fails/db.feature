# language: dsl
Feature: DSL XLS contains assertion fails

  Scenario: Database does not contain the expected row from Excel file
    * use db "test"
    * db has XLS "data.xlsx"
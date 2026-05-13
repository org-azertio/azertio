Feature: XLS contains assertion passes

  Scenario: Database contains the rows from Excel file
    Given I use datasource "test"
    Then the database contains the rows from Excel file "data.xlsx"
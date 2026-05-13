Feature: XLS contains assertion fails

  Scenario: Database does not contain the expected row from Excel file
    Given I use datasource "test"
    Then the database contains the rows from Excel file "data.xlsx"
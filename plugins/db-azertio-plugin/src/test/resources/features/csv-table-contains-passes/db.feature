Feature: CSV table contains assertion passes

  Scenario: Table contains a subset of rows from CSV file
    Given I use datasource "test"
    Then the table users contains the CSV file "users.csv"
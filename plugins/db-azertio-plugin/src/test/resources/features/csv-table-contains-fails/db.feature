Feature: CSV table contains assertion fails

  Scenario: Table does not contain the expected row from CSV file
    Given I use datasource "test"
    Then the table users contains the CSV file "users.csv"
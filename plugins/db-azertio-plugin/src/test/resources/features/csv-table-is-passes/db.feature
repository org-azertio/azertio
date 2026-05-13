Feature: CSV table is assertion passes

  Scenario: Table matches exactly the rows from CSV file
    Given I use datasource "test"
    Then the table users is exactly the CSV file "users.csv"
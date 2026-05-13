Feature: CSV table is assertion fails

  Scenario: Table row count does not match CSV file
    Given I use datasource "test"
    Then the table users is exactly the CSV file "users.csv"
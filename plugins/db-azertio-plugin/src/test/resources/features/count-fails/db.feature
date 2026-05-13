Feature: Count assertion fails

  Scenario: Row count does not match expected value
    Given I use datasource "test"
    Then the count of rows of table users is equal to 99
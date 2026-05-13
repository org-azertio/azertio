Feature: Count assertion passes

  Scenario: Row count matches expected value
    Given I use datasource "test"
    Then the count of rows of table users is equal to 3
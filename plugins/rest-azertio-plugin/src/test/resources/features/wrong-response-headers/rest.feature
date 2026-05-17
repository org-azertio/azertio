Feature: Wrong response headers fail

  Scenario: Missing expected header causes failure
    When I make a GET request to "/users"
    Then the response headers include:
      | X-Non-Existent | any-value |
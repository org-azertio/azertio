Feature: Wrong response body fails

  Scenario: Body mismatch causes FAILED result
    When I make a GET request to "/users/1"
    Then the response body contains:
      """json
      {"name":"Bob"}
      """

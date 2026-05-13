Feature: GET request passes

  Scenario: GET request returns expected status code
    When I make a GET request to "/users"
    Then the HTTP status code is equal to 200

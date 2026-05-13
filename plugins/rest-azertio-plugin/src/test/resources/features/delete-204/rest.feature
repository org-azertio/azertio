Feature: DELETE request passes

  Scenario: DELETE request returns expected status code
    When I make a DELETE request to "/users/1"
    Then the HTTP status code is equal to 204

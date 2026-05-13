Feature: POST request passes

  Scenario: POST request creates resource and returns expected status and body
    When I make a POST request to "/users" with body:
      """json
      {"name":"Alice"}
      """
    Then the HTTP status code is equal to 201
    And the response body contains:
      """json
      {"name":"Alice"}
      """

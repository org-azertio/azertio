Feature: Extract field from response

  Scenario: Extract a field from JSON response and use it in a subsequent request
    When I make a GET request to "/users/1"
    Then the HTTP status code is equal to 200
    And I store the value of field "name" from the response body into variable userName
    When I make a GET request to "/users?name=${userName}"
    Then the HTTP status code is equal to 200
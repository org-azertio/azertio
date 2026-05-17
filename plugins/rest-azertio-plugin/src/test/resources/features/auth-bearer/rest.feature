Feature: Bearer token authorization

  Scenario: Bearer token is sent with all subsequent requests
    Given the authorization is Bearer test-token
    When I make a GET request to "/bearer-protected"
    Then the HTTP status code is equal to 200
    When I make a GET request to "/bearer-protected"
    Then the HTTP status code is equal to 200
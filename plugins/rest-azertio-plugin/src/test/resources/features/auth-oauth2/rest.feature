Feature: OAuth2 client credentials authorization

  Scenario: Obtain token and use it to access a protected resource
    Given I obtain an OAuth2 client credentials token from "/oauth/token" with client "my-client" and secret "my-secret" into variable accessToken
    And the authorization is Bearer "${accessToken}"
    When I make a GET request to "/oauth-protected"
    Then the HTTP status code is equal to 200
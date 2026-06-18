Feature: OAuth2 password grant (ROPC) authorization

  Scenario: Obtain user token and use it to access a user-protected resource
    Given I obtain an OAuth2 password grant token from "${rest.baseURL}/oauth/token" with client "my-client" and secret "my-secret" as user "admin" with password "admin123" into variable accessToken
    And the authorization is Bearer "${accessToken}"
    When I make a GET request to "/user-protected"
    Then the HTTP status code is equal to 200
Feature: Basic authorization

  Scenario: Basic credentials are sent with all subsequent requests
    Given the authorization is Basic with username "user" and password "pass"
    When I make a GET request to "/basic-protected"
    Then the HTTP status code is equal to 200
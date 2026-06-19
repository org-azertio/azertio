Feature: Assert response cookies

  Scenario: Response sets expected cookies
    When I make a POST request to "/cookie-login"
    Then the HTTP status code is equal to 200
    And the response sets the cookies:
      | session_id | abc123 |
      | theme      | dark   |
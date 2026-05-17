Feature: API key authorization via query parameter

  Scenario: API key is appended to every subsequent request URL
    Given the API key "secret-key" is sent as query parameter "api_key"
    When I make a GET request to "/apikey-query-protected"
    Then the HTTP status code is equal to 200
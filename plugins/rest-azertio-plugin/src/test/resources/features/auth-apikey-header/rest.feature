Feature: API key authorization via header

  Scenario: API key header is sent with all subsequent requests
    Given the API key "secret-key" is sent in header "X-API-Key"
    When I make a GET request to "/apikey-header-protected"
    Then the HTTP status code is equal to 200
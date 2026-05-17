Feature: Extract response header into variable

  Scenario: Response header value is stored and used in a subsequent request
    When I make a GET request to "/headers-test"
    Then I store the value of response header "X-Custom-Header" into variable customHeader
    And the HTTP status code is equal to 200
    When I make a GET request to "/echo-header"
    Then the HTTP status code is equal to 200
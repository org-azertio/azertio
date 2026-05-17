Feature: Response headers can be verified

  Scenario: Expected headers are present in the response
    When I make a GET request to "/headers-test"
    Then the HTTP status code is equal to 200
    And the response headers include:
      | Content-Type    | application/json |
      | X-Custom-Header | my-value         |
Feature: Request headers are sent with the next request only

  Scenario: Custom headers are forwarded to the server
    Given the next request has the headers:
      | Authorization | Bearer secret |
      | X-Tenant-Id   | acme          |
    When I make a GET request to "/secure"
    Then the HTTP status code is equal to 200

  Scenario: Headers from a previous step are not forwarded to subsequent requests
    Given the next request has the headers:
      | Authorization | Bearer secret |
      | X-Tenant-Id   | acme          |
    When I make a GET request to "/secure"
    Then the HTTP status code is equal to 200
    When I make a GET request to "/users"
    Then the HTTP status code is equal to 200
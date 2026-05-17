Feature: Persistent headers applied to all requests

  Scenario: Persistent headers are sent with every subsequent request
    Given the following headers are set for all subsequent requests:
      | X-Tenant-Id | acme |
      | X-Version   | 2    |
    When I make a GET request to "/persistent-headers-test"
    Then the HTTP status code is equal to 200
    When I make a GET request to "/persistent-headers-test"
    Then the HTTP status code is equal to 200
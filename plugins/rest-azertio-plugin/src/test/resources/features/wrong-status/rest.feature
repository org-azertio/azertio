Feature: Wrong status code fails

  Scenario: Status code mismatch causes FAILED result
    When I make a GET request to "/missing"
    Then the HTTP status code is equal to 200

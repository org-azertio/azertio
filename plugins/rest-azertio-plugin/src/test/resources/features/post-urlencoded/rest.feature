Feature: POST with URL-encoded form body

  Scenario: Submit a form with URL-encoded fields
    When I make a POST request to "/form-login" with URL-encoded form:
      | username | alice  |
      | password | s3cr3t |
    Then the HTTP status code is equal to 200
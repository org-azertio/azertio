Feature: POST with multipart/form-data body

  Scenario: Submit a form with multipart fields
    When I make a POST request to "/form-upload" with multipart form:
      | name  | Alice             |
      | email | alice@example.com |
    Then the HTTP status code is equal to 201
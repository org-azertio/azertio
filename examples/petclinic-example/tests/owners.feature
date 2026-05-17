Feature: Owners
  REST and database tests for the /owners endpoint.
  Seed data: 10 owners pre-loaded by Spring PetClinic.

  @GET
  Scenario: List all owners
    When I make a GET request to "owners"
    Then the HTTP status code is equal to 200
    And the response body contains:
      """json
      [{"firstName": "George", "lastName": "Franklin", "id": 1}]
      """

  @smoke @GET
  Scenario: Get owner by ID
    When I make a GET request to "owners/1"
    Then the HTTP status code is equal to 200
    And the response body contains:
      """json
      {
        "firstName": "George",
        "lastName": "Franklin",
        "address": "110 W. Liberty St.",
        "city": "Madison",
        "id": 1
      }
      """

  @smoke @GET
  Scenario: Get owner with a pet
    When I make a GET request to "owners/1"
    Then the HTTP status code is equal to 200
    And the response body contains:
      """json
      {
        "pets": [{"name": "Leo", "type": {"name": "cat"}}]
      }
      """

  @smoke @GET
  Scenario: Get non-existent owner returns 404
    When I make a GET request to "owners/9999"
    Then the HTTP status code is equal to 404

  @smoke @GET
  Scenario: Search owners by last name
    When I make a GET request to "owners?lastName=Franklin"
    Then the HTTP status code is equal to 200
    And the response body contains:
      """json
      [{"lastName": "Franklin"}]
      """

  @crud @POST @DB
  Scenario: Create a new owner and verify in the database
    Given I use datasource "petclinic"
    When I make a POST request to "owners" with body:
      """json
      {
        "firstName": "Alice",
        "lastName": "Smith",
        "address": "456 Oak Ave",
        "city": "Portland",
        "telephone": "5035551234"
      }
      """
    Then the HTTP status code is equal to 201
    And the response body contains:
      """json
      {"firstName": "Alice", "lastName": "Smith", "pets": []}
      """
    And I store the value of field 'id' from the response body into variable newOwnerId
    And I execute the SQL query:
      """sql
      SELECT id, first_name, last_name, city FROM owners WHERE id = ${newOwnerId}
      """
    And the SQL result row count is equal to 1

  @crud @PUT @DB
  Scenario: Update an owner's address and verify in the database
    Given I use datasource "petclinic"
    When I make a PUT request to "owners/6" with body:
      """json
      {
        "firstName": "Jean",
        "lastName": "Coleman",
        "address": "999 New Address Blvd",
        "city": "Monona",
        "telephone": "6085552654"
      }
      """
    Then the HTTP status code is equal to 204
    And I execute the SQL query:
      """sql
      SELECT address FROM owners WHERE id = 6 AND address = '999 New Address Blvd'
      """
    And the SQL result row count is equal to 1
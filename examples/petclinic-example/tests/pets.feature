Feature: Pets
  REST and database tests for the /pets and /owners/{id}/pets endpoints.
  Seed data: 13 pets pre-loaded by Spring PetClinic.

  @GET
  Scenario: List all pets
    When I make a GET request to "pets"
    Then the HTTP status code is equal to 200
    And the response body contains:
      """json
      [{"name": "Leo", "type": {"name": "cat"}, "ownerId": 1}]
      """

  @GET
  Scenario: Get a single pet
    When I make a GET request to "pets/1"
    Then the HTTP status code is equal to 200
    And the response body contains:
      """json
      {
        "name": "Leo",
        "birthDate": "2000-09-07",
        "type": {"name": "cat"},
        "ownerId": 1,
        "id": 1
      }
      """

  @GET
  Scenario: Get all pets for an owner via the owner detail
    When I make a GET request to "owners/3"
    Then the HTTP status code is equal to 200
    And the response body contains:
      """json
      {
        "pets": [{"name": "Rosy"}, {"name": "Jewel"}]
      }
      """

  @crud @POST @DB
  Scenario: Add a pet to an owner and verify in the database
    Given I use datasource "petclinic"
    When I make a POST request to "owners/2/pets" with body:
      """json
      {
        "name": "Peanut",
        "birthDate": "2023-06-01",
        "type": {"id": 6, "name": "hamster"}
      }
      """
    Then the HTTP status code is equal to 201
    And the response body contains:
      """json
      {"name": "Peanut", "ownerId": 2}
      """
    And I store the value of field 'id' from the response body into variable newPetId
    And I execute the SQL query:
      """sql
      SELECT p.name, p.owner_id, t.name AS type_name
      FROM pets p
      JOIN types t ON t.id = p.type_id
      WHERE p.id = ${newPetId}
      """
    And the SQL result row count is equal to 1

  @DB
  Scenario: There are at least 13 pets (seed data)
    Given I use datasource "petclinic"
    Then the count of rows of table pets is greater than 12

  @DB
  Scenario: Every pet has a valid type
    Given I use datasource "petclinic"
    When I execute the SQL query:
      """sql
      SELECT p.id FROM pets p
      LEFT JOIN types t ON t.id = p.type_id
      WHERE t.id IS NULL
      """
    Then the SQL result row count is equal to 0

  @DB
  Scenario: Every pet belongs to an existing owner
    Given I use datasource "petclinic"
    When I execute the SQL query:
      """sql
      SELECT p.id FROM pets p
      LEFT JOIN owners o ON o.id = p.owner_id
      WHERE o.id IS NULL
      """
    Then the SQL result row count is equal to 0
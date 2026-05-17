Feature: Veterinarians
  REST and database tests for the /vets endpoint.
  Seed data: 6 vets and 3 specialties pre-loaded by Spring PetClinic.

  Background:
    Given I use datasource "petclinic"

  @smoke @GET
  Scenario: List all vets
    When I make a GET request to "vets"
    Then the HTTP status code is equal to 200
    And the response body contains:
      """json
      [
        {"firstName": "James", "lastName": "Carter", "specialties": []},
        {"firstName": "Helen", "lastName": "Leary"}
      ]
      """

  @smoke @GET
  Scenario: Get a single vet
    When I make a GET request to "vets/2"
    Then the HTTP status code is equal to 200
    And the response body contains:
      """json
      {
        "firstName": "Helen",
        "lastName": "Leary",
        "specialties": [{"name": "radio"}],
        "id": 2
      }
      """

  @DB
  Scenario: There are exactly 6 vets in the database
    Then the count of rows of table vets is equal to 6

  @DB
  Scenario: There are exactly 3 specialties in the database
    Then the count of rows of table specialties is equal to 3

  @DB
  Scenario: All specialties are assigned to at least one vet
    When I execute the SQL query:
      """sql
      SELECT s.name
      FROM specialties s
      LEFT JOIN vet_specialties vs ON vs.specialty_id = s.id
      WHERE vs.vet_id IS NULL
      """
    Then the SQL result row count is equal to 0

  @DB
  Scenario: Helen Leary specialises in radiology
    When I execute the SQL query:
      """sql
      SELECT v.first_name, v.last_name, s.name AS specialty
      FROM vets v
      JOIN vet_specialties vs ON vs.vet_id = v.id
      JOIN specialties s ON s.id = vs.specialty_id
      WHERE v.last_name = 'Leary' AND s.name = 'radiology'
      """
    Then the SQL result row count is equal to 1

  @DB
  Scenario: Linda Douglas has two specialties
    When I execute the SQL query:
      """sql
      SELECT vs.specialty_id FROM vet_specialties vs
      JOIN vets v ON v.id = vs.vet_id
      WHERE v.last_name = 'Douglas'
      """
    Then the SQL result row count is equal to 2
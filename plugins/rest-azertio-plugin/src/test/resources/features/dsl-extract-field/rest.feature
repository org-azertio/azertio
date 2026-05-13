# language: dsl
Feature: DSL - Extract field from response

  Scenario: Extract a field from JSON response and use it in a subsequent request
    * do HTTP GET "users/1"
    * assert HTTP status code = 200
    * var userName = 'name' from HTTP response
    * do HTTP GET "users?name=${userName}"
    * assert HTTP status code = 200
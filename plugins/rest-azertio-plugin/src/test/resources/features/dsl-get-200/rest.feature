# language: dsl
Feature: DSL - GET request passes

  Scenario: GET request returns 200
    * do HTTP GET "/users"
    * assert HTTP status code = 200

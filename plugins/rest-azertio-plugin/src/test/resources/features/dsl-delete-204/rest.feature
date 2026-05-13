# language: dsl
Feature: DSL - DELETE request passes

  Scenario: DELETE request returns 204
    * do HTTP DELETE "/users/1"
    * assert HTTP status code = 204

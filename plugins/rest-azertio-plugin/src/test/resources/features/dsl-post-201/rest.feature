# language: dsl
Feature: DSL - POST request passes

  Scenario: POST request creates resource and returns 201 with expected body
    * do HTTP POST "/users":
    """json
    {"name":"Alice"}
    """
    * assert HTTP status code = 201
    * assert HTTP response body (loose):
    """json
    {"name":"Alice"}
    """

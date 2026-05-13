# language: dsl
Feature: DSL count assertion fails

  Scenario: Row count does not match expected value
    * use db "test"
    * count db table users = 99
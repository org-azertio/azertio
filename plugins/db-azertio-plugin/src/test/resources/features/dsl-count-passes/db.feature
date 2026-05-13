# language: dsl
Feature: DSL count assertion passes

  Scenario: Row count matches expected value
    * use db "test"
    * count db table users = 3
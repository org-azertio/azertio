# language: en
@definition
Feature: Test Redefined feature - Definition

Background:
  * Since this is a definition, background steps are ignored

@ID-1
Scenario: Test scenario
 Given two numbers
 When they are multiplied
 Then the matchResult is the product
 And the world is wonderful

@ID-2
Scenario Outline: Test scenario outline
 Given a number with value <a>
 And another number with value <b>
 When they are multiplied
 Then the matchResult is <c>
 Examples:
 | a   | b | c    |
 | 1.0 | 2 | 2.0  |
 | 2.0 | 4 | 8.0  |
 | 3.0 | 6 | 18.0 |

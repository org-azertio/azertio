# This is a comment on the feature
# featureProperty: A
@Test1 @A
Feature: Test 1 - Simple Scenario
  This is a simple scenario feature without additional behaviour.
  And this is the second line of comments.

Background:
  Given the set of real numbers

# This is a comment on the scenario
# scenarioProperty: B
@ID-Test1_Scenario1 @ScenarioA
Scenario: Test Scenario
  # This is a comment on the step
  # stepProperty: C
  Given a number with value 8.02 and another number with value 9
  When both numbers are multiplied
  Then the matchResult is equals to 72.18

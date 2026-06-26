Feature: JMS Queues
  Publish/subscribe scenarios on JMS queues and field extraction into variables.
  Unlike topics, queues retain messages until consumed — subscribe before each test
  to establish a consumer anchor and avoid picking up stale messages.

  Background:
    Given I use the messaging system "activemq"

  @queue
  Scenario: Publish to a queue and assert received
    Given I subscribe to destination "queue://tasks"
    When I publish to destination "queue://tasks" the message:
      """json
      {"taskId": 1, "action": "process", "priority": "high"}
      """
    Then destination "queue://tasks" should have received a message containing:
      """json
      {"action": "process", "priority": "high"}
      """

  @queue
  Scenario: Extract a JSON field from a received message into a variable
    Given I subscribe to destination "queue://results"
    When I publish to destination "queue://results" the message:
      """json
      {"correlationId": "abc-123", "status": "ok"}
      """
    Then I extract field "correlationId" from destination "queue://results" into correlationId

  @queue
  Scenario: Plain-text message assertion
    Given I subscribe to destination "queue://alerts"
    When I publish to destination "queue://alerts" the message:
      """
      CRITICAL: service response time exceeded threshold
      """
    Then destination "queue://alerts" should have received a message containing:
      """
      CRITICAL: service response time exceeded threshold
      """
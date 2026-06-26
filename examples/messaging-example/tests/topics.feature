Feature: JMS Topics
  Publish/subscribe scenarios on JMS topics.
  For non-durable subscriptions, only messages published AFTER subscribing are visible —
  which naturally isolates each scenario from messages left by previous runs.

  Background:
    Given I use the messaging system "activemq"

  @topic
  Scenario: Assert received message contains expected JSON fields
    Given I subscribe to destination "topic://orders"
    When I publish to destination "topic://orders" the message:
      """json
      {"orderId": 42, "status": "placed", "customer": "alice"}
      """
    Then destination "topic://orders" should have received a message containing:
      """json
      {"orderId": 42, "status": "placed"}
      """

  @topic
  Scenario: Assert received message matches exactly
    Given I subscribe to destination "topic://notifications"
    When I publish to destination "topic://notifications" the message:
      """json
      {"event": "user.signup", "username": "bob"}
      """
    Then destination "topic://notifications" should have received exactly:
      """json
      {"event": "user.signup", "username": "bob"}
      """

  @topic
  Scenario: Publish a message with a routing key
    Given I subscribe to destination "topic://payments"
    When I publish to destination "topic://payments" with key "pay-001" the message:
      """json
      {"amount": 99.99, "currency": "EUR"}
      """
    Then destination "topic://payments" should have received a message containing:
      """json
      {"amount": 99.99}
      """

  @topic
  Scenario: Variable interpolation in the published message body
    Given I subscribe to destination "queue://order-ref"
    When I publish to destination "queue://order-ref" the message:
      """json
      {"orderId": "order-789"}
      """
    Then I extract field "orderId" from destination "queue://order-ref" into orderId
    And I subscribe to destination "topic://shipments"
    When I publish to destination "topic://shipments" the message:
      """json
      {"reference": "${orderId}", "status": "dispatched"}
      """
    Then destination "topic://shipments" should have received a message containing:
      """json
      {"reference": "order-789"}
      """
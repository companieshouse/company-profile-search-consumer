Feature: Company Search Consumer Error Scenarios

  Scenario: Processing a Changed message with an invalid payload
    Given the application is running
    When the consumer receives an invalid payload
    Then the message should be moved to the Invalid topic

  Scenario: Processing a Changed message when the Api returns a 400 BadRequest
    Given the application is running
    When the consumer receives a "changed" message and the Api returns a 400
    Then the message should be moved to the Invalid topic

  Scenario Outline: Processing a Changed message when there is a Retryable error
    Given the application is running
    When the consumer receives a "changed" message and the Api returns a <code>
    Then the message should retry 3 times and then error
    Examples:
      | code |
      | 404  |
      | 503  |

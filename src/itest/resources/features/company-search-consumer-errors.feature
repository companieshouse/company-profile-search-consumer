Feature: Company Search Consumer Error Scenarios

  Scenario: Processing a Changed message with an invalid payload
    Given the application is running
    When the consumer receives an invalid payload
    Then the message should be moved to the Invalid topic

  Scenario Outline: Processing a message when there is a Non-Retryable error
    Given the application is running
    When the consumer receives a <event_type> message and the Api returns a <status_code>
    Then the message should be moved to the Invalid topic
    Examples:
      | event_type | status_code |
      | "changed"  | 400         |
      | "changed"  | 409         |
      | "deleted"  | 400         |
      | "deleted"  | 409         |

  Scenario Outline: Processing a message when there is a Retryable error
    Given the application is running
    When the consumer receives a <event_type> message and the Api returns a <status_code>
    Then the message should retry 3 times and then error
    Examples:
      | event_type | status_code |
      | "changed"  | 401         |
      | "changed"  | 403         |
      | "changed"  | 404         |
      | "changed"  | 405         |
      | "changed"  | 500         |
      | "changed"  | 503         |
      | "deleted"  | 401         |
      | "deleted"  | 403         |
      | "deleted"  | 404         |
      | "deleted"  | 405         |
      | "deleted"  | 500         |
      | "deleted"  | 503         |

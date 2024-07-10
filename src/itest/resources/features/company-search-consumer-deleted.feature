Feature: Company Search Consumer Deleted Message

  Scenario: Processing a Deleted message and sending a Delete to the SearchApi
    Given the application is running
    When the consumer receives a "deleted" message and the Api returns a 200
    Then a DeleteSearchRecord request is sent to the SearchApi

#ToDo: Fix error: No records found for topic
  Scenario: Processing a Deleted message with an invalid payload
    Given the application is running
    When the consumer receives an invalid "deleted" payload
    Then the message should be moved to the Invalid topic

  Scenario: Processing a Deleted message when the Api returns a 400 BadRequest
    Given the application is running
    When the consumer receives a "deleted" message and the Api returns a 400
    Then the message should be moved to the Invalid topic

#ToDo: Fix error: No records found for topic
  Scenario: Processing a Deleted message when the user is unauthorized
    Given the application is running
    When the consumer receives a "deleted" message and the Api returns a 401
    Then the message should be moved to the Invalid topic

  Scenario Outline: Processing a Deleted message when there is a Retryable error
    Given the application is running
    When the consumer receives a "deleted" message and the Api returns a <code>
    Then the message should retry 3 times and then error
    Examples:
      | code |
      | 404  |
      | 503  |

Feature: Delete company search

  Scenario: consume DELETE request and send to search Api
    Given the application is running
    When the consumer receives a delete payload
    Then a DELETE request is sent to the search Api

  Scenario: send DELETE with invalid JSON
    Given the application is running
    When the consumer receives an invalid delete payload
    Then the message should be moved to topic stream-company-profile-company-profile-search-consumer-invalid

  Scenario: send DELETE with 400 from data api
    Given the application is running
    When the consumer receives a delete message but the api will return 400
    Then the message should be moved to topic stream-company-profile-company-profile-search-consumer-invalid

  Scenario: send DELETE when user is unauthorized
    Given the application is running
    And The user is unauthorized
    When the consumer receives a delete message but the api returns a 401
    Then the message should be moved to topic stream-company-profile-company-profile-search-consumer-invalid

  Scenario: send DELETE when the service is unavailable
    Given the application is running
    And the search API and the api.ch.gov.uk is unavailable
    When the consumer receives a delete message but the api returns a 503
    Then the message should be moved to topic stream-company-profile-company-profile-search-consumer-retry

  Scenario Outline: consume DELETE request and republish to error topic when number of retries are exceeded
    Given the application is running
    When the consumer receives a delete message but the api should return a <code>
    Then the message should retry 3 times and then error
    Examples:
      | code |
      | 404  |
      | 503  |

  Scenario: Process message which causes an error
    Given the application is running
    When the consumer receives a message that causes an error
    Then the message should retry 3 times and then error
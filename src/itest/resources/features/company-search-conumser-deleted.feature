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
    When the consumer receives a delete message but the api returns a 400
    Then the message should be moved to topic stream-company-profile-company-profile-search-consumer-invalid

  Scenario: send DELETE with invalid JSON
    Given the application is running
    And The user is unauthorized
    When the consumer receives a delete message but the api returns a 401
    Then the message should be moved to topic stream-company-profile-company-profile-search-consumer-invalid
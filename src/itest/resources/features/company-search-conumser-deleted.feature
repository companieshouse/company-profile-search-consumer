Feature: Delete company search

  Scenario: consume DELETE request and send to search Api
    Given the application is running
    When the consumer receives a delete payload
    Then a DELETE request is sent to the search Api
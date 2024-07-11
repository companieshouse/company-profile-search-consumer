Feature: Company Search Consumer Deleted Message

  Scenario: Processing a Deleted message and sending a Delete to the SearchApi
    Given the application is running
    When the consumer receives a "deleted" message and the Api returns a 200
    Then a DeleteSearchRecord request is sent to the SearchApi

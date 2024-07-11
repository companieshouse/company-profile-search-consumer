Feature: Company Search Consumer Changed Message

  Scenario: Processing a Changed message and sending a Put to the SearchApi
    Given the application is running
    When the consumer receives a "changed" message and the Api returns a 200
    Then a PutSearchRecord request is sent to the SearchApi

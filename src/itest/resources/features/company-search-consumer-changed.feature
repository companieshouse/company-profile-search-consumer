Feature: Company Search Consumer Changed

  Scenario: Can process a changed message and send a put
    Given the application is running
    When the consumer receives a changed message
    Then a putSearchRecord request is sent
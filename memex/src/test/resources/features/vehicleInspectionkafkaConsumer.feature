Feature: Vehicle Inspection Kafka consumer

  Scenario: Vehicle Inspection Kafka consumer listens to sent messages
    Given the vehicle inspections in range 91000-91099 do not exist:
    Given 100 vehicle inspections records starting from id 91000 are sent to kafka with capacity 60
    And I wait for 2 second
    Then verify 100 records are saved starting from id 91000 in mongo with capacity 60
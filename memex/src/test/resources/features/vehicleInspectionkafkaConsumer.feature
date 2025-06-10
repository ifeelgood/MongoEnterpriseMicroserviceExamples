Feature: Vehicle Inspection Kafka consumer

  Scenario: Vehicle Inspection Kafka consumer listens to sent messages
    Given the vehicle inspections in range 10000-10099 do not exist
    When I send 100 vehicle inspections starting with id 10000 to kafka with:
      | capacity | vehicle.make |
      | 60       | Ford         |
    Then I wait for 2 second
    And verify each vehicle inspection in range 10000-10099 should exist and contain:
      | capacity | vehicle.make |
      | 60       | Ford         |
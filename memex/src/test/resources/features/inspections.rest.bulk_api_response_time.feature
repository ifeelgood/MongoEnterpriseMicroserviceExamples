@bulk_api @vehicle_inspection @performance
Feature: Bulk Vehicle Inspection API
  This feature tests the bulk vehicle inspection API endpoint.
  It ensures that a batch of vehicle inspections can be submitted successfully,
  all required IDs are valid, and the API responds within an acceptable time frame.

  @sunny_day @fast_response
  Scenario: Successfully submit vehicle inspections in bulk within time limit
    Given I load vehicle inspections from file "test-data/vehicle-inspections.json"
    And I validate all inspection test IDs
    When I send inspections to the bulk API: "/api/inspections?updateStrategy=INSERT&futz=true"
    Then the response status should be 2xx
    And the response time should be under 4 seconds

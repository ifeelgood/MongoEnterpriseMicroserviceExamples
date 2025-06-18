@bulk_api @vehicle_inspection @performance
Feature: Bulk Vehicle Inspection API
  This feature tests the bulk vehicle inspection API endpoint.
  It ensures that a batch of vehicle inspections can be submitted successfully,
  all required IDs are valid, and the API responds within an acceptable time frame.

  @sunny_day @fast_response
  Scenario: Successfully submit vehicle inspections in bulk within time limit
    When I send a POST request to "/api/inspections?updateStrategy=INSERT&futz=true" with the payload from "test-data/vehicle-inspections.zip"
    Then the response status code should be 200
    And the response time should be under 3000 milliseconds

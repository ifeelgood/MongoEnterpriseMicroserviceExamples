package com.johnlpage.memex.cucumber.steps;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.johnlpage.memex.cucumber.service.MacrosRegister;
import com.johnlpage.memex.cucumber.service.VehicleInspectionIdRangeValidator;
import com.johnlpage.memex.model.VehicleInspection;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;


import static org.junit.jupiter.api.Assertions.*;

import static io.restassured.RestAssured.given;

public class BulkInspectionApiSteps {

    @Value("${memex.base-url}")
    private String baseUrl;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private VehicleInspectionIdRangeValidator idRangeValidator;

    @Autowired
    private MacrosRegister macroRegister;

    private String jsonString;
    private List<VehicleInspection> inspections;

    private Response response;
    private long durationMs;

    @Given("I load vehicle inspections from file {string}")
    public void loadVehicleInspectionsFromFile(String fileName) throws IOException {
        ClassPathResource resource = new ClassPathResource(fileName);
        if (!resource.exists()) {
            throw new RuntimeException("File not found: " + fileName);
        }

        try (InputStream inputStream = resource.getInputStream()) {
                jsonString = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                inspections = objectMapper.readValue(jsonString, new TypeReference<List<VehicleInspection>>() {
            });
        }
    }

    @And("I validate all inspection test IDs")
    public void validateAllInspectionTestIds() {
        inspections.forEach(inspection -> idRangeValidator.validate(inspection.getTestid()));
    }

    @When("I send inspections to the bulk API: {string}")
    public void sendInspectionsToBulkApi(String localUrl) {
        long startTime = System.nanoTime();

        String processedUrl = macroRegister.replaceMacros(localUrl);
        response = given()
                .baseUri(baseUrl)
                .contentType(ContentType.JSON)
                .body(jsonString)
                .post(processedUrl);

        long endTime = System.nanoTime();
        durationMs = (endTime - startTime) / 1_000_000;
    }

    @Then("the response status should be 2xx")
    public void theResponseStatusShouldBe2xx() {
        assertNotNull(response, "Response should not be null");
        assertTrue(response.getStatusCode() >= 200 && response.getStatusCode() < 300,
                "Expected 2xx response, but got " + response.getStatusCode());
    }

    @And("the response time should be under {int} seconds")
    public void responseTimeShouldBeUnderLimit(int maxAllowedSeconds) {
        double actualSeconds = durationMs / 1000.0;
        String formattedSeconds = String.format("%.1f", actualSeconds);

        assertTrue(actualSeconds <= maxAllowedSeconds,
                "API call took too long: " + formattedSeconds + "s (limit: " + maxAllowedSeconds + " s)");
    }
}
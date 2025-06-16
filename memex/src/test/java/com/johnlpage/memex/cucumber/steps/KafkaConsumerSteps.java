

package com.johnlpage.memex.cucumber.steps;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.johnlpage.memex.cucumber.service.VehicleInspectionIdRangeValidator;
import com.johnlpage.memex.model.VehicleInspection;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import jakarta.annotation.PostConstruct;
import org.assertj.core.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class KafkaConsumerSteps {

    @Value("${memex.base-url}")
    private String apiBaseUrl;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    RestClient.Builder restClientBuilder;

    private RestClient restClient;

    @PostConstruct
    public void init() {
        this.restClient = restClientBuilder.baseUrl(apiBaseUrl).build();
    }

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private VehicleInspectionIdRangeValidator idRangeValidator;

    @When("I send {int} vehicle inspections starting with id {long} to kafka with:")
    public void sendVehicleInspectionsToKafka(int count, long startId, String jsonTemplate) throws JsonProcessingException {
        idRangeValidator.validate(startId);
        long endIdInclusive = startId + count - 1;
        idRangeValidator.validate(endIdInclusive);

        for (int i = 0; i < count; i++) {
            long testId = startId + i;
            VehicleInspection vehicleInspection = objectMapper.readValue(jsonTemplate, VehicleInspection.class);
            vehicleInspection.setTestid(testId);

            String message = objectMapper.writeValueAsString(vehicleInspection);
            kafkaTemplate.send("test", message);
        }
    }

    @Then("verify {int} vehicle inspections starting from id {long} do exist with:")
    public void verifyVehicleInspectionsSaved(int count, long startId, String expectedJson) throws JsonProcessingException {
        long endId = startId + count - 1;
        idRangeValidator.validateRange(startId, endId);

        JsonNode expectedNode = objectMapper.readTree(expectedJson);

        List<CompletableFuture<Void>> vehicleInspectionFutures = new ArrayList<>();

        for (long i = startId; i <= endId; i++) {
            long currentId = i;
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    ResponseEntity<VehicleInspection> response = makeGetVehicleInspectionByIdRequest(currentId);
                    assertEquals(200, response.getStatusCode().value());

                    VehicleInspection inspection = response.getBody();
                    JsonNode actualJson = objectMapper.readTree(objectMapper.writeValueAsString(inspection));
                    assertJsonContains(expectedNode, actualJson);

                } catch (Exception e) {
                    throw new RuntimeException("Vehicle inspection verification failed for testid: " + currentId, e);
                }
            });

            vehicleInspectionFutures.add(future);
        }

        CompletableFuture.allOf(vehicleInspectionFutures.toArray(new CompletableFuture[0])).join();
    }


    private void assertJsonContains(JsonNode expected, JsonNode actual) {
        for (Iterator<Map.Entry<String, JsonNode>> it = expected.fields(); it.hasNext(); ) {
            Map.Entry<String, JsonNode> field = it.next();
            String fieldName = field.getKey();
            JsonNode expectedValue = field.getValue();
            JsonNode actualValue = actual.get(fieldName);

            Assertions.assertThat(actualValue)
                    .withFailMessage("Expected field '%s' to exist", fieldName)
                    .isNotNull();

            if (expectedValue.isObject()) {
                assertJsonContains(expectedValue, actualValue);
            } else {
                Assertions.assertThat(actualValue).isEqualTo(expectedValue);
            }
        }
    }

    public ResponseEntity<VehicleInspection> makeGetVehicleInspectionByIdRequest(long id) {
        return restClient.get()
                .uri(apiBaseUrl + "/api/inspections/id/" + id)
                .retrieve()
                .toEntity(VehicleInspection.class);
    }

}
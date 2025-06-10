package com.johnlpage.memex.cucumber.steps;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.johnlpage.memex.model.VehicleInspection;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.assertj.core.api.Assertions;
import org.bson.Document;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class VehicleInspectionKafkaConsumerSteps extends BaseCucumberSteps {

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @When("I send {int} vehicle inspections starting with id {long} to kafka with:")
    public void givenSendKafkaVehicleInspectionRecords(int iterationCount, long startingId, DataTable dataTable) throws JsonProcessingException {
        List<Map<String, String>> rows = dataTable.asMaps(String.class, String.class);
        Map<String, String> properties = rows.get(0);

        for (int i = 0; i < iterationCount; i++) {
            long testId = startingId + i;

            Map<String, Object> jsonMap = new HashMap<>();
            jsonMap.put("testid", testId);

            for (Map.Entry<String, String> entry : properties.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();

                Object parsedValue = value;
                if (value.matches("\\d+")) {
                    parsedValue = Long.parseLong(value);
                }

                // For nested keys, like "vehicle.make"
                String[] keys = key.split("\\.");
                Map<String, Object> nested = jsonMap;

                for (int k = 0; k < keys.length - 1; k++) {
                    String nestedKey = keys[k];
                    if (!nested.containsKey(nestedKey)) {
                        nested.put(nestedKey, new HashMap<String, Object>());
                    }
                    nested = (Map<String, Object>) nested.get(nestedKey);
                }

                nested.put(keys[keys.length - 1], parsedValue);
            }

            String message = objectMapper.writeValueAsString(jsonMap);
            kafkaTemplate.send("test", message);
        }

    }


    @Then("verify each vehicle inspection in range {long}-{long} should exist and contain:")
    public void verifyRecordsSavedInMongoWithProperties(long startingId, long endingId, DataTable dataTable) {
        if (endingId < startingId || !isIdWithinRange(startingId) || !isIdWithinRange(endingId)) {
            throw new IllegalArgumentException("Invalid range of IDs specified, startId: "+startingId +" , endId: "+endingId);
        }
        int documentsCount = (int) (endingId - startingId + 1);
        List<Map<String, String>> rows = dataTable.asMaps(String.class, String.class);
        Map<String, String> expectedProps = rows.get(0);

        Criteria rangeCriteria = Criteria.where("testid").gte(startingId).lte(endingId);
        Query query = new Query(rangeCriteria);

        List<VehicleInspection> results = mongoTemplate.find(query, VehicleInspection.class);
        Assertions.assertThat(results).hasSize(documentsCount);

        for (VehicleInspection vi : results) {
            for (Map.Entry<String, String> entry : expectedProps.entrySet()) {
                String key = entry.getKey();
                String expectedValue = entry.getValue();

                // For nested keys, like "vehicle.make"
                String[] keys = key.split("\\.");
                Object currentValue = vi;
                for (String k : keys) {
                    try {
                        assertNotNull(currentValue);
                        currentValue = new BeanWrapperImpl(currentValue).getPropertyValue(k);
                    } catch (Exception e) {
                        throw new AssertionError("Unable to get value for key: " + key, e);
                    }
                }

                Assertions.assertThat(String.valueOf(currentValue)).isEqualTo(expectedValue);
            }
        }
    }

}

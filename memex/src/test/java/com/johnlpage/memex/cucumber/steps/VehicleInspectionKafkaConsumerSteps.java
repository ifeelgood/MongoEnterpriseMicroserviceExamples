package com.johnlpage.memex.cucumber.steps;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.johnlpage.memex.model.VehicleInspection;
import io.cucumber.datatable.DataTable;
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
import java.util.Iterator;
import java.util.List;
import java.util.Map;


public class VehicleInspectionKafkaConsumerSteps {

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private VehicleInspectionIdRangeValidator idRangeValidator;

    @When("I send {int} vehicle inspections starting with id {long} to kafka with:")
    public void sendVehicleInspectionsAsJsonToKafka(int count, long startId, DataTable dataTable) throws JsonProcessingException {
        List<Map<String, String>> rows = dataTable.asMaps(String.class, String.class);
        String baseJson = rows.get(0).get("vehicleInspection");

        JsonNode baseNode = objectMapper.readTree(baseJson);

        idRangeValidator.validateId(startId);
        long endIdInclusive = startId + count - 1;
        idRangeValidator.validateId(endIdInclusive);

        for (int i = 0; i < count; i++) {
            long testId = startId + i;
            ObjectNode inspectionNode = baseNode.deepCopy();
            inspectionNode.put("testid", testId);

            String message = objectMapper.writeValueAsString(inspectionNode);
            kafkaTemplate.send("test", message);
        }
    }

    @Then("verify {int} vehicle inspections are saved starting from id {long} in mongo with:")
    public void verifySavedVehicleInspectionsMatchJson(int count, long startId, DataTable dataTable) throws JsonProcessingException {
        long endId = startId + count - 1;
        idRangeValidator.validateStartAndEndId(startId, endId);

        List<Map<String, String>> rows = dataTable.asMaps(String.class, String.class);
        String expectedJson = rows.get(0).get("vehicleInspection");

        JsonNode expectedNode = objectMapper.readTree(expectedJson);

        Query query = new Query(Criteria.where("testid").gte(startId).lte(endId));
        List<VehicleInspection> inspections = mongoTemplate.find(query, VehicleInspection.class);

        Assertions.assertThat(inspections).hasSize(count);

        for (VehicleInspection vi : inspections) {
            JsonNode actualNode = objectMapper.readTree(objectMapper.writeValueAsString(vi));

            for (Iterator<Map.Entry<String, JsonNode>> it = expectedNode.fields(); it.hasNext(); ) {
                Map.Entry<String, JsonNode> field = it.next();
                String fieldName = field.getKey();
                JsonNode expectedValue = field.getValue();
                JsonNode actualValue = actualNode.get(fieldName);

                // If nested object (like vehicle.make)
                if (expectedValue.isObject()) {
                    Iterator<Map.Entry<String, JsonNode>> nestedFields = expectedValue.fields();
                    while (nestedFields.hasNext()) {
                        Map.Entry<String, JsonNode> nested = nestedFields.next();
                        JsonNode nestedActual = actualValue.get(nested.getKey());
                        Assertions.assertThat(nestedActual).isEqualTo(nested.getValue());
                    }
                } else {
                    Assertions.assertThat(actualValue).isEqualTo(expectedValue);
                }
            }
        }
    }

}

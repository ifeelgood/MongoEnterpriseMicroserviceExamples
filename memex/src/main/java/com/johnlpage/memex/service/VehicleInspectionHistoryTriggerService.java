package com.johnlpage.memex.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.johnlpage.memex.model.VehicleInspection;
import com.johnlpage.memex.service.generic.HistoryTriggerService;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

// This is just Layering Glue, Business logic goes here not direct database access
// That goes via the Repository, you may choose to add logging here for example

@Service
public class VehicleInspectionHistoryTriggerService
    extends HistoryTriggerService<VehicleInspection> {

  public VehicleInspectionHistoryTriggerService(
      MongoTemplate mongoTemplate, ObjectMapper objectMapper) {
    super(mongoTemplate, objectMapper);
  }
}

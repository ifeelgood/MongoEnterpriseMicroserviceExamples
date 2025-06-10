package com.johnlpage.memex.cucumber.steps;

import com.johnlpage.memex.cucumber.CucumberTestsContainersConfig;
import io.cucumber.java.Before;
import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@CucumberContextConfiguration
@SpringBootTest(classes = CucumberTestsContainersConfig.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@EmbeddedKafka(partitions = 1, topics = {"test"})
@TestPropertySource(properties = {
        "memex.kafkaexmple.enabled=true"
})
public class CucumberSpringContextConfig {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Value("${memex.test.data.vehicleinspection-testid-range.start:1000}")
    private long idStart;

    @Value("${memex.test.data.vehicleinspection-testid-range.end:100000}")
    private long idEnd;

    @Before
    public void wipeTestData() {
        for (String collectionName : mongoTemplate.getCollectionNames()) {
            Query query = new Query(Criteria.where("_id").gte(idStart).lt(idEnd));
            mongoTemplate.remove(query, collectionName);
        }
    }
}

package com.johnlpage.memex.cucumber.steps;

import org.springframework.beans.factory.annotation.Value;

public class BaseCucumberSteps {

    @Value("${memex.test.data.vehicleinspection-testid-range.start:10000}")
    protected long idStart;

    @Value("${memex.test.data.vehicleinspection-testid-range.end:20000}")
    protected long idEnd;

    protected boolean isIdWithinRange(long testId) {
        return testId>=idStart && testId <=idEnd;
    }

}

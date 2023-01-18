package com.garyzhangscm.cwms.adminserver.model.tester;


import com.garyzhangscm.cwms.adminserver.model.wms.Warehouse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

public class TestScenarioSuit {

    private static final Logger logger = LoggerFactory.getLogger(TestScenarioSuit.class);

    private List<TestScenario> testScenarios;

    private TestScenario currentTestScenario;

    private TestScenarioSuitStatus status;

    private LocalDateTime startDateTime;
    private LocalDateTime endDateTime;

    private String lastErrorMessage;



    public TestScenarioSuit() {
        testScenarios = new ArrayList<>();
        status = TestScenarioSuitStatus.PENDING;
    }

    public void reset() {

        status = TestScenarioSuitStatus.PENDING;

        startDateTime = null;
        endDateTime = null;
        lastErrorMessage = "";

        testScenarios.forEach(testScenario ->  testScenario.reset());
    }
    public void registerTestScenario(TestScenario testScenario) {
        this.testScenarios.add(testScenario);
        testScenarios.sort(Comparator.comparingInt(scenario -> scenario.getSequence()));

    }
    public boolean deregisterTestScenario(TestScenario testScenario) {
        boolean removed = false;
        Iterator<TestScenario> testScenarioIterator = testScenarios.iterator();
        while(testScenarioIterator.hasNext()) {
            TestScenario existingTestScenario = testScenarioIterator.next();
            if (existingTestScenario.equals(testScenario)) {
                testScenarioIterator.remove();
                removed = true;
            }
        }
        return removed;
    }

    public void run(Warehouse warehouse) {
        status = TestScenarioSuitStatus.RUNNING;
        startDateTime = LocalDateTime.now();
        for (TestScenario testScenario : getTestScenarios()) {

            try {
                currentTestScenario = testScenario;
                if (!testScenario.execute(warehouse)) {
                    status = TestScenarioSuitStatus.FAILED;
                    lastErrorMessage = testScenario.getErrorMessage();
                    endDateTime = LocalDateTime.now();
                    logger.debug("Test scenario {} return fail", testScenario.getName());

                    return;
                }

            }
            catch(Exception ex) {
                status = TestScenarioSuitStatus.FAILED;
                testScenario.setErrorMessage(ex.getMessage());
                lastErrorMessage = ex.getMessage();
                endDateTime = LocalDateTime.now();
                logger.debug("Test scenario {} return exception {}", testScenario.getName(), ex.getMessage());
                return;

            }
        }
        currentTestScenario = null;
        if (status.equals(TestScenarioSuitStatus.RUNNING)) {
            status = TestScenarioSuitStatus.COMPLETED;

        }

        endDateTime = LocalDateTime.now();


    }

    public List<TestScenario> getTestScenarios() {
        return testScenarios;
    }

    public void setTestScenarios(List<TestScenario> testScenarios) {
        this.testScenarios = testScenarios;
    }

    public TestScenarioSuitStatus getStatus() {
        return status;
    }

    public void setStatus(TestScenarioSuitStatus status) {
        this.status = status;
    }

    public LocalDateTime getStartDateTime() {
        return startDateTime;
    }

    public void setStartDateTime(LocalDateTime startDateTime) {
        this.startDateTime = startDateTime;
    }

    public LocalDateTime getEndDateTime() {
        return endDateTime;
    }

    public void setEndDateTime(LocalDateTime endDateTime) {
        this.endDateTime = endDateTime;
    }

    public String getLastErrorMessage() {
        return lastErrorMessage;
    }

    public void setLastErrorMessage(String lastErrorMessage) {
        this.lastErrorMessage = lastErrorMessage;
    }

    public TestScenario getCurrentTestScenario() {
        return currentTestScenario;
    }

    public void setCurrentTestScenario(TestScenario currentTestScenario) {
        this.currentTestScenario = currentTestScenario;
    }
}

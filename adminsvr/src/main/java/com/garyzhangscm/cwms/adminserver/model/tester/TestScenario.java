package com.garyzhangscm.cwms.adminserver.model.tester;

import com.garyzhangscm.cwms.adminserver.model.wms.Warehouse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.util.Objects;

public abstract  class TestScenario {

    private static final Logger logger = LoggerFactory.getLogger(TestScenario.class);

    private final String defaultWarehouseName = "WMOR";


    TestScenarioType testScenarioType;

    String warehouseName;

    TestScenarioStatus status;

    private String errorMessage;

    private int sequence;




    public TestScenario(TestScenarioType testScenarioType, int sequence){
        this.testScenarioType = testScenarioType;
        this.warehouseName = defaultWarehouseName;
        status = TestScenarioStatus.PENDING;
        this.sequence = sequence;
    }

    public TestScenario(TestScenarioType testScenarioType, String warehouseName, int sequence){
        this.testScenarioType = testScenarioType;
        this.warehouseName = warehouseName;
        status = TestScenarioStatus.PENDING;
        this.sequence = sequence;
    }

    public boolean run(Warehouse warehouse) {

        logger.debug("Start to run test scenario: {} - {}", warehouse.getName(), getName());
        try{
            runTest(warehouse);
        } catch (Exception e) {
            e.printStackTrace();
            setErrorMessage(e.getMessage());
            return false;
        }

        return true;
    }

    public abstract void runTest(Warehouse warehouse);

    public boolean execute(Warehouse warehouse){
        logger.debug("Start to run test scenario: {} - {}", warehouse.getName(), getName());
        status = TestScenarioStatus.RUNNING;
        logger.debug("test scenario {} - {} / status: {}",
                warehouse.getName(), testScenarioType.name(), status);
        if (!run(warehouse)) {
            status = TestScenarioStatus.FAILED;
            return false;
        }
        else {

            status = TestScenarioStatus.COMPLETED;
            return true;
        }

    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TestScenario that = (TestScenario) o;
        return Objects.equals(testScenarioType, that.testScenarioType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(testScenarioType);
    }

    public String getName() {
        return testScenarioType.name();
    }

    public String getDescription(){return testScenarioType.getDescription();}

    public String getWarehouseName() {
        return warehouseName;
    }

    public void setWarehouseName(String warehouseName) {
        this.warehouseName = warehouseName;
    }

    public TestScenarioStatus getStatus() {
        return status;
    }

    public void setStatus(TestScenarioStatus status) {
        this.status = status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public void reset() {
         status = TestScenarioStatus.PENDING;

         errorMessage = "";
    }

    public int getSequence() {
        return sequence;
    }

    public void setSequence(int sequence) {
        this.sequence = sequence;
    }
}

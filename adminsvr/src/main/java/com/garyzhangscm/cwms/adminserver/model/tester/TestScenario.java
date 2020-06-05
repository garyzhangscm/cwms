package com.garyzhangscm.cwms.adminserver.model.tester;

import com.garyzhangscm.cwms.adminserver.model.wms.Warehouse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.util.Objects;

public abstract  class TestScenario {

    private static final Logger logger = LoggerFactory.getLogger(TestScenario.class);

    private final String defaultWarehouseName = "WMOR";

    String name;


    String warehouseName;

    TestScenarioStatus status;

    private String errorMessage;


    public TestScenario(String name){
        this.name = name;
        this.warehouseName = defaultWarehouseName;
        status = TestScenarioStatus.PENDING;
    }

    public TestScenario(String name, String warehouseName){
        this.name = name;
        this.warehouseName = warehouseName;
        status = TestScenarioStatus.PENDING;
    }

    public abstract boolean run(Warehouse warehouse);

    public boolean execute(Warehouse warehouse){
        status = TestScenarioStatus.RUNNING;
        logger.debug("test scenario {} - {} / status: {}",
                warehouse.getName(), name, status);
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
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }


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
}

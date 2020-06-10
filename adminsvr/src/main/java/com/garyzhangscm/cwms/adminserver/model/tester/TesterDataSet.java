package com.garyzhangscm.cwms.adminserver.model.tester;

import java.util.HashMap;
import java.util.Map;


public abstract class TesterDataSet {

    private final String warehouseName;

    private Map<TestScenarioType, String[]> itemNames = new HashMap<>();

    private Map<TestScenarioType, String[]> locationNames = new HashMap<>();

    public TesterDataSet(String warehouseName) {
        this.warehouseName = warehouseName;
    }

    public String getWarehouseName() {
        return warehouseName;
    }

    // key: test scenario type
    // value: Item names for the test scenario
    public Map<TestScenarioType, String[]> getItemNames() {
        return itemNames;
    }
    public String[] getItemNames(TestScenarioType testScenarioType) {
        return itemNames.getOrDefault(testScenarioType, new String[]{});
    }

    // key: test scenario type
    // value: Item names for the test scenario
    public Map<TestScenarioType, String[]> getLocationNames() {
        return locationNames;
    }
    public String[] getLocationNames(TestScenarioType testScenarioType) {

        return locationNames.getOrDefault(testScenarioType, new String[]{});
    }
}

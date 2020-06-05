package com.garyzhangscm.cwms.adminserver.model.tester;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class TestScenarioFactory {

    // Autowire will load all test scenario beans
    @Autowired
    List<TestScenario> testScenarios;

    public List<TestScenario> getTestScenarios(){
        return testScenarios;
    }
    public List<TestScenario> getTestScenarios(String warehouseName){
        return testScenarios
                .stream()
                .filter(testScenario -> StringUtils.isBlank(testScenario.getName()) || testScenario.getName().equals(warehouseName))
                .collect(Collectors.toList());
    }


}

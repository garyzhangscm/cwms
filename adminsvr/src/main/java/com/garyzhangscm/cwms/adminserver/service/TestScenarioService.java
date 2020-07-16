package com.garyzhangscm.cwms.adminserver.service;

import com.garyzhangscm.cwms.adminserver.model.tester.TestScenario;
import com.garyzhangscm.cwms.adminserver.clients.ResourceServiceRestemplateClient;
import com.garyzhangscm.cwms.adminserver.clients.WarehouseLayoutServiceRestemplateClient;

import com.garyzhangscm.cwms.adminserver.model.tester.TestScenarioFactory;
import com.garyzhangscm.cwms.adminserver.model.tester.TestScenarioSuit;
import com.garyzhangscm.cwms.adminserver.model.tester.TestScenarioSuitStatus;
import com.garyzhangscm.cwms.adminserver.model.wms.Warehouse;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import javax.transaction.Synchronization;
import java.time.LocalDateTime;
import java.util.*;

/**
 *
 */
@Service
public class TestScenarioService {

    private static final Logger logger = LoggerFactory.getLogger(TestScenarioService.class);

    private final String defaultWarehouseName = "WMOR";


    // Map of all test scenarios
    // key: warehouse name
    // value: test scenario running in separate thread
    private Map<String, TestScenarioSuit> testScenarioSuits;


    private TestScenarioFactory testScenarioFactory;

    private ResourceServiceRestemplateClient resourceServiceRestemplateClient;

    private WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient;


    @Autowired
    public TestScenarioService(TestScenarioFactory testScenarioFactory,
                               ResourceServiceRestemplateClient resourceServiceRestemplateClient,
                               WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient) {
        this.testScenarioFactory = testScenarioFactory;

        this.resourceServiceRestemplateClient = resourceServiceRestemplateClient;

        this.warehouseLayoutServiceRestemplateClient = warehouseLayoutServiceRestemplateClient;
        // load all test scenario
        loadTestScenarioSuits();
    }

    private void loadTestScenarioSuits() {
        testScenarioSuits = new HashMap<>();

        logger.debug("testScenarioFactory is null? : {}", Objects.isNull(testScenarioFactory));
        List<TestScenario> testScenarios = testScenarioFactory.getTestScenarios();

        testScenarios.forEach(testScenario -> {
            String warehouseName = StringUtils.isBlank(testScenario.getWarehouseName()) ?
                    defaultWarehouseName : testScenario.getWarehouseName();
            TestScenarioSuit testScenarioSuit = testScenarioSuits.getOrDefault(
                    warehouseName, new TestScenarioSuit()
            );
            testScenarioSuit.registerTestScenario(testScenario);
            testScenarioSuits.putIfAbsent(warehouseName, testScenarioSuit);
        });
        logger.debug("After register, we got {} test suite", testScenarioSuits.size());
    }
    /**
     * We will always run our test scenario against the standard
     * warehouse WMOR
     */
    private boolean warmUp(Warehouse warehouse, TestScenarioSuit testScenarioSuit) {

        testScenarioSuit.setStatus(TestScenarioSuitStatus.WARMING_UP);
        // First of all, clear all bad data
        logger.debug("Start to warm up for warehouse {}", warehouse.getName());
        if (!tearDown(warehouse, testScenarioSuit)) {

            return false;
        }

        try {
            testScenarioSuit.setStatus(TestScenarioSuitStatus.WARMING_UP);
            resourceServiceRestemplateClient.initData(warehouse.getName());
        }
        catch (Exception ex) {

            testScenarioSuit.setStatus(TestScenarioSuitStatus.FAILED);
            testScenarioSuit.setLastErrorMessage(ex.getMessage());
            return false;
        }
        return true;

    }

    private boolean tearDown(Warehouse warehouse, TestScenarioSuit testScenarioSuit) {

        testScenarioSuit.setStatus(TestScenarioSuitStatus.TEARING_DOWN);
        try {

            resourceServiceRestemplateClient.clearData(warehouse.getName());
        }
        catch (Exception ex) {

            testScenarioSuit.setStatus(TestScenarioSuitStatus.FAILED);
            testScenarioSuit.setLastErrorMessage(ex.getMessage());
            return false;
        }
        return true;

    }

    public void run(String warehouseName) {

        if (StringUtils.isBlank(warehouseName)) {
            warehouseName = defaultWarehouseName;
        }
        TestScenarioSuit testScenarioSuit = testScenarioSuits.get(warehouseName);

        if (Objects.isNull(testScenarioSuit)) {

            logger.debug("No test suit defined for this warehouse {} yet", warehouseName);
            return;
        }
        else if (testScenarioSuit.getTestScenarios().size() == 0) {

            logger.debug("test suit defined for this warehouse {} has no test scenario", warehouseName);
            return;
        }
        // if we already have a thread that running the test scenario
        Warehouse warehouse = warehouseLayoutServiceRestemplateClient.getWarehouseByName(warehouseName);
        synchronized(testScenarioSuit) {
            if (testScenarioSuit.getStatus() == TestScenarioSuitStatus.RUNNING) {
                // It is already running, let's do nothing
                return;
            }
            new Thread(() -> {
                testScenarioSuit.reset();
                testScenarioSuit.setStartDateTime(LocalDateTime.now());

                if (!warmUp(warehouse, testScenarioSuit)) {
                    return;
                }

                testScenarioSuit.setStatus(TestScenarioSuitStatus.RUNNING);

                testScenarioSuit.run(warehouse);

                // Save the final result(Succeed or fail)
                // so that after tear down, we can reset the test suit
                // with the right result
                TestScenarioSuitStatus status = testScenarioSuit.getStatus();
                testScenarioSuit.setStatus(TestScenarioSuitStatus.TEARING_DOWN);

                // ignore the tear down for now so we can double check
                // the data
                // tearDown(warehouse, testScenarioSuit);

                // tear down successfully, reset the status
                // back
                testScenarioSuit.setStatus(status);
                testScenarioSuit.setEndDateTime(LocalDateTime.now());


            }).start();
        }




    }

    public void registerTestScenario(TestScenario testScenario) {
        registerTestScenario(defaultWarehouseName, testScenario);
    }

    public void registerTestScenario(String warehouseName, TestScenario testScenario) {
        TestScenarioSuit testScenarioSuit = testScenarioSuits.getOrDefault(warehouseName,
                new TestScenarioSuit());
        testScenarioSuit.registerTestScenario(testScenario);
        testScenarioSuits.putIfAbsent(warehouseName, testScenarioSuit);
    }

    public TestScenarioSuit getTestSuits(String warehouseName) {
        return testScenarioSuits.getOrDefault(warehouseName, new TestScenarioSuit());
    }


}

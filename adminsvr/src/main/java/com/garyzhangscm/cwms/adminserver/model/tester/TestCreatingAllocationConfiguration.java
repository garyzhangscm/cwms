package com.garyzhangscm.cwms.adminserver.model.tester;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.garyzhangscm.cwms.adminserver.clients.*;
import com.garyzhangscm.cwms.adminserver.exception.TestFailException;
import com.garyzhangscm.cwms.adminserver.model.wms.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class TestCreatingAllocationConfiguration extends TestScenario{

    private static final Logger logger = LoggerFactory.getLogger(TestCreatingAllocationConfiguration.class);

    // Will create allocation configuration by  item family
    //
    // ITEM family: TEST_HIGH_VALUE
    // pick from             unit of measure
    // TEST-EA                EA
    // TEST-CS                CS
    // TEST-PL                PL
    // Replenish from
    // TEST-CS                CS
    // TEST-PL                PL
    private final String itemFamilyName = "TEST_HIGH_VALUE";
    Map<String, Integer> allocationConfigurationStrategiesForPicking = new HashMap<>();
    Map<String, Integer> allocationConfigurationStrategiesForReplenishment = new HashMap<>();

    @Autowired
    private InventoryServiceRestemplateClient inventoryServiceRestemplateClient;
    @Autowired
    private OutbuondServiceRestemplateClient outbuondServiceRestemplateClient;
    @Autowired
    private WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient;
    @Autowired
    private CommonServiceRestemplateClient commonServiceRestemplateClient;
    @Autowired
    private IntegrationServiceRestemplateClient integrationServiceRestemplateClient;


    public TestCreatingAllocationConfiguration() {
        super(TestScenarioType.CREATE_ALLOCATION_CONFIGURATION, 30300);

        allocationConfigurationStrategiesForPicking.put("TEST-EA|EA", 10);
        allocationConfigurationStrategiesForPicking.put("TEST-CS|CS", 11);
        allocationConfigurationStrategiesForPicking.put("TEST-PL|PL", 12);

        allocationConfigurationStrategiesForReplenishment.put("TEST-CS|CS", 20);
        allocationConfigurationStrategiesForReplenishment.put("TEST-PL|PL", 21);


    }
    @Override
    public void runTest(Warehouse warehouse) {

        ItemFamily itemFamily =
                inventoryServiceRestemplateClient.getItemFamilyByName(warehouse.getId(), itemFamilyName);

        createAllocationConfigurationByItemFamilyForPicking(warehouse, itemFamily);
        createAllocationConfigurationByItemFamilyForReplenishment(warehouse, itemFamily);

        assertResult(warehouse, itemFamily);
    }

    private void createAllocationConfigurationByItemFamilyForPicking(Warehouse warehouse, ItemFamily itemFamily) {
        createAllocationConfigurationByItemFamily(warehouse, itemFamily,
                allocationConfigurationStrategiesForPicking,
                AllocationConfigurationType.PICKING
                );
    }
        private void createAllocationConfigurationByItemFamilyForReplenishment(Warehouse warehouse, ItemFamily itemFamily) {
            createAllocationConfigurationByItemFamily(warehouse, itemFamily,
                    allocationConfigurationStrategiesForReplenishment,
                    AllocationConfigurationType.REPLENISHMENT
            );}

    private void createAllocationConfigurationByItemFamily(Warehouse warehouse, ItemFamily itemFamily,
                                                           Map<String, Integer> allocationConfigurationStrategies,
                                                           AllocationConfigurationType allocationConfigurationType) {


        Iterator<Map.Entry<String, Integer>> entryIterable = allocationConfigurationStrategies.entrySet().iterator();
        while (entryIterable.hasNext()) {
            Map.Entry<String, Integer> entry = entryIterable.next();

            String[] configurationTuple = entry.getKey().split("\\|");
            String locationGroupName = configurationTuple[0];
            String pickableUnitOfMeasureName = configurationTuple[1];

            int sequence = entry.getValue();


            LocationGroup locationGroup
                    = warehouseLayoutServiceRestemplateClient.getLocationGroupByName(warehouse.getId(), locationGroupName);


            UnitOfMeasure unitOfMeasure
                    = commonServiceRestemplateClient.getUnitOfMeasureByName(warehouse.getId(), pickableUnitOfMeasureName);


            AllocationConfiguration allocationConfiguration
                    = new AllocationConfiguration(sequence,
                    warehouse, itemFamily, allocationConfigurationType,
                    locationGroup, AllocationStrategy.FIRST_IN_FIRST_OUT,
                    new ArrayList<>());

            try {
                allocationConfiguration
                        = outbuondServiceRestemplateClient.createAllocationConfiguration(allocationConfiguration);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
                throw TestFailException.raiseException("Can't create allocation configuration: \n" + allocationConfiguration);
            }

            PickableUnitOfMeasure pickableUnitOfMeasure =
                    new PickableUnitOfMeasure(warehouse, unitOfMeasure.getId(), allocationConfiguration);

            try {
                outbuondServiceRestemplateClient.addPickableUnitOfMeasure(allocationConfiguration,
                        pickableUnitOfMeasure);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
                throw TestFailException.raiseException("Can't add pickable unit of measure: \n" + allocationConfiguration);
            }
        }
    }



    private void assertResult(Warehouse warehouse, ItemFamily itemFamily) {


        assertAllocationConfigurationByItemFamily(warehouse, itemFamily);


    }

    private void assertAllocationConfigurationByItemFamily(Warehouse warehouse, ItemFamily itemFamily) {

        List<AllocationConfiguration> allocationConfigurations =
                outbuondServiceRestemplateClient.getAllocationConfiguration(warehouse, itemFamily);


        if (allocationConfigurations.size() == 0) {
            throw TestFailException.raiseException("The item family doesn't have any allocation configuration: " + itemFamilyName);
        }

        if (allocationConfigurations.size() !=
                (allocationConfigurationStrategiesForPicking.size() + allocationConfigurationStrategiesForReplenishment.size())) {
            throw TestFailException.raiseException("The allocation configuration created doesn't match with the strategies: \n" +
                    "expected quantity: " + (allocationConfigurationStrategiesForPicking.size() + allocationConfigurationStrategiesForReplenishment.size()) +
                    "\n actual quantity: " + allocationConfigurations.size());

        }

        // We should have 3 allocation configuration defined for picking
        // and 2  allocation configuration defined for replenishment
        // one for each location group
        for (AllocationConfiguration allocationConfiguration : allocationConfigurations) {
            List<PickableUnitOfMeasure> pickableUnitOfMeasures = allocationConfiguration.getPickableUnitOfMeasures();
            if (pickableUnitOfMeasures.size() != 1) {

                throw TestFailException.raiseException("The allocation configuration is supposed to have only one pickable UOM: \n" + allocationConfiguration);
            }
            UnitOfMeasure unitOfMeasure =
                    commonServiceRestemplateClient.getUnitOfMeasureById(
                            pickableUnitOfMeasures.get(0).getUnitOfMeasureId());
            logger.debug("Will check if we already created the configuration to allocate {} from {}",
                    unitOfMeasure.getName(), allocationConfiguration.getLocationGroup().getName());
            if (allocationConfiguration.getType().equals(AllocationConfigurationType.PICKING)) {

                long count = allocationConfigurationStrategiesForPicking.entrySet().stream()
                        .filter(entry ->
                                entry.getKey().equals(
                                        allocationConfiguration.getLocationGroup().getName() + "|" + unitOfMeasure.getName())
                                        &&
                                        entry.getValue().equals(allocationConfiguration.getSequence())).count();
                if (count == 0) {
                    throw TestFailException.raiseException("Allocation configuration for picking is not expected: \n" + allocationConfiguration);
                }
            }
            else {
                long count = allocationConfigurationStrategiesForReplenishment.entrySet().stream()
                        .filter(entry ->
                                entry.getKey().equals(
                                        allocationConfiguration.getLocationGroup().getName() + "|" + unitOfMeasure.getName())
                                        &&
                                        entry.getValue().equals(allocationConfiguration.getSequence())).count();
                if (count == 0) {
                    throw TestFailException.raiseException("Allocation configuration for replenishment is not expected: \n" + allocationConfiguration);
                }

            }

        }



    }


}

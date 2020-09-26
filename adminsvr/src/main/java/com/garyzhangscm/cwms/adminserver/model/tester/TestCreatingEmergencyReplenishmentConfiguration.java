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

@Service
public class TestCreatingEmergencyReplenishmentConfiguration extends TestScenario{

    private static final Logger logger = LoggerFactory.getLogger(TestCreatingEmergencyReplenishmentConfiguration.class);

    // Will create emergency replenishment cnofiguration by  item family
    //
    // ITEM family: TEST_HIGH_VALUE
    // replenishment to       unit of measure
    // TEST-EA                 CS
    // TEST-EA                 PL
    private final String itemFamilyName = "TEST_HIGH_VALUE";
    Map<String, Integer> emergencyReplenishmentConfigurationStrategies = new HashMap<>();

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


    public TestCreatingEmergencyReplenishmentConfiguration() {
        super(TestScenarioType.CREATE_EMERGENCY_REPLENISHMENT_CONFIGURATION, 30400);

        emergencyReplenishmentConfigurationStrategies.put("TEST-EA|CS", 10);
        emergencyReplenishmentConfigurationStrategies.put("TEST-EA|PL", 11);

    }
    @Override
    public void runTest(Warehouse warehouse) {

        ItemFamily itemFamily =
                inventoryServiceRestemplateClient.getItemFamilyByName(warehouse.getId(), itemFamilyName);

            createEmergencyReplenishmentConfigurationByItemFamily(warehouse, itemFamily);

            assertResult(warehouse, itemFamily);
    }

    private void createEmergencyReplenishmentConfigurationByItemFamily(Warehouse warehouse, ItemFamily itemFamily) {



        Iterator<Map.Entry<String, Integer>> entryIterable
                = emergencyReplenishmentConfigurationStrategies.entrySet().iterator();
        while (entryIterable.hasNext()) {
            Map.Entry<String, Integer> entry = entryIterable.next();
            String[] configurationTuple = entry.getKey().split("\\|");
            String locationGroupName = configurationTuple[0];
            String pickableUnitOfMeasureName = configurationTuple[1];
            int sequence = entry.getValue();

            LocationGroup eaLocationGroup
                    = warehouseLayoutServiceRestemplateClient.getLocationGroupByName(warehouse.getId(), locationGroupName);


            UnitOfMeasure unitOfMeasure
                    = commonServiceRestemplateClient.getUnitOfMeasureByName(warehouse.getId(), pickableUnitOfMeasureName);


            EmergencyReplenishmentConfiguration configuration
                    = new EmergencyReplenishmentConfiguration(sequence,
                                        warehouse, unitOfMeasure, itemFamily, eaLocationGroup);



            try {
                logger.debug("Start to create emergency replenishment configuration\n{}", configuration);
                configuration
                        = outbuondServiceRestemplateClient.createEmergencyReplenishmentConfiguration(configuration);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
                throw TestFailException.raiseException("Can't create emergency replenishment configuration: \n" + configuration);
            }

        }
    }



    private void assertResult(Warehouse warehouse, ItemFamily itemFamily) {


        assertEmergencyReplenishemntConfigurationByItemFamily(warehouse, itemFamily);


    }

    private void assertEmergencyReplenishemntConfigurationByItemFamily(Warehouse warehouse, ItemFamily itemFamily) {

        List<EmergencyReplenishmentConfiguration> configurations =
                outbuondServiceRestemplateClient.getEmergencyReplenishmentConfiguration(warehouse, itemFamily);


        if (configurations.size() == 0) {
            throw TestFailException.raiseException("The item family doesn't have any emergency replenishment configuration: " + itemFamilyName);
        }

        if (configurations.size() != emergencyReplenishmentConfigurationStrategies.size()) {
            throw TestFailException.raiseException("The emergency replenishment configuration created doesn't match with the strategies: \n" +
                    "expected quantity: " + emergencyReplenishmentConfigurationStrategies.size() +
                    "\n actual quantity: " + configurations.size());

        }

        // We should have 2 emergency replenishment configuration defined for this item family
        // one for each pickable unit of measure
        for (EmergencyReplenishmentConfiguration emergencyReplenishmentConfiguration : configurations) {
            long count = emergencyReplenishmentConfigurationStrategies.entrySet().stream()
                    .filter(entry -> {

                        String[] configurationTuple = entry.getKey().split("\\|");
                        String destinationLocationGroupName = configurationTuple[0];
                        String unitOfMeasureName = configurationTuple[1];

                        UnitOfMeasure unitOfMeasure
                                        = commonServiceRestemplateClient.getUnitOfMeasureByName(warehouse.getId(), unitOfMeasureName);


                        return destinationLocationGroupName.equals(emergencyReplenishmentConfiguration.getDestinationLocationGroup().getName())
                                &&
                                unitOfMeasure.getId().equals(emergencyReplenishmentConfiguration.getUnitOfMeasureId());

                    }).count();
            if (count == 0) {
                throw TestFailException.raiseException("Emergency replenishment configuration is not expected: \n" + emergencyReplenishmentConfiguration);
            }
        }



    }


}

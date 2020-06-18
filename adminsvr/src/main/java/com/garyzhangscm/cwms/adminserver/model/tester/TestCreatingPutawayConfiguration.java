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
public class TestCreatingPutawayConfiguration extends TestScenario{

    private static final Logger logger = LoggerFactory.getLogger(TestCreatingPutawayConfiguration.class);

    // Will create putaway configuration by item and item family
    //
    // ITEM family: TEST_HIGH_VALUE
    // key: Location Group:
    // -- TEST-EA
    // -- TEST-CS
    // -- TEST-PL
    // value: putaway strategies
    Map<String, List<PutawayConfigurationStrategy>> putawayConfigurationStrategies = new HashMap<>();
    String[] locationGroupNames = new String[]{"TEST-EA","TEST-CS","TEST-PL"};
    String itemFamilyName = "TEST_HIGH_VALUE";

    @Autowired
    private InventoryServiceRestemplateClient inventoryServiceRestemplateClient;
    @Autowired
    private InboundServiceRestemplateClient inboundServiceRestemplateClient;
    @Autowired
    private WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient;
    @Autowired
    private CommonServiceRestemplateClient commonServiceRestemplateClient;
    @Autowired
    private IntegrationServiceRestemplateClient integrationServiceRestemplateClient;


    public TestCreatingPutawayConfiguration() {
        super(TestScenarioType.CREATE_PUTAWAY_CONFIGURATION, 30200);

        for(int i = 0; i < locationGroupNames.length; i++) {
            String locationGroupName = locationGroupNames[i];
            putawayConfigurationStrategies.put(locationGroupName,
                    Arrays.asList(new PutawayConfigurationStrategy[]{
                            PutawayConfigurationStrategy.EMPTY_LOCATIONS,
                            PutawayConfigurationStrategy.PARTIAL_LOCATIONS}));
        }


    }
    @Override
    public void runTest(Warehouse warehouse) {

            createPutawayConfigurationByItem(warehouse);
            createPutawayConfigurationByItemFamily(warehouse, itemFamilyName);

            assertResult(warehouse);
    }

    private void createPutawayConfigurationByItemFamily(Warehouse warehouse, String itemFamilyName) {

        ItemFamily itemFamily =
                inventoryServiceRestemplateClient.getItemFamilyByName(warehouse.getId(), itemFamilyName);

        // Get all the location group and create one putaway configuration for each group
        // in sequence
        for(int i = 0; i < locationGroupNames.length; i++) {
            LocationGroup locationGroup = warehouseLayoutServiceRestemplateClient.getLocationGroupByName(
                    warehouse.getId(), locationGroupNames[i]
            );
            if (Objects.isNull(locationGroup)) {
                throw TestFailException.raiseException("Location group : " + locationGroupNames[i] + " is created yet");
            }
            List<PutawayConfigurationStrategy> putawayConfigurationStrategyList =
                    putawayConfigurationStrategies.get(locationGroupNames[i]);

            logger.debug("putawayConfigurationStrategyList: {}",
                    putawayConfigurationStrategyList.stream().map(PutawayConfigurationStrategy::name).collect(Collectors.joining(","))
                    );
            int sequence = i;
            PutawayConfiguration putawayConfiguration = PutawayConfiguration.byItemFamilyAndLocationGroup(
                    sequence, warehouse, itemFamily, locationGroup,
                    putawayConfigurationStrategyList.stream().map(PutawayConfigurationStrategy::name).collect(Collectors.joining(","))
                    );
            try {
                logger.debug("Start to create putaway configuration {} ", putawayConfiguration);
                inboundServiceRestemplateClient.createPutawayConfiguration(
                        putawayConfiguration
                );
            } catch (JsonProcessingException e) {
                e.printStackTrace();
                throw TestFailException.raiseException("Error while creating putaway configuration : " + putawayConfiguration);
            }
        }
    }

    private void createPutawayConfigurationByItem(Warehouse warehouse) {
        // TO-DO
    }



    private void assertResult(Warehouse warehouse) {


        assertPutawayConfigurationByItem(warehouse);
        assertPutawayConfigurationByItemFamily(warehouse);


    }
    private void assertPutawayConfigurationByItem(Warehouse warehouse) {

        // TO-DO

    }
    private void assertPutawayConfigurationByItemFamily(Warehouse warehouse) {

        List<PutawayConfiguration> putawayConfigurations =
                            inboundServiceRestemplateClient.getPutawayConfigurationByItemFamily(
                                    warehouse.getId(), itemFamilyName);


        if (putawayConfigurations.size() == 0) {
            throw TestFailException.raiseException("The item family doesn't have any putaway configuration: " + itemFamilyName);
        }

        // We should have 3 putaway configuration defined for this item family
        // one for each location group
        for (PutawayConfiguration putawayConfiguration : putawayConfigurations) {
            List<PutawayConfigurationStrategy> putawayConfigurationStrategyList =
                    putawayConfigurationStrategies.get(putawayConfiguration.getLocationGroup().getName());
            for (PutawayConfigurationStrategy putawayConfigurationStrategy : putawayConfigurationStrategyList) {
                if (!putawayConfiguration.getStrategies().contains(putawayConfigurationStrategy.name())){

                    throw TestFailException.raiseException("Putaway configuration strategies doesn't match. \n " +
                            "location group: " + putawayConfiguration.getLocationGroup() + "\n" +
                            "expected: " + putawayConfigurationStrategyList + "\n" +
                            "actual: " + putawayConfiguration.getStrategies());
                }

            }

        }



    }


}

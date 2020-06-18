package com.garyzhangscm.cwms.adminserver.model.tester;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.garyzhangscm.cwms.adminserver.clients.CommonServiceRestemplateClient;
import com.garyzhangscm.cwms.adminserver.clients.IntegrationServiceRestemplateClient;
import com.garyzhangscm.cwms.adminserver.clients.InventoryServiceRestemplateClient;
import com.garyzhangscm.cwms.adminserver.clients.WarehouseLayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.adminserver.exception.TestFailException;
import com.garyzhangscm.cwms.adminserver.model.wms.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Test creating locations
 * 1. EACH Picking locations
 * 2. Case Picking Locations
 * 3. Pallet picking locations
 */
@Service
public class TestCreatingLocation extends TestScenario{

    private static final Logger logger = LoggerFactory.getLogger(TestCreatingLocation.class);

    // key: location group name
    // value: location names
    Map<String, String[]> testingLocations = new HashMap<>();
    private final int startSequence = 10000;

    private final String storageLocationGroupTypeName = "Storage";

    @Autowired
    private InventoryServiceRestemplateClient inventoryServiceRestemplateClient;
    @Autowired
    private CommonServiceRestemplateClient commonServiceRestemplateClient;
    @Autowired
    private IntegrationServiceRestemplateClient integrationServiceRestemplateClient;
    @Autowired
    private WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient;


    public TestCreatingLocation() {
        super(TestScenarioType.CREATE_LOCATION, 30000);

        String eachPickingLocationGroup = "TEST-EA";
        String casePickingLocationGroup = "TEST-CS";
        String palletPickingLocationGroup = "TEST-PL";
        String[] eachPickingLocationNames = new String[50];
        String[] casePickingLocationNames = new String[50];
        String[] palletPickingLocationNames = new String[50];

        for(int i = 1; i <= 50; i++) {

            // each picking location
            String eachPickingLocationName = String.format("%s-%03d", eachPickingLocationGroup, i);
            eachPickingLocationNames[i-1] = eachPickingLocationName;

            // each picking location
            String casePickingLocationName = String.format("%s-%03d", casePickingLocationGroup, i);
            casePickingLocationNames[i-1] = casePickingLocationName;


            // each picking location
            String palletPickingLocationName = String.format("%s-%03d", palletPickingLocationGroup, i);
            palletPickingLocationNames[i-1] = palletPickingLocationName;

        }
        testingLocations.put(eachPickingLocationGroup, eachPickingLocationNames);
        testingLocations.put(casePickingLocationGroup, casePickingLocationNames);
        testingLocations.put(palletPickingLocationGroup, palletPickingLocationNames);

    }
    @Override
    public void runTest(Warehouse warehouse) {

            // Get the location group type
            LocationGroupType storageLocationGroupType = getLocationGroupType(storageLocationGroupTypeName);
            // create location groups
            Map<String, LocationGroup> locationGroups =
                    createLocationGroups(warehouse, storageLocationGroupType);

            createLocations(warehouse, locationGroups);
            assertResult(warehouse);

    }

    private void assertResult(Warehouse warehouse) {
        assertLocationGroups(warehouse);
        assertLocations(warehouse);
    }
    private void assertLocationGroups(Warehouse warehouse) {
        testingLocations.keySet().forEach(locationGroupName ->
        {
            LocationGroup locationGroup
                    = warehouseLayoutServiceRestemplateClient.getLocationGroupByName(warehouse.getId(), locationGroupName);
            if (Objects.isNull(locationGroup)) {
                throw TestFailException.raiseException("Location group for " + locationGroupName + " is not created");
            }
        });
    }
    private void assertLocations(Warehouse warehouse) {
        testingLocations.keySet().forEach(locationGroupName ->
        {
            String[] locationNames = testingLocations.get(locationGroupName);
            Arrays.stream(locationNames).forEach(locationName -> {

                Location location =
                    warehouseLayoutServiceRestemplateClient.getLocationByName(warehouse.getId(), locationName);
                if (Objects.isNull(location)) {
                    throw TestFailException.raiseException("Location " + locationGroupName +
                            " / " + locationName + " is not created");
                }
            });
        });
    }

    private Map<String, LocationGroup> createLocationGroups(Warehouse warehouse, LocationGroupType storageLocationGroupType) {
        // key: location group name
        // value: location group
        // we will save it so we can reuse when creating location in the group
        Map<String, LocationGroup> locationGroups = new HashMap<>();

        testingLocations.keySet().forEach(locationGroupName ->
        {
            try {
                locationGroups.put(locationGroupName,
                        createLocationGroup(warehouse, storageLocationGroupType, locationGroupName));
            } catch (JsonProcessingException e) {
                e.printStackTrace();
                throw TestFailException.raiseException("Not able to create location groups: " + e.getMessage());
            }
        });
        return locationGroups;
    }

    private LocationGroupType getLocationGroupType(String name) {
        return warehouseLayoutServiceRestemplateClient.getLocationGroupTypeByName(name);
    }

    private LocationGroup createLocationGroup(Warehouse warehouse, LocationGroupType locationGroupType,
                                              String name) throws JsonProcessingException {
        LocationGroup locationGroup = new LocationGroup(warehouse, locationGroupType,
                name, "Test : " + name, InventoryConsolidationStrategy.CONSOLIDATE_BY_INVENTORY);
        return warehouseLayoutServiceRestemplateClient.createLocationGroup(locationGroup);
    }

    private void createLocations(Warehouse warehouse, Map<String, LocationGroup> locationGroups) {


        int i = 0;
        for (Map.Entry<String, String[]> entry : testingLocations.entrySet()) {
            i++;
            int locationGroupStartSequence = i * startSequence;



            String locationGroupName = entry.getKey();
            String[] locationNames = entry.getValue();

            // Get the location gorup we just created
            LocationGroup locationGroup = locationGroups.get(locationGroupName);
            for (int j = 0; j < locationNames.length; j++) {
                String locationName = locationNames[j];
                int locationSequence = locationGroupStartSequence + j * 100;

                try {
                    createLocation(warehouse, locationGroup, locationName, locationSequence);
                } catch (JsonProcessingException e) {
                    throw TestFailException.raiseException("Error while creating location: " + e.getMessage());
                }
            }
        }

    }

    private void createLocation(Warehouse warehouse, LocationGroup locationGroup, String locationName, int locationSequence)
            throws JsonProcessingException {

        Location location = new Location(warehouse, locationGroup, locationName, locationSequence);
        warehouseLayoutServiceRestemplateClient.createLocation(location);
    }



}

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
import java.util.stream.Collectors;

@Service
public class TestCycleCount extends TestScenario{

    private static final Logger logger = LoggerFactory.getLogger(TestCycleCount.class);

    private final String itemName = "TEST-ITEM-HV-002";
    private final String[] locationNamesEA = {"TEST-EA-010", "TEST-EA-011", "TEST-EA-012", "TEST-EA-013"};
    private final String[] locationNamesCS = {"TEST-CS-010", "TEST-CS-011", "TEST-CS-012", "TEST-CS-013"};
    private final String[] locationNamesPL = {"TEST-PL-010", "TEST-PL-011", "TEST-PL-012", "TEST-PL-013"};

    private final String inventoryStatusName = "AVAL";

    @Autowired
    private InventoryServiceRestemplateClient inventoryServiceRestemplateClient;
    @Autowired
    private CommonServiceRestemplateClient commonServiceRestemplateClient;
    @Autowired
    private IntegrationServiceRestemplateClient integrationServiceRestemplateClient;
    @Autowired
    private WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient;


    public TestCycleCount() {

        super(TestScenarioType.CYCLE_AUDIT_COUNT, 50100);
    }
    @Override
    public void runTest(Warehouse warehouse) {

            testEALocations(warehouse);
            testCSLocations(warehouse);
            testPLLocations(warehouse);


    }

    private void testPLLocations(Warehouse warehouse) {
        logger.debug("Start to test PL locations");
    }

    private void testEALocations(Warehouse warehouse) {

        logger.debug("Start to test EA locations");
        // Make sure all EA locations are empty
        List<Location> eaLocations =
                assertLocationsEmpty(warehouse, locationNamesEA);
        logger.debug("All location are  empty: {}", locationNamesEA);

        // request a cycle count for the EA locations
        // include empty locations
        boolean includeEmptyLocations = true;
        List<CycleCountRequest> cycleCountRequests =
                requestCycleCount(warehouse, eaLocations, includeEmptyLocations);

        assertLocationWithCycleCountRequest(eaLocations, cycleCountRequests, includeEmptyLocations);
        logger.debug("cycle count requests for those locations are created!");
        // finish locations without any discrepancy so that
        // all locations should be still empty

        List<CycleCountResult> cycleCountResults =
                confirmCycleCountRequests(cycleCountRequests);
        assertLocationWithCycleCountResult(eaLocations, cycleCountResults);
        logger.debug("cycle count requests for those locations are confirmed!");
    }

    private void testCSLocations(Warehouse warehouse) {
        logger.debug("Start to test CS locations");
        // Make sure all CS locations are empty
        List<Location> csLocations =
                assertLocationsEmpty(warehouse, locationNamesCS);

        // Adjust one location up so it can have inventory
        String nonEmptyLocationName = "TEST-CS-010";
        Location cs010Location = assertLocationEmpty(warehouse, nonEmptyLocationName);

        Item item = assertItem(warehouse, itemName);
        logger.debug("Will start to adjust inventory with item \n{}", item);
        InventoryStatus inventoryStatus = assertInventoryStatus(warehouse, inventoryStatusName);

        logger.debug("Start to test adding new inventory");
        Long quantity = 100L;
        Inventory cs010Inventory = createInventory(warehouse, cs010Location, item, quantity, inventoryStatus);

        // current inventory quantity: 100
        assertInventory(cs010Inventory.getId(), warehouse, cs010Location, item, quantity, inventoryStatus);
        logger.debug("New inventory added");
        // Generate cycle count

        boolean includeEmptyLocations = true;
        List<CycleCountRequest> cycleCountRequests =
                requestCycleCount(warehouse, csLocations, includeEmptyLocations);

        assertLocationWithCycleCountRequest(csLocations, cycleCountRequests, includeEmptyLocations);

        // location        up / down         from quantity         to quantity
        // TEST-CS-010     down               100                    50
        // TEST-CS-011      up                 0                     75
        // TEST-CS-012      --                 0                      0
        // TEST-CS-013      --                 0                      0
        // We will test
        // TEST-CS-010 :
        //     1. We can get audit count when count down with existing inventory
        //     2. We can finish the audit count and adjust the inventory
        //     3. We can get intergration data for the adjustment(TO-DO)
        // TEST-CS-011:
        //     1. We can get audit count when count UP from empty location
        //     2. We can finish the audit count and adjust the inventory
        //     3. We can get intergration data for the adjustment(TO-DO)
        // TEST-CS-012 / TEST-CS-013:
        //     1. We can cancel the cycle count
        //     2. we can reopen the count
        //     3. We can finish the audit count and adjust the inventory
        //     4. We can get intergration data for the adjustment(TO-DO)


        /////////////   TEST-CS-010                               /////////
        //     1. We can get audit count when count down with existing inventory
        //     2. We can finish the audit count and adjust the inventory
        //     3. We can get intergration data for the adjustment(TO-DO)
        Map<String, CycleCountRequest> cycleCountRequestMap = new HashMap<>();
        cycleCountRequests.forEach(cycleCountRequest ->
                cycleCountRequestMap.put(cycleCountRequest.getLocation().getName(), cycleCountRequest));

        CycleCountRequest cs010Request = cycleCountRequestMap.get("TEST-CS-010");
        List<CycleCountResult> cs010CycleCountResults
                = confirmCycleCountRequests(cs010Request, item, 100L, 50L);
        // Make sure there's only one cycle count result
        assertSingleCycleCountResultWithDiscrepancy(cs010CycleCountResults);
        logger.debug("TEST-CS-010 was count down to 50 and audit count request is generated without any issue");
        // make suer the inventory quantity has not been changed
        assertInventoryQuantity(cs010Inventory, 100);
        assertLocationLocked(warehouse, "TEST-CS-010");
        // Let's process the audit count and adjust the inventory quantity to 50
        // When we are here, we should be sure there's only one cycle count result regards to
        // the location TEST-CS-010
        AuditCountRequest cs010AuditCountRequest = cs010CycleCountResults.get(0).getAuditCountRequest();
        List<AuditCountResult> cs010AuditCountResult = confirmAuditCountRequest(cs010AuditCountRequest, cs010Inventory,50L);
        assertAuditCountResult(cs010AuditCountResult, cs010Inventory, 50L);
        assertLocationUnLocked(warehouse, "TEST-CS-010");


        /////////////////// TEST-CS-011:   //////////////////////////////
        //     1. We can get audit count when count UP from empty location
        //     2. We can finish the audit count and adjust the inventory
        //     3. We can get intergration data for the adjustment(TO-DO)
        CycleCountRequest cs011CycleCountRequest = cycleCountRequestMap.get("TEST-CS-011");
        List<CycleCountResult> cs011CycleCountResults = confirmCycleCountRequests(cs011CycleCountRequest, item, 0L, 75L);
        assertSingleCycleCountResultWithDiscrepancy(cs011CycleCountResults);
        logger.debug("TEST-CS-011 was count up to 75 and audit count request is generated without any issue");
        // Make sure the locatino is still empty
        Location cs011Location = assertLocationEmpty(warehouse, "TEST-CS-011");
        Inventory cs011Inventory = createInventoryStructure(warehouse, cs011Location, item,
                inventoryStatus, 75L);
        AuditCountRequest cs011AuditCountRequest = cs011CycleCountResults.get(0).getAuditCountRequest();
        List<AuditCountResult> cs011AuditCountResult = confirmAuditCountRequest(cs011AuditCountRequest, cs011Inventory,75L);
        logger.debug("cs011AuditCountResult: {} ", cs011AuditCountResult);
        // assertAuditCountResult(cs011AuditCountResult, cs011Inventory, 75L);



        ///////////////   TEST-CS-012 / TEST-CS-013:    ///////////////////////////
        //     1. We can cancel the cycle count
        //     2. we can reopen the count
        //     3. We can get audit count when cycle counting with discrepancy
        //     4. We can cancel the audit count --- TO-DO: Audit Count Cancellation is not support yet
        CycleCountRequest[] cycleCountRequestArray
                = {cycleCountRequestMap.get("TEST-CS-012"), cycleCountRequestMap.get("TEST-CS-013")};
        String cycleCountRequestIds = Arrays.stream(cycleCountRequestArray).map(CycleCountRequest::getId)
                .map(String::valueOf).collect(Collectors.joining(","));

        List<CycleCountRequest> cancelledCycleCountRequests
                = cancelCycleCountRequests(cycleCountRequestIds);
        assertCycleCountRequestCancelled(cancelledCycleCountRequests);
        logger.debug("cycle count on TEST-CS-012 and TEST-CS-013  were cancelled");

        cycleCountRequestIds = String.valueOf(cycleCountRequestMap.get("TEST-CS-012").getId());
        reopenCycleCountRequests(cycleCountRequestIds);
        assertCycleCountRequestReopened(Collections.singletonList(cycleCountRequestMap.get("TEST-CS-012")));
        logger.debug("cycle count on TEST-CS-012 was reopened");


    }

    private void assertLocationUnLocked(Warehouse warehouse, String locationName) {
        assertLocationLockStatus(warehouse, locationName, false);
    }

    private void assertLocationLocked(Warehouse warehouse, String locationName) {
        assertLocationLockStatus(warehouse, locationName, true);
    }
    private void assertLocationLockStatus(Warehouse warehouse, String locationName, boolean locked) {
        Location location = warehouseLayoutServiceRestemplateClient.getLocationByName(warehouse.getId(), locationName);

        logger.debug("Check if location {} is locked. Expected: {}, actual: {}",
                locationName, locked, location.getLocked());

        if (!location.getLocked().equals(locked)) {

            throw TestFailException.raiseException("location " + locationName +
                    ", expected lock status: " + locked +
                    ", actual lock status: " + location.getLocked());
        }

    }

    private void assertAuditCountResult(List<AuditCountResult> auditCountResult, Inventory inventory, long countQuantity) {
        assertInventoryQuantity(inventory, countQuantity);
    }


    private List<AuditCountResult> confirmAuditCountRequest(AuditCountRequest auditCountRequest, Inventory inventory, long countQuantity) {

        AuditCountResult auditCountResult = new AuditCountResult(auditCountRequest, inventory, countQuantity);
        try {
            return inventoryServiceRestemplateClient.confirmAuditCountRequest(auditCountResult);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            throw TestFailException.raiseException("Can't confirm the audit count request" + auditCountRequest);
        }
    }

    private void assertCycleCountRequestStatus(List<CycleCountRequest> cancelledCycleCountRequests,
                                               CycleCountRequestStatus status) {
        cancelledCycleCountRequests.forEach(cycleCountRequest -> {
            CycleCountRequest cancelledCycleCountRequest =
                    inventoryServiceRestemplateClient.getCycleCountRequestById(cycleCountRequest.getId());
            if (Objects.isNull(cancelledCycleCountRequest)) {

                throw TestFailException.raiseException("Can't find the cancelled cycle count by id " + cycleCountRequest.getId() );
            }
            if (!cancelledCycleCountRequest.getStatus().equals(status)){

                throw TestFailException.raiseException("Cycle count " + cycleCountRequest.getId() +
                        " 's status doesn't match the expectation.  " +
                        ", expected: " + status +
                        ", actual: " + cancelledCycleCountRequest.getStatus());
            }
        });

    }
    private void assertCycleCountRequestReopened(List<CycleCountRequest> cancelledCycleCountRequests) {
        assertCycleCountRequestStatus(cancelledCycleCountRequests, CycleCountRequestStatus.OPEN);
    }

    private void assertCycleCountRequestCancelled(List<CycleCountRequest> cancelledCycleCountRequests) {
        assertCycleCountRequestStatus(cancelledCycleCountRequests, CycleCountRequestStatus.CANCELLED);

    }

    private List<CycleCountRequest> cancelCycleCountRequests(String cycleCountRequestIds) {
        return inventoryServiceRestemplateClient.cancelCycleCountRequests(cycleCountRequestIds);
    }
    private List<CycleCountRequest> reopenCycleCountRequests(String cycleCountRequestIds) {
        return inventoryServiceRestemplateClient.reopenCycleCountRequests(cycleCountRequestIds);
    }

    private void assertSingleCycleCountResultWithDiscrepancy(List<CycleCountResult> cycleCountResults) {
        if (cycleCountResults.size() == 0) {

            throw TestFailException.raiseException("There's no cycle count result to be checked" );
        }
        else if (cycleCountResults.size() > 1){

            throw TestFailException.raiseException("There're multiple cycle count results to be checked" );
        }
        else {
            CycleCountResult cycleCountResult = cycleCountResults.get(0);
            if (cycleCountResult.getQuantity().equals(cycleCountResult.getCountQuantity())) {

                throw TestFailException.raiseException("Current cycle count result has no discrepancy!" );
            }
            // Let's make sure we have a audit count request generated
            if (Objects.isNull(cycleCountResult.getAuditCountRequest())) {

                throw TestFailException.raiseException("Current cycle count result has discrepancy but we didn't find the audit count!" );
            }
            // Double check the audit count from the server
            AuditCountRequest auditCountRequest =
                    inventoryServiceRestemplateClient.getAuditCountRequestById(
                            cycleCountResult.getAuditCountRequest().getId()
                    );
            // make sure they belong to the same batch and location
            if (!cycleCountResult.getBatchId().equals(auditCountRequest.getBatchId()) ||
                !cycleCountResult.getLocationId().equals(auditCountRequest.getLocationId())) {

                throw TestFailException.raiseException("Current cycle count result doesn't match with its audit count request!" );
            }

        }
    }


    private void assertLocationWithCycleCountResult(List<Location> locations, List<CycleCountResult> cycleCountResults) {
        // make sure each location has one cycle count result
        // key: location id
        // value: whether the location is empty
        Map<Long, Boolean> locationIds = new HashMap<>();
        locations.stream().forEach(location -> {
            if (location.getCurrentVolume() == 0) {
                locationIds.put(location.getId(), true);
            }
            else {
                locationIds.put(location.getId(), false);
            }
        });
        cycleCountResults.stream().forEach(cycleCountResult -> {
            if (!locationIds.containsKey(cycleCountResult.getLocationId())) {

                throw TestFailException.raiseException("Get an unknown ID from current cycle count result." +
                        " cycle count request id: " + cycleCountResult.getId() +
                        ", location id: " + cycleCountResult.getLocationId());
            }
            // make sure the cycle count result is confirmed without any discrepancy
            if (!cycleCountResult.getQuantity().equals(cycleCountResult.getCountQuantity())) {

                throw TestFailException.raiseException("Cycle count result has discrepancy." +
                        " cycle count result id: " + cycleCountResult.getId() +
                        ", location id: " + cycleCountResult.getLocationId());
            }
        });


    }

    private List<CycleCountResult> confirmCycleCountRequests(List<CycleCountRequest> cycleCountRequests) {

        return inventoryServiceRestemplateClient.confirmCycleCountRequests(cycleCountRequests);
    }

    private List<CycleCountResult> confirmCycleCountRequests(CycleCountRequest cycleCountRequest,
                                                       Item item,
                                                       Long quantity,
                                                       Long countQuantity) {

        CycleCountResult cycleCountResult = new CycleCountResult(cycleCountRequest, item, quantity, countQuantity);
        try {
            return inventoryServiceRestemplateClient.confirmCycleCountRequests(cycleCountRequest, Collections.singletonList(cycleCountResult));
        }
        catch (Exception ex) {

            throw TestFailException.raiseException("Not able to confirm cycle count request." +
                    " cycle count result id: " + cycleCountResult.getId() +
                    ", item : " + item.getId() + " / " + item.getName() +
                    ", quantity: " + quantity +
                    ", new quantity : " + countQuantity +
                    ", location id: " + cycleCountResult.getLocationId());
        }
    }


    private void assertLocationWithCycleCountRequest(List<Location> locations, List<CycleCountRequest> cycleCountRequests, boolean includeEmptyLocations) {

        // If we include empty location, then the location's size should match with cycle count request
        if(includeEmptyLocations && locations.size() != cycleCountRequests.size()) {
            throw TestFailException.raiseException("Cycle count request's number doesn't match with location's number" +
                    ", cycle count request number: " + cycleCountRequests.size() +
                    ", location number: " + locations.size());
        }

        // make sure each location has one cycle count request
        // key: location id
        // value: whether the location is empty
        Map<Long, Boolean> locationIds = new HashMap<>();
        locations.stream().forEach(location -> {
            if (location.getCurrentVolume() == 0) {
                locationIds.put(location.getId(), true);
            }
            else {
                locationIds.put(location.getId(), false);
            }
        });
        cycleCountRequests.stream().forEach(cycleCountRequest -> {
            if (!locationIds.containsKey(cycleCountRequest.getLocationId())) {

                throw TestFailException.raiseException("Get an unknown ID from current cycle count request." +
                        " cycle count request id: " + cycleCountRequest.getId() +
                        ", location id: " + cycleCountRequest.getLocationId());
            }
            if (!includeEmptyLocations && locationIds.get(cycleCountRequest.getLocationId()).booleanValue() == true) {
                // OK, we are not allowing empty location in the request
                // but we do get empty location in current batch
                throw TestFailException.raiseException(
                        "Get an empty location from current cycle count request, while are not allowing empty location." +
                        " cycle count request id: " + cycleCountRequest.getId() +
                        ", location id: " + cycleCountRequest.getLocationId());
            }
        });


    }

    private List<CycleCountRequest> requestCycleCount(Warehouse warehouse, List<Location> locations, boolean includeEmptyLocation) {
        if (locations.size() == 0) {

            throw TestFailException.raiseException("Can't request cycle count, no location found in the list");
        }
        long countSequenceMax = locations.get(0).getCountSequence();
        long countSequenceMin = locations.get(0).getCountSequence();
        String beginLocation = locations.get(0).getName();
        String endLocation = locations.get(0).getName();

        for (Location location : locations) {
            if (location.getCountSequence() > countSequenceMax) {
                countSequenceMax = location.getCountSequence();
                endLocation = location.getName();
            }
            if (location.getCountSequence() < countSequenceMin) {
                countSequenceMin = location.getCountSequence();
                beginLocation = location.getName();
            }
        }
        logger.debug("Start to request cycle count by sequence [" + countSequenceMin + ", " + countSequenceMax + "]");

        return inventoryServiceRestemplateClient.requestCycleCount(warehouse.getId(), beginLocation, endLocation, includeEmptyLocation);
    }


    private InventoryStatus assertInventoryStatus(Warehouse warehouse, String inventoryStatusName) {
        InventoryStatus inventoryStatus =
                inventoryServiceRestemplateClient.getInventoryStatusByName(
                        warehouse.getId(), inventoryStatusName
                );

        if (Objects.isNull(inventoryStatus)) {
            throw TestFailException.raiseException("Can't find inventory status by name: " + inventoryStatusName);

        }
        return inventoryStatus;
    }

    private Item assertItem(Warehouse warehouse, String itemName) {
        Item item = inventoryServiceRestemplateClient.getItemByName(warehouse.getId(), itemName);

        if (Objects.isNull(item)) {
            throw TestFailException.raiseException("Can't find item by name: " + itemName);
        }
        if (item.getItemPackageTypes().size() == 0) {
            throw TestFailException.raiseException("The item doesn't have any package type: " + itemName);
        }
        return item;
    }

    private Location assertLocationEmpty(Warehouse warehouse, String locationName) {

        Location location = warehouseLayoutServiceRestemplateClient.getLocationByName(
                warehouse.getId(), locationName
        );
        if (Objects.isNull(location)) {
            throw TestFailException.raiseException("Can't find location by name: " + locationName);
        }
        if (location.getCurrentVolume() > 0.0) {
            throw TestFailException.raiseException("Test fail as location  " + locationName + " is not empty" );

        }
        return location;

    }

    private Inventory createInventory(Warehouse warehouse, Location location,
                                 Item item, Long quantity,
                                 InventoryStatus inventoryStatus) {
        Inventory inventory = createInventoryStructure(warehouse, location, item,
                                inventoryStatus, quantity);

        try {
            inventory = inventoryServiceRestemplateClient.createInventory(inventory);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            throw TestFailException.raiseException("Error while creating create inventory. " + e.getMessage());
        }
        return inventory;
    }

    private Inventory createInventoryStructure(Warehouse warehouse, Location location, Item item,
                                               InventoryStatus inventoryStatus, Long quantity) {
        String lpn = commonServiceRestemplateClient.getNextLpn(warehouse.getId());

        ItemPackageType itemPackageType = item.getItemPackageTypes().get(0);

        return new Inventory(lpn,  warehouse, location,
                item, itemPackageType,  inventoryStatus,
                quantity);
    }

    private void assertInventory(Long inventoryId, Warehouse warehouse,
                                       Location location, Item item,
                                       Long quantity, InventoryStatus inventoryStatus) {
        // Let's get inventory
        Inventory existingInventory =
                inventoryServiceRestemplateClient.getInventoryById(inventoryId);
        if (Objects.isNull(existingInventory)) {

            throw TestFailException.raiseException("Error while get inventory we just created. ");
        }

        if (!existingInventory.getWarehouseId().equals(warehouse.getId())) {
            throw TestFailException.raiseException("Error creating inventory, warehouse ID doesn't match!" +
                    "expected warehouse id: " + warehouse.getId() + ", " +
                    "actual warehouse id: " + existingInventory.getWarehouseId());
        }

        if (!existingInventory.getLocationId().equals(location.getId())) {
            throw TestFailException.raiseException("Error creating inventory, location ID doesn't match!" +
                    "expected location id: " + location.getId() + ", " +
                    "actual location id: " + existingInventory.getLocationId());
        }

        if (!existingInventory.getItem().getId().equals(item.getId())) {
            throw TestFailException.raiseException("Error creating inventory, item ID doesn't match!" +
                    "expected item id: " + item.getId() + ", " +
                    "actual item id: " + existingInventory.getItem().getId());
        }

        if (!existingInventory.getQuantity().equals(quantity)) {
            throw TestFailException.raiseException("Error creating inventory, quantity doesn't match!" +
                    "expected quantity: " + quantity + ", " +
                    "actual quantity: " + existingInventory.getQuantity());
        }

        if (!existingInventory.getInventoryStatus().getId().equals(inventoryStatus.getId())) {
            throw TestFailException.raiseException("Error creating inventory, inventory status ID doesn't match!" +
                    "expected inventory status id: " + inventoryStatus.getId() + ", " +
                    "actual inventory status id: " + existingInventory.getInventoryStatus().getId());
        }
    }


    private List<Location> assertLocationsEmpty(Warehouse warehouse, String[] locationNames) {
        List<Location> locations = new ArrayList<>();
        Arrays.stream(locationNames).forEach(locationName -> {
            locations.add(assertLocationEmpty(warehouse, locationName));
        });
        return locations;
    }


    private void assertInventoryQuantity(Inventory inventory, long quantity) {
        Inventory existingInventory =
                inventoryServiceRestemplateClient.getInventoryById(inventory.getId());
        if (Objects.isNull(existingInventory)) {

            throw TestFailException.raiseException("Error while get inventory we just created. ");
        }
        if (!existingInventory.getQuantity().equals(quantity)) {
            throw TestFailException.raiseException("Error while verify quantity." +
                    "Expected: " + quantity + ", got: " + existingInventory.getQuantity());

        }
    }

}

/**
 * Copyright 2018
 *
 * @author gzhang
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.garyzhangscm.cwms.inventory.service;


import com.garyzhangscm.cwms.inventory.clients.WarehouseLayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.inventory.exception.GenericException;
import com.garyzhangscm.cwms.inventory.model.*;
import com.garyzhangscm.cwms.inventory.repository.CycleCountRequestRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class CycleCountRequestService{
    private static final Logger logger = LoggerFactory.getLogger(CycleCountRequestService.class);

    @Autowired
    private CycleCountRequestRepository cycleCountRequestRepository;
    @Autowired
    private InventoryService inventoryService;
    @Autowired
    private CycleCountBatchService cycleCountBatchService;
    @Autowired
    private CycleCountResultService cycleCountResultService;
    @Autowired
    private AuditCountRequestService auditCountRequestService;
    @Autowired
    private ItemService itemService;

    @Autowired
    private WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient;

    public CycleCountRequest findById(Long id) {
        return cycleCountRequestRepository.findById(id).orElse(null);
    }
    public boolean exists(Long id) {
        return findById(id) != null;
    }
    @Transactional
    public void delete(Long id) {
        cycleCountRequestRepository.deleteById(id);
    }
    public List<CycleCountRequest> findByBatchId(String batchId) {
        return cycleCountRequestRepository.findByBatchId(batchId);
    }

    public List<CycleCountRequest> getOpenCycleCountRequests(String batchId) {
        return warehouseLayoutServiceRestemplateClient.setupCycleCountRequestLocations(cycleCountRequestRepository.getOpenRequests(batchId));
    }

    public List<CycleCountRequest> getCancelledCycleCountRequests(String batchId) {
        return warehouseLayoutServiceRestemplateClient.setupCycleCountRequestLocations(cycleCountRequestRepository.getCancelledRequests(batchId));
    }

    public CycleCountRequest findOpenCycleCountRequestByLocationId(Long locationId) {
        return cycleCountRequestRepository.findOpenCycleCountRequestByLocationId(locationId);
    }

    @Transactional
    public CycleCountRequest save(CycleCountRequest cycleCountRequest) {
        cycleCountBatchService.createCycleCountBatch(cycleCountRequest.getWarehouseId(), cycleCountRequest.getBatchId());
        return cycleCountRequestRepository.save(cycleCountRequest);
    }

    @Transactional
    public List<CycleCountResult> confirmCycleCountRequests(String cycleCountRequestIds) {

        List<CycleCountRequest> cycleCountRequests =
                Arrays.stream(cycleCountRequestIds.split(","))
                        .map(Long::parseLong)    // Convert id from string to long
                        .map(id -> cycleCountRequestRepository.findById(id).orElse(null))
                .collect(Collectors.toList());

        // For each cycle count request(location based), generate result for each item in the location
        Iterator<CycleCountRequest> cycleCountRequestIterator = cycleCountRequests.iterator();
        List<CycleCountResult> resultCycleCountResult = new ArrayList<>();

        while(cycleCountRequestIterator.hasNext()) {
            CycleCountRequest cycleCountRequest = cycleCountRequestIterator.next();

            // Generate empty cycle count result
            List<CycleCountResult> cycleCountResults = generateEmptyCycleCountResults(cycleCountRequest);

            // confirm with the actual inventory in the location
            resultCycleCountResult.addAll(
                    cycleCountResults.stream()
                      .map(cycleCountResult -> confirmCycleCountRequest(cycleCountResult))
                            .collect(Collectors.toList()));

            // Remove the cycle count request after all the results are confirmed
            cycleCountRequestRepository.deleteById(cycleCountRequest.getId());
        }

        return resultCycleCountResult;
    }

    @Transactional
    private CycleCountResult confirmCycleCountRequest(CycleCountResult cycleCountResult) {
        return confirmCycleCountRequest(cycleCountResult, cycleCountResult.getQuantity());
    }


    @Transactional
    private CycleCountResult confirmCycleCountRequest(CycleCountResult cycleCountResult, Long countQuantity) {
        cycleCountResult.setCountQuantity(countQuantity);
        logger.debug("confirm request with quantity: {}. Inventory Quantity: {}", countQuantity, cycleCountResult.getQuantity());
        if (cycleCountResult.getCountQuantity() != cycleCountResult.getQuantity()) {
            // OK, we will need to generate a new audit count on this location
            AuditCountRequest auditCountRequest = auditCountRequestService.generateAuditCountRequest(cycleCountResult);
            cycleCountResult.setAuditCountRequest(auditCountRequest);
        }
        return cycleCountResultService.save(cycleCountResult);
    }

    @Transactional
    private CycleCountResult confirmCycleCountRequest(Long cycleCountResultId) {
        return confirmCycleCountRequest(cycleCountResultService.findById(cycleCountResultId));
    }


    @Transactional
    private CycleCountResult confirmCycleCountRequest(Long cycleCountResultId, Long countQuantity) {
        return confirmCycleCountRequest(cycleCountResultService.findById(cycleCountResultId), countQuantity);
    }


    @Transactional
    public List<CycleCountResult> saveCycleCountResults(String cycleCountRequestId,
                                                        List<CycleCountResult> cycleCountResults) {
        logger.debug("Start to confirm: {}", cycleCountRequestId);
        cycleCountResults.forEach(cycleCountResult -> logger.debug("result: \n {}", cycleCountResult.toString()));
        CycleCountRequest cycleCountRequest = findById(Long.parseLong(cycleCountRequestId));
        boolean isLocationEmpty = isLocationEmpty(cycleCountRequest.getLocationId());
        boolean isCountAsEmptyLocation = isCountAsEmptyLocation(cycleCountResults);

        List<CycleCountResult> savedCycleCountResults;
        if (isCountAsEmptyLocation) {
            // if we count the location as an empty location, we will
            // rely on the generateEmptyCycleCountResults() function to generate
            // count result for each item in the location and count every item
            // as 0 quantity. If this location is an empty location, generateEmptyCycleCountResults
            // will generate one result structure with no item information and
            // we will count as 0 quantity
            savedCycleCountResults =
                    generateEmptyCycleCountResults(cycleCountRequest)
                            .stream()
                            .map(cycleCountResult ->  confirmCycleCountRequest(
                                    cycleCountResult,
                                    0L)
                            )
                            .collect(Collectors.toList());
        }
        else if (isLocationEmpty) {
            // OK, location is empty but we found inventory in this location
            savedCycleCountResults = cycleCountResults.stream()
                    .map(cycleCountResult -> confirmCycleCountRequest(cycleCountResult, cycleCountResult.getCountQuantity()))
                    .collect(Collectors.toList());
        }
        else{
            // this is an non empty location and we count inventory in this locations.
            List<CycleCountResult> emptyCycleCountResults =
                    generateEmptyCycleCountResults(cycleCountRequest);


            List<CycleCountResult> cycleCountResultsUnion = getCycleCountResultsUnion(
                    cycleCountRequest, emptyCycleCountResults, cycleCountResults);

            savedCycleCountResults = cycleCountResultsUnion.stream()
                    .map(cycleCountResult -> confirmCycleCountRequest(cycleCountResult, cycleCountResult.getCountQuantity()))
                    .collect(Collectors.toList());
        }

        // After we saved all the result, let's remove request
        if (exists(Long.parseLong(cycleCountRequestId))) {
            delete(Long.parseLong(cycleCountRequestId));
        }
        return savedCycleCountResults;
    }

    // Get the union set of the 2 cycle count result list.
    // 1. Each result is a summary result, which means the only key is the item and the value is the quantity of the item
    // 2. If there's inventory of this item exists and the user count the item as well, we will use the quantity that
    //    count by user
    // 3. If there's inventory of this item exists but the user doesn't count the item, the the item is count as 0 quantity
    // 4. If there's no inventory of this item exists in the system but the user count the item, we will use the result
    //    that count by the user
    // inventoryBasedCycleCountResults: summary of the items that already exists in the system
    // userInputCycleCountResults: result count by user
    private List<CycleCountResult> getCycleCountResultsUnion(
                                                             CycleCountRequest cycleCountRequest,
                                                             List<CycleCountResult> inventoryBasedCycleCountResults,
                                                             List<CycleCountResult> userInputCycleCountResults) {

        List<CycleCountResult> cycleCountResultsUnion = new ArrayList<>();
        // summarize the result count by user, group by item
        // Map key: item id
        // Map value: count quantity
        Map<Item, Long> cycleCountResultQuantityMap = new HashMap<>();
        userInputCycleCountResults.forEach(cycleCountResult -> {
            if (cycleCountResult.getItem() == null) {
                // item is not passed in, we will count this as an empty location;
                cycleCountResultQuantityMap.put(null, 0L);
            }
            else  {
                cycleCountResultQuantityMap.put(cycleCountResult.getItem(),
                        cycleCountResultQuantityMap.getOrDefault(cycleCountResult.getItem(), 0L) + cycleCountResult.getCountQuantity());

            }
        });

        // for each inventory based cycle count result, let's see if we can find from
        // the result passed from the client
        for(CycleCountResult cycleCountResult : inventoryBasedCycleCountResults) {
            cycleCountResult.setCountQuantity(
                    cycleCountResultQuantityMap.getOrDefault(cycleCountResult.getItem(), 0L)
            );
            cycleCountResultsUnion.add(cycleCountResult);
            // Remove the saved item from the map
            // so we know that anything left after this loop is
            // 'unexpected' inventory
            cycleCountResultQuantityMap.remove(cycleCountResult.getItem());
        }

        // For each result count by user that still left, we know that there's no
        // correspondent inventory in the system.
        if (cycleCountResultQuantityMap.size() > 0) {
            // OK, we have some unexpected inventory that
            // 1. not exists in system
            // 2. but user count it with quantity
            for(Map.Entry<Item, Long> entry : cycleCountResultQuantityMap.entrySet()) {
                if (entry.getValue() > 0) {
                    CycleCountResult unexpctedInventoryBeingCount = new CycleCountResult();
                    unexpctedInventoryBeingCount.setItem(entry.getKey());
                    unexpctedInventoryBeingCount.setBatchId(cycleCountRequest.getBatchId());
                    unexpctedInventoryBeingCount.setLocationId(cycleCountRequest.getLocationId());
                    unexpctedInventoryBeingCount.setLocation(cycleCountRequest.getLocation());
                    unexpctedInventoryBeingCount.setQuantity(0L);
                    unexpctedInventoryBeingCount.setCountQuantity(entry.getValue());
                    unexpctedInventoryBeingCount.setWarehouseId(cycleCountRequest.getWarehouseId());
                    cycleCountResultsUnion.add(unexpctedInventoryBeingCount);
                }
            }
        }
        return cycleCountResultsUnion;

    }
    private boolean isLocationEmpty(Long locationId) {
        return inventoryService.findByLocationId(locationId).size() == 0;
    }
    private boolean isCountAsEmptyLocation(List<CycleCountResult> cycleCountResults) {
        for(CycleCountResult cycleCountResult : cycleCountResults) {
            if (cycleCountResult.getItem() != null) {
                return false;
            }
        }
        return true;
    }


    // create empty result structure
    // quantity will be the expected quantity. actual count quantity is 0
    @Transactional
    public List<CycleCountResult> generateEmptyCycleCountResults(CycleCountRequest cycleCountRequest) {
        // Get all inventory from the location
        List<Inventory> inventories = inventoryService.findByLocationId(cycleCountRequest.getLocationId());

        if (inventories.size() == 0) {
            // this is an empty location, let's count with 0 quantity
            return warehouseLayoutServiceRestemplateClient.setupCycleCountResultLocations(
                    Arrays.asList(
                            new CycleCountResult[]{CycleCountResult.cycleCountResultForEmptyLocation(cycleCountRequest)}
                            )
            );
        }

        Map<Item, Long> inventorySummaries = new HashMap<>();
        inventories.stream().forEach(
                inventory ->
                        inventorySummaries.put(inventory.getItem(),
                                               inventorySummaries.getOrDefault(inventory.getItem(), 0L) + inventory.getQuantity()
                                              )
                );

        return warehouseLayoutServiceRestemplateClient.setupCycleCountResultLocations(
                        inventorySummaries.entrySet().stream().map(
                            mapEntry -> new CycleCountResult(cycleCountRequest, mapEntry.getKey(), mapEntry.getValue()))
                            .collect(Collectors.toList())
        );
    }

    @Transactional
    public List<CycleCountRequest> cancelCycleCountRequests(String cycleCountRequestIds) {
        return Arrays.stream(cycleCountRequestIds.split(","))
                .map(Long::parseLong)
                .map(id -> cycleCountRequestRepository.findById(id).orElse(null))
                .map(cycleCountRequest -> cancelCycleCountRequest(cycleCountRequest))
                .map(cycleCountRequest -> save(cycleCountRequest))
                .collect(Collectors.toList());
    }

    @Transactional
    public CycleCountRequest cancelCycleCountRequest(CycleCountRequest cycleCountRequest) {
        cycleCountRequest.setStatus(CycleCountRequestStatus.CANCELLED);
        return cycleCountRequest;
    }

    @Transactional
    public List<CycleCountRequest> reopenCancelledCycleCountRequests(String cycleCountRequestIds) {
        return Arrays.stream(cycleCountRequestIds.split(","))
                .map(Long::parseLong)
                .map(id -> cycleCountRequestRepository.findById(id).orElse(null))
                .map(cycleCountRequest -> reopenCancelledCycleCountRequest(cycleCountRequest))
                .map(cycleCountRequest -> save(cycleCountRequest))
                .collect(Collectors.toList());
    }

    @Transactional
    public CycleCountRequest reopenCancelledCycleCountRequest(CycleCountRequest cycleCountRequest) {
        // if we have outstanding cycle count request or audit count in the same location,
        // let's ignore this request
        if (findOpenCycleCountRequestByLocationId(cycleCountRequest.getLocationId()) == null &&
                auditCountRequestService.findByLocationId(cycleCountRequest.getLocationId()) == null) {

            cycleCountRequest.setStatus(CycleCountRequestStatus.OPEN);
            return cycleCountRequest;
        }
        else {

            return cycleCountRequest;
        }
    }

    public List<CycleCountResult> getInventorySummariesForCount(Long cycleCountRequestId) {
        return generateEmptyCycleCountResults(cycleCountRequestRepository.findById(cycleCountRequestId).orElse(null));
    }

    public List<CycleCountResult> getInventorySummariesForCount(String cycleCountRequestIds) {
        return Arrays.stream(cycleCountRequestIds.split(","))
                .map(Long::parseLong)
                .map(id -> cycleCountRequestRepository.findById(id).orElse(null))
                .map(cycleCountRequest -> getInventorySummariesForCount(cycleCountRequest.getId()))
                .flatMap(List::stream)
                .collect(Collectors.toList());

    }

    @Transactional
    public List<CycleCountRequest> generateCycleCountRequest(
            String batchId, CycleCountRequestType cycleCountRequestType,
            Long warehouseId, String beginValue, String endValue,
            Boolean includeEmptyLocation) {
        List<Location> locations = getCycleCountRequestLocations(cycleCountRequestType, warehouseId, beginValue, endValue, includeEmptyLocation);
        logger.debug("Get {} potential locations for cycle count request",
                locations.size());
        return locations.stream()
                // only return those locations that doesn't have any count request or audit request
                .filter(location ->
                        findOpenCycleCountRequestByLocationId(location.getId()) == null &&
                                auditCountRequestService.findByLocationId(location.getId()) == null)
                .map(location -> {
                    logger.debug("create new cycle count request {} for location {}, warehouse id: {}",
                            batchId, location.getName(), warehouseId);
                    return new CycleCountRequest(warehouseId, batchId, location);
                })
                .map(cycleCountRequest -> save(cycleCountRequest))
                .collect(Collectors.toList());
    }


    private List<Location> getCycleCountRequestLocations(
            CycleCountRequestType cycleCountRequestType, Long warehouseId, String beginValue, String endValue, Boolean includeEmptyLocation) {
        switch (cycleCountRequestType) {
            case BY_LOCATION_RANGE:
                return getCycleCountRequestLocationsByLocationRange(warehouseId, beginValue, endValue, includeEmptyLocation);
            case BY_ITEM_RANGE:
                return getCycleCountRequestLocationsByItemRange(beginValue, endValue, includeEmptyLocation);
            case BY_AISLE_RANGE:
                return getCycleCountRequestLocationsByAisleRange(beginValue, endValue, includeEmptyLocation);
            default:
                return getCycleCountRequestLocationsByLocationRange(warehouseId, beginValue, endValue, includeEmptyLocation);
        }
    }

    private List<Location> getCycleCountRequestLocationsByLocationRange(Long warehouseId, String beginValue, String endValue, Boolean includeEmptyLocation) {
        if (beginValue.isEmpty() && endValue.isEmpty()) {
            return new ArrayList<>();
        }
        else if (beginValue.isEmpty()) {
            Location location = warehouseLayoutServiceRestemplateClient.getLocationByName(warehouseId, endValue);
            if (location.isEmpty() && !includeEmptyLocation) {
                // current location is empty but we don't want to count empty location,
                // don't return this location
                return new ArrayList<>();
            }
            return Arrays.asList(new Location[]{location});
        }
        else if (endValue.isEmpty()) {
            Location location = warehouseLayoutServiceRestemplateClient.getLocationByName(warehouseId, beginValue);
            if (location.isEmpty() && !includeEmptyLocation) {
                // current location is empty but we don't want to count empty location,
                // don't return this location
                return new ArrayList<>();
            }
            return Arrays.asList(new Location[]{location});
        }
        else {
            Location beginLocation = warehouseLayoutServiceRestemplateClient.getLocationByName(warehouseId, beginValue);
            if (beginLocation == null) {
                throw new GenericException(10000, "can't find the begin location by value: " + beginValue);
            }
            Location endLocation = warehouseLayoutServiceRestemplateClient.getLocationByName(warehouseId, endValue);
            if (endLocation == null) {
                throw new GenericException(10000, "can't find the end location by value: " + endValue);
            }

            // Ok we got begin location and end location, let's get all locations between the begin
            // location and end location, including both begin location and end location
            return warehouseLayoutServiceRestemplateClient.getLocationsByRange(
                    warehouseId,
                    beginLocation.getCountSequence(),
                    endLocation.getCountSequence(),
                    "count", includeEmptyLocation);
        }
    }
    private List<Location> getCycleCountRequestLocationsByItemRange(String beginValue, String endValue, Boolean includeEmptyLocation) {return new ArrayList<>();}
    private List<Location> getCycleCountRequestLocationsByAisleRange(String beginValue, String endValue, Boolean includeEmptyLocation) {
        if (beginValue.isEmpty() && endValue.isEmpty()) {
            return new ArrayList<>();
        } else if (beginValue.isEmpty()) {
            return warehouseLayoutServiceRestemplateClient.getLocationByAisle(endValue);
        } else if (endValue.isEmpty()) {
            return warehouseLayoutServiceRestemplateClient.getLocationByAisle(beginValue);
        } else {
            return warehouseLayoutServiceRestemplateClient.getLocationByAisleRange(beginValue, endValue);
        }
    }



}


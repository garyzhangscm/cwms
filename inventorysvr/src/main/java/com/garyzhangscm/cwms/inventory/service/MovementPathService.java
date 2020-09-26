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

import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.garyzhangscm.cwms.inventory.clients.InboundServiceRestemplateClient;
import com.garyzhangscm.cwms.inventory.clients.WarehouseLayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.inventory.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.inventory.model.*;
import com.garyzhangscm.cwms.inventory.repository.MovementPathRepository;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import javax.persistence.criteria.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class MovementPathService implements TestDataInitiableService{
    private static final Logger logger = LoggerFactory.getLogger(MovementPathService.class);

    @Autowired
    private MovementPathRepository movementPathRepository;


    @Autowired
    private WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient;
    @Autowired
    private InboundServiceRestemplateClient inboundServiceRestemplateClient;

    @Autowired
    private FileService fileService;

    @Value("${fileupload.test-data.movement-paths:movement-paths}")
    String testDataFile;

    public MovementPath findById(Long id) {
        return findById(id, true);
    }
    public MovementPath findById(Long id, boolean includeDetails) {
        MovementPath movementPath = movementPathRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("movement path not found by id: " + id));
        if (includeDetails) {
            loadMovementPathAttribute(movementPath);
        }
        return movementPath;
    }

    public List<MovementPath> findAll() {
        return findAll(true);
    }

    public List<MovementPath> findAll(boolean includeDetails) {

        // Only return actual inventory
        List<MovementPath> movementPaths = movementPathRepository.findAll();
        if (includeDetails && movementPaths.size() > 0) {
            loadMovementPathAttribute(movementPaths);
        }
        return movementPaths;
    }

    public MovementPath save(MovementPath movementPath) {
        return movementPathRepository.save(movementPath);
    }

    public MovementPath findByNaturalKeys(MovementPath movementPath) {
        List<MovementPath> movementPaths = findAll(movementPath.getWarehouseId(),
                movementPath.getFromLocationId(),
                movementPath.getFromLocationGroupId(), movementPath.getToLocationId(),
                movementPath.getToLocationGroupId());
        if (movementPaths.size() > 0) {
            return movementPaths.get(0);
        }
        else {
            return null;
        }
    }

    public List<MovementPath> findAll(Long warehouseId,
                                      Long fromLocationId,
                                      Long fromLocationGroupId,
                                      Long toLocationId,
                                      Long toLocationGroupId) {
        return findAll(warehouseId, fromLocationId, "", fromLocationGroupId, toLocationId, "", toLocationGroupId, true);
    }

    public List<MovementPath> findAll(Long warehouseId,
                                      Long fromLocationId,
                                      String fromLocationName,
                                      Long fromLocationGroupId,
                                      Long toLocationId,
                                      String toLocationName,
                                      Long toLocationGroupId) {
        return findAll(warehouseId, fromLocationId, fromLocationName, fromLocationGroupId, toLocationId, toLocationName, toLocationGroupId, true);
    }

    public List<MovementPath> findAll(Long warehouseId,
                                      Long fromLocationId,
                                      String fromLocationName,
                                      Long fromLocationGroupId,
                                      Long toLocationId,
                                      String toLocationName,
                                      Long toLocationGroupId, boolean includeDetails) {

        List<MovementPath> movementPaths =  movementPathRepository.findAll(
                (Root<MovementPath> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();
                    if (fromLocationId != null) {
                        predicates.add(criteriaBuilder.equal(root.get("fromLocationId"), fromLocationId));
                    }
                    else if (warehouseId != null
                              && !StringUtils.isBlank(fromLocationName)) {
                        Location location =
                                warehouseLayoutServiceRestemplateClient.getLocationByName(warehouseId, fromLocationName);
                        if (location != null) {
                            predicates.add(criteriaBuilder.equal(root.get("fromLocationId"), location.getId()));
                        }

                    }
                    if (fromLocationGroupId != null) {
                        predicates.add(criteriaBuilder.equal(root.get("fromLocationGroupId"), fromLocationGroupId));
                    }
                    if (toLocationId != null) {
                        predicates.add(criteriaBuilder.equal(root.get("toLocationId"), toLocationId));
                    }
                    else if (warehouseId != null &&
                             !StringUtils.isBlank(toLocationName)) {
                        Location location = warehouseLayoutServiceRestemplateClient.getLocationByName(warehouseId, toLocationName);
                        if (location != null) {
                            predicates.add(criteriaBuilder.equal(root.get("toLocationId"), location.getId()));
                        }

                    }
                    if (toLocationGroupId != null) {
                        predicates.add(criteriaBuilder.equal(root.get("toLocationGroupId"), toLocationGroupId));
                    }


                    Predicate[] p = new Predicate[predicates.size()];
                    return criteriaBuilder.and(predicates.toArray(p));
                }
        );

        if (movementPaths.size() > 0 && includeDetails) {
            loadMovementPathAttribute(movementPaths);
        }
        return movementPaths;
    }

    /**
     * Get matched movement path based upon the from / to location or location group
     * We will only get by 3 pairs of parameters
     * 1. from / to location id
     * 2. from / to location name
     * 3. from / to location group
     * If both location(either id pair or name pair) and location group are passed in, then
     * we will try both criteria and group the result together for returnning
     * @param warehouseId
     * @param fromLocationId
     * @param fromLocationName
     * @param fromLocationGroupId
     * @param toLocationId
     * @param toLocationName
     * @param toLocationGroupId
     * @return
     */
    public List<MovementPath> findMatchedMovementPaths(Long warehouseId,
                                                       Long fromLocationId,
                             String fromLocationName,
                             Long fromLocationGroupId,
                             Long toLocationId,
                             String toLocationName,
                             Long toLocationGroupId) {
        logger.debug("Will try to find movement path by \n" +
                "from / to location id: {} / {} \n " +
                "from / to location name: {} / {} \n " +
                "from / to location group id: {} / {} ",
                fromLocationId, toLocationId,
                fromLocationName, toLocationName,
                fromLocationGroupId, toLocationGroupId);
        Set<MovementPath> matchedMovementPathSet = new HashSet<>();
        if (fromLocationId != null && toLocationId != null) {
            List<MovementPath> matchedMovementPath = findAll(warehouseId,
                    fromLocationId, "", null,
                    toLocationId, "", null);
            if (matchedMovementPath.size() > 0) {
                matchedMovementPathSet.addAll(matchedMovementPath);
            }
        }
        else if (!StringUtils.isBlank(fromLocationName) && !StringUtils.isBlank(toLocationName)) {
            List<MovementPath> matchedMovementPath = findAll(warehouseId,
                    null, fromLocationName, null,
                    null, toLocationName, null);
            if (matchedMovementPath.size() > 0) {
                matchedMovementPathSet.addAll(matchedMovementPath);
            }
        }

        if (fromLocationGroupId != null && toLocationGroupId != null) {
            List<MovementPath> matchedMovementPath = findAll(warehouseId,
                    null, "", fromLocationGroupId,
                    null, "", toLocationGroupId);
            if (matchedMovementPath.size() > 0) {
                matchedMovementPathSet.addAll(matchedMovementPath);
            }
        }

        // Sort the result by the movement path sequence
        List<MovementPath> matchedMovementPathList = new ArrayList<>(matchedMovementPathSet);
        matchedMovementPathList.sort(Comparator.comparing(MovementPath::getSequence));
        return matchedMovementPathList;

    }
    public MovementPath saveOrUpdate(MovementPath movementPath) {
        if (movementPath.getId() == null && findByNaturalKeys(movementPath) != null) {
            movementPath.setId(findByNaturalKeys(movementPath).getId());
        }
        return save(movementPath);
    }
    public void delete(MovementPath movementPath) {
        movementPathRepository.delete(movementPath);
    }
    public void delete(Long id) {
        movementPathRepository.deleteById(id);
    }
    public void delete(String movementPathIds) {
        if (!StringUtils.isBlank(movementPathIds)) {
            long[] movementPathIdArray = Arrays.asList(movementPathIds.split(",")).stream().mapToLong(Long::parseLong).toArray();
            for(long id : movementPathIdArray) {
                delete(id);
            }
        }
    }

    public void loadMovementPathAttribute(List<MovementPath> movementPaths) {
        for(MovementPath movementPath : movementPaths) {
            loadMovementPathAttribute(movementPath);
        }
    }

    public void loadMovementPathAttribute(MovementPath movementPath) {

        // Load from / to location
        if (movementPath.getFromLocationId() != null && movementPath.getFromLocation() == null) {
            movementPath.setFromLocation(warehouseLayoutServiceRestemplateClient.getLocationById(movementPath.getFromLocationId()));
        }
        if (movementPath.getToLocationId() != null && movementPath.getToLocation() == null) {
            movementPath.setToLocation(warehouseLayoutServiceRestemplateClient.getLocationById(movementPath.getToLocationId()));
        }
        // load from / to location group
        if (movementPath.getFromLocationGroupId() != null && movementPath.getFromLocationGroup() == null) {
            movementPath.setFromLocationGroup(warehouseLayoutServiceRestemplateClient.getLocationGroupById(movementPath.getFromLocationGroupId()));
        }
        if (movementPath.getToLocationGroupId() != null && movementPath.getToLocationGroup() == null) {
            movementPath.setToLocationGroup(warehouseLayoutServiceRestemplateClient.getLocationGroupById(movementPath.getToLocationGroupId()));
        }

        // load details for each movement path details
        if(movementPath.getMovementPathDetails().size() > 0) {
            movementPath.getMovementPathDetails().forEach(
                    movementPathDetail -> {
                        if (movementPathDetail.getHopLocationId() != null && movementPathDetail.getHopLocation() == null) {
                            movementPathDetail.setHopLocation(warehouseLayoutServiceRestemplateClient.getLocationById(movementPathDetail.getHopLocationId()));
                        }
                        if (movementPathDetail.getHopLocationGroupId() != null && movementPathDetail.getHopLocationGroup() == null) {
                            movementPathDetail.setHopLocationGroup(warehouseLayoutServiceRestemplateClient.getLocationGroupById(movementPathDetail.getHopLocationGroupId()));
                        }
                    }
            );
        }
    }


    public List<MovementPathCSVWrapper> loadData(File file) throws IOException {

        CsvSchema schema = CsvSchema.builder().
                addColumn("company").
                addColumn("warehouse").
                addColumn("fromLocation").
                addColumn("toLocation").
                addColumn("fromLocationGroup").
                addColumn("toLocationGroup").
                addColumn("sequence").
                addColumn("hopLocation").
                addColumn("hopLocationGroup").
                addColumn("strategy").
                build().withHeader();
        return fileService.loadData(file, schema, MovementPathCSVWrapper.class);
    }


    public List<MovementPathCSVWrapper> loadData(InputStream inputStream) throws IOException {

        CsvSchema schema = CsvSchema.builder().
                addColumn("company").
                addColumn("warehouse").
                addColumn("fromLocation").
                addColumn("toLocation").
                addColumn("fromLocationGroup").
                addColumn("toLocationGroup").
                addColumn("sequence").
                addColumn("hopLocation").
                addColumn("hopLocationGroup").
                addColumn("strategy").
                build().withHeader();

        return fileService.loadData(inputStream, schema, MovementPathCSVWrapper.class);
    }

    public void initTestData(String warehouseName) {
        try {
            String testDataFileName = StringUtils.isBlank(warehouseName) ?
                    testDataFile + ".csv" :
                    testDataFile + "-" + warehouseName + ".csv";
            InputStream inputStream = new ClassPathResource(testDataFileName).getInputStream();
            List<MovementPathCSVWrapper> movementPathCSVWrappers = loadData(inputStream);

            logger.debug("movementPathCSVWrappers:\n " + movementPathCSVWrappers);

            List<MovementPath> movementPaths = convertFromWrapper(movementPathCSVWrappers);
            logger.debug("movementPaths:\n " + movementPaths);
            movementPaths.forEach(movementPath -> saveOrUpdate(movementPath));
        } catch (IOException ex) {
            logger.debug("Exception while load test data: {}", ex.getMessage());
        }
    }

    private List<MovementPath> convertFromWrapper(List<MovementPathCSVWrapper> movementPathCSVWrappers) {
        Map<String, MovementPath> movementPathMap = new HashMap<>();
        for(MovementPathCSVWrapper movementPathCSVWrapper : movementPathCSVWrappers) {
            String key = movementPathCSVWrapper.getFromLocation() + "-" +
                    movementPathCSVWrapper.getFromLocationGroup() + "-" +
                    movementPathCSVWrapper.getToLocation() + "-" +
                    movementPathCSVWrapper.getToLocationGroup();
            MovementPath movementPath;
            if (movementPathMap.containsKey(key)) {
                movementPath = movementPathMap.get(key);
            } else {
                movementPath = new MovementPath();

                // warehouse
                Warehouse warehouse =
                            warehouseLayoutServiceRestemplateClient.getWarehouseByName(
                                    movementPathCSVWrapper.getCompany(),
                                    movementPathCSVWrapper.getWarehouse());


                movementPath.setWarehouseId(warehouse.getId());

                if (!StringUtils.isBlank(movementPathCSVWrapper.getFromLocation())) {
                    movementPath.setFromLocationId(
                            warehouseLayoutServiceRestemplateClient.getLocationByName(
                                    warehouse.getId(), movementPathCSVWrapper.getFromLocation()).getId());
                }
                if (!StringUtils.isBlank(movementPathCSVWrapper.getFromLocationGroup())) {
                    movementPath.setFromLocationGroupId(
                            warehouseLayoutServiceRestemplateClient.getLocationGroupByName(
                                    warehouse.getId(), movementPathCSVWrapper.getFromLocationGroup()).getId());
                }
                if (!StringUtils.isBlank(movementPathCSVWrapper.getToLocation())) {
                    movementPath.setToLocationId(
                            warehouseLayoutServiceRestemplateClient.getLocationByName(
                                    warehouse.getId(), movementPathCSVWrapper.getToLocation()).getId());
                }
                if (!StringUtils.isBlank(movementPathCSVWrapper.getToLocationGroup())) {
                    movementPath.setToLocationGroupId(
                            warehouseLayoutServiceRestemplateClient.getLocationGroupByName(
                                    warehouse.getId(), movementPathCSVWrapper.getToLocationGroup()).getId());
                }
                movementPath.setSequence(Integer.parseInt(movementPathCSVWrapper.getSequence()));
                movementPathMap.put(key, movementPath);

            }
            // Ok, we already got the movement path, let's fill in the details
            MovementPathDetail movementPathDetail = new MovementPathDetail();
            movementPathDetail.setMovementPath(movementPath);
            movementPathDetail.setSequence(Integer.parseInt(movementPathCSVWrapper.getSequence()));
            movementPathDetail.setStrategy(MovementPathStrategy.valueOf(movementPathCSVWrapper.getStrategy()));

            // warehouse
            if (!StringUtils.isBlank(movementPathCSVWrapper.getWarehouse())) {
                Warehouse warehouse = warehouseLayoutServiceRestemplateClient.getWarehouseByName(
                        movementPathCSVWrapper.getCompany(),
                        movementPathCSVWrapper.getWarehouse());
                if (warehouse != null) {
                    movementPathDetail.setWarehouseId(warehouse.getId());
                }
            }

            if (!StringUtils.isBlank(movementPathCSVWrapper.getHopLocation())) {
                movementPathDetail.setHopLocationId(
                        warehouseLayoutServiceRestemplateClient.getLocationByName(
                                movementPath.getWarehouseId(), movementPathCSVWrapper.getHopLocation()).getId());
            }
            if (!StringUtils.isBlank(movementPathCSVWrapper.getHopLocationGroup())) {
                movementPathDetail.setHopLocationGroupId(
                        warehouseLayoutServiceRestemplateClient.getLocationGroupByName(
                                movementPath.getWarehouseId(), movementPathCSVWrapper.getHopLocationGroup()).getId());
            }
            movementPath.getMovementPathDetails().add(movementPathDetail);
        }

        return new ArrayList<>(movementPathMap.values());
    }

    public List<Location> reserveHopLocations(Long fromLocationId, Long toLocationId, Inventory inventory) {
        logger.debug("Start to reserve hop locations for movement [{}, {}], lpn: {}", fromLocationId, toLocationId, inventory.getLpn());
        List<Location> hopLocations = getHopLocations(fromLocationId, toLocationId, inventory);

        logger.debug("We will hop through those locations: \n {}", hopLocations);
        // we already reserved the location in the function getHopLocations
        /********
        hopLocations.forEach(location ->
                warehouseLayoutServiceRestemplateClient.reserveLocation(location.getId(),location.getReservedCode(),
                                                    inventory.getSize(), inventory.getQuantity(), 1));
         ****/
        return hopLocations;
    }

    public List<Location> getHopLocations(Long fromLocationId, Long toLocationId, Inventory inventory) {
        logger.debug("Start to get hop locations for movement [{}, {}], lpn: {}", fromLocationId, toLocationId, inventory.getLpn());
        List<MovementPath> movementPaths = getMatchedMovementPath(fromLocationId, toLocationId);
        logger.debug("get {} movement path configured for this movement", movementPaths.size());

        // Loop through each movement path definition until we get a viable path to move the inventory from
        // begin to the end
        for(MovementPath movementPath : movementPaths) {
            logger.debug("Test movement path: {}", movementPath.getId());
            List<Location> hopLocations = getHopLocations(movementPath, inventory);
            if (hopLocations.size() > 0) {
                logger.debug(">> We found it! Totally there will be {} hop locations ", hopLocations.size());
                // OK, we find a solution for this movement path, let's return
                return hopLocations;
            }
        }
        return new ArrayList<>();
    }

    private List<Location> getHopLocations(MovementPath movementPath, Inventory inventory) {
        // Let's loop through all hop location / location groups that defined by the movement path
        // and find a location by the strategy

        List<Location> hopLocations = new ArrayList<>();
        // Load all the details of the movement path
        loadMovementPathAttribute(movementPath);
        // Sort the hop locatin / groups by sequence
        movementPath.getMovementPathDetails().sort(Comparator.comparingInt(MovementPathDetail::getSequence));
        logger.debug("Start to get hop locations with movement path configuration {}", movementPath.getId());
        for(MovementPathDetail movementPathDetail : movementPath.getMovementPathDetails()) {

            logger.debug("Try movement path detail {} / sequence {}", movementPathDetail.getId(), movementPathDetail.getSequence());
            if (movementPathDetail.getHopLocation() != null) {
                // OK, we defined by location, so the location become our hop location. But
                // we will make sure if we can reserve the location
                logger.debug(">> Movement path detail defined by location: {}. See if we can reserve it ",
                        movementPathDetail.getHopLocation().getName());
                if (reserveLocationForMovement(movementPathDetail.getHopLocation(), inventory, movementPathDetail.getStrategy())) {

                    logger.debug(">> we are ok to reseve by location: {}. ",
                            movementPathDetail.getHopLocation().getName());
                    hopLocations.add(movementPathDetail.getHopLocation());
                }
            }
            else if (movementPathDetail.getHopLocationGroup() != null) {
                // OK, we defined by location group, let's see if we can find a suitable location

                logger.debug(">> Movement path detail defined by location group : {}. See if we can find any location in this group to reserve",
                        movementPathDetail.getHopLocationGroup().getName());
                Location location = reserveLocationForMovement(movementPathDetail.getHopLocationGroup(), inventory, movementPathDetail.getStrategy());
                if (location != null) {

                    logger.debug(">> Get a suitable location : {} from group {} to reserve",
                            location.getName(), movementPathDetail.getHopLocationGroup().getName());
                    hopLocations.add(location);
                }
                else {
                    // No location suitable, let's return empty list from this funciton
                    // so we know this movement path configuration fails
                    return new ArrayList<>();
                }

            }
            else {
                // Either location or location group needs to be defined for the movement path configuration.
                // If we are here, it means the configuration itself is wrong, let's return empty list
                // to indicate we won't use this configuration
                return new ArrayList<>();
            }
        }

        return hopLocations;
    }

    private boolean reserveLocationForMovement(Location location, Inventory inventory, MovementPathStrategy movementPathStrategy) {

        String reservedCode = getReservedCode(inventory, movementPathStrategy);
        logger.debug("Get reserved code {} by strategy {}", reservedCode, movementPathStrategy);
        return reserveLocationForMovement(location, reservedCode);
    }
    private boolean reserveLocationForMovement(Location location, String reservedCode) {
        logger.debug("See if we can reserve this location: {}", location.getName() );
        if (StringUtils.isBlank(location.getReservedCode())) {
            // OK, we are good to reserve the location as long as it is not
            // reserved by other resource yet
            location.setReservedCode(reservedCode);
            logger.debug(">> We are OK to reserve location and will update the reserved code to {}", reservedCode);
            return true;
        }
        else if (location.getReservedCode().equals(reservedCode)) {
            // OK, the location is already reserved by same resource code
            // we are good to use this
            logger.debug(">> We are OK to reserve location and will keep the original reserved code {}", reservedCode);
            return true;
        }
        else {
            return false;
        }

    }
    private Location reserveLocationForMovement(LocationGroup locationGroup, Inventory inventory, MovementPathStrategy movementPathStrategy) {

        String reservedCode = getReservedCode(inventory, movementPathStrategy);

        return warehouseLayoutServiceRestemplateClient.reserveLocationFromGroup(
                locationGroup.getId(), reservedCode, inventory.getSize(), inventory.getQuantity(), 1);

    }

    private String getReservedCode(Inventory inventory, MovementPathStrategy movementPathStrategy) {
        switch (movementPathStrategy) {
            case BY_RECEIPT:
                return inboundServiceRestemplateClient.getReceiptById(inventory.getReceiptId()).getNumber();
            case BY_SUPPLIER:
                return inboundServiceRestemplateClient.getReceiptById(inventory.getReceiptId()).getSupplier().getName();
            case BY_ITEM_FAMILY:
                return inventory.getItem().getItemFamily().getName();
            default:
                return "";

        }
    }

    private List<MovementPath> getMatchedMovementPath(Long fromLocationId, Long toLocationId) {
        LocationGroup fromLocationGroup = warehouseLayoutServiceRestemplateClient.getLocationById(fromLocationId).getLocationGroup();
        LocationGroup toLocationGroup = warehouseLayoutServiceRestemplateClient.getLocationById(toLocationId).getLocationGroup();

        return findAll().stream()
                .filter(movementPath -> match(fromLocationId, toLocationId, fromLocationGroup, toLocationGroup, movementPath)).collect(Collectors.toList());

    }

    private boolean match(Long fromLocationId, Long toLocationId, LocationGroup fromLocationGroup, LocationGroup toLocationGroup, MovementPath movementPath) {
        // From location is defined for the movement path but doesn't match with the from location passed in
        // fail
        if (movementPath.getFromLocationId() != null && movementPath.getFromLocationGroupId() != fromLocationId) {
            return false;
        }
        // From location group  is defined for the movement path but doesn't match with the from location group passed in
        // fail
        if (movementPath.getFromLocationGroupId() != null && movementPath.getFromLocationGroupId() != fromLocationGroup.getId()) {
            return false;
        }
        // To location is defined for the movement path but doesn't match with the to location passed in
        // fail
        if (movementPath.getToLocationId() != null && movementPath.getToLocationId() != toLocationId) {
            return false;
        }
        // To location is defined for the movement path but doesn't match with the to location passed in
        // fail
        if (movementPath.getToLocationGroupId() != null && movementPath.getToLocationGroupId() != toLocationGroup.getId()) {
            return false;
        }
        return  true;
    }


    public void removeMovementPaths(String movementPathIds) {

        if (!movementPathIds.isEmpty()) {
            long[] movementPathIdArray = Arrays.asList(movementPathIds.split(",")).stream().mapToLong(Long::parseLong).toArray();
            for(long id : movementPathIdArray) {
                delete(id);
            }
        }
    }

    private Long getWarehouseId(String companyCode, String warehouseName) {
        Warehouse warehouse = warehouseLayoutServiceRestemplateClient.getWarehouseByName(companyCode, warehouseName);
        if (warehouse == null) {
            return null;
        }
        else {
            return warehouse.getId();
        }
    }

}

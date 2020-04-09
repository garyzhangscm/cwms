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

package com.garyzhangscm.cwms.layout.service;

import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.garyzhangscm.cwms.layout.exception.GenericException;
import com.garyzhangscm.cwms.layout.clients.CommonServiceRestemplateClient;
import com.garyzhangscm.cwms.layout.exception.LocationOperationException;
import com.garyzhangscm.cwms.layout.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.layout.model.*;
import com.garyzhangscm.cwms.layout.repository.LocationRepository;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import javax.persistence.criteria.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class LocationService implements TestDataInitiableService {

    private static final Logger logger = LoggerFactory.getLogger(LocationService.class);
    @Autowired
    private LocationRepository locationRepository;

    @Autowired
    private LocationGroupService locationGroupService;
    @Autowired
    private WarehouseService warehouseService;
    @Autowired
    private FileService fileService;

    @Value("${fileupload.test-data.locations:locations}")
    String testDataFile;

    @Autowired
    private CommonServiceRestemplateClient commonServiceRestemplateClient;

    public Location findById(Long id) {
        return locationRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("location not found by id: " + id));
    }

    public List<Location> findAll() {

        return locationRepository.findAll();
    }

    public List<Location> findAll(Long warehouseId, String locationGroupTypeIds,
                                  String locationGroupIds,
                                  String name,
                                  Long beginSequence,
                                  Long endSequence,
                                  String sequenceType,
                                  Boolean includeEmptyLocation,
                                  Boolean emptyLocationOnly,
                                  Double minEmptyCapacity,
                                  Boolean pickableLocationOnly,
                                  Boolean includeDisabledLocation) {

        return locationRepository.findAll(
            (Root<Location> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                List<Predicate> predicates = new ArrayList<Predicate>();
                if (!locationGroupTypeIds.isEmpty()) {

                    Join<Location, LocationGroup> joinLocationGroup = root.join("locationGroup", JoinType.INNER);
                    Join<LocationGroup, LocationGroupType> joinLocationGroupType = joinLocationGroup.join("locationGroupType", JoinType.INNER);

                    if (!locationGroupIds.isEmpty()) {

                        CriteriaBuilder.In<Long> inLocationGroupIds = criteriaBuilder.in(joinLocationGroup.get("id"));
                        for(String id : locationGroupIds.split(",")) {
                            inLocationGroupIds.value(Long.parseLong(id));
                        }
                        predicates.add(criteriaBuilder.and(inLocationGroupIds));
                    }

                    CriteriaBuilder.In<Long> inLocationGroupTypeIds = criteriaBuilder.in(joinLocationGroupType.get("id"));
                    for(String id : locationGroupTypeIds.split(",")) {
                        inLocationGroupTypeIds.value(Long.parseLong(id));
                    }
                    predicates.add(criteriaBuilder.and(inLocationGroupTypeIds));
                }
                else if (!locationGroupIds.isEmpty()) {

                    Join<Location, LocationGroup> joinLocationGroup = root.join("locationGroup", JoinType.INNER);
                    CriteriaBuilder.In<Long> in = criteriaBuilder.in(joinLocationGroup.get("id"));
                    for(String id : locationGroupIds.split(",")) {
                        in.value(Long.parseLong(id));
                    }
                    predicates.add(criteriaBuilder.and(in));
                }

                if (!name.isEmpty()) {
                    predicates.add(criteriaBuilder.equal(root.get("name"), name));
                }
                if (beginSequence != null  && endSequence != null) {
                    if (sequenceType.equals("count")) {

                        predicates.add(criteriaBuilder.between(root.get("countSequence"), beginSequence, endSequence));
                    }
                    else if (sequenceType.equals("putaway")) {
                        predicates.add(criteriaBuilder.between(root.get("putawaySequence"), beginSequence, endSequence));
                    }
                    else if (sequenceType.equals("pick")) {
                        predicates.add(criteriaBuilder.between(root.get("pickSequence"), beginSequence, endSequence));
                    }
                }
                if (!includeEmptyLocation) {
                    Expression<Double> totalVolume = criteriaBuilder.sum(root.get("currentVolume"), root.get("pendingVolume"));

                    predicates.add(criteriaBuilder.greaterThan(totalVolume, 0.0));
                }
                if (emptyLocationOnly == true) {
                    Expression<Double> totalVolume = criteriaBuilder.sum(root.get("currentVolume"), root.get("pendingVolume"));

                    predicates.add(criteriaBuilder.equal(totalVolume, 0.0));

                }
                if (pickableLocationOnly == true ) {
                    // only return pickable location
                    Join<Location, LocationGroup> joinLocationGroup = root.join("locationGroup", JoinType.INNER);
                    predicates.add(criteriaBuilder.equal(joinLocationGroup.get("pickable"), true));
                }
                if (minEmptyCapacity > 0.0) {
                    // current capacity = total capacity * capacity fill rate - current volume - pending volume
                    Expression<Double> emptyCapacity =
                            criteriaBuilder.diff(
                                    criteriaBuilder.diff(
                                            criteriaBuilder.prod(
                                                    root.get("capacity"),
                                                    root.get("fillPercentage")
                                            )
                                            ,
                                            root.get("currentVolume")
                                    ),
                                    root.get("pendingVolume")
                            );

                    predicates.add(criteriaBuilder.ge(emptyCapacity, minEmptyCapacity));

                }
                if (!includeDisabledLocation){

                    predicates.add(criteriaBuilder.equal(root.get("enabled"), true));
                }
                if (warehouseId != null) {

                    Join<Location, Warehouse> joinWarehouse = root.join("warehouse", JoinType.INNER);
                    predicates.add(criteriaBuilder.equal(joinWarehouse.get("id"), warehouseId));
                }

                Predicate[] p = new Predicate[predicates.size()];
                return criteriaBuilder.and(predicates.toArray(p));
            }
        );

    }



    public Location findLogicLocation(String locationType, Long warehouseId) {

        // Get the logic location's name by policy
        String policyKey = "LOCATION-" + locationType;
        logger.debug("Start to find policy by key: {}", policyKey);
        Policy policy = commonServiceRestemplateClient.getPolicyByKey(policyKey);
        return findByName(policy.getValue(), warehouseId);
    }

    public List<Location> findByLocationGroup(Long locationGroupId) {
        return findAll(new long[]{locationGroupId});
    }

    public List<Location> findAll(long[] locationGroupIdArray) {

        List<Long> locationGroupList = Arrays.stream(locationGroupIdArray).boxed().collect(Collectors.toList());
        return locationRepository.findByLocationGroups(locationGroupList);
    }

    public List<Location> listLocationsByTypes(String locationGroups) {
        if (locationGroups.isEmpty()) {
            return findAll();
        }
        else {
            long[] locationGroupArray = Arrays.asList(locationGroups.split(",")).stream().mapToLong(Long::parseLong).toArray();
            return findAll(locationGroupArray);
        }
    }

    public Location findByName(String name, Long warehouseId){
        return locationRepository.findByName(warehouseId, name);
    }

    public Location save(Location location) {
        return locationRepository.save(location);
    }
    public Location saveOrUpdate(Location location) {
        if (findByName(location.getName(), location.getWarehouse().getId()) != null) {
            location.setId(findByName(location.getName(), location.getWarehouse().getId()).getId());
        }
        return save(location);
    }

    public void delete(Location location) {
        locationRepository.delete(location);
    }
    public void delete(Long id) {
        locationRepository.deleteById(id);
    }

    @Transactional
    public void delete(String locationIds) {
        // remove a list of location groups based upon the id passed in
        if (!locationIds.isEmpty()) {
            long[] locationIdArray = Arrays.asList(locationIds.split(",")).stream().mapToLong(Long::parseLong).toArray();
            List<Long> locationIdList = Arrays.asList(ArrayUtils.toObject(locationIdArray));
            locationRepository.deleteByLocationIds(locationIdList);
        }
    }

    public List<Location> loadLocationData(File file) throws IOException {
        List<LocationCSVWrapper> locationCSVWrappers = loadData(file);
        return locationCSVWrappers.stream().map(locationCSVWrapper -> convertFromWrapper(locationCSVWrapper)).collect(Collectors.toList());
    }

    public List<LocationCSVWrapper> loadData(File file) throws IOException {
        CsvSchema schema = CsvSchema.builder().
                addColumn("warehouse").
                addColumn("name").
                addColumn("aisle").
                addColumn("x").
                addColumn("y").
                addColumn("z").
                addColumn("length").
                addColumn("width").
                addColumn("height").
                addColumn("pickSequence").
                addColumn("putawaySequence").
                addColumn("countSequence").
                addColumn("capacity").
                addColumn("fillPercentage").
                addColumn("locationGroup").
                addColumn("enabled").
                build().withHeader();
        return fileService.loadData(file, schema, LocationCSVWrapper.class);
    }
    public List<LocationCSVWrapper> loadData(InputStream inputStream) throws IOException {

        CsvSchema schema = CsvSchema.builder().
                addColumn("warehouse").
                addColumn("name").
                addColumn("aisle").
                addColumn("x").
                addColumn("y").
                addColumn("z").
                addColumn("length").
                addColumn("width").
                addColumn("height").
                addColumn("pickSequence").
                addColumn("putawaySequence").
                addColumn("countSequence").
                addColumn("capacity").
                addColumn("fillPercentage").
                addColumn("locationGroup").
                addColumn("enabled").
                build().withHeader();

        return fileService.loadData(inputStream, schema, LocationCSVWrapper.class);
    }

    public void initTestData(String warehouseName) {
        try {
            String testDataFileName  = StringUtils.isBlank(warehouseName) ?
                    testDataFile + ".csv" :
                    testDataFile + "-" + warehouseName + ".csv";
            logger.debug("Start to load location from file: {}", testDataFileName);
            InputStream inputStream = new ClassPathResource(testDataFileName).getInputStream();
            List<LocationCSVWrapper> locationCSVWrappers = loadData(inputStream);
            locationCSVWrappers.stream().forEach(locationCSVWrapper -> saveOrUpdate(convertFromWrapper(locationCSVWrapper)));
            logger.debug("==>  location loaded from file: {}", testDataFileName);
        } catch (IOException ex) {
            logger.debug("Exception while load test data: {}", ex.getMessage());
        }
    }

    private Location convertFromWrapper(LocationCSVWrapper locationCSVWrapper) {
        Location location = new Location();
        location.setName(locationCSVWrapper.getName());
        location.setX(locationCSVWrapper.getX());
        location.setY(locationCSVWrapper.getY());
        location.setZ(locationCSVWrapper.getZ());
        location.setAisle(locationCSVWrapper.getAisle());
        location.setLength(locationCSVWrapper.getLength());
        location.setWidth(locationCSVWrapper.getWidth());
        location.setHeight(locationCSVWrapper.getHeight());
        location.setPickSequence(locationCSVWrapper.getPickSequence());
        location.setPutawaySequence(locationCSVWrapper.getPutawaySequence());
        location.setCountSequence(locationCSVWrapper.getCountSequence());
        location.setCapacity(locationCSVWrapper.getCapacity());
        location.setFillPercentage(locationCSVWrapper.getFillPercentage());
        location.setEnabled(locationCSVWrapper.getEnabled());
        location.setCurrentVolume(0.0);
        location.setPendingVolume(0.0);

        location.setWarehouse(warehouseService.findByName(locationCSVWrapper.getWarehouse()));

        if (!locationCSVWrapper.getLocationGroup().isEmpty()) {
            LocationGroup locationGroup = locationGroupService.findByName(
                    warehouseService.findByName(locationCSVWrapper.getWarehouse()).getId(),locationCSVWrapper.getLocationGroup());
            location.setLocationGroup(locationGroup);
        }
        return location;

    }

    public double getLocationVolume(Long inventoryQuantity, Double inventorySize) {
        return inventorySize;
    }

    @Transactional
    public Location reserveLocation(Long id, String reservedCode) {
        return reserveLocation(findById(id), reservedCode);

    }

    @Transactional
    public Location reserveLocation(Location location, String reservedCode) {
        return reserveLocation(location, reservedCode, 0.0);

    }

    @Transactional
    public Location reserveLocation(Long id, String reservedCode, Double pendingVolume) {
        return reserveLocation(findById(id), reservedCode, pendingVolume);
    }
    @Transactional
    public Location reserveLocation(Location location, String reservedCode, Double pendingVolume) {

        if (StringUtils.isBlank(location.getReservedCode())) {
            location.setReservedCode(reservedCode);
        }
        else if (!location.getReservedCode().equals(reservedCode)){
            // Current location is already reserved but not the same
            // as the one we are trying to reserve. we will raise
            // an exception
            throw LocationOperationException.raiseException("Location is already reserved by other code");
        }

        location.setPendingVolume(location.getPendingVolume() + pendingVolume);
        return save(location);

    }

    @Transactional
    public Location reserveLocation(Long id, String reservedCode,
                                    Double pendingSize, Long pendingQuantity, Integer pendingPalletQuantity) {

        return reserveLocation(findById(id), reservedCode, pendingSize, pendingQuantity, pendingPalletQuantity);
    }
    public Location reserveLocation(Location location, String reservedCode,
                                    Double pendingSize, Long pendingQuantity, Integer pendingPalletQuantity) {

        // See how we can get the pending volume for the location to be reserved, based on
        // the configuration on the location group
        switch (location.getLocationGroup().getVolumeTrackingPolicy()) {
            case BY_VOLUME:
                return reserveLocation(location, reservedCode, pendingSize);
            case BY_EACH:
                return reserveLocation(location, reservedCode, (double)pendingQuantity);
            case BY_PALLET:
                return reserveLocation(location, reservedCode, (double)pendingPalletQuantity);

        }
        throw  LocationOperationException.raiseException("can't find the right volume tracking policy for the location:" + location.getLocationGroup().getName());
    }

    public Location allocateLocation(Long id, Double inventorySize) {
        Location location = findById(id);

        logger.debug("before allocation, location {} has pending qvl: {}", location.getName(), location.getPendingVolume());
        location.setPendingVolume(location.getPendingVolume() + inventorySize);

        logger.debug("Start to allocate {} from location {}", location.getPendingVolume(), location.getName());
        return save(location);
    }

    public Location changePendingVolume(Long id, Double reducedPendingVolume, Double increasedPendingVolume) {

        Location location = findById(id);
        logger.debug("Start to adjust pending volume for location: {}, current pending volume: {}, reduced by {}, increased by {}",
                location.getName(), location.getPendingVolume(), reducedPendingVolume, increasedPendingVolume);
        location.setPendingVolume(location.getPendingVolume() - reducedPendingVolume + increasedPendingVolume);
        logger.debug("afect adjusting pending volume for location: {}, pending volume: {}",
                location.getName(), location.getPendingVolume());
        return save(location);

    }

    public Location changeLocationVolume(Long id, Double reducedVolume, Double increasedVolume) {

        Location location = findById(id);
        logger.debug("Start to adjust location volume for location: {}, current volume: {}, reduced by {}, increased by {}",
                location.getName(), location.getCurrentVolume(), reducedVolume, increasedVolume);
        location.setCurrentVolume(location.getCurrentVolume() - reducedVolume + increasedVolume);
        logger.debug("afect adjusting location volume for location: {}, current volume: {}",
                location.getName(), location.getCurrentVolume());
        return save(location);

    }

    public List<Location> getDockLocations(Long warehouseId, Boolean emptyDockOnly) {
        List<Location> dockLocations = locationRepository.getDockLocations(warehouseId);
        if (emptyDockOnly) {
            return dockLocations.stream().filter(Location::isEmpty).collect(Collectors.toList());
        }
        return dockLocations;
    }

    public Location checkInTrailerAtDock(Long dockLocationId, Long trailerId) {
        return checkInTrailerAtDock(findById(dockLocationId), trailerId);
    }
    public Location checkInTrailerAtDock(Location dockLocation, Long trailerId) {
        // Create a fake location for the trailer. Location's name will be the trailer ID
        Location trailerLocation = createTrailerLocation(dockLocation.getWarehouse().getId(), trailerId);
        logger.debug(">> trailer location created: {} / {}",
                trailerLocation.getId(), trailerLocation.getName());
        // update the location's volume to 1 when we check in the trailer
        // at the dock
        dockLocation.setCurrentVolume(1.0);
        return saveOrUpdate(dockLocation);
    }
    public Location moveTrailerFromDock(Long dockLocationId) {
        return moveTrailerFromDock(findById(dockLocationId));
    }
    public Location moveTrailerFromDock(Location dockLocation) {
        dockLocation.setCurrentVolume(0.0);
        return saveOrUpdate(dockLocation);
    }
    public Location createTrailerLocation(Long warehouseId, Long trailerId) {
        Location location = new Location();
        location.setName("TRLR-" + String.valueOf(trailerId));
        location.setLocationGroup(locationGroupService.getDockLocationGroup(warehouseId));
        location.setEnabled(true);
        location.setWarehouse(warehouseService.findById(warehouseId));
        return saveOrUpdate(location);
    }


    public Location processLocationLock(Long id, Boolean lock) {

        Location location = findById(id);
        location.setLocked(lock);
        return saveOrUpdate(location);

    }

}

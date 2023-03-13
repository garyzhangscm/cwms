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
import com.garyzhangscm.cwms.layout.clients.InventoryServiceRestemplateClient;
import com.garyzhangscm.cwms.layout.clients.CommonServiceRestemplateClient;
import com.garyzhangscm.cwms.layout.exception.LocationOperationException;
import com.garyzhangscm.cwms.layout.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.layout.model.*;
import com.garyzhangscm.cwms.layout.repository.LocationRepository;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;

import javax.persistence.criteria.*;
import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class LocationService {

    private static final Logger logger = LoggerFactory.getLogger(LocationService.class);
    @Autowired
    private LocationRepository locationRepository;

    @Autowired
    private LocationGroupService locationGroupService;
    @Autowired
    private LocationGroupTypeService locationGroupTypeService;
    @Autowired
    private WarehouseService warehouseService;
    @Autowired
    private CompanyService companyService;
    @Autowired
    private FileService fileService;

    @Value("${fileupload.test-data.locations:locations}")
    String testDataFile;

    @Autowired
    private CommonServiceRestemplateClient commonServiceRestemplateClient;
    @Autowired
    private InventoryServiceRestemplateClient inventoryServiceRestemplateClient;

    public Location findById(Long id) {
        return locationRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("location not found by id: " + id));
    }

    public List<Location> findAll() {

        return locationRepository.findAll();
    }

    public List<Location> findAll(Long warehouseId,
                                  String locationGroupTypeIds,
                                  String locationGroupIds,
                                  String name,
                                  Long beginSequence,
                                  Long endSequence,
                                  String beginAisle,
                                  String endAisle,
                                  String sequenceType,
                                  Boolean includeEmptyLocation,
                                  Boolean emptyLocationOnly,
                                  Double minEmptyCapacity,
                                  Boolean pickableLocationOnly,
                                  String reservedCode,
                                  Boolean includeDisabledLocation,
                                  Boolean emptyReservedCodeOnly,
                                  String code,
                                  String locationStatus) {

        List<Location> locations = locationRepository.findAll(
            (Root<Location> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                List<Predicate> predicates = new ArrayList<Predicate>();
                if (StringUtils.isNotBlank(locationGroupTypeIds)) {
                    logger.debug("Will filter the location by group type id {}", locationGroupTypeIds);

                    Join<Location, LocationGroup> joinLocationGroup = root.join("locationGroup", JoinType.INNER);
                    Join<LocationGroup, LocationGroupType> joinLocationGroupType = joinLocationGroup.join("locationGroupType", JoinType.INNER);

                    if (StringUtils.isNotBlank(locationGroupIds)) {

                        logger.debug("Will filter the location by group id {}", locationGroupIds);
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
                else if (StringUtils.isNotBlank(locationGroupIds)) {

                    logger.debug("Will filter the location by group id {}", locationGroupIds);
                    Join<Location, LocationGroup> joinLocationGroup = root.join("locationGroup", JoinType.INNER);
                    CriteriaBuilder.In<Long> in = criteriaBuilder.in(joinLocationGroup.get("id"));
                    for(String id : locationGroupIds.split(",")) {
                        in.value(Long.parseLong(id));
                    }
                    predicates.add(criteriaBuilder.and(in));
                }

                if (StringUtils.isNotBlank(name)) {

                    if (name.contains("*")) {
                        predicates.add(criteriaBuilder.like(root.get("name"), name.replaceAll("\\*", "%")));
                    }
                    else {
                        predicates.add(criteriaBuilder.equal(root.get("name"), name));
                    }
                }
                if (StringUtils.isNotBlank(code)) {
                        predicates.add(criteriaBuilder.equal(root.get("code"), code));
                }
                if (Objects.nonNull(beginSequence)  && Objects.nonNull(endSequence)) {
                    logger.debug("Will filter the location by {} sequence [{}, {}]",
                            sequenceType, beginSequence, endSequence);
                    if (sequenceType.equals("count")) {

                        logger.debug("start to find locations with count sequence between [{}, {}]",
                                beginSequence, endSequence);
                        predicates.add(criteriaBuilder.between(root.get("countSequence"), beginSequence, endSequence));
                    }
                    else if (sequenceType.equals("putaway")) {
                        predicates.add(criteriaBuilder.between(root.get("putawaySequence"), beginSequence, endSequence));
                    }
                    else if (sequenceType.equals("pick")) {
                        predicates.add(criteriaBuilder.between(root.get("pickSequence"), beginSequence, endSequence));
                    }
                }
                if (StringUtils.isNotBlank(beginAisle)) {

                    predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("aisle"), beginAisle));
                }
                if (StringUtils.isNotBlank(endAisle)) {
                    predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("aisle"), endAisle));

                }
                if (Objects.nonNull(includeEmptyLocation) && !includeEmptyLocation) {
                    logger.debug("Will filter the location by includeEmptyLocation {}", includeEmptyLocation);
                    Expression<Double> totalVolume = criteriaBuilder.sum(root.get("currentVolume"), root.get("pendingVolume"));

                    predicates.add(criteriaBuilder.greaterThan(totalVolume, 0.0));
                }
                if (Objects.nonNull(emptyLocationOnly) && emptyLocationOnly == true) {
                    logger.debug("Will filter the location by emptyLocationOnly {}", emptyLocationOnly);
                    Expression<Double> totalVolume = criteriaBuilder.sum(root.get("currentVolume"), root.get("pendingVolume"));
                    predicates.add(criteriaBuilder.equal(totalVolume, 0.0));
                }
                if (Objects.nonNull(pickableLocationOnly) && pickableLocationOnly == true ) {
                    logger.debug("Will filter the location by pickableLocationOnly {}", pickableLocationOnly);
                    // only return pickable location
                    Join<Location, LocationGroup> joinLocationGroup = root.join("locationGroup", JoinType.INNER);
                    predicates.add(criteriaBuilder.equal(joinLocationGroup.get("pickable"), true));
                }
                if (Objects.nonNull(minEmptyCapacity) && minEmptyCapacity > 0.0) {
                    logger.debug("Will filter the location by minEmptyCapacity {}", minEmptyCapacity);
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
                if (Objects.nonNull(includeDisabledLocation) && !includeDisabledLocation){

                    logger.debug("Will filter the location by includeDisabledLocation {}", includeDisabledLocation);
                    predicates.add(criteriaBuilder.equal(root.get("enabled"), true));
                }
                if (Objects.nonNull(warehouseId)) {

                    logger.debug("Will filter the location by warehouseId {}", warehouseId);
                    Join<Location, Warehouse> joinWarehouse = root.join("warehouse", JoinType.INNER);
                    predicates.add(criteriaBuilder.equal(joinWarehouse.get("id"), warehouseId));
                }

                if (StringUtils.isNotBlank(reservedCode)) {
                    predicates.add(criteriaBuilder.equal(root.get("reservedCode"), reservedCode));

                }
                Predicate[] p = new Predicate[predicates.size()];
                return criteriaBuilder.and(predicates.toArray(p));
            }
                ,
                Sort.by(Sort.Direction.ASC, "locationGroup", "name")

        );

        // only return the locations with empty reserve code
        if (Boolean.TRUE.equals(emptyReservedCodeOnly)) {
            locations = locations.stream().filter(
                    location -> Strings.isBlank(location.getReservedCode())).collect(Collectors.toList());

        }
        return locations;

    }



    public Location findLogicLocation(String locationType, Long warehouseId) {

        // Get the logic location's name by policy
        String policyKey = "LOCATION-" + locationType;
        logger.debug("Start to find policy by key: {}", policyKey);
        Policy policy = commonServiceRestemplateClient.getPolicyByKey(warehouseId, policyKey);
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
        logger.debug("Start to find by name {} /  {}", warehouseId, name);
        return locationRepository.findByName(warehouseId, name);
    }

    public Location save(Location location) {
        logger.debug("Start to save location {} with volume {}",
                location.getName(), location.getCurrentVolume());
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

    public List<Location> loadLocationData(Long warehouseId, File file) throws IOException {
        List<LocationCSVWrapper> locationCSVWrappers = loadData(file);
        return locationCSVWrappers.stream()
                .map(locationCSVWrapper -> saveOrUpdate(convertFromWrapper(warehouseId, locationCSVWrapper)))
                .collect(Collectors.toList());
    }

    public List<LocationCSVWrapper> loadData(File file) throws IOException {
        return fileService.loadData(file, getLocationCsvSchema(), LocationCSVWrapper.class);
    }
    public List<LocationCSVWrapper> loadData(InputStream inputStream) throws IOException {


        return fileService.loadData(inputStream, getLocationCsvSchema(), LocationCSVWrapper.class);
    }

    private CsvSchema getLocationCsvSchema() {
        return  CsvSchema.builder().
                addColumn("company").
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
    }

    /**
    public void initTestData(Long companyId, String warehouseName) {
        try {
            String companyCode = companyService.findById(companyId).getCode();

            String testDataFileName = StringUtils.isBlank(warehouseName) ?
                    testDataFile + ".csv" :
                    testDataFile + "-" + companyCode + "-" + warehouseName + ".csv";

            logger.debug("Start to load location from file: {}", testDataFileName);
            InputStream inputStream = new ClassPathResource(testDataFileName).getInputStream();
            List<LocationCSVWrapper> locationCSVWrappers = loadData(inputStream);
            locationCSVWrappers.stream().forEach(locationCSVWrapper -> saveOrUpdate(convertFromWrapper(locationCSVWrapper)));
            logger.debug("==>  location loaded from file: {}", testDataFileName);
        } catch (IOException ex) {
            logger.debug("Exception while load test data: {}", ex.getMessage());
        }
    }
     **/

    private Location convertFromWrapper(Long warehouseId,
                                        LocationCSVWrapper locationCSVWrapper) {
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

        location.setWarehouse(warehouseService.findById(warehouseId));

        logger.debug("process location {} with group {}",
                locationCSVWrapper.getName(),
                locationCSVWrapper.getLocationGroup());
        if (StringUtils.isNotBlank(locationCSVWrapper.getLocationGroup())) {
            LocationGroup locationGroup = locationGroupService.findByName(
                    warehouseId,
                    locationCSVWrapper.getLocationGroup());
            logger.debug("Get location group id {} by warehouse {} / name {}",
                    locationGroup.getId(),
                    warehouseId,
                    locationCSVWrapper.getLocationGroup());
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
        logger.debug("Start to reserve location {} with resource code: {}",
                location.getName(), reservedCode);
        logger.debug("Location's group: {}, volume tracking policy: {}",
                location.getLocationGroup().getName(),
                location.getLocationGroup().getVolumeTrackingPolicy());
        if (Objects.isNull(location.getLocationGroup().getVolumeTrackingPolicy())) {
            // location is not setup to volume tracking,
            return reserveLocation(location, reservedCode, 0.0);
        }

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
        // we will reset the pending volume only if the location's group has the check volume setup
        if (location.getLocationGroup().getTrackingVolume() == false) {
            return location;
        }
        logger.debug("Start to adjust pending volume for location: {}, current pending volume: {}, reduced by {}, increased by {}",
                location.getName(), location.getPendingVolume(), reducedPendingVolume, increasedPendingVolume);

        double pendingVolume = location.getPendingVolume() - reducedPendingVolume + increasedPendingVolume;
        if (pendingVolume < 0) {
            pendingVolume = 0.0;
        }

        location.setPendingVolume(pendingVolume);
        logger.debug("afect adjusting pending volume for location: {}, pending volume: {}",
                location.getName(), location.getPendingVolume());
        return save(location);

    }

    public Location changeLocationVolume(Long id, Double reducedVolume, Double increasedVolume, Boolean fromPendingVolume) {

        Location location = findById(id);
        // we will reset the volume only if the location's group has the check volume setup
        if (location.getLocationGroup().getTrackingVolume() == false) {
            return location;
        }
        logger.debug("Start to adjust location volume for location: {}, current volume: {}, reduced by {}, increased by {}",
                location.getName(), location.getCurrentVolume(), reducedVolume, increasedVolume);
        location.setCurrentVolume(location.getCurrentVolume() - reducedVolume + increasedVolume);
        if (increasedVolume > 0.0 && fromPendingVolume == true) {
            // OK, we know we are increasing the volume and we need to reduce the same amount from
            // the pending volume
            // which is normally the case the we are moving inventory into the location
            // which is supposed to be moved in(that's why the quantity is in the pending volume,
            // and we have to deduct after the movement and increase the volume)
            double pendingVolume = location.getPendingVolume();
            pendingVolume = (pendingVolume >= increasedVolume ?
                    (pendingVolume - increasedVolume) : 0.0);
            location.setPendingVolume(pendingVolume);
            logger.debug("# Will set location's pending volume to {} after we increase the location's volume by {}",
                    pendingVolume, increasedVolume);
        }
        if (location.getCurrentVolume() < 0) {
            location.setCurrentVolume(0.0);
        }
        if (location.getPendingVolume() < 0) {
            location.setPendingVolume(0.0);
        }
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

    public Location createShippedInventoryLocation(Long warehouseId, String locationName) {

        Location location = findByName(locationName, warehouseId);
        if (Objects.nonNull(location)) {
            return location;
        }
        // create the location if it doesn't exists yet
        location = new Location();
        location.setName(locationName);
        location.setLocationGroup(locationGroupService.getDockLocationGroup(warehouseId));
        location.setEnabled(true);
        location.setWarehouse(warehouseService.findById(warehouseId));
        return saveOrUpdate(location);
    }

    public Location createShippedTrailerLocation(Long warehouseId, String trailerNumber) {
        String locationName = "TRLR-" + trailerNumber;
        return createShippedInventoryLocation(
                warehouseId, locationName
        );
    }
    public Location createShippedTrailerAppointmentLocation(Long warehouseId, String trailerAppointmentNumber) {
        String locationName = "TRLR-APT-" + trailerAppointmentNumber;
        return createShippedInventoryLocation(
                warehouseId, locationName
        );

    }

    @Transactional
    public void removeLocationByGroup(LocationGroup locationGroup) {
        // make sure the location is empty

        int inventoryCount = inventoryServiceRestemplateClient.getInventoryCountByLocationGroup(
                locationGroup.getWarehouse().getId(), locationGroup
        );
        logger.debug("There's {} inventory record in the location group {}",
                inventoryCount, locationGroup.getName());
        if(inventoryCount > 0) {
            throw LocationOperationException.raiseException("There's inventory in the location group, can't remove it");
        }
        locationRepository.deleteByLocationGroupId(locationGroup.getId());
    }
    @Transactional
    public void removeLocations(Long warehouseId,  String locationIds) {

        int inventoryCount = inventoryServiceRestemplateClient.getInventoryCountByLocations(
                warehouseId, locationIds
        );
        logger.debug("There's {} inventory record in the locations {}",
                inventoryCount, locationIds);
        if(inventoryCount > 0) {
            throw LocationOperationException.raiseException("There's inventory in those locations, can't remove it");
        }
        List<Long> locationIdList =
                Arrays.stream(StringUtils.split(locationIds, ","))
                        .map(Long::parseLong).collect(Collectors.toList());
        locationRepository.deleteByLocationIds(locationIdList);

    }


    public Location processLocationLock(Long id, Boolean lock) {

        Location location = findById(id);
        location.setLocked(lock);
        return saveOrUpdate(location);

    }


    /**
     * @param warehouseId
     * @param containerName
     * @return
     */
    public Location getOrCreateContainerLocation(Long warehouseId,
                                                 String containerName) {
        // Make sure we will only have one thread that create the container
        // This normally happens when the use is confirm multiple picks
        // from the same container at the same time, which may cause
        // multiple threads try to get or create containers at the same time
        synchronized (this) {
            if (Objects.nonNull(findByName(containerName, warehouseId))) {
                return findByName(containerName, warehouseId);
            }

            // The location for the container is not created yet, let's
            // create one in the specific location group
            LocationGroup locationGroup = locationGroupService.getContainerLocationGroup(warehouseId);

            return createContainerLocation(warehouseId, containerName, locationGroup);
        }


    }

    public Location createContainerLocation(Long warehouseId,
                                            String containerName,
                                            LocationGroup locationGroup) {
        Location location = new Location();
        location.setName(containerName);
        location.setLocationGroup(locationGroup);
        location.setWarehouse(warehouseService.findById(warehouseId));
        location.setEnabled(true);
        location.setCapacity(Double.valueOf(Integer.MAX_VALUE));
        location.setCurrentVolume(0.0);
        location.setPendingVolume(0.0);
        location.setLocked(false);
        location.setFillPercentage(100.0);
        return save(location);
    }

    /**
     * Get all the packing stations
     * @param warehouseId
     * @return
     */
    public List<Location> getPackingStations(Long warehouseId) {
        List<LocationGroupType> locationGroupTypes
                = locationGroupTypeService.findAll(null, null)
                .stream()
                .filter(locationGroupType -> locationGroupType.getPackingStation() == true)
                .collect(Collectors.toList());
        if (locationGroupTypes.size() == 0) {
            return new ArrayList<>();
        }
        String locationGroupTypeIds = locationGroupTypes.stream()
                .map(locationGroupType -> locationGroupType.getId())
                .map(String::valueOf).collect(Collectors.joining(","));
        return findAll(warehouseId,locationGroupTypeIds,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null);

    }

    public Location getShippedParcelLocation(Long warehouseId, String carrierName, String serviceLevelName) {
        String locationName = carrierName + "-" + serviceLevelName;
        Optional<Location> locationOptional = Optional.ofNullable(findByName(locationName, warehouseId));
        return locationOptional.orElse(createShippedParcelLocation(warehouseId, locationName));


    }

    public Location createShippedParcelLocation(Long warehouseId, String locationName) {

        Location location = new Location(warehouseService.findById(warehouseId),
                locationName, locationGroupService.getShippedParcelLocationGroup(warehouseId));

        return saveOrUpdate(location);
    }


    public Location createOrderLocation(Long warehouseId,String orderNumber) {
        Location location = new Location(warehouseService.findById(warehouseId),
                orderNumber, locationGroupService.getShippedOrderLocationGroup(warehouseId));

        return saveOrUpdate(location);

    }

    /**
     * release location from certain reserve code
     * @param warehouseId warehouse id
     * @param reservedCode reserve code
     * @return all locations that used to have this reserve code
     */
    public List<Location> unreserveLocation(Long warehouseId, String reservedCode,
                                            Long locationId, Boolean clearReservedVolume) {
        List<Location> locations = new ArrayList<>();
        if (Objects.nonNull(locationId)) {
            locations = Collections.singletonList(findById(locationId));
        }
        else {
            locations = findByReserveCode(warehouseId, reservedCode);
        }
        if (locations.size() == 0) {
            return locations;
        }


        return locations.stream().map(location -> {
            location.setReservedCode("");
            if (Boolean.TRUE.equals(clearReservedVolume)) {
                location.setPendingVolume(0.0);
            }
            return saveOrUpdate(location);
        }).collect(Collectors.toList());
    }

    private List<Location> findByReserveCode(Long warehouseId, String reservedCode) {

        // reserved code should not be empty
        if (Strings.isBlank(reservedCode)) {
            return new ArrayList<>();
        }
        return findAll(warehouseId,null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                reservedCode,
                null,
                null,
                null,
                null);
    }

    public Location addRFLocation(Long warehouseId, String rfCode) {
        // see if we already hava the location
        logger.debug("will add RF location {} / {}",
                warehouseId, rfCode);
        Location location = findByName(rfCode, warehouseId);
        if (Objects.nonNull(location)) {
            logger.debug("RF location {} already exists",
                    location.getName());
            return location;
        }
        else {

            location = new Location(warehouseService.findById(warehouseId),
                    rfCode, locationGroupService.getRFLocationGroup(warehouseId));

            logger.debug("Created a new location {} for the RF",
                    location.getName());
            return saveOrUpdate(location);
        }
    }

    @Transactional
    public void removeLocations(Warehouse warehouse) {
        locationRepository.deleteByWarehouseId(warehouse.getId());

    }


    public Location removeRFLocation(Long warehouseId, String rfCode) {
        logger.debug("will add RF location {} / {}",
                warehouseId, rfCode);
        Location location = findByName(rfCode, warehouseId);
        if (Objects.nonNull(location)) {
            logger.debug("RF location {} already exists, will remove it",
                    location.getName());
            delete(location);
            return location;
        }
        else {

            logger.debug("location {} doesn't exists, will return nothing",
                    rfCode);
            return null;
        }
    }

    /**
     * Create a location for customer return order. The name will be the same as
     * the customer return order number. The inventory will be received into this
     * location first before it will be further checked
     * @param warehouseId
     * @param name
     * @return
     */
    public Location addCustomerReturnOrderStageLocation(Long warehouseId, String name) {
        // see if we already hava the location
        logger.debug("will add customer return order stage location {} / {}",
                warehouseId, name);
        Location location = findByName(name, warehouseId);
        if (Objects.nonNull(location)) {
            logger.debug("customer return order stage location {} already exists",
                    location.getName());
            return location;
        }
        else {

            location = new Location(warehouseService.findById(warehouseId),
                    name, locationGroupService.getCustomerReturnStageLocationGroup(warehouseId));

            logger.debug("Created a new location {} for the customer return order",
                    location.getName());
            return saveOrUpdate(location);
        }
    }

    /**
     * Return utilization tracking locations. we normally use those locations to calculate the location
     * utilization and the storage fee for the client
     * @param warehouseId
     * @return A map, key will be the ItemVolumeTrackingLevel, value will be a list of location id separated by comma
     */
    public Map<String, String> getUtilizationTrackingLocations(Long warehouseId) {
        logger.debug("Start to get utilization tracking locations for warehouse id {}",
                warehouseId);
        List<LocationGroup> locationGroups =
                locationGroupService.getUtilizationTrackingLocationGroups(warehouseId);
        logger.debug("Get {} locations groups that has utilization tracking flag on. They are",
                locationGroups.size());
        locationGroups.forEach(
                locationGroup -> logger.debug(">> {}", locationGroup.getName())
        );
        logger.debug("start to get locations from the location groups and group them by " +
                "how we calculate the inventory's volume(by stock uom or by case uom)");
        Map<String, String> utilizationTrackingLocations = new HashMap<>();
        for (ItemVolumeTrackingLevel itemVolumeTrackingLevel : ItemVolumeTrackingLevel.values()) {
            // get all the location groups with certain item volume tracking level
            String locationGroupIds = locationGroups.stream().filter(
                    locationGroup -> itemVolumeTrackingLevel.equals(locationGroup.getItemVolumeTrackingLevel())
            ).map(locationGroup -> String.valueOf(locationGroup.getId())).collect(Collectors.joining(","));

            logger.debug(">> item volume tracking level: {}", itemVolumeTrackingLevel);
            logger.debug(">>>> location group ids: {}", locationGroupIds);
            // get all locations from those location groups
            if (Strings.isNotBlank(locationGroupIds)) {
                List<Location> locations = findAll(warehouseId,
                        null,
                        locationGroupIds,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                         true,   // note: we will need to include empty location as the location may not be actual empty
                        // if the location's volume is not tracked
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null);

                String locationIds =
                        locations.stream().map(location -> String.valueOf(location.getId())).collect(Collectors.joining(","));
                utilizationTrackingLocations.put(
                        itemVolumeTrackingLevel.name(),locationIds
                        );

                logger.debug(">>>> location ids: {}", locationIds);
            }


        }
        logger.debug("We get {} groups of locations that has volume tracking flag on",
                utilizationTrackingLocations.size());
        utilizationTrackingLocations.entrySet().forEach(
                entry -> logger.debug("======   {} ======\n >> {}",
                        entry.getKey(), entry.getValue())
        );
        return utilizationTrackingLocations;
    }

    public Location addLocation(Long warehouseId, Location location) {
        if (Objects.isNull(location.getWarehouse())) {
            location.setWarehouse(
                    warehouseService.findById(warehouseId)
            );
        }
        location.setCurrentVolume(0.0);
        location.setPendingVolume(0.0);
        return saveOrUpdate(location);
    }

    public List<Location> findReceivingStageLocations(Long warehouseId) {

        return locationRepository.getReceivingStageLocations(warehouseId);
    }
}

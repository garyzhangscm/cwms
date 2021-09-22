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
import com.garyzhangscm.cwms.layout.exception.LocationOperationException;
import com.garyzhangscm.cwms.layout.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.layout.model.*;
import com.garyzhangscm.cwms.layout.repository.LocationGroupRepository;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import javax.persistence.criteria.*;
import javax.transaction.Transactional;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@CacheConfig(cacheNames = "location_groups")
public class LocationGroupService implements TestDataInitiableService {

    private static final Logger logger = LoggerFactory.getLogger(LocationGroupService.class);
    @Autowired
    private LocationGroupRepository locationGroupRepository;
    @Autowired
    private LocationGroupTypeService locationGroupTypeService;
    @Autowired
    private CompanyService companyService;
    @Autowired
    private WarehouseService warehouseService;
    @Autowired
    private LocationService locationService;
    @Autowired
    private FileService fileService;

    @Value("${fileupload.test-data.location-groups:location_groups}")
    String testDataFile;

    public LocationGroup findById(Long id) {
        return locationGroupRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("location group not found by id: " + id));
    }

    public List<LocationGroup> findAll(Long warehouseId) {

        return locationGroupRepository.findAll(warehouseId);
    }

    public List<LocationGroup> findAll(Long warehouseId, String locationGroupTypeIds, String locationGroupIds, String name) {
        return locationGroupRepository.findAll(
                (Root<LocationGroup> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();

                    Join<LocationGroup, Warehouse> joinWarehouse = root.join("warehouse", JoinType.INNER);
                    predicates.add(criteriaBuilder.equal(joinWarehouse.get("id"), warehouseId));

                    if (StringUtils.isNotBlank(locationGroupTypeIds)) {
                        Join<LocationGroup, LocationGroupType> joinLocationGroupType = root.join("locationGroupType", JoinType.INNER);
                        CriteriaBuilder.In<Long> inLocationGroupTypeIds = criteriaBuilder.in(joinLocationGroupType.get("id"));
                        for(String id : locationGroupTypeIds.split(",")) {
                            inLocationGroupTypeIds.value(Long.parseLong(id));
                        }
                        predicates.add(criteriaBuilder.and(inLocationGroupTypeIds));
                    }

                    if (StringUtils.isNotBlank(locationGroupIds)) {
                        CriteriaBuilder.In<Long> inLocationGroupIds = criteriaBuilder.in(root.get("id"));
                        for(String id : locationGroupIds.split(",")) {
                            inLocationGroupIds.value(Long.parseLong(id));
                        }
                        predicates.add(criteriaBuilder.and(inLocationGroupIds));
                    }

                    if (StringUtils.isNotBlank(name)) {
                        predicates.add(criteriaBuilder.equal(root.get("name"), name));
                    }

                    Predicate[] p = new Predicate[predicates.size()];
                    return criteriaBuilder.and(predicates.toArray(p));
                }
        );

    }

    public List<LocationGroup> findAll(Long warehouseId, long[] locationGroupTypeIdArray) {

        List<Long> locationGroupTypeList = Arrays.stream(locationGroupTypeIdArray).boxed().collect( Collectors.toList());
        return locationGroupRepository.findByLocationGroupTypes(warehouseId, locationGroupTypeList);
    }


    public LocationGroup findByName(Long warehouseId, String name){
        return locationGroupRepository.findByName(warehouseId, name);
    }

    public LocationGroup save(LocationGroup locationGroup) {
        return locationGroupRepository.save(locationGroup);
    }

    public LocationGroup saveOrUpdate(LocationGroup locationGroup) {
        logger.debug("Will try to find existing location group by \n ==> {} / {}"
                ,locationGroup.getWarehouse().getId(), locationGroup.getWarehouse().getName());
        if (Objects.isNull(locationGroup.getId()) &&
                Objects.nonNull(findByName(locationGroup.getWarehouse().getId(), locationGroup.getName()))) {
            logger.debug("===> Find such location group {}",
                    findByName(locationGroup.getWarehouse().getId(), locationGroup.getName()).getId());
            locationGroup.setId(findByName(locationGroup.getWarehouse().getId(), locationGroup.getName()).getId());
        }
        return save(locationGroup);
    }
    public void delete(LocationGroup locationGroup) {
        locationGroupRepository.delete(locationGroup);
    }
    public void delete(Long id) {
        locationGroupRepository.deleteById(id);
    }

    public void delete(String locationGroupIds) {
        // remove a list of location groups based upon the id passed in
        if (!locationGroupIds.isEmpty()) {
            long[] locationGroupIdArray = Arrays.asList(locationGroupIds.split(",")).stream().mapToLong(Long::parseLong).toArray();
            for(long id : locationGroupIdArray) {
                delete(id);
            }
        }

    }


    public List<LocationGroupCSVWrapper> loadData(File file) throws IOException {

        CsvSchema schema = CsvSchema.builder().
                addColumn("company").
                addColumn("warehouse").
                addColumn("name").
                addColumn("description").
                addColumn("locationGroupType").
                addColumn("pickable").
                addColumn("storable").
                addColumn("countable").
                addColumn("trackingVolume").
                addColumn("volumeTrackingPolicy").
                addColumn("inventoryConsolidationStrategy").
                addColumn("allowCartonization").
                addColumn("adjustable").
                build().withHeader();
        return fileService.loadData(file, schema, LocationGroupCSVWrapper.class);
    }
    public List<LocationGroupCSVWrapper> loadData(InputStream inputStream) throws IOException {

        CsvSchema schema = CsvSchema.builder().
                addColumn("company").
                addColumn("warehouse").
                addColumn("name").
                addColumn("description").
                addColumn("locationGroupType").
                addColumn("pickable").
                addColumn("storable").
                addColumn("countable").
                addColumn("trackingVolume").
                addColumn("volumeTrackingPolicy").
                addColumn("inventoryConsolidationStrategy").
                addColumn("allowCartonization").
                addColumn("adjustable").
                build().withHeader();

        return fileService.loadData(inputStream, schema, LocationGroupCSVWrapper.class);
    }

    public void initTestData(Long companyId, String warehouseName) {
        try {
            String companyCode = companyService.findById(companyId).getCode();

            String testDataFileName = StringUtils.isBlank(warehouseName) ?
                    testDataFile + ".csv" :
                    testDataFile + "-" + companyCode + "-" + warehouseName + ".csv";
            InputStream inputStream = new ClassPathResource(testDataFileName).getInputStream();
            logger.debug("Start to load location group from file: {}", testDataFile);
            List<LocationGroupCSVWrapper> locationGroupCSVWrappers = loadData(inputStream);
            locationGroupCSVWrappers.stream().forEach(locationGroupCSVWrapper -> {
                try {
                    saveOrUpdate(convertFromWrapper(locationGroupCSVWrapper));
                } catch(Exception ex) {
                    logger.debug("Exception while saving location group " + locationGroupCSVWrapper.getName());
                }
            });

            logger.debug("==> location group loaded from file: {}", testDataFileName);
        } catch (IOException ex) {
            logger.debug("Exception while load test data: {}", ex.getMessage());
        }
    }


    private LocationGroup convertFromWrapper(LocationGroupCSVWrapper locationGroupCSVWrapper) {
        LocationGroup locationGroup = new LocationGroup();
        locationGroup.setName(locationGroupCSVWrapper.getName());
        locationGroup.setDescription(locationGroupCSVWrapper.getDescription());
        locationGroup.setPickable(locationGroupCSVWrapper.getPickable());
        locationGroup.setCountable(locationGroupCSVWrapper.getCountable());
        locationGroup.setStorable(locationGroupCSVWrapper.getStorable());
        locationGroup.setAdjustable(locationGroupCSVWrapper.getAdjustable());


        locationGroup.setAllowCartonization(locationGroupCSVWrapper.getAllowCartonization());
        locationGroup.setWarehouse(warehouseService.findByName(
                locationGroupCSVWrapper.getCompany(), locationGroupCSVWrapper.getWarehouse()));


        locationGroup.setTrackingVolume(locationGroupCSVWrapper.getTrackingVolume());
        if (!StringUtils.isBlank(locationGroupCSVWrapper.getVolumeTrackingPolicy())) {
            locationGroup.setVolumeTrackingPolicy(LocationVolumeTrackingPolicy.valueOf(locationGroupCSVWrapper.getVolumeTrackingPolicy()));
        }

        if (!StringUtils.isBlank(locationGroupCSVWrapper.getInventoryConsolidationStrategy())) {
            locationGroup.setInventoryConsolidationStrategy(InventoryConsolidationStrategy.valueOf(locationGroupCSVWrapper.getInventoryConsolidationStrategy()));
        }
        logger.debug("locationGroupCSVWrapper.getLocationGroupType().isEmpty()? " + locationGroupCSVWrapper.getLocationGroupType().isEmpty());
        if (!locationGroupCSVWrapper.getLocationGroupType().isEmpty()) {
            logger.debug("locationGroupCSVWrapper.getLocationGroupType():" + locationGroupCSVWrapper.getLocationGroupType());
            LocationGroupType locationGroupType = locationGroupTypeService.findByName(locationGroupCSVWrapper.getLocationGroupType().trim());
            logger.debug("locationGroupType == null ? : " + (locationGroupType==null));
            if (locationGroupType != null) {
                logger.debug("locationGroupType.getId():" + locationGroupType.getId());
            }
            locationGroup.setLocationGroupType(locationGroupType);
        }
        return locationGroup;

    }

    public LocationGroup getContainerLocationGroup(Long warehouseId) {

        long[] locationGroupTypeIdArray =
                locationGroupTypeService.getContainerLocationGroupType().stream()
                    .mapToLong(locationGroupType -> locationGroupType.getId()).toArray();

        return findAll( warehouseId,  locationGroupTypeIdArray).stream().findFirst()
                .orElseThrow(() -> LocationOperationException.raiseException("Can't find location group for container on the fly"));
    }

    public LocationGroup getRFLocationGroup(Long warehouseId) {

        long[] locationGroupTypeIdArray =
                locationGroupTypeService.getRFLocationGroupType().stream()
                        .mapToLong(locationGroupType -> locationGroupType.getId()).toArray();

        return findAll( warehouseId,  locationGroupTypeIdArray).stream().findFirst()
                .orElseThrow(() -> LocationOperationException.raiseException("Can't find location group for rf on the fly"));
    }

    // Reserve a location from a group with the specific code.
    // We will first try to allocate a location with the same reserve code. If
    // we can't find such location, we will find a empty location and reserve the
    // location with the code
    @Transactional
    public Location reserveLocation(Long id, String reservedCode, Double pendingSize, Long pendingQuantity, int pendingPalletQuantity) {
        LocationGroup locationGroup = findById(id);
        logger.debug("Start to reserve a location from group: {}", locationGroup.getName());

        // Let's check if we have any location in the group that has the same reserve code
        List<Location> locations = locationService.findByLocationGroup(id);
        logger.debug("Get {} locations from the group: {}",
                      locations.size(), locationGroup.getName());

        List<Location> locationsWithSameReservedCode = locations.stream().filter(location -> reservedCode.equals(location.getReservedCode())).collect(Collectors.toList());

        logger.debug(">> including {} locations that have hte same reserve code", locationsWithSameReservedCode.size());
        for (Location location : locationsWithSameReservedCode) {
            logger.debug(">>   ===> try location {} with same reserve code", location.getName());
            // See if we can still add more inventory into the location with the same reserve code
            if (!locationGroup.getTrackingVolume()) {
                // OK, the location doesn't need volume tracking. We can add infinite volume to this location
                // we will just return this location without even change it

                logger.debug(">> return location {} same reserve code, size is not tracked for the location", location.getName());
                return location;
            }
            else {
                // OK, we tracking volume for the location. Let's see how we tracking the location
                if (ifVolumeFitForLocation(locationGroup.getVolumeTrackingPolicy(), location, pendingSize, pendingQuantity, pendingPalletQuantity)) {
                    logger.debug(">> return location {} same reserve code, volume is tracked and validated", location.getName());
                    return reserveLocation(locationGroup.getVolumeTrackingPolicy(), location, reservedCode, pendingSize, pendingQuantity, pendingPalletQuantity);

                }
            }
        }

        // If we are still here, we can't find any location with same reserve code, let's find any location that
        // is not reserved yet
        List<Location> locationsWithoutReservedCode =
                locations.stream().filter(location -> StringUtils.isBlank(location.getReservedCode())).collect(Collectors.toList());

        logger.debug(">> including {} locations that have empty reserve code", locationsWithoutReservedCode.size());
        for (Location location : locationsWithoutReservedCode) {
            logger.debug(">>   ===> try location {} with EMPTY reserve code", location.getName());
            // See if we can still add more inventory into the location
            if (!locationGroup.getTrackingVolume()) {
                // OK, the location doesn't need volume tracking. We can add infinite volume to this location
                // we will just return this location without even change it
                logger.debug(">> return location {} with empty reserved code, size is not tracked for the location", location.getName());
                return locationService.reserveLocation(location, reservedCode);
            }
            else {
                // OK, we tracking volume for the location. Let's see how we tracking the location
                if (ifVolumeFitForLocation(locationGroup.getVolumeTrackingPolicy(), location, pendingSize, pendingQuantity, pendingPalletQuantity)) {
                    logger.debug(">> return location {} EMPTY reserve code, volume is tracked and validated", location.getName());
                    return reserveLocation(locationGroup.getVolumeTrackingPolicy(), location, reservedCode, pendingSize, pendingQuantity, pendingPalletQuantity);

                }
            }
        }

        logger.debug(">> No location has been found");
        // If we are here, we fail to find any location
        throw LocationOperationException.raiseException("fail to reserve a location from the group");
    }

    private boolean ifVolumeFitForLocation(LocationVolumeTrackingPolicy locationVolumeTrackingPolicy,
                                           Location location, Double pendingSize,
                                           Long pendingQuantity, int pendingPalletQuantity) {
        // Get the empty space from the location
        Double emptySpace = location.getCapacity() - location.getCurrentVolume() - location.getPendingVolume();
        // If we don't have any empty space, return false
        if (emptySpace <= 0) {
            return false;
        }
        switch (locationVolumeTrackingPolicy) {
            case BY_EACH:
                return emptySpace >= pendingQuantity;
            case BY_VOLUME:
                return emptySpace >= pendingSize;
            case BY_PALLET:
                return emptySpace >= pendingPalletQuantity;

        }
        return false;
    }

    private Location reserveLocation(LocationVolumeTrackingPolicy locationVolumeTrackingPolicy,
                                    Location location, String reservedCode, Double pendingSize,
                                           Long pendingQuantity, int pendingPalletQuantity) {
        switch (locationVolumeTrackingPolicy) {
            case BY_EACH:
                return locationService.reserveLocation(location, reservedCode, (double)pendingQuantity);
            case BY_VOLUME:
                return locationService.reserveLocation(location, reservedCode, (double)pendingSize);
            case BY_PALLET:
                return locationService.reserveLocation(location, reservedCode, (double)pendingPalletQuantity);

        }
        return locationService.reserveLocation(location, reservedCode, (double)pendingQuantity);

    }

    public LocationGroup getDockLocationGroup(Long warehouseId) {

        List<LocationGroup> locationGroups = locationGroupRepository.getDockLocationGroup(warehouseId);
        if (locationGroups.size() > 0) {
            return locationGroups.get(0);
        }
        else {
            return null;
        }
    }

    public LocationGroup getShippedParcelLocationGroup(Long warehouseId) {
        List<LocationGroup> locationGroups = locationGroupRepository.getShippedParcelLocationGroup(warehouseId);
        if (locationGroups.size() > 0) {
            return locationGroups.get(0);
        }
        else {
            return null;
        }
    }

    public LocationGroup getShippedOrderLocationGroup(Long warehouseId) {
        List<LocationGroup> locationGroups = locationGroupRepository.getShippedOrderLocationGroup(warehouseId);
        if (locationGroups.size() > 0) {
            return locationGroups.get(0);
        }
        else {
            return null;
        }
    }

    public void removeLocationGroup(long id) {
        LocationGroup locationGroup = findById(id);
        // Remove all the locations in this group
        locationService.removeLocationByGroup(locationGroup);

        // remove the location group
        delete(id);
    }

    public LocationGroup addLocationGroups(LocationGroup locationGroup) {
        if(locationGroup.getTrackingVolume() == false) {
            locationGroup.setVolumeTrackingPolicy(null);
        }
        locationGroup.getPickableUnitOfMeasures().forEach(
                pickableUnitOfMeasure -> pickableUnitOfMeasure.setLocationGroup(locationGroup)
        );
        return save(locationGroup);
    }

    @Transactional
    public void removeLocationGroups(Warehouse warehouse) {
        locationGroupRepository.deleteByWarehouseId(warehouse.getId());
    }
}

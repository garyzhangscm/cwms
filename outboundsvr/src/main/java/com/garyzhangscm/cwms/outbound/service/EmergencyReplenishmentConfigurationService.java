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

package com.garyzhangscm.cwms.outbound.service;

import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.garyzhangscm.cwms.outbound.clients.CommonServiceRestemplateClient;
import com.garyzhangscm.cwms.outbound.clients.InventoryServiceRestemplateClient;
import com.garyzhangscm.cwms.outbound.clients.WarehouseLayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.outbound.exception.GenericException;
import com.garyzhangscm.cwms.outbound.exception.ReplenishmentException;
import com.garyzhangscm.cwms.outbound.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.outbound.model.*;
import com.garyzhangscm.cwms.outbound.repository.AllocationConfigurationRepository;
import com.garyzhangscm.cwms.outbound.repository.EmergencyReplenishmentConfigurationRepository;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import javax.persistence.criteria.CriteriaBuilder;
import javax.transaction.Transactional;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;


@Service
public class EmergencyReplenishmentConfigurationService implements TestDataInitiableService {
    private static final Logger logger = LoggerFactory.getLogger(EmergencyReplenishmentConfigurationService.class);

    @Autowired
    private EmergencyReplenishmentConfigurationRepository emergencyReplenishmentConfigurationRepository;

    @Autowired
    private InventorySummaryService inventorySummaryService;
    @Autowired
    private ShortAllocationService shortAllocationService;
    @Autowired
    private PickService pickService;
    @Autowired
    private PickableUnitOfMeasureService pickableUnitOfMeasureService;

    @Autowired
    private CommonServiceRestemplateClient commonServiceRestemplateClient;
    @Autowired
    private WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient;
    @Autowired
    private InventoryServiceRestemplateClient inventoryServiceRestemplateClient;
    @Autowired
    private FileService fileService;

    @Value("${fileupload.test-data.emergency-replenishment-configuration:emergency-replenishment-configuration}")
    String testDataFile;

    public EmergencyReplenishmentConfiguration findById(Long id, boolean loadDetails) {
        EmergencyReplenishmentConfiguration emergencyReplenishmentConfiguration
                = emergencyReplenishmentConfigurationRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("emergency replenishment configuration not found by id: " + id));
        if (loadDetails) {
            loadAttribute(emergencyReplenishmentConfiguration);
        }
        return emergencyReplenishmentConfiguration;
    }

    public EmergencyReplenishmentConfiguration findBySequence(int sequence) {
        return findBySequence(sequence, true);
    }


    public EmergencyReplenishmentConfiguration findBySequence(int sequence, boolean loadDetails) {
        EmergencyReplenishmentConfiguration emergencyReplenishmentConfiguration = emergencyReplenishmentConfigurationRepository.findBySequence(sequence);
        if (emergencyReplenishmentConfiguration != null && loadDetails) {
            loadAttribute(emergencyReplenishmentConfiguration);
        }
        return emergencyReplenishmentConfiguration;
    }

    public EmergencyReplenishmentConfiguration findById(Long id) {
        return findById(id, true);
    }


    public List<EmergencyReplenishmentConfiguration> findAll(boolean loadDetails) {
        List<EmergencyReplenishmentConfiguration> emergencyReplenishmentConfigurations = emergencyReplenishmentConfigurationRepository.findAll();

        if (emergencyReplenishmentConfigurations.size() > 0 && loadDetails) {
            loadAttribute(emergencyReplenishmentConfigurations);
        }
        return emergencyReplenishmentConfigurations;
    }

    public List<EmergencyReplenishmentConfiguration> findAll() {
        return findAll(true);
    }



    private void loadAttribute(EmergencyReplenishmentConfiguration emergencyReplenishmentConfiguration) {

        if (emergencyReplenishmentConfiguration.getItemId() != null && emergencyReplenishmentConfiguration.getItem() == null) {
            emergencyReplenishmentConfiguration.setItem(inventoryServiceRestemplateClient.getItemById(emergencyReplenishmentConfiguration.getItemId()));
        }
        if (emergencyReplenishmentConfiguration.getItemFamilyId() != null && emergencyReplenishmentConfiguration.getItemFamily() == null) {
            emergencyReplenishmentConfiguration.setItemFamily(inventoryServiceRestemplateClient.getItemFamilyById(emergencyReplenishmentConfiguration.getItemFamilyId()));
        }
        if (emergencyReplenishmentConfiguration.getSourceLocationId() != null && emergencyReplenishmentConfiguration.getSourceLocation() == null) {
            emergencyReplenishmentConfiguration.setSourceLocation(
                    warehouseLayoutServiceRestemplateClient.getLocationById(
                            emergencyReplenishmentConfiguration.getSourceLocationId()));
        }
        if (emergencyReplenishmentConfiguration.getSourceLocationGroupId() != null && emergencyReplenishmentConfiguration.getSourceLocationGroup() == null) {
            emergencyReplenishmentConfiguration.setSourceLocationGroup(
                    warehouseLayoutServiceRestemplateClient.getLocationGroupById(
                            emergencyReplenishmentConfiguration.getSourceLocationGroupId()));
        }


        if (emergencyReplenishmentConfiguration.getDestinationLocationId() != null && emergencyReplenishmentConfiguration.getDestinationLocation() == null) {
            emergencyReplenishmentConfiguration.setDestinationLocation(
                    warehouseLayoutServiceRestemplateClient.getLocationById(
                            emergencyReplenishmentConfiguration.getDestinationLocationId()));
        }
        if (emergencyReplenishmentConfiguration.getDestinationLocationGroupId() != null && emergencyReplenishmentConfiguration.getDestinationLocationGroup() == null) {
            emergencyReplenishmentConfiguration.setDestinationLocationGroup(
                    warehouseLayoutServiceRestemplateClient.getLocationGroupById(
                            emergencyReplenishmentConfiguration.getDestinationLocationGroupId()));
        }


    }

    private void loadAttribute(List<EmergencyReplenishmentConfiguration> emergencyReplenishmentConfigurations) {
        emergencyReplenishmentConfigurations.forEach(emergencyReplenishmentConfiguration -> loadAttribute(emergencyReplenishmentConfiguration));
    }

    public EmergencyReplenishmentConfiguration save(EmergencyReplenishmentConfiguration emergencyReplenishmentConfiguration) {
        EmergencyReplenishmentConfiguration newEmergencyReplenishmentConfiguration = emergencyReplenishmentConfigurationRepository.save(emergencyReplenishmentConfiguration);
        loadAttribute(newEmergencyReplenishmentConfiguration);
        return newEmergencyReplenishmentConfiguration;
    }

    public EmergencyReplenishmentConfiguration saveOrUpdate(EmergencyReplenishmentConfiguration emergencyReplenishmentConfiguration) {
        if (emergencyReplenishmentConfiguration.getId() == null && findBySequence(emergencyReplenishmentConfiguration.getSequence()) != null) {
            emergencyReplenishmentConfiguration.setId(findBySequence(emergencyReplenishmentConfiguration.getSequence()).getId());
        }
        return save(emergencyReplenishmentConfiguration);
    }


    public void delete(EmergencyReplenishmentConfiguration emergencyReplenishmentConfiguration) {
        emergencyReplenishmentConfigurationRepository.delete(emergencyReplenishmentConfiguration);
    }

    public void delete(Long id) {
        emergencyReplenishmentConfigurationRepository.deleteById(id);
    }

    public void delete(String emergencyReplenishmentConfigurationIds) {
        if (!emergencyReplenishmentConfigurationIds.isEmpty()) {
            long[] emergencyReplenishmentConfigurationIdArray = Arrays.asList(emergencyReplenishmentConfigurationIds.split(",")).stream().mapToLong(Long::parseLong).toArray();
            for (long id : emergencyReplenishmentConfigurationIdArray) {
                delete(id);
            }
        }
    }


    public List<EmergencyReplenishmentConfigurationCSVWrapper> loadData(InputStream inputStream) throws IOException {

        CsvSchema schema = CsvSchema.builder().
                addColumn("warehouse").
                addColumn("sequence").
                addColumn("item").
                addColumn("itemFamily").
                addColumn("sourceLocation").
                addColumn("sourceLocationGroup").
                addColumn("destinationLocation").
                addColumn("destinationLocationGroup").
                build().withHeader();

        return fileService.loadData(inputStream, schema, EmergencyReplenishmentConfigurationCSVWrapper.class);
    }

    public void initTestData(String warehouseName) {
        try {
            String testDataFileName = StringUtils.isBlank(warehouseName) ?
                    testDataFile + ".csv" :
                    testDataFile + "-" + warehouseName + ".csv";
            InputStream inputStream = new ClassPathResource(testDataFileName).getInputStream();
            List<EmergencyReplenishmentConfigurationCSVWrapper> emergencyReplenishmentConfigurationCSVWrappers = loadData(inputStream);
            emergencyReplenishmentConfigurationCSVWrappers
                    .stream()
                    .forEach(
                        emergencyReplenishmentConfigurationCSVWrapper
                                -> saveOrUpdate(convertFromWrapper(emergencyReplenishmentConfigurationCSVWrapper)));
        } catch (IOException ex) {
            logger.debug("Exception while load test data: {}", ex.getMessage());
        }
    }

    private EmergencyReplenishmentConfiguration convertFromWrapper(EmergencyReplenishmentConfigurationCSVWrapper emergencyReplenishmentConfigurationCSVWrapper) {

        EmergencyReplenishmentConfiguration emergencyReplenishmentConfiguration = new EmergencyReplenishmentConfiguration();
        emergencyReplenishmentConfiguration.setSequence(emergencyReplenishmentConfigurationCSVWrapper.getSequence());

        Warehouse warehouse =
                warehouseLayoutServiceRestemplateClient.getWarehouseByName(emergencyReplenishmentConfigurationCSVWrapper.getWarehouse());

        emergencyReplenishmentConfiguration.setWarehouseId(warehouse.getId());

        if (!StringUtils.isBlank(emergencyReplenishmentConfigurationCSVWrapper.getItem())) {
            Item item = inventoryServiceRestemplateClient.getItemByName(
                    warehouse.getId(), emergencyReplenishmentConfigurationCSVWrapper.getItem());
            if (item != null) {
                emergencyReplenishmentConfiguration.setItemId(item.getId());
            }
        }
        if (!StringUtils.isBlank(emergencyReplenishmentConfigurationCSVWrapper.getItemFamily())) {
            ItemFamily itemFamily = inventoryServiceRestemplateClient.getItemFamilyByName(
                    warehouse.getId(), emergencyReplenishmentConfigurationCSVWrapper.getItemFamily());
            if (itemFamily != null) {
                emergencyReplenishmentConfiguration.setItemFamilyId(itemFamily.getId());
            }
        }

        if (!StringUtils.isBlank(emergencyReplenishmentConfigurationCSVWrapper.getSourceLocation())) {
            Location location = warehouseLayoutServiceRestemplateClient.getLocationByName(
                    emergencyReplenishmentConfigurationCSVWrapper.getWarehouse(), emergencyReplenishmentConfigurationCSVWrapper.getSourceLocation());
            if (location != null) {
                emergencyReplenishmentConfiguration.setSourceLocationId(location.getId());
            }
        }

        if (!StringUtils.isBlank(emergencyReplenishmentConfigurationCSVWrapper.getSourceLocationGroup())) {
            LocationGroup locationGroup = warehouseLayoutServiceRestemplateClient.getLocationGroupByName(
                    emergencyReplenishmentConfigurationCSVWrapper.getWarehouse(), emergencyReplenishmentConfigurationCSVWrapper.getSourceLocationGroup());
            if (locationGroup != null) {
                emergencyReplenishmentConfiguration.setSourceLocationGroupId(locationGroup.getId());
            }
        }


        if (!StringUtils.isBlank(emergencyReplenishmentConfigurationCSVWrapper.getDestinationLocation())) {
            Location location = warehouseLayoutServiceRestemplateClient.getLocationByName(
                    emergencyReplenishmentConfigurationCSVWrapper.getWarehouse(), emergencyReplenishmentConfigurationCSVWrapper.getDestinationLocation());
            if (location != null) {
                emergencyReplenishmentConfiguration.setDestinationLocationId(location.getId());
            }
        }

        if (!StringUtils.isBlank(emergencyReplenishmentConfigurationCSVWrapper.getDestinationLocationGroup())) {
            LocationGroup locationGroup = warehouseLayoutServiceRestemplateClient.getLocationGroupByName(
                    emergencyReplenishmentConfigurationCSVWrapper.getWarehouse(), emergencyReplenishmentConfigurationCSVWrapper.getDestinationLocationGroup());
            if (locationGroup != null) {
                emergencyReplenishmentConfiguration.setDestinationLocationGroupId(locationGroup.getId());
            }
        }

        return emergencyReplenishmentConfiguration;

    }

    public Location getEmergencyReplenishmentDestination(Pick pick) {
        // make sure the pick is a emergency replneishment type of pick
        if (pick.getShortAllocation() == null) {
            throw ReplenishmentException.raiseException("The pick is not type of emergency replenishment");

        }

        List<EmergencyReplenishmentConfiguration> matchedEmergencyReplenishmentConfigurations
                = getMatchedEmergencyReplenishmentConfiguration(pick);

        if (matchedEmergencyReplenishmentConfigurations.size() == 0) {
            // no matched emergency replenishment configuration defined for this pick, throw an exception
            // so that we won't even generate the pick.
            // The emergency replenishment configuration needs to be defined so that we can
            // find the destination location for the pick
            throw ReplenishmentException.raiseException("No emergency replenishment found for the pick");
        }

        // Let's loop through each emergency replenishment until we find a suitable location
        // as the destination location for the emergency replenishment that we are going to generate
        // The location needs to be
        // enabled
        // empty or mixed with same item and same inventory status
        // pickable

        matchedEmergencyReplenishmentConfigurations.sort(Comparator.comparing(EmergencyReplenishmentConfiguration::getSequence));
        for(EmergencyReplenishmentConfiguration emergencyReplenishmentConfiguration : matchedEmergencyReplenishmentConfigurations) {
            if (emergencyReplenishmentConfiguration.getDestinationLocation() != null) {
                // The configuration want us to replenish into a specific location.
                // check if we can replenish into this location
                Location location = emergencyReplenishmentConfiguration.getDestinationLocation();
                if (locationGoodForEmergencyReplenishment(location, pick)) {
                    return location;
                }
                else {
                    continue;
                }
            }
            else if (emergencyReplenishmentConfiguration.getDestinationLocationGroup() != null) {
                // first, let's check if there's any location in the destination location group
                // that already have the same item and inventory status
                List<Inventory> inventories = inventoryServiceRestemplateClient.getInventoryByLocationGroup(
                        pick.getItem(),pick.getInventoryStatus(),emergencyReplenishmentConfiguration.getDestinationLocationGroup()
                );
                if (inventories.size() > 0) {
                    // Ok we found inventory with same item and inventory status exists in the
                    // destination location group, let's loop each location and see if we can replenishment into
                    // that location
                    List<Long> locationIdsWithExistingInventory =
                            inventories.stream()
                                .map(inventory -> inventory.getLocationId())
                                .filter(locationId -> locationGoodForEmergencyReplenishment(locationId, pick))
                                .collect(Collectors.toList());
                    if (locationIdsWithExistingInventory.size() > 0) {
                        // OK, we find location with existing inventory and
                        // good for the emergency replenishment, let's just return the
                        // first location we ever find
                        return warehouseLayoutServiceRestemplateClient.getLocationById(locationIdsWithExistingInventory.get(0));
                    }
                }


                // OK, if we are still here, it means we will need to find an empty location
                // as the destination of the emergency replenishment
                try {
                    Location location = warehouseLayoutServiceRestemplateClient.findEmptyDestinationLocationForEmergencyReplenishment(
                            emergencyReplenishmentConfiguration.getDestinationLocationGroup(), pick.getSize());
                    if (location != null) {
                        return location;
                    }
                }
                catch(GenericException ex) {
                    // in case we can't find any empty location for by the configuration,
                    // ignore the error and continue with next configuration;
                }

            }

        }


        // If we are still here, it means we already looped through all the configuration but still
        // not able to find any destination location
        throw ReplenishmentException.raiseException("Can't find any destination location for the emergency replenishment");
    }

    private List<EmergencyReplenishmentConfiguration> getMatchedEmergencyReplenishmentConfiguration(Pick pick) {
        List<EmergencyReplenishmentConfiguration> emergencyReplenishmentConfigurations = findAll();
        return emergencyReplenishmentConfigurations
                .stream()
                .filter(emergencyReplenishmentConfiguration -> match(pick, emergencyReplenishmentConfiguration))
                .collect(Collectors.toList());

    }



    // Whether the emergency replenishment configuration matches with the pick only when
    // if configuration has item defined and match with the pick's item
    // if configuration has item family defined and match with the pick's item
    // if configuration has source location and match with the pick's source location
    // if configuration has source location group defined and match with the pick's source location group
    private boolean match(Pick pick, EmergencyReplenishmentConfiguration emergencyReplenishmentConfiguration) {
        logger.debug("==================   Emergency Replenishment VS Pick ======================");
        logger.debug("Step 1.1 check {} matches with item {}", emergencyReplenishmentConfiguration.getId(), pick.getNumber());
        if (emergencyReplenishmentConfiguration.getItemId() != null &&
                pick.getItem().getId() != emergencyReplenishmentConfiguration.getItemId()) {

            logger.debug("Step 1.2 >> fail as the item doesn't match.");
            logger.debug(">>>>>>>>>>> emergencyReplenishmentConfiguration.getItemId(): {} / pick.getItem().getId(): {}",
                    emergencyReplenishmentConfiguration.getItemId(), pick.getItem().getId());
            return false;
        }

        if (emergencyReplenishmentConfiguration.getItemFamilyId() != null &&
                pick.getItem().getItemFamily().getId() != emergencyReplenishmentConfiguration.getItemFamilyId()) {

            logger.debug("Step 1.2 >> fail as the item family doesn't match.");
            logger.debug(">>>>>>>>>>> emergencyReplenishmentConfiguration.getItemFamilyId(): {} / pick.getItem().getItemFamily().getId(): {}",
                    emergencyReplenishmentConfiguration.getItemFamilyId(), pick.getItem().getItemFamily().getId());
            return false;
        }


        if (emergencyReplenishmentConfiguration.getSourceLocationId() != null &&
                pick.getSourceLocationId() != emergencyReplenishmentConfiguration.getSourceLocationId()) {

            logger.debug("Step 1.2 >> fail as the item family doesn't match.");
            logger.debug(">>>>>>>>>>> emergencyReplenishmentConfiguration.getSourceLocationId(): {} / pick.getSourceLocationId(): {}",
                    emergencyReplenishmentConfiguration.getSourceLocationId(), pick.getSourceLocationId());
            return false;
        }
        if (emergencyReplenishmentConfiguration.getSourceLocationGroupId() != null &&
                pick.getSourceLocation().getLocationGroup().getId() != emergencyReplenishmentConfiguration.getSourceLocationGroupId()) {

            logger.debug("Step 1.2 >> fail as the item family doesn't match.");
            logger.debug(">>>>>>>>>>> emergencyReplenishmentConfiguration.getSourceLocationGroupId(): {} / pick.getSourceLocation().getLocationGroup().getId(): {}",
                    emergencyReplenishmentConfiguration.getSourceLocationGroupId(), pick.getSourceLocation().getLocationGroup().getId());
            return false;
        }

        logger.debug("Step 1.2 >> pick matches with emergency replenishment configuration.");
        return true;

    }

    private boolean locationGoodForEmergencyReplenishment(Long locationId, Pick pick) {

        Location location = warehouseLayoutServiceRestemplateClient.getLocationById(locationId);
        if (location != null) {
            return locationGoodForEmergencyReplenishment(location, pick);
        }
        else {
            // Can't find the location by the ID,
            return false;
        }
    }
    // Check if we can use the location as a destination location for an emergency replenishment
    // The location needs to be
    // 1. enabled
    // 2. pickable
    // 3. empty or mixed with same item and same inventory status
    private boolean locationGoodForEmergencyReplenishment(Location location, Pick pick) {
        if (!location.getEnabled()) {
            return false;
        }

        if (!location.getLocationGroup().getPickable()) {
            return false;
        }

        if (location.getCurrentVolume() == 0 && location.getPendingVolume() == 0) {
            // location is empty, we are good to go
            return true;
        }
        else{
            if (location.getCurrentVolume() > 0) {
                List<Inventory> inventories = inventoryServiceRestemplateClient.getInventoryByLocation(location);
                for (Inventory inventory : inventories) {
                    if (!isEmergencyReplenishmentMixableWithInventory(pick, inventory)) {
                        return false;
                    }
                }
            }

            if (location.getPendingVolume() > 0) {
                List<Inventory> inventories = inventoryServiceRestemplateClient.getPendingInventoryByLocation(location);
                for (Inventory inventory : inventories) {
                    if (!isEmergencyReplenishmentMixableWithInventory(pick, inventory)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    // Check if a emergency replenishment can be mixed with the inventory in the destination location
    // the function will return true only when the inventory has the same
    // 1. item
    // 2. inventory status
    private boolean isEmergencyReplenishmentMixableWithInventory(Pick pick, Inventory inventory) {
        return pick.getItem().equals(inventory.getItem()) &&
                pick.getInventoryStatus().equals(inventory.getInventoryStatus());
    }

}

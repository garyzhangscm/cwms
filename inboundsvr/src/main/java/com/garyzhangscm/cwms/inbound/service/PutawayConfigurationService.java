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

package com.garyzhangscm.cwms.inbound.service;

import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.garyzhangscm.cwms.inbound.clients.InventoryServiceRestemplateClient;
import com.garyzhangscm.cwms.inbound.clients.WarehouseLayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.inbound.exception.GenericException;
import com.garyzhangscm.cwms.inbound.exception.PutawayException;
import com.garyzhangscm.cwms.inbound.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.inbound.model.*;
import com.garyzhangscm.cwms.inbound.repository.PutawayConfigurationRepository;
import net.bytebuddy.asm.Advice;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.criteria.*;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PutawayConfigurationService implements TestDataInitiableService{
    private static final Logger logger = LoggerFactory.getLogger(PutawayConfigurationService.class);

    @Autowired
    private PutawayConfigurationRepository putawayConfigurationRepository;
    @Autowired
    private InventoryServiceRestemplateClient inventoryServiceRestemplateClient;
    @Autowired
    private WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient;
    @Autowired
    private PutawayConfigurationStrategyService putawayConfigurationStrategyService;

    @Autowired
    private FileService fileService;

    @Value("${fileupload.test-data.putaway_configuration:putaway_configuration}")
    String testDataFile;

    public PutawayConfiguration findById(Long id) {
        return putawayConfigurationRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("putaway configuration not found by id: " + id));
    }

    public List<PutawayConfiguration> findAll() {
        return putawayConfigurationRepository.findAll();
    }

    public List<PutawayConfiguration> findAll(Long warehouseId,
                                              Integer sequence,
                                              String itemName,
                                              String itemFamilyName,
                                              Long inventoryStatusId) {

        List<PutawayConfiguration> putawayConfigurations =  putawayConfigurationRepository.findAll(
                (Root<PutawayConfiguration> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();
                    if (warehouseId != null) {
                        predicates.add(criteriaBuilder.equal(root.get("warehouseId"), warehouseId));
                    }
                    if (sequence != null) {
                        predicates.add(criteriaBuilder.equal(root.get("sequence"), sequence));

                    }

                    if (!StringUtils.isBlank(itemName) && warehouseId != null) {
                        Item item = inventoryServiceRestemplateClient.getItemByName(warehouseId, itemName);
                        if (item != null) {
                            predicates.add(criteriaBuilder.equal(root.get("itemId"), item.getId()));
                        }
                        else {
                            // web client passed in a incorrect item name, we will return an empty result

                            predicates.add(criteriaBuilder.equal(root.get("itemId"), -1L));
                        }
                    }
                    if (!StringUtils.isBlank(itemFamilyName) && warehouseId != null) {
                        ItemFamily itemFamily = inventoryServiceRestemplateClient.getItemFamilyByName(warehouseId, itemFamilyName);
                        if (itemFamily != null) {
                            predicates.add(criteriaBuilder.equal(root.get("itemFamilyId"), itemFamily.getId()));
                        }
                        else {
                            // web client passed in a incorrect item family name, we will return an empty result
                            predicates.add(criteriaBuilder.equal(root.get("itemFamilyId"), -1L));
                        }
                    }
                    if (inventoryStatusId != null) {
                        predicates.add(criteriaBuilder.equal(root.get("inventoryStatusId"), inventoryStatusId));
                    }
                    Predicate[] p = new Predicate[predicates.size()];
                    return criteriaBuilder.and(predicates.toArray(p));
                }
        );

        if (putawayConfigurations.size() > 0) {
            loadAttribute(putawayConfigurations);
        }
        return putawayConfigurations;
    }

    private void loadAttribute(PutawayConfiguration putawayConfiguration) {

        if (putawayConfiguration.getItemId() != null && putawayConfiguration.getItem() == null) {
            putawayConfiguration.setItem(inventoryServiceRestemplateClient.getItemById(putawayConfiguration.getItemId()));
        }
        if (putawayConfiguration.getItemFamilyId() != null && putawayConfiguration.getItemFamily() == null) {
            putawayConfiguration.setItemFamily(inventoryServiceRestemplateClient.getItemFamilyById(putawayConfiguration.getItemFamilyId()));
        }

        if (putawayConfiguration.getInventoryStatusId() != null && putawayConfiguration.getInventoryStatus() == null) {
            putawayConfiguration.setInventoryStatus(inventoryServiceRestemplateClient.getInventoryStatusById(putawayConfiguration.getInventoryStatusId()));
        }


        if (putawayConfiguration.getLocationId() != null && putawayConfiguration.getLocation() == null) {
            putawayConfiguration.setLocation(warehouseLayoutServiceRestemplateClient.getLocationById(putawayConfiguration.getLocationId()));
        }
        if (putawayConfiguration.getLocationGroupId() != null && putawayConfiguration.getLocationGroup() == null) {
            putawayConfiguration.setLocationGroup(warehouseLayoutServiceRestemplateClient.getLocationGroupById(putawayConfiguration.getLocationGroupId()));
        }
        if (putawayConfiguration.getLocationGroupTypeId() != null && putawayConfiguration.getLocationGroupType() == null) {
            putawayConfiguration.setLocationGroupType(warehouseLayoutServiceRestemplateClient.getLocationGroupTypeById(putawayConfiguration.getLocationGroupTypeId()));
        }

        if (!StringUtils.isBlank(putawayConfiguration.getStrategies()) && putawayConfiguration.getPutawayConfigurationStrategies().size() == 0) {

            putawayConfiguration.setPutawayConfigurationStrategies(
                    Arrays.stream(putawayConfiguration.getStrategies().split(","))
                            .map(strategy -> PutawayConfigurationStrategy.valueOf(strategy)).collect(Collectors.toList()));
        }
    }

    private void loadAttribute(List<PutawayConfiguration> putawayConfigurations) {
        putawayConfigurations.forEach(putawayConfiguration -> loadAttribute(putawayConfiguration));
    }

    public PutawayConfiguration findBySequence(int sequence) {
        return putawayConfigurationRepository.findBySequence(sequence);
    }

    @Transactional
    public PutawayConfiguration save(PutawayConfiguration putawayConfiguration) {
        return putawayConfigurationRepository.save(putawayConfiguration);
    }


    @Transactional
    public PutawayConfiguration saveOrUpdate(PutawayConfiguration putawayConfiguration) {
        if (putawayConfiguration.getId() == null && findBySequence(putawayConfiguration.getSequence()) != null) {
            putawayConfiguration.setId(findBySequence(putawayConfiguration.getSequence()).getId());
        }
        return putawayConfigurationRepository.save(putawayConfiguration);
    }

    @Transactional
    public void delete(PutawayConfiguration putawayConfiguration) {
        putawayConfigurationRepository.delete(putawayConfiguration);
    }
    @Transactional
    public void delete(Long id) {
        putawayConfigurationRepository.deleteById(id);
    }
    @Transactional
    public void delete(String putawayConfigurationIds) {
        if (!StringUtils.isBlank(putawayConfigurationIds)) {
            long[] putawayConfigurationIdArray = Arrays.asList(putawayConfigurationIds.split(",")).stream().mapToLong(Long::parseLong).toArray();
            for(long id : putawayConfigurationIdArray) {
                delete(id);
            }
        }
    }

    public List<PutawayConfigurationCSVWrapper> loadData(InputStream inputStream) throws IOException {

        CsvSchema schema = CsvSchema.builder().
                addColumn("warehouse").
                addColumn("sequence").
                addColumn("item").
                addColumn("itemFamily").
                addColumn("inventoryStatus").
                addColumn("location").
                addColumn("locationGroup").
                addColumn("locationGroupType").
                addColumn("strategies").
                build().withHeader();

        return fileService.loadData(inputStream, schema, PutawayConfigurationCSVWrapper.class);
    }



    @Transactional
    public void initTestData(String warehouseName) {
        try {
            String testDataFileName = StringUtils.isBlank(warehouseName) ?
                    testDataFile + ".csv" :
                    testDataFile + "-" + warehouseName + ".csv";
            InputStream inputStream = new ClassPathResource(testDataFileName).getInputStream();
            List<PutawayConfigurationCSVWrapper> putawayConfigurationCSVWrappers = loadData(inputStream);
            putawayConfigurationCSVWrappers.stream().forEach(putawayConfigurationCSVWrapper -> saveOrUpdate(convertFromWrapper(putawayConfigurationCSVWrapper)));
        } catch (IOException ex) {
            logger.debug("Exception while load test data: {}", ex.getMessage());
        }
    }

    private PutawayConfiguration convertFromWrapper(PutawayConfigurationCSVWrapper putawayConfigurationCSVWrapper) {

        PutawayConfiguration putawayConfiguration = new PutawayConfiguration();
        putawayConfiguration.setSequence(putawayConfigurationCSVWrapper.getSequence());
        putawayConfiguration.setStrategies(putawayConfigurationCSVWrapper.getStrategies());

        // Warehouse is mandate
        Warehouse warehouse = warehouseLayoutServiceRestemplateClient.getWarehouseByName(putawayConfigurationCSVWrapper.getWarehouse());

        putawayConfiguration.setWarehouseId(warehouse.getId());

        if (!StringUtils.isBlank(putawayConfigurationCSVWrapper.getItem())) {
            Item item =
                    inventoryServiceRestemplateClient.getItemByName(warehouse.getId(), putawayConfigurationCSVWrapper.getItem());
            if (item != null) {
                putawayConfiguration.setItemId(item.getId());
            }
        }
        if (!StringUtils.isBlank(putawayConfigurationCSVWrapper.getItemFamily())) {
            ItemFamily itemFamily
                    = inventoryServiceRestemplateClient.getItemFamilyByName(warehouse.getId(), putawayConfigurationCSVWrapper.getItemFamily());
            if (itemFamily != null) {
                putawayConfiguration.setItemFamilyId(itemFamily.getId());
            }
        }
        if (!StringUtils.isBlank(putawayConfigurationCSVWrapper.getInventoryStatus())) {
            InventoryStatus inventoryStatus
                    = inventoryServiceRestemplateClient.getInventoryStatusByName(warehouse.getId(), putawayConfigurationCSVWrapper.getInventoryStatus());
            if (inventoryStatus != null) {
                putawayConfiguration.setInventoryStatusId(inventoryStatus.getId());
            }
        }

        if (!StringUtils.isBlank(putawayConfigurationCSVWrapper.getLocation())) {
            Location location = warehouseLayoutServiceRestemplateClient.getLocationByName(
                    getWarehouseId(putawayConfigurationCSVWrapper.getWarehouse()), putawayConfigurationCSVWrapper.getLocation());
            if (location != null) {
                putawayConfiguration.setLocationId(location.getId());
            }
        }

        if (!StringUtils.isBlank(putawayConfigurationCSVWrapper.getLocationGroup())) {
            LocationGroup locationGroup = warehouseLayoutServiceRestemplateClient.getLocationGroupByName(
                    getWarehouseId(putawayConfigurationCSVWrapper.getWarehouse()), putawayConfigurationCSVWrapper.getLocationGroup());
            if (locationGroup != null) {
                putawayConfiguration.setLocationGroupId(locationGroup.getId());
            }
        }

        if (!StringUtils.isBlank(putawayConfigurationCSVWrapper.getLocationGroupType())) {
            LocationGroupType locationGroupType = warehouseLayoutServiceRestemplateClient.getLocationGroupTypeByName(putawayConfigurationCSVWrapper.getLocationGroupType());
            if (locationGroupType != null) {
                putawayConfiguration.setLocationGroupTypeId(locationGroupType.getId());
            }
        }
        return putawayConfiguration;
    }

    @Transactional
    public Inventory allocateLocation(Inventory inventory){
        logger.debug("start to allocate location for inventory: {}", inventory.getLpn());
        Location location = allocateSuitableLocation(inventory);
        if (location == null) {
            throw PutawayException.raiseException("fail to allocate location for the inventory");
        }
        else {
            warehouseLayoutServiceRestemplateClient.allocateLocation(location, inventory.getSize());

        }
        InventoryMovement inventoryMovement = new InventoryMovement();
        inventoryMovement.setInventory(inventory);
        inventoryMovement.setLocation(location);

        return inventoryServiceRestemplateClient.setupMovementPath(inventory.getId(), Arrays.asList(new InventoryMovement[]{inventoryMovement}));
    }


    private Location allocateSuitableLocation(Inventory inventory) {
        // First of all, let's find all suitable putaway configuration and
        // sort by sequence
        logger.debug("Step 1: get the putaway configuration for the inventory");
        List<PutawayConfiguration> suitablePutawayConfiguration = findSuitablePutawayConfiguration(inventory);
        // Loop through every suitable putaway configuration until we find a location

        logger.debug("Step 1 - Result: get total {} putaway configuration for the inventory", suitablePutawayConfiguration.size());
        for(PutawayConfiguration putawayConfiguration : suitablePutawayConfiguration) {
            logger.debug("Step 2 - try to find a location based on the putaway configuration {}", putawayConfiguration.getId());
            Location location  = findSuitableLocation(putawayConfiguration, inventory);
            if (location != null) {

                logger.debug("Step 2 RESULT!!! - WE FOUND A LOCATION: {} " + location.getName());
                return location;
            }
        }
        return null;
    }

    public Location findSuitableLocation(PutawayConfiguration putawayConfiguration, Inventory inventory){

        // Loop through each strategy to get the right location
        for(PutawayConfigurationStrategy putawayConfigurationStrategy : putawayConfiguration.getPutawayConfigurationStrategies()) {
            logger.debug("Step 2.1 - Will find location with configuraiton {}, strategy {}",
                    putawayConfiguration.getId(), putawayConfigurationStrategy.name());
            List<Location> locations = findLocation(putawayConfiguration);
            logger.debug("Step 2.1.1 - Get totally {} locations according to the putaway configuratioln", locations.size());
            if (locations.size() == 0) {
                continue;
            }

            // we find some locations according to the putaway configuration criteria,
            // let's apply the strategy to future filter out those locations that
            // doesn't meet with the strategy

            logger.debug("Step 2.1.2 - Start to filter by strategy");
            locations = putawayConfigurationStrategyService.fitlerLocationByStrategy(locations, inventory, putawayConfigurationStrategy);
            logger.debug("Step 2.1.2 Result - Get totally {} locations according to the strategy {}", locations.size(), putawayConfigurationStrategy);

            for(Location location : locations) {
                if (fit(inventory, location)) {
                    return location;
                }
            }
        }
        return null;
    }
    private boolean fit(Inventory inventory, Location location) {
        // TO-DO:
        // will need to check if the inventory can be fit into the location
        return true;
    }

    // Get all the locations according to the putaway configuration
    public List<Location> findLocation(PutawayConfiguration putawayConfiguration){
        List<Location> locations = new ArrayList<>();
        if (putawayConfiguration.getLocationId() != null) {
            Location location = warehouseLayoutServiceRestemplateClient.getLocationById(putawayConfiguration.getLocationId());
            if (location != null) {
                locations.add(location);
            }
        }
        else if (putawayConfiguration.getLocationGroupId() != null) {
            Location[] locationsByGroup = warehouseLayoutServiceRestemplateClient.getLocationByLocationGroups(
                    putawayConfiguration.getWarehouseId(),
                    String.valueOf(putawayConfiguration.getLocationGroupId()));
            if (locationsByGroup.length > 0) {
                locations = Arrays.asList(locationsByGroup);
            }
        }
        else if (putawayConfiguration.getLocationGroupTypeId() != null) {
            Location[] locationsByGroupType = warehouseLayoutServiceRestemplateClient.getLocationByLocationGroupTypes(
                    putawayConfiguration.getWarehouseId(),
                    String.valueOf(putawayConfiguration.getLocationGroupTypeId()));
            if (locationsByGroupType.length > 0) {
                locations = Arrays.asList(locationsByGroupType);
            }
        }

        return locations;

    }


    public List<PutawayConfiguration> findSuitablePutawayConfiguration(Inventory inventory){

        List<PutawayConfiguration> putawayConfigurations = findAll();
        logger.debug("Step 1.1 get {} putaway configurations", putawayConfigurations.size());
        putawayConfigurations.sort(Comparator.comparingInt(PutawayConfiguration::getSequence));

        return putawayConfigurations.stream()
                .filter(putawayConfiguration -> match(putawayConfiguration, inventory))
                .collect(Collectors.toList());


    }

    // Whether the putaway configuration matches with the inventory only when
    // if configuration has item defined and the inventory has the same item
    // if configuration has item family defined and the inventory has the same item family
    // if configuration has invenotry status defined and the inventory has the same inventory status
    public boolean match(PutawayConfiguration putawayConfiguration, Inventory inventory) {
        logger.debug("Step 1.2 check {} matches with inventory {}", putawayConfiguration.getId(), inventory.getLpn());
        if (putawayConfiguration.getItemId() != null &&
                inventory.getItem().getId() != putawayConfiguration.getItemId()) {

            logger.debug("Step 1.2 >> fail as the item doesn't match.");
            logger.debug(">>>>>>>>>>> putawayConfiguration.getItemId(): {} / inventory.getItem().getId(): {}",
                    putawayConfiguration.getItemId(), inventory.getItem().getId());
            return false;
        }

        if (putawayConfiguration.getItemFamilyId() != null &&
                inventory.getItem().getItemFamily().getId() != putawayConfiguration.getItemFamilyId()) {

            logger.debug("Step 1.2 >> fail as the item family doesn't match.");
            logger.debug(">>>>>>>>>>> putawayConfiguration.getItemFamilyId(): {} / inventory.getItem().getItemFamily().getId(): {}",
                    putawayConfiguration.getItemFamilyId(), inventory.getItem().getItemFamily().getId());
            return false;
        }

        if (putawayConfiguration.getInventoryStatusId() != null &&
                inventory.getInventoryStatus().getId() != putawayConfiguration.getInventoryStatusId()) {

            logger.debug("Step 1.2 >> fail as the inventory status doesn't match.");
            logger.debug(">>>>>>>>>>> putawayConfiguration.getInventoryStatusId(): {} / inventory.getInventoryStatus().getId(): {}",
                    putawayConfiguration.getInventoryStatusId(), inventory.getInventoryStatus().getId());
            return false;
        }

        logger.debug("Step 1.2 >> inventory matches with putaway configuration.");
        return true;

    }

    private Long getWarehouseId(String warehouseName) {
        Warehouse warehouse = warehouseLayoutServiceRestemplateClient.getWarehouseByName(warehouseName);
        if (warehouse == null) {
            return null;
        }
        else {
            return warehouse.getId();
        }
    }



}

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
import com.garyzhangscm.cwms.outbound.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.outbound.model.*;
import com.garyzhangscm.cwms.outbound.repository.AllocationConfigurationRepository;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;


@Service
public class AllocationConfigurationService implements TestDataInitiableService {
    private static final Logger logger = LoggerFactory.getLogger(AllocationConfigurationService.class);

    @Autowired
    private AllocationConfigurationRepository allocationConfigurationRepository;
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

    @Value("${fileupload.test-data.allocation-configuration:allocation-configuration}")
    String testDataFile;

    public AllocationConfiguration findById(Long id, boolean loadDetails) {
        AllocationConfiguration allocationConfiguration
                = allocationConfigurationRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("allocation configuration not found by id: " + id));

        if (loadDetails) {
            loadAttribute(allocationConfiguration);
        }
        return allocationConfiguration;
    }

    public AllocationConfiguration findBySequence(int sequence) {
        return findBySequence(sequence, true);
    }


    public AllocationConfiguration findBySequence(int sequence, boolean loadDetails) {
        AllocationConfiguration allocationConfiguration = allocationConfigurationRepository.findBySequence(sequence);
        if (allocationConfiguration != null && loadDetails) {
            loadAttribute(allocationConfiguration);
        }
        return allocationConfiguration;
    }

    public AllocationConfiguration findById(Long id) {
        return findById(id, true);
    }


    public List<AllocationConfiguration> findAll(boolean loadDetails) {
        List<AllocationConfiguration> allocationConfigurations = allocationConfigurationRepository.findAll();

        if (allocationConfigurations.size() > 0 && loadDetails) {
            loadAttribute(allocationConfigurations);
        }
        return allocationConfigurations;
    }

    public List<AllocationConfiguration> findAll() {
        return findAll(true);
    }

    public List<AllocationConfiguration> findAllocationConfigurationForPicking() {
        return findByType(AllocationConfigurationType.PICKING);
    }

    public List<AllocationConfiguration> findAllocationConfigurationForReplenishment() {
        return findByType(AllocationConfigurationType.REPLENISHMENT);
    }
    public List<AllocationConfiguration> findByType(AllocationConfigurationType allocationConfigurationType) {
        return findByType(allocationConfigurationType, true);

    }
    public List<AllocationConfiguration> findByType(AllocationConfigurationType allocationConfigurationType, boolean loadDetails) {
        List<AllocationConfiguration> allocationConfigurations = allocationConfigurationRepository.findByType(allocationConfigurationType);

        if (allocationConfigurations.size() > 0 && loadDetails) {
            loadAttribute(allocationConfigurations);
        }
        return allocationConfigurations;

    }



    private void loadAttribute(AllocationConfiguration allocationConfiguration) {

        if (allocationConfiguration.getItemId() != null && allocationConfiguration.getItem() == null) {
            allocationConfiguration.setItem(inventoryServiceRestemplateClient.getItemById(allocationConfiguration.getItemId()));
        }
        if (allocationConfiguration.getItemFamilyId() != null && allocationConfiguration.getItemFamily() == null) {
            allocationConfiguration.setItemFamily(inventoryServiceRestemplateClient.getItemFamilyById(allocationConfiguration.getItemFamilyId()));
        }
        if (allocationConfiguration.getLocationId() != null && allocationConfiguration.getLocation() == null) {
            allocationConfiguration.setLocation(warehouseLayoutServiceRestemplateClient.getLocationById(allocationConfiguration.getLocationId()));
        }
        if (allocationConfiguration.getLocationGroupId() != null && allocationConfiguration.getLocationGroup() == null) {
            allocationConfiguration.setLocationGroup(warehouseLayoutServiceRestemplateClient.getLocationGroupById(allocationConfiguration.getLocationGroupId()));
        }
        if (allocationConfiguration.getLocationGroupTypeId() != null && allocationConfiguration.getLocationGroupType() == null) {
            allocationConfiguration.setLocationGroupType(warehouseLayoutServiceRestemplateClient.getLocationGroupTypeById(allocationConfiguration.getLocationGroupTypeId()));
        }

    }

    private void loadAttribute(List<AllocationConfiguration> allocationConfigurations) {
        allocationConfigurations.forEach(allocationConfiguration -> loadAttribute(allocationConfiguration));
    }

    public AllocationConfiguration save(AllocationConfiguration allocationConfiguration) {
        AllocationConfiguration newAllocationConfiguration = allocationConfigurationRepository.save(allocationConfiguration);
        loadAttribute(newAllocationConfiguration);
        return newAllocationConfiguration;
    }

    public AllocationConfiguration saveOrUpdate(AllocationConfiguration allocationConfiguration) {
        if (allocationConfiguration.getId() == null && findBySequence(allocationConfiguration.getSequence()) != null) {
            allocationConfiguration.setId(findBySequence(allocationConfiguration.getSequence()).getId());
        }
        return save(allocationConfiguration);
    }


    public void delete(AllocationConfiguration allocationConfiguration) {
        allocationConfigurationRepository.delete(allocationConfiguration);
    }

    public void delete(Long id) {
        allocationConfigurationRepository.deleteById(id);
    }

    public void delete(String allocationConfigurationIds) {
        if (!allocationConfigurationIds.isEmpty()) {
            long[] allocationConfigurationIdArray = Arrays.asList(allocationConfigurationIds.split(",")).stream().mapToLong(Long::parseLong).toArray();
            for (long id : allocationConfigurationIdArray) {
                delete(id);
            }
        }
    }

    public List<AllocationConfigurationCSVWrapper> loadData(InputStream inputStream) throws IOException {

        CsvSchema schema = CsvSchema.builder().
                addColumn("warehouse").
                addColumn("sequence").
                addColumn("item").
                addColumn("itemFamily").
                addColumn("inventoryStatus").
                addColumn("location").
                addColumn("locationGroup").
                addColumn("locationGroupType").
                addColumn("allocationStrategy").
                addColumn("pickableUnitOfMeasures").
                addColumn("type").
                build().withHeader();

        return fileService.loadData(inputStream, schema, AllocationConfigurationCSVWrapper.class);
    }

    public void initTestData(String warehouseName) {
        try {
            String testDataFileName = StringUtils.isBlank(warehouseName) ?
                    testDataFile + ".csv" :
                    testDataFile + "-" + warehouseName + ".csv";
            InputStream inputStream = new ClassPathResource(testDataFileName).getInputStream();
            List<AllocationConfigurationCSVWrapper> allocationConfigurationCSVWrappers = loadData(inputStream);
            allocationConfigurationCSVWrappers.stream().forEach(allocationConfigurationCSVWrapper -> saveFromWrapper(allocationConfigurationCSVWrapper));
        } catch (IOException ex) {
            logger.debug("Exception while load test data: {}", ex.getMessage());
        }
    }

    private void saveFromWrapper(AllocationConfigurationCSVWrapper allocationConfigurationCSVWrapper) {

        logger.debug("Start to save allocation configuration with meta value:\n {}",
                allocationConfigurationCSVWrapper);
        AllocationConfiguration allocationConfiguration = new AllocationConfiguration();
        allocationConfiguration.setSequence(allocationConfigurationCSVWrapper.getSequence());
        allocationConfiguration.setAllocationStrategy(AllocationStrategy.valueOf(allocationConfigurationCSVWrapper.getAllocationStrategy()));
        allocationConfiguration.setType(AllocationConfigurationType.valueOf(allocationConfigurationCSVWrapper.getType()));

        Warehouse warehouse =
                warehouseLayoutServiceRestemplateClient.getWarehouseByName(allocationConfigurationCSVWrapper.getWarehouse());

        allocationConfiguration.setWarehouseId(warehouse.getId());

        if (!StringUtils.isBlank(allocationConfigurationCSVWrapper.getItem())) {
            Item item = inventoryServiceRestemplateClient.getItemByName(
                    warehouse.getId(), allocationConfigurationCSVWrapper.getItem());
            if (item != null) {
                allocationConfiguration.setItemId(item.getId());
            }
        }
        if (!StringUtils.isBlank(allocationConfigurationCSVWrapper.getItemFamily())) {
            ItemFamily itemFamily = inventoryServiceRestemplateClient.getItemFamilyByName(
                    warehouse.getId(), allocationConfigurationCSVWrapper.getItemFamily());
            if (itemFamily != null) {
                allocationConfiguration.setItemFamilyId(itemFamily.getId());
            }
        }


        if (!StringUtils.isBlank(allocationConfigurationCSVWrapper.getLocation())) {
            Location location =
                    warehouseLayoutServiceRestemplateClient.getLocationByName(
                            allocationConfigurationCSVWrapper.getWarehouse(),allocationConfigurationCSVWrapper.getLocation());
            if (location != null) {
                allocationConfiguration.setLocationId(location.getId());
            }
        }

        if (!StringUtils.isBlank(allocationConfigurationCSVWrapper.getLocationGroup())) {
            LocationGroup locationGroup =
                    warehouseLayoutServiceRestemplateClient.getLocationGroupByName(
                            allocationConfigurationCSVWrapper.getWarehouse(),allocationConfigurationCSVWrapper.getLocationGroup());
            if (locationGroup != null) {
                allocationConfiguration.setLocationGroupId(locationGroup.getId());
            }
        }

        if (!StringUtils.isBlank(allocationConfigurationCSVWrapper.getLocationGroupType())) {
            LocationGroupType locationGroupType =
                    warehouseLayoutServiceRestemplateClient.getLocationGroupTypeByName(
                            allocationConfigurationCSVWrapper.getLocationGroupType());
            if (locationGroupType != null) {
                allocationConfiguration.setLocationGroupTypeId(locationGroupType.getId());
            }
        }

        // Save the configuration first, then save all the pickable unit of measure
        AllocationConfiguration savedAllocationConfiguration = saveOrUpdate(allocationConfiguration);


        if (!StringUtils.isBlank(allocationConfigurationCSVWrapper.getPickableUnitOfMeasures())) {
            logger.debug("Start to save pickable unit of measure:\n,{}",
                    allocationConfigurationCSVWrapper.getPickableUnitOfMeasures());
            long warehouseId = warehouse.getId();
            List<String> pickableUnitOfMeasureNames = Arrays.asList(allocationConfigurationCSVWrapper.getPickableUnitOfMeasures().split(";"));
                    pickableUnitOfMeasureNames.stream()
                            .map(pickableUnitOfMeasureName ->  commonServiceRestemplateClient.getUnitOfMeasureByName(pickableUnitOfMeasureName))
                            .filter(Objects::nonNull)
                            .map(unitOfMeasure -> new PickableUnitOfMeasure(warehouseId, unitOfMeasure.getId(), savedAllocationConfiguration))
                            .forEach(pickableUnitOfMeasure -> pickableUnitOfMeasureService.save(pickableUnitOfMeasure));
        }
    }

    @Transactional
    // Allocate shipment line
    public AllocationResult allocate(ShipmentLine shipmentLine, List<Pick> existingPicks, List<Inventory> pickableInventory) {

        AllocationResult allocationResult = new AllocationResult();

        // open quantity is the quantity we are about to allocate this time
        Long openQuantity = shipmentLine.getOpenQuantity();
        logger.debug("Start to allocate shipment line: {} / {} by going through all the strategy",
                shipmentLine.getId(), shipmentLine.getNumber());

        // Let's get the item by shipment line
        // Then we will get all the strategy by the item attribute
        // After we get all the strategy, we will loop through each strategy until
        // we either generate enough picks, or we generate emergency replenishment for the shortage
        Item item = shipmentLine.getOrderLine().getItem();


        // Get all allocation configuration that match with the item
        List<AllocationConfiguration> matchedAllocationConfiguration = getMatchedAllocationConfiguration(item, AllocationConfigurationType.PICKING);
        logger.debug("We got {} allocation configuration by item {}",
                        matchedAllocationConfiguration.size(), item.getName());

        if (matchedAllocationConfiguration.size() == 0) {
            // OK, we don't have any allocation configuration defined for this item, let's return
            // a short allocation for the whole open quantity

            allocationResult.addShortAllocation(generateShortAllocation(item, shipmentLine, openQuantity));
            return allocationResult;
        }

        // Sort the allocation configuration based upon the sequence then try one by one
        matchedAllocationConfiguration.sort(Comparator.comparing(AllocationConfiguration::getSequence));
        // We will allocate based on inventory group(group by location and status and etc)
        List<InventorySummary> inventorySummaries = inventorySummaryService.getInventorySummaryForAllocation(pickableInventory);
        logger.debug("We have {} pickable inventory summary against the item with {}",
                inventorySummaries.size(), item.getName());

        List<Pick> picks = new ArrayList<>();

        // Let's loop through each allocation configuration to see if we can allocate by the configuration
        for (AllocationConfiguration allocationConfiguration : matchedAllocationConfiguration) {
            logger.debug("Start to allocate against the configuration: {} / {}",
                    allocationConfiguration.getId(), allocationConfiguration.getSequence());
            if (openQuantity <= 0) {
                logger.debug("1. open quantity is 0. We have already allocate the full quantity.");
                break;
            }
            for (InventorySummary inventorySummary : inventorySummaries) {
                if (openQuantity <= 0) {
                    logger.debug("2. open quantity is 0. We have already allocate the full quantity.");
                    break;
                }
                Long smallestPickableUnitOfMeasureQuantity
                        = getSmallestPickableUnitOfMeasureQuantity(allocationConfiguration, inventorySummary);
                if (smallestPickableUnitOfMeasureQuantity == 0) {
                    logger.debug("No pickable unit of measure defined for the inventory summary, {} / {}",
                            inventorySummary.getItem().getName(), inventorySummary.getItemPackageType().getName());
                    continue;
                }

                Long pickableQuantity = getPickableQuantity(allocationConfiguration, existingPicks,
                        inventorySummary, smallestPickableUnitOfMeasureQuantity);
                logger.debug("We can pick {} from location {}, item {}", pickableQuantity,
                        inventorySummary.getLocation().getName(), inventorySummary.getItem().getName());

                logger.debug("Start to generate pick\n pickable quantity {}\n open quantity {}\n pickable unit of measure quantity {}",
                        pickableQuantity, openQuantity, smallestPickableUnitOfMeasureQuantity);
                if (Math.min(pickableQuantity, openQuantity) > smallestPickableUnitOfMeasureQuantity) {
                    Long pickQuantity = Math.min(pickableQuantity, openQuantity);
                    // Make sure we pick by unit of measure
                    pickQuantity = (pickQuantity / smallestPickableUnitOfMeasureQuantity) * smallestPickableUnitOfMeasureQuantity;

                    Pick pick = pickService.generatePick(inventorySummary, shipmentLine, pickQuantity);
                    picks.add(pick);
                    openQuantity -= pickQuantity;
                    logger.debug("OK, we generate a pick with quantity {}. The open quantity become {}",
                            pickQuantity, openQuantity);

                    // deduct the quantity from inventory summary so that we can have the right available quantity
                    // in the following round
                    inventorySummary.setQuantity(inventorySummary.getQuantity() - pickQuantity);
                }

            }
        }

        allocationResult.setPicks(picks);
        logger.debug("After we tried all the configurations, we still have {} quantity left", openQuantity);
        if (openQuantity > 0) {
            // OK, we don't have any allocation configuration defined for this item, let's return
            // a short allocation for the whole open quantity

            allocationResult.addShortAllocation(generateShortAllocation(item, shipmentLine, openQuantity));
        }
        return allocationResult;

    }


    @Transactional
    // Allocate short allocation
    public List<Pick> allocate(ShortAllocation shortAllocation, List<Pick> existingPicks, List<Inventory> pickableInventory) {


        // open quantity is the quantity we are about to allocate this time
        Long openQuantity = shortAllocation.getOpenQuantity();
        logger.debug("Start to allocate short allocation: {} / {} / {} by going through all the strategy",
                shortAllocation.getId(), shortAllocation.getItem(), shortAllocation.getItem().getItemFamily());

        // Let's get the item by short allocation
        // Then we will get all the strategy by the item attribute
        // After we get all the strategy, we will loop through each strategy until
        // we either generate enough picks, or we generate emergency replenishment for the shortage
        Item item = shortAllocation.getItem();


        // Get all allocation configuration that match with the item
        List<AllocationConfiguration> matchedAllocationConfiguration = getMatchedAllocationConfiguration(item, AllocationConfigurationType.REPLENISHMENT);
        logger.debug("We got {} allocation configuration by item {}",
                matchedAllocationConfiguration.size(), item.getName());

        if (matchedAllocationConfiguration.size() == 0) {
            // OK, we don't have any allocation configuration defined for this item,
            // Let's return nothing
            return new ArrayList<>();
        }

        // Sort the allocation configuration based upon the sequence then try one by one
        matchedAllocationConfiguration.sort(Comparator.comparing(AllocationConfiguration::getSequence));
        // We will allocate based on inventory group(group by location and status and etc)
        List<InventorySummary> inventorySummaries = inventorySummaryService.getInventorySummaryForAllocation(pickableInventory);
        logger.debug("We have {} pickable inventory summary against the item with {}",
                inventorySummaries.size(), item.getName());

        List<Pick> picks = new ArrayList<>();

        // Let's loop through each allocation configuration to see if we can allocate by the configuration
        for (AllocationConfiguration allocationConfiguration : matchedAllocationConfiguration) {
            logger.debug("Start to allocate against the configuration: {} / {}",
                    allocationConfiguration.getId(), allocationConfiguration.getSequence());
            if (openQuantity <= 0) {
                logger.debug("1. open quantity is 0. We have already allocate the full quantity.");
                break;
            }
            for (InventorySummary inventorySummary : inventorySummaries) {
                if (openQuantity <= 0) {
                    logger.debug("2. open quantity is 0. We have already allocate the full quantity.");
                    break;
                }
                Long smallestPickableUnitOfMeasureQuantity
                        = getSmallestPickableUnitOfMeasureQuantity(allocationConfiguration, inventorySummary);
                if (smallestPickableUnitOfMeasureQuantity == 0) {
                    logger.debug("No pickable unit of measure defined for the inventory summary, {} / {}",
                            inventorySummary.getItem().getName(), inventorySummary.getItemPackageType().getName());
                    continue;
                }

                Long pickableQuantity = getPickableQuantity(allocationConfiguration, existingPicks,
                        inventorySummary, smallestPickableUnitOfMeasureQuantity);
                logger.debug("We can pick {} from location {}, item {}", pickableQuantity,
                        inventorySummary.getLocation().getName(), inventorySummary.getItem().getName());

                logger.debug("Start to generate pick\n pickable quantity {}\n open quantity {}\n pickable unit of measure quantity {}",
                        pickableQuantity, openQuantity, smallestPickableUnitOfMeasureQuantity);
                if (Math.min(pickableQuantity, openQuantity) > smallestPickableUnitOfMeasureQuantity) {
                    Long pickQuantity = Math.min(pickableQuantity, openQuantity);
                    // Make sure we pick by unit of measure
                    pickQuantity = (pickQuantity / smallestPickableUnitOfMeasureQuantity) * smallestPickableUnitOfMeasureQuantity;

                    try {
                        Pick pick = pickService.generatePick(inventorySummary, shortAllocation, pickQuantity);
                        picks.add(pick);
                        openQuantity -= pickQuantity;
                        logger.debug("OK, we generate a pick with quantity {}. The open quantity become {}",
                                pickQuantity, openQuantity);

                        // deduct the quantity from inventory summary so that we can have the right available quantity
                        // in the following round
                        inventorySummary.setQuantity(inventorySummary.getQuantity() - pickQuantity);
                    }
                    catch(GenericException ex) {
                        // in case we can't generate the pick, let's just ignore the exception and continue with next possible pick
                        logger.debug("Error while generate the pick: " + ex.getMessage());
                    }
                }

            }
        }

        return picks;

    }


    @Transactional
    // Allocate Work order line
    public AllocationResult allocate(WorkOrder workOrder, WorkOrderLine workOrderLine, List<Pick> existingPicks, List<Inventory> pickableInventory) {

        AllocationResult allocationResult = new AllocationResult();

        // open quantity is the quantity we are about to allocate this time
        Long openQuantity = workOrderLine.getOpenQuantity();
        logger.debug("Start to allocate work order line: {} / {} by going through all the strategy",
                workOrderLine.getId(), workOrderLine.getNumber());

        // Let's get the item by work order line
        // Then we will get all the strategy by the item attribute
        // After we get all the strategy, we will loop through each strategy until
        // we either generate enough picks, or we generate emergency replenishment for the shortage
        Item item = workOrderLine.getItem();

        // Get all allocation configuration that match with the item
        List<AllocationConfiguration> matchedAllocationConfiguration = getMatchedAllocationConfiguration(item, AllocationConfigurationType.PICKING);
        logger.debug("We got {} allocation configuration by item {}",
                matchedAllocationConfiguration.size(), item.getName());

        if (matchedAllocationConfiguration.size() == 0) {
            // OK, we don't have any allocation configuration defined for this item, let's return
            // a short allocation for the whole open quantity

            allocationResult.addShortAllocation(generateShortAllocation(workOrder, item, workOrderLine, openQuantity));
            return allocationResult;
        }

        // Sort the allocation configuration based upon the sequence then try one by one
        matchedAllocationConfiguration.sort(Comparator.comparing(AllocationConfiguration::getSequence));
        // We will allocate based on inventory group(group by location and status and etc)
        List<InventorySummary> inventorySummaries = inventorySummaryService.getInventorySummaryForAllocation(pickableInventory);
        logger.debug("We have {} pickable inventory summary against the item with {}",
                inventorySummaries.size(), item.getName());

        List<Pick> picks = new ArrayList<>();

        // Let's loop through each allocation configuration to see if we can allocate by the configuration
        for (AllocationConfiguration allocationConfiguration : matchedAllocationConfiguration) {
            logger.debug("Start to allocate against the configuration: {} / {}",
                    allocationConfiguration.getId(), allocationConfiguration.getSequence());
            if (openQuantity <= 0) {
                logger.debug("1. open quantity is 0. We have already allocate the full quantity.");
                break;
            }
            for (InventorySummary inventorySummary : inventorySummaries) {
                if (openQuantity <= 0) {
                    logger.debug("2. open quantity is 0. We have already allocate the full quantity.");
                    break;
                }
                Long smallestPickableUnitOfMeasureQuantity
                        = getSmallestPickableUnitOfMeasureQuantity(allocationConfiguration, inventorySummary);
                if (smallestPickableUnitOfMeasureQuantity == 0) {
                    logger.debug("No pickable unit of measure defined for the inventory summary, {} / {}",
                            inventorySummary.getItem().getName(), inventorySummary.getItemPackageType().getName());
                    continue;
                }

                Long pickableQuantity = getPickableQuantity(allocationConfiguration, existingPicks,
                        inventorySummary, smallestPickableUnitOfMeasureQuantity);
                logger.debug("We can pick {} from location {}, item {}", pickableQuantity,
                        inventorySummary.getLocation().getName(), inventorySummary.getItem().getName());

                logger.debug("Start to generate pick\n pickable quantity {}\n open quantity {}\n pickable unit of measure quantity {}",
                        pickableQuantity, openQuantity, smallestPickableUnitOfMeasureQuantity);
                if (Math.min(pickableQuantity, openQuantity) > smallestPickableUnitOfMeasureQuantity) {
                    Long pickQuantity = Math.min(pickableQuantity, openQuantity);
                    // Make sure we pick by unit of measure
                    pickQuantity = (pickQuantity / smallestPickableUnitOfMeasureQuantity) * smallestPickableUnitOfMeasureQuantity;

                    Pick pick = pickService.generatePick(workOrder, inventorySummary, workOrderLine, pickQuantity);
                    picks.add(pick);
                    openQuantity -= pickQuantity;
                    logger.debug("OK, we generate a pick with quantity {}. The open quantity become {}",
                            pickQuantity, openQuantity);

                    // deduct the quantity from inventory summary so that we can have the right available quantity
                    // in the following round
                    inventorySummary.setQuantity(inventorySummary.getQuantity() - pickQuantity);
                }

            }
        }

        allocationResult.setPicks(picks);
        logger.debug("After we tried all the configurations, we still have {} quantity left", openQuantity);
        if (openQuantity > 0) {
            // OK, we don't have any allocation configuration defined for this item, let's return
            // a short allocation for the whole open quantity

            allocationResult.addShortAllocation(generateShortAllocation(workOrder, item, workOrderLine, openQuantity));
        }
        return allocationResult;

    }

    private ShortAllocation generateShortAllocation(Item item, ShipmentLine shipmentLine, Long quantity) {

        return shortAllocationService.generateShortAllocation(item, shipmentLine, quantity);

    }

    private ShortAllocation generateShortAllocation(WorkOrder workOrder, Item item, WorkOrderLine workOrderLine, Long quantity) {

        return shortAllocationService.generateShortAllocation(workOrder, item, workOrderLine, quantity);

    }


    // Get the smallest pickable unit of measure quantity
    // We will get all the unit of measure defined for this inventory summary
    // then for each unit of measure, check if it is defined in the allocation configuraiton
    // (allocationConfiguration.getPickableUnitOfMeasures()).
    // After we get all those unit of measure, we get the smallest unit of measure's quantity
    // Then we will pick by this smallest unit of measure.
    private Long getSmallestPickableUnitOfMeasureQuantity(AllocationConfiguration allocationConfiguration,
                                                          InventorySummary inventorySummary) {
        return inventorySummary.getItemPackageType()
                .getItemUnitOfMeasures()
                .stream().filter(itemUnitOfMeasure -> {
                    logger.debug(">> getSmallestPickableUnitOfMeasureQuantity / compare: {}",
                            itemUnitOfMeasure.getUnitOfMeasure().getName());
                    boolean match = allocationConfiguration.getPickableUnitOfMeasures().stream()
                            .map(PickableUnitOfMeasure::getUnitOfMeasureId)
                            .filter(id -> {
                                logger.debug("Will compare ");
                                return itemUnitOfMeasure.getUnitOfMeasureId().equals(id);
                            })
                            .filter(Objects::nonNull).count() > 0;

                    return match;

                })
                .map(ItemUnitOfMeasure::getQuantity)
                .min(Long::compare)
                .orElse(0L);
    }

    private Long getPickableQuantity(AllocationConfiguration allocationConfiguration,
                               List<Pick> existingPicks,
                               InventorySummary inventorySummary,
                               Long smallestPickableUnitOfMeasureQuantity) {

        if (!isPickable(allocationConfiguration, inventorySummary)) {
            return 0L;
        }

        // OK. The inventory matches with the allocatino cnofiguration, let's check
        // the quantity we can still allocate from this location
        Long pickableQuantity = inventorySummary.getQuantity();



        if (smallestPickableUnitOfMeasureQuantity.equals(0)) {
            return 0L;
        }
        // Loop through all existing picks to deduct the quantity
        for(Pick pick : existingPicks) {
            if (pick.getSourceLocationId().equals(inventorySummary.getLocationId())) {
                // The pick is from the same location. let's deduct the quantity
                pickableQuantity -= (pick.getQuantity() - pick.getPickedQuantity());
            }
        }
        if (pickableQuantity > smallestPickableUnitOfMeasureQuantity) {
            // we will pick by unit of measure
            Long pickableUnitOfMeasureQuantity = pickableQuantity / smallestPickableUnitOfMeasureQuantity;

            return pickableUnitOfMeasureQuantity * smallestPickableUnitOfMeasureQuantity;
        }
        else {
            return 0L;
        }

    }

    private boolean isPickable(AllocationConfiguration allocationConfiguration,
                               InventorySummary inventorySummary) {
        logger.debug("Check if we can apply allocation configuration {} to inventory: location  {}, item {},  ",
                        allocationConfiguration.getSequence(),
                inventorySummary.getLocation().getName(), inventorySummary.getItem().getName());
        // Make sure the inventory is in the location / group / group type that
        // defined by the configuration
        if (allocationConfiguration.getLocationId() != null &&
                !allocationConfiguration.getLocationId().equals(inventorySummary.getLocationId())) {

            logger.debug("Step 1.2 >> fail as the location doesn't match.");
            logger.debug(">>>>>>>>>>> allocationConfiguration.getLocationId(): {} / inventorySummary.getLocationId(): {}",
                    allocationConfiguration.getLocationId(), inventorySummary.getLocationId());
            return false;
        }
        if (allocationConfiguration.getLocationGroupId() != null &&
                !allocationConfiguration.getLocationGroupId().equals(inventorySummary.getLocation().getLocationGroup().getId())) {

            logger.debug("Step 1.2 >> fail as the location group doesn't match.");
            logger.debug(">>>>>>>>>>> allocationConfiguration.getLocationGroupId(): {} / inventorySummary.getLocation().getLocationGroup().getId(): {}",
                    allocationConfiguration.getLocationGroupId(), inventorySummary.getLocation().getLocationGroup().getId());
            return false;
        }
        if (allocationConfiguration.getLocationGroupTypeId() != null &&
                !allocationConfiguration.getLocationGroupTypeId().equals(inventorySummary.getLocation().getLocationGroup().getLocationGroupType().getId())) {

            logger.debug("Step 1.2 >> fail as the location group type doesn't match.");
            logger.debug(">>>>>>>>>>> allocationConfiguration.getLocationGroupTypeId(): {} / inventorySummary.getLocation().getLocationGroup().getLocationGroupType().getId(): {}",
                    allocationConfiguration.getLocationGroupTypeId(), inventorySummary.getLocation().getLocationGroup().getLocationGroupType().getId());
            return false;
        }

        return true;
    }

    private List<AllocationConfiguration> getMatchedAllocationConfiguration(Item item, AllocationConfigurationType allocationConfigurationType) {
        List<AllocationConfiguration> allocationConfigurations = new ArrayList<>();
        List<AllocationConfiguration> allAllocationConfigurations = findByType(allocationConfigurationType);
        logger.debug("We have {} allocation configuration defined in the system", allAllocationConfigurations.size());

        allAllocationConfigurations.forEach(allocationConfiguration -> {
            logger.debug("> Check configuration {} / {} against item {}",
                    allocationConfiguration.getId(), allocationConfiguration.getSequence(),
                    item.getName());
            if (match(allocationConfiguration, item)) {
                logger.debug("> Matched!");
                allocationConfigurations.add(allocationConfiguration);
            }
        });
        return allocationConfigurations;
    }

    // Whether the allocation configuration matches with the item only when
    // if configuration has item defined and match with the item
    // if configuration has item family defined and match with the item
    public boolean match(AllocationConfiguration allocationConfiguration, Item item) {
        logger.debug("==================   Allocation Configuration V.S Item ======================");
        logger.debug("Step 1.1 check {} matches with item {}", allocationConfiguration.getId(), item.getName());
        if (allocationConfiguration.getItemId() != null &&
                item.getId() != allocationConfiguration.getItemId()) {

            logger.debug("Step 1.2 >> fail as the item doesn't match.");
            logger.debug(">>>>>>>>>>> allocationConfiguration.getItemId(): {} / item.getId(): {}",
                    allocationConfiguration.getItemId(), item.getId());
            return false;
        }

        if (allocationConfiguration.getItemFamilyId() != null &&
                item.getItemFamily().getId() != allocationConfiguration.getItemFamilyId()) {

            logger.debug("Step 1.2 >> fail as the item family doesn't match.");
            logger.debug(">>>>>>>>>>> allocationConfiguration.getItemFamilyId(): {} / item.getItemFamily().getId(): {}",
                    allocationConfiguration.getItemFamilyId(), item.getItemFamily().getId());
            return false;
        }
        logger.debug("Step 1.2 >> inventory matches with putaway configuration.");
        return true;

    }

    public AllocationResult allocateWorkOrder(WorkOrder workOrder){
        logger.debug("Start to allocate Work Order {} ", workOrder.getNumber());
        AllocationResult allocationResult = new AllocationResult();

        workOrder.getWorkOrderLines().forEach(workOrderLine -> {
            AllocationResult workOrderAllocationResult
                    = allocateWorkOrderLine(workOrder, workOrderLine);
            allocationResult.addPicks(workOrderAllocationResult.getPicks());
            allocationResult.addShortAllocations(workOrderAllocationResult.getShortAllocations());
        });
        return allocationResult;

    }

    public AllocationResult allocateWorkOrderLine(WorkOrder workOrder, WorkOrderLine workOrderLine) {
        logger.debug("Start to allocate Work Order Line: {} / {}", workOrderLine.getNumber(), workOrderLine.getItem().getName());
        if (  workOrderLine.getOpenQuantity() <= 0) {
            logger.debug("Shipment line is not allocatable! is allocatable? open quantity? {}",
                      workOrderLine.getOpenQuantity());
            return new AllocationResult();
        }

        // Let's get all the pickable inventory and existing picking so we can start to calculate
        // how to generate picks for the shipment
        Long itemId = workOrderLine.getItemId();

        List<Pick> existingPicks = pickService.getOpenPicksByItemId(itemId);
        logger.debug("We have {} existing picks against the item with id {}",
                existingPicks.size(), itemId);

        // Get all pickable inventory
        List<Inventory> pickableInventory
                = inventoryServiceRestemplateClient.getPickableInventory(
                itemId, workOrderLine.getInventoryStatusId());
        logger.debug("We have {} pickable inventory against the item with id {}",
                pickableInventory.size(), itemId);


        AllocationResult allocationResult = allocate(workOrder, workOrderLine, existingPicks, pickableInventory);


        return allocationResult;
    }


}

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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.garyzhangscm.cwms.layout.clients.CommonServiceRestemplateClient;
import com.garyzhangscm.cwms.layout.clients.InboundServiceRestemplateClient;
import com.garyzhangscm.cwms.layout.clients.InventoryServiceRestemplateClient;
import com.garyzhangscm.cwms.layout.clients.OutboundServiceRestemplateClient;
import com.garyzhangscm.cwms.layout.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.layout.model.*;
import com.garyzhangscm.cwms.layout.repository.WarehouseRepository;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import javax.persistence.criteria.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class WarehouseService implements TestDataInitiableService {

    private static final Logger logger = LoggerFactory.getLogger(WarehouseService.class);

    @Autowired
    private WarehouseRepository warehouseRepository;
    @Autowired
    private LocationGroupService locationGroupService;
    @Autowired
    private LocationService locationService;
    @Autowired
    private LocationGroupTypeService locationGroupTypeService;
    @Autowired
    private CompanyService companyService;
    @Autowired
    private CommonServiceRestemplateClient commonServiceRestemplateClient;
    @Autowired
    private OutboundServiceRestemplateClient outboundServiceRestemplateClient;
    @Autowired
    private InventoryServiceRestemplateClient inventoryServiceRestemplateClient;
    @Autowired
    private InboundServiceRestemplateClient inboundServiceRestemplateClient;
    @Autowired
    private FileService fileService;

    @Value("${fileupload.test-data.warehouses:warehouses.csv}")
    String testDataFile;

    public Warehouse findById(Long id) {
        return findById(id, true);
    }
    public Warehouse findById(Long id, boolean loadAttribute) {
        Warehouse warehouse = warehouseRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("warehouse not found by id: " + id));
        if(loadAttribute) {
            loadAttribute(warehouse);
        }
        return warehouse;
    }

    public List<Warehouse> findAll() {
        return findAll(null, null, null);
    }

    public List<Warehouse> findAll(Long companyId,
                                   String companyCode,
                                   String name) {
        return findAll(companyId, companyCode, name, true);
    }

    public List<Warehouse> findAll(Long companyId,
                                   String companyCode,
                                   String name,
                                   boolean loadAttribute) {

        List<Warehouse> warehouses =  warehouseRepository.findAll(
                (Root<Warehouse> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();



                    if (Objects.nonNull(companyId)) {
                        Join<Warehouse, Company> joinCompany = root.join("company", JoinType.INNER);

                        predicates.add(criteriaBuilder.equal(joinCompany.get("id"), companyId));
                    }

                    if (StringUtils.isNotBlank(companyCode)) {
                        Join<Warehouse, Company> joinCompany = root.join("company", JoinType.INNER);

                        predicates.add(criteriaBuilder.equal(joinCompany.get("code"), companyCode));
                    }


                    if (StringUtils.isNotBlank(name)) {
                        predicates.add(criteriaBuilder.equal(root.get("name"), name));
                    }
                    Predicate[] p = new Predicate[predicates.size()];
                    return criteriaBuilder.and(predicates.toArray(p));
                }
        );

        if (warehouses.size() > 0 && loadAttribute) {
            loadAttribute(warehouses);
        }
        return warehouses;
    }

    private void loadAttribute(List<Warehouse> warehouses) {
        warehouses.forEach(this::loadAttribute);
    }

    private void loadAttribute(Warehouse warehouse) {

        warehouse.setCompanyId(warehouse.getCompany().getId());
    }
    public Warehouse findByName(String companyCode, String name){
        return findByName(companyService.findByCode(companyCode).getId(), name);
    }
    public Warehouse findByName(Long companyId, String name){
        return warehouseRepository.findByName(companyId, name);
    }

    public Warehouse save(Warehouse warehouse) {
        return warehouseRepository.save(warehouse);
    }

    public Warehouse saveOrUpdate(Warehouse warehouse) {
        if (warehouse.getId() == null && findByName(warehouse.getCompany().getId(), warehouse.getName()) != null) {
            warehouse.setId(findByName(warehouse.getCompany().getId(), warehouse.getName()).getId());
        }
        return save(warehouse);
    }

    public void delete(Warehouse warehouse) {
        warehouseRepository.delete(warehouse);
    }
    public void delete(Long id) {
        warehouseRepository.deleteById(id);
    }
    public List<WarehouseCSVWrapper> loadData(File file) throws IOException {

        CsvSchema schema = CsvSchema.builder().
                addColumn("company").
                addColumn("name").
                addColumn("size").
                addColumn("addressCountry").
                addColumn("addressState").
                addColumn("addressCounty").
                addColumn("addressCity").
                addColumn("addressDistrict").
                addColumn("addressLine1").
                addColumn("addressLine2").
                addColumn("addressPostcode").
                build().withHeader();
        return fileService.loadData(file, schema, WarehouseCSVWrapper.class);
    }

    public List<WarehouseCSVWrapper> loadData(InputStream inputStream) throws IOException {

        CsvSchema schema = CsvSchema.builder().
                addColumn("company").
                addColumn("name").
                addColumn("size").
                addColumn("addressCountry").
                addColumn("addressState").
                addColumn("addressCounty").
                addColumn("addressCity").
                addColumn("addressDistrict").
                addColumn("addressLine1").
                addColumn("addressLine2").
                addColumn("addressPostcode").
                build().withHeader();

        return fileService.loadData(inputStream, schema, WarehouseCSVWrapper.class);
    }

    public void initTestData(Long companyId, String warehouseName) {
        try {
            String companyCode = companyService.findById(companyId).getCode();

            String testDataFileName = StringUtils.isBlank(warehouseName) ?
                    testDataFile + ".csv" :
                    testDataFile + "-" + companyCode + "-" + warehouseName + ".csv";
            InputStream inputStream = new ClassPathResource(testDataFileName).getInputStream();
            List<WarehouseCSVWrapper> warehouseCSVWrappers = loadData(inputStream);
            warehouseCSVWrappers.stream().forEach(warehouseCSVWrapper -> saveOrUpdate(convertFromWrapper(warehouseCSVWrapper)));
        } catch (IOException ex) {
            logger.debug("Exception while load test data: {}", ex.getMessage());
        }
    }


    private Warehouse convertFromWrapper(WarehouseCSVWrapper warehouseCSVWrapper) {
        Warehouse warehouse = new Warehouse();
        BeanUtils.copyProperties(warehouseCSVWrapper, warehouse);

        warehouse.setCompany(companyService.findByCode(warehouseCSVWrapper.getCompany()));

        return warehouse;

    }

    public Warehouse changeWarehouse(long id, Warehouse warehouse) {
        Warehouse existingWarehouse = findById(id);
        BeanUtils.copyProperties(warehouse, existingWarehouse, "id", "name", "company");
        return saveOrUpdate(existingWarehouse);
    }

    public Warehouse addWarehouses(Long companyId, Warehouse warehouse) throws JsonProcessingException {
        Company company = companyService.findById(companyId);
        warehouse.setCompany(company);
        Warehouse newWarehouse = saveOrUpdate(warehouse);

        // we will need to setup all the default configuration /
        // location group and locations

        setupDefaultLocationGroupAndLocation(newWarehouse);
        setupDefaultConfiguration(newWarehouse);


        return newWarehouse;
    }

    private void setupDefaultLocationGroupAndLocation(Warehouse newWarehouse) {
        // Setup receiving stage location group and one example location
        // Receiving Stage
        LocationGroup locationGroup = setupLocationGroup(newWarehouse, "RECV_STG", "Receiving Stage", "Receive_Stage",
                false, false, false,false,null,
                InventoryConsolidationStrategy.NONE,false,false);
        // Receiving Stage Location
        for(int i = 0; i < 20; i++) {
            String locationName = "RSTG0" + String.format("%02d", i);
            setupSampleLocation(newWarehouse, locationGroup, locationName, "201",
                    999.0, 999.0, 999.0,
                    20100000L + i, 20100000L + i,20100000L + i,
                    999999.0, 100.0, true);
        }

        // Receiving Dock
        locationGroup = setupLocationGroup(newWarehouse, "RECV_DCK", "Receiving Dock", "Receive_Dock",
                false, false, false,false,null,
                InventoryConsolidationStrategy.NONE,false,false);
        // Receiving Dock Location

        for(int i = 0; i < 20; i++) {
            String locationName = "RDOCK0" + String.format("%02d", i);
            setupSampleLocation(newWarehouse, locationGroup, locationName, "301",
                    999.0, 999.0, 999.0,
                    30100000L + i, 30100000L + i,30100000L + i,
                    999999.0, 100.0, true);
        }

        // Receipt. Inventory received from the receipt and without any destination location will be
        // in those locations temporarily
        // locations will be created temporarily in the name of the receipt number
        locationGroup = setupLocationGroup(newWarehouse, "RECEIPT", "Receipt", "RECEIPT",
                false, false, false,false,null,
                InventoryConsolidationStrategy.NONE,false,false);

        // Shipping Stage
        locationGroup = setupLocationGroup(newWarehouse, "SHIP_STG", "Shipping Stage", "Shipping_Stage",
                false, false, false,false,null,
                InventoryConsolidationStrategy.NONE,false,false);

        // Shipping Stage Locations
        for(int i = 1; i <= 20; i++) {
            String locationName = "SSTG00" + String.format("%02d", i);
            setupSampleLocation(newWarehouse, locationGroup, locationName, "401",
                    999.0, 999.0, 999.0,
                    40100000L + i, 40100000L + i,40100000L + i,
                    999999.0, 100.0, true);

        }

        // Shipping Dock
        locationGroup = setupLocationGroup(newWarehouse, "SHIP_DCK", "Shipping Dock", "Shipping_Dock",
                false, false, false,false,null,
                InventoryConsolidationStrategy.NONE,false,false);
        // Shipping Dock Locations
        for(int i = 0; i < 20; i++) {
            String locationName = "SDOCK0" + String.format("%02d", i);
            setupSampleLocation(newWarehouse, locationGroup, locationName, "401",
                    999.0, 999.0, 999.0,
                    40100000L + i, 40100000L + i,40100000L + i,
                    999999.0, 100.0, true);

        }

        // dispatched trailer location group
        // location will be created by the name of trailer number
        locationGroup = setupLocationGroup(newWarehouse, "DISPATCHED", "Dispatched Trailer", "Dispatched",
                false, false, false,false,null,
                InventoryConsolidationStrategy.NONE,false,false);

        // Trailer locations
        locationGroup = setupLocationGroup(newWarehouse, "TRAILER", "Trailer", "Trailer",
                false, false, false,false,null,
                InventoryConsolidationStrategy.NONE,false,false);

        // RF locations
        locationGroup = setupLocationGroup(newWarehouse, "RF", "RF locations", "RF",
                false, false, false,false,null,
                InventoryConsolidationStrategy.NONE,false,false);

        for(int i = 0; i < 20; i++) {
            String locationName = "RF0" + String.format("%02d", i);
            setupSampleLocation(newWarehouse, locationGroup, locationName, "401",
                    999.0, 999.0, 999.0,
                    50100000L + i, 50100000L + i,50100000L + i,
                    999999.0, 100.0, true);

        }

        // Yard Locations
        locationGroup = setupLocationGroup(newWarehouse, "YARD", "Yard for parking", "Yard",
                false, false, false,false,null,
                InventoryConsolidationStrategy.NONE,false,false);

        // Pickup and Deposit locations
        locationGroup = setupLocationGroup(newWarehouse, "P&D", "Pickup and Deposit", "PickupDeposit",
                false, false, false,false,null,
                InventoryConsolidationStrategy.NONE,false,true);

        // Removed inventory(used by system)
        locationGroup = setupLocationGroup(newWarehouse, "Default_Removed_Inventory_Location", "Default Location For Removed Inventory",
                "Removed_Inventory",
                false, false, false,false,null,
                InventoryConsolidationStrategy.NONE,false,false);
        setupSampleLocation(newWarehouse, locationGroup, "REMOVE_INV", "1001",
                999.0, 999.0, 999.0,
                100100000L, 100200000L,100100000L ,
                999999.0, 100.0, true);

        // locations for inventory being removed by count / audit count
        locationGroup = setupLocationGroup(newWarehouse, "Audit_Count_Inventory_Location", "Location For Inventory Removed By Audit Count",
                "Removed_Inventory",
                false, false, false,false,null,
                InventoryConsolidationStrategy.NONE,false,false);

        setupSampleLocation(newWarehouse, locationGroup, "AUDIT", "1001",
                999.0, 999.0, 999.0,
                100100001L, 100200001L,100200001L ,
                999999.0, 100.0, true);
        setupSampleLocation(newWarehouse, locationGroup, "COUNT", "1001",
                999.0, 999.0, 999.0,
                100100002L, 10100002L,100100002L ,
                999999.0, 100.0, true);

        // locations for inventory being removed by adjustment
        locationGroup = setupLocationGroup(newWarehouse, "Inventory_Adjustment_Location", "Location For Inventory Removed By Adjust",
                "Removed_Inventory",
                false, false, false,false,null,
                InventoryConsolidationStrategy.NONE,false,false);
        setupSampleLocation(newWarehouse, locationGroup, "INVADJ", "1001",
                999.0, 999.0, 999.0,
                100100003L, 10100003L,100100003L ,
                999999.0, 100.0, true);

        // locations for inventory removed by receiving
        locationGroup = setupLocationGroup(newWarehouse, "Inventory_Receiving_Location", "Location For Inventory Removed By Inventory receiving",
                "Removed_Inventory",
                false, false, false,false,null,
                InventoryConsolidationStrategy.NONE,false,false);
        setupSampleLocation(newWarehouse, locationGroup, "INVRCV", "1001",
                999.0, 999.0, 999.0,
                100100004L, 10100004L,100100004L ,
                999999.0, 100.0, true);

        // Production Line
        locationGroup = setupLocationGroup(newWarehouse, "ProductionLine", "Production Line",
                "ProductionLine",
                false, false, false,false,null,
                InventoryConsolidationStrategy.NONE,false,false);
        for(int i = 0; i < 20; i++) {
            String locationName = "LINE" + String.format("%02d", i);
            setupSampleLocation(newWarehouse, locationGroup, locationName, "601",
                    999.0, 999.0, 999.0,
                    60100000L + i, 60100000L + i,60100000L + i,
                    999999.0, 100.0, true);

        }
        // Production Line Inbound
        locationGroup = setupLocationGroup(newWarehouse, "ProductionLineInbound", "Production Line Inbound Stage",
                "ProductionLineInbound",
                false, false, false,false,null,
                InventoryConsolidationStrategy.NONE,false,false);
        for(int i = 0; i < 20; i++) {
            String locationName = "LINE" + String.format("%02d", i) + "-IN";
            setupSampleLocation(newWarehouse, locationGroup, locationName, "602",
                    999.0, 999.0, 999.0,
                    60200000L + i, 60200000L + i,60200000L + i,
                    999999.0, 100.0, true);

        }
        // production line outbound
        locationGroup = setupLocationGroup(newWarehouse, "ProductionLineOutbound", "Production Line Outbound Stage",
                "ProductionLineOutbound",
                false, false, false,false,null,
                InventoryConsolidationStrategy.NONE,false,false);
        for(int i = 0; i < 20; i++) {
            String locationName = "LINE" + String.format("%02d", i) + "-OUT";
            setupSampleLocation(newWarehouse, locationGroup, locationName, "603",
                    999.0, 999.0, 999.0,
                    60300000L + i, 60300000L + i,60300000L + i,
                    999999.0, 100.0, true);

        }
        /// order that is shipped
        // locations will be created per order in the name of order number
        locationGroup = setupLocationGroup(newWarehouse, "Order", "Order",
                "Shipped_Order",
                false, false, false,false,null,
                InventoryConsolidationStrategy.NONE,false,false);

        // storage locations

    }

    private Location setupSampleLocation(Warehouse warehouse, LocationGroup locationGroup, String locationName,
                                     String aisle, Double length, Double width, Double height,
                                     Long pickSequence, Long putawaySequence, Long countSequence,
                                     Double capacity, Double fillPercentage, boolean enabled) {
        Location location = new Location();
        location.setWarehouse(warehouse);
        location.setLocationGroup(locationGroup);
        location.setName(locationName);
        location.setAisle(aisle);
        location.setLength(length);
        location.setWidth(width);
        location.setHeight(height);
        location.setPickSequence(pickSequence);
        location.setPutawaySequence(putawaySequence);
        location.setCountSequence(countSequence);
        location.setCapacity(capacity);
        location.setFillPercentage(fillPercentage);
        location.setEnabled(enabled);
        location.setPendingVolume(0.0);
        location.setCurrentVolume(0.0);
        return locationService.save(location);

    }

    private LocationGroup setupLocationGroup(
            Warehouse warehouse, String name, String description, String locationGroupTypeName,
            boolean pickable, boolean storable, boolean countable,
            boolean trackingVolume, LocationVolumeTrackingPolicy locationVolumeTrackingPolicy,
            InventoryConsolidationStrategy inventoryConsolidationStrategy,
            boolean allowCartonization, boolean adjustable) {
        LocationGroupType locationGroupType = locationGroupTypeService.findByName(locationGroupTypeName);
        LocationGroup locationGroup = new LocationGroup();
        locationGroup.setLocationGroupType(locationGroupType);
        locationGroup.setWarehouse(warehouse);
        locationGroup.setName(name);
        locationGroup.setDescription(description);
        locationGroup.setPickable(pickable);
        locationGroup.setStorable(storable);
        locationGroup.setCountable(countable);
        locationGroup.setTrackingVolume(trackingVolume);
        locationGroup.setVolumeTrackingPolicy(locationVolumeTrackingPolicy);
        locationGroup.setInventoryConsolidationStrategy(inventoryConsolidationStrategy);
        locationGroup.setAllowCartonization(allowCartonization);
        locationGroup.setAdjustable(adjustable);

        return locationGroupService.save(locationGroup);


    }

    private void setupDefaultConfiguration(Warehouse newWarehouse) throws JsonProcessingException {
        setupDefaultPoliciesForNewWarehouse(newWarehouse);

        setupDefaultShippingStageAreaConfigurationForNewWarehouse(newWarehouse);

        setupDefaultInventoryStatusForNewWarehouse(newWarehouse);

        // setupDefaultPutawayConfigurationForNewWarehouse(newWarehouse, inventoryStatusList);
    }

    /**
    private void setupDefaultPutawayConfigurationForNewWarehouse(
            Warehouse newWarehouse, List<InventoryStatus> inventoryStatusList) {
        // we will setup default putaway configuration so everything will
        // go into the storage areas
        logger.debug("Start setup putaway configuration");

        List<LocationGroup> storageLocationGroups
                = locationGroupService.getStorageLocationGroup(newWarehouse.getId());
        logger.debug("Find {} storage location groups, from warehouse id {}",
                storageLocationGroups.size(), newWarehouse.getId());
        int sequence = 1;
        // setup the putaway configuration for available inventory only
        InventoryStatus availableInventoryStatus = null;
        for (InventoryStatus inventoryStatus : inventoryStatusList) {
            if (inventoryStatus.getName().equals("AVAL")) {
                availableInventoryStatus = inventoryStatus;
                break;
            }
        }
        logger.debug("find available inventory status : {}",
                Objects.nonNull(availableInventoryStatus));

        // we don't have the available inventory status defined. we will do nothing
        if (Objects.isNull(availableInventoryStatus)) {
            return;
        }

        for(LocationGroup locationGroup : storageLocationGroups) {
            PutawayConfiguration putawayConfiguration =
                    new PutawayConfiguration(
                            sequence,
                            newWarehouse.getId(),
                            null, // criteria: item
                            null, // criteria: item family
                            availableInventoryStatus.getId(),
                            null, // destination： location
                            locationGroup.getId() , // destination: location group
                            null , // destination:  location group type
                            PutawayConfigurationStrategy.EMPTY_LOCATIONS.toString()
                    );

            try {
                logger.debug("Will save putaway configuration: /n {}", putawayConfiguration);
                inboundServiceRestemplateClient.addPutawayConfiguration(putawayConfiguration);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        }
    }
*/
    private List<InventoryStatus> setupDefaultInventoryStatusForNewWarehouse(Warehouse newWarehouse) {

        List<InventoryStatus> inventoryStatusList = new ArrayList<>();
        InventoryStatus available = new InventoryStatus(
                "AVAL", "Available",
                newWarehouse.getId(), newWarehouse
        );
        InventoryStatus damaged = new InventoryStatus(
                "DMG", "Damaged",
                newWarehouse.getId(), newWarehouse
        );
        try {
            inventoryStatusList.add(
                    inventoryServiceRestemplateClient.addInventoryStatus(available));
            inventoryStatusList.add(
                    inventoryServiceRestemplateClient.addInventoryStatus(damaged));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        return inventoryStatusList;

    }

    /**
     * Setup default shipping stage area configuration. This is normally called when we initial
     * a new warehouse with some default data
     * @param newWarehouse
     */
    private void setupDefaultShippingStageAreaConfigurationForNewWarehouse(Warehouse newWarehouse) {

        LocationGroup shippingLocationGroup = locationGroupService.getShippingStageLocationGroup(newWarehouse.getId());
        if (Objects.nonNull(shippingLocationGroup)) {

            ShippingStageAreaConfiguration shippingStageAreaConfiguration =
                    new ShippingStageAreaConfiguration(
                            1,
                            newWarehouse.getId(),
                            newWarehouse,
                            shippingLocationGroup.getId(),
                            shippingLocationGroup,
                            ShippingStageLocationReserveStrategy.BY_SHIPMENT
                            );
            try {
                outboundServiceRestemplateClient.addShippingStageAreaConfiguration(shippingStageAreaConfiguration);
            } catch (JsonProcessingException e) {
                // ignore any exception
                e.printStackTrace();
            }
        }
    }

    private void setupDefaultPoliciesForNewWarehouse(Warehouse newWarehouse) throws JsonProcessingException {

        Policy policy = setupPolicy(newWarehouse,
                "LOCATION-DEFAULT-REMOVED-INVENTORY-LOCATION","REMOVE_INV","Location to Save Removed Inventory");
        commonServiceRestemplateClient.createPolicy(policy);

        policy = setupPolicy(newWarehouse,
                "LOCATION-AUDIT-COUNT","AUDIT","Location to Save Removed Inventory by Audit Count");
        commonServiceRestemplateClient.createPolicy(policy);

        policy = setupPolicy(newWarehouse,
                "LOCATION-COUNT","COUNT","Location to Save Removed Inventory by Count");
        commonServiceRestemplateClient.createPolicy(policy);

        policy = setupPolicy(newWarehouse,
                "LOCATION-INVENTORY-ADJUST","INVADJ","Location to Save Removed Inventory by Inventory Adjustment");
        commonServiceRestemplateClient.createPolicy(policy);

        policy = setupPolicy(newWarehouse,
                "LOCATION-RECEIVING","INVRCV","Location to Save Removed Inventory by Inventory Receiving");
        commonServiceRestemplateClient.createPolicy(policy);

        policy = setupPolicy(newWarehouse,
                "LOCATION-GROUP-RECEIPT","RECEIPT","Location Group for Receipt");
        commonServiceRestemplateClient.createPolicy(policy);

        policy = setupPolicy(newWarehouse,
                "JOB-EMERGENCY-REPLENISHMENT-MAX-COUNT","50","Max emergency replenishment can be processed in one round");
        commonServiceRestemplateClient.createPolicy(policy);

    }

    private Policy setupPolicy(Warehouse warehouse, String key, String value, String description) {
        Policy policy = new Policy();
        policy.setWarehouseId(warehouse.getId());
        policy.setKey(key);
        policy.setValue(value);
        policy.setDescription(description);
        return policy;
    }

    public Warehouse removeWarehouses(long id) {
        Warehouse warehouse = findById(id);
        // remove the location and location group

        locationService.removeLocations(warehouse);
        locationGroupService.removeLocationGroups(warehouse);

        // remove the configuration
        commonServiceRestemplateClient.removePolicy(warehouse, "");

        delete(id);

        return warehouse;
    }
}

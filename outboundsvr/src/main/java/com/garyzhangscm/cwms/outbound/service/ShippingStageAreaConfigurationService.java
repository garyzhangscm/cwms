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
import com.garyzhangscm.cwms.outbound.exception.ShippingException;
import com.garyzhangscm.cwms.outbound.model.*;
import com.garyzhangscm.cwms.outbound.repository.AllocationConfigurationRepository;
import com.garyzhangscm.cwms.outbound.repository.ShippingStageAreaConfigurationRepository;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.transaction.Transactional;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;


@Service
public class ShippingStageAreaConfigurationService implements TestDataInitiableService {
    private static final Logger logger = LoggerFactory.getLogger(ShippingStageAreaConfigurationService.class);

    @Autowired
    private ShippingStageAreaConfigurationRepository shippingStageAreaConfigurationRepository;

    @Autowired
    private CommonServiceRestemplateClient commonServiceRestemplateClient;
    @Autowired
    private WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient;
    @Autowired
    private InventoryServiceRestemplateClient inventoryServiceRestemplateClient;
    @Autowired
    private FileService fileService;
    @Autowired
    private UnitService unitService;

    @Value("${fileupload.test-data.shipping-stage-area-configuration:shipping-stage-area-configuration}")
    String testDataFile;

    public ShippingStageAreaConfiguration findById(Long id, boolean loadDetails) {
        ShippingStageAreaConfiguration shippingStageAreaConfiguration
                = shippingStageAreaConfigurationRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("shipping stage area configuration not found by id: " + id));
        if (loadDetails) {
            loadAttribute(shippingStageAreaConfiguration);
        }
        return shippingStageAreaConfiguration;
    }

    public ShippingStageAreaConfiguration findBySequence(Long warehouseId, int sequence) {
        return findBySequence(warehouseId, sequence, true);
    }


    public ShippingStageAreaConfiguration findBySequence(Long warehouseId,int sequence, boolean loadDetails) {
        ShippingStageAreaConfiguration shippingStageAreaConfiguration
                = shippingStageAreaConfigurationRepository.findByWarehouseIdAndSequence(warehouseId, sequence);
        if (shippingStageAreaConfiguration != null && loadDetails) {
            loadAttribute(shippingStageAreaConfiguration);
        }
        return shippingStageAreaConfiguration;
    }

    public ShippingStageAreaConfiguration findById(Long id) {
        return findById(id, true);
    }


    public List<ShippingStageAreaConfiguration> findAll(Long warehouseId, boolean loadDetails) {


        List<ShippingStageAreaConfiguration> shippingStageAreaConfigurations =
            shippingStageAreaConfigurationRepository.findAll(
                (Root<ShippingStageAreaConfiguration> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();

                    predicates.add(criteriaBuilder.equal(root.get("warehouseId"), warehouseId));

                    Predicate[] p = new Predicate[predicates.size()];
                    return criteriaBuilder.and(predicates.toArray(p));
                }
        );



        if (shippingStageAreaConfigurations.size() > 0 && loadDetails) {
            loadAttribute(shippingStageAreaConfigurations);
        }
        return shippingStageAreaConfigurations;
    }

    public List<ShippingStageAreaConfiguration> findAll(Long warehouseId) {
        return findAll(warehouseId, true);
    }

    private void loadAttribute(ShippingStageAreaConfiguration shippingStageAreaConfiguration) {

        if (shippingStageAreaConfiguration.getLocationGroupId() != null && shippingStageAreaConfiguration.getLocationGroup() == null) {
            try {
                shippingStageAreaConfiguration.setLocationGroup(
                        warehouseLayoutServiceRestemplateClient.getLocationGroupById(shippingStageAreaConfiguration.getLocationGroupId()));
            }
            catch (Exception ex) {}
        }

    }

    private void loadAttribute(List<ShippingStageAreaConfiguration> shippingStageAreaConfigurations) {
        shippingStageAreaConfigurations.forEach(shippingStageAreaConfiguration -> loadAttribute(shippingStageAreaConfiguration));
    }

    public ShippingStageAreaConfiguration save(ShippingStageAreaConfiguration shippingStageAreaConfiguration) {
        ShippingStageAreaConfiguration newShippingStageAreaConfiguration = shippingStageAreaConfigurationRepository.save(shippingStageAreaConfiguration);
        loadAttribute(newShippingStageAreaConfiguration);
        return newShippingStageAreaConfiguration;
    }

    public ShippingStageAreaConfiguration saveOrUpdate(ShippingStageAreaConfiguration shippingStageAreaConfiguration) {
        if (shippingStageAreaConfiguration.getId() == null
                && findBySequence(
                    shippingStageAreaConfiguration.getWarehouseId(),
                    shippingStageAreaConfiguration.getSequence(),
                    false) != null) {
            shippingStageAreaConfiguration.setId(
                    findBySequence(
                            shippingStageAreaConfiguration.getWarehouseId(),
                            shippingStageAreaConfiguration.getSequence(),
                            false
                    ).getId());
        }
        return save(shippingStageAreaConfiguration);
    }


    public void delete(ShippingStageAreaConfiguration shippingStageAreaConfiguration) {
        shippingStageAreaConfigurationRepository.delete(shippingStageAreaConfiguration);
    }

    public void delete(Long id) {
        shippingStageAreaConfigurationRepository.deleteById(id);
    }

    public void delete(String shippingStageAreaConfigurationIds) {
        if (!StringUtils.isBlank(shippingStageAreaConfigurationIds)) {
            long[] shippingStageAreaConfigurationIdArray = Arrays.asList(shippingStageAreaConfigurationIds.split(",")).stream().mapToLong(Long::parseLong).toArray();
            for (long id : shippingStageAreaConfigurationIdArray) {
                delete(id);
            }
        }
    }


    public List<ShippingStageAreaConfigurationCSVWrapper> loadData(InputStream inputStream) throws IOException {

        CsvSchema schema = CsvSchema.builder().
                addColumn("company").
                addColumn("warehouse").
                addColumn("sequence").
                addColumn("locationGroup").
                addColumn("locationReserveStrategy").
                build().withHeader();

        return fileService.loadData(inputStream, schema, ShippingStageAreaConfigurationCSVWrapper.class);
    }

    public void initTestData(Long companyId, String warehouseName) {
        try {

            String companyCode = warehouseLayoutServiceRestemplateClient.getCompanyById(companyId).getCode();

            String testDataFileName = StringUtils.isBlank(warehouseName) ?
                    testDataFile + ".csv" :
                    testDataFile + "-" + companyCode + "-" + warehouseName + ".csv";
            InputStream inputStream = new ClassPathResource(testDataFileName).getInputStream();
            List<ShippingStageAreaConfigurationCSVWrapper> shippingStageAreaConfigurationCSVWrappers = loadData(inputStream);
            shippingStageAreaConfigurationCSVWrappers
                    .stream()
                    .forEach(shippingStageAreaConfigurationCSVWrapper -> saveOrUpdate(convertFromWrapper(shippingStageAreaConfigurationCSVWrapper)));
        } catch (IOException ex) {
            logger.debug("Exception while load test data: {}", ex.getMessage());
        }
    }

    private ShippingStageAreaConfiguration convertFromWrapper(ShippingStageAreaConfigurationCSVWrapper shippingStageAreaConfigurationCSVWrapper) {

        ShippingStageAreaConfiguration shippingStageAreaConfiguration = new ShippingStageAreaConfiguration();
        shippingStageAreaConfiguration.setSequence(shippingStageAreaConfigurationCSVWrapper.getSequence());
        shippingStageAreaConfiguration.setLocationReserveStrategy(
                ShippingStageLocationReserveStrategy.valueOf(shippingStageAreaConfigurationCSVWrapper.getLocationReserveStrategy()));

        if (!StringUtils.isBlank(shippingStageAreaConfigurationCSVWrapper.getWarehouse())) {
            Warehouse warehouse = warehouseLayoutServiceRestemplateClient.getWarehouseByName(
                    shippingStageAreaConfigurationCSVWrapper.getCompany(),
                    shippingStageAreaConfigurationCSVWrapper.getWarehouse());
            if (warehouse != null) {
                shippingStageAreaConfiguration.setWarehouseId(warehouse.getId());
            }
        }
        if (!StringUtils.isBlank(shippingStageAreaConfigurationCSVWrapper.getLocationGroup())) {
            LocationGroup locationGroup =
                    warehouseLayoutServiceRestemplateClient.getLocationGroupByName(
                            shippingStageAreaConfigurationCSVWrapper.getCompany(),
                            shippingStageAreaConfigurationCSVWrapper.getWarehouse(),
                            shippingStageAreaConfigurationCSVWrapper.getLocationGroup());
            if (locationGroup != null) {
                shippingStageAreaConfiguration.setLocationGroupId(locationGroup.getId());
            }
        }

        return shippingStageAreaConfiguration;
    }

    // Reserve a location by the strategy
    public Location reserveShippingStageLocation(ShippingStageAreaConfiguration shippingStageAreaConfiguration, Pick pick) {
        logger.debug("Start to reserve a ship stage location for the pick {}, by configuration {} / strategy {}",
                pick.getNumber(), shippingStageAreaConfiguration.getSequence(),
                shippingStageAreaConfiguration.getLocationReserveStrategy());
        String reserveCode = "";
        switch (shippingStageAreaConfiguration.getLocationReserveStrategy()) {
            case BY_ORDER:
                reserveCode = pick.getOrderNumber();
                break;
            case BY_SHIPMENT:
                reserveCode = pick.getShipmentLine().getShipmentNumber();
                break;
            case BY_WAVE:
                // if the configuration is setup to reserve the locations by wave but for some reason
                // the shipment is not group into wave, then we will still use the
                // shipment number to reserve the location
                if (Objects.isNull(pick.getShipmentLine().getWave())) {
                    reserveCode = pick.getShipmentLine().getShipmentNumber();

                }
                else {

                    reserveCode = pick.getShipmentLine().getWave().getNumber();
                }
                break;
        }
        logger.debug(" will reserve ship stage with code: {}", reserveCode);

        if (StringUtils.isBlank(reserveCode)) {
            throw ShippingException.raiseException("Shipping Stage Area Configuration is no correctly defined");
        }

        return warehouseLayoutServiceRestemplateClient.reserveLocationFromGroup(
                shippingStageAreaConfiguration.getLocationGroupId(), reserveCode,
                pick.getSize(unitService).getFirst(), pick.getQuantity(), 1);


    }
    public ShippingStageAreaConfiguration getShippingStageArea(Pick pick, Long stagingLocationGroupId) {
        logger.debug("=====   Get ship stage area from pick / {} from warehouse ======",
                stagingLocationGroupId, pick.getWarehouseId());
        List<ShippingStageAreaConfiguration> shippingStageAreaConfigurations = findAll(pick.getWarehouseId());
        logger.debug(">> We have {} ship stage area configurations", shippingStageAreaConfigurations.size());
        for (ShippingStageAreaConfiguration shippingStageAreaConfiguration : shippingStageAreaConfigurations) {
            // Check if the pick match with the configuration, if so,
            // we will return the configuration
            logger.debug(">>> check ship stage configuration {} with pick {}",
                    shippingStageAreaConfiguration.getSequence(), pick.getNumber());
            if (shippingStageAreaConfiguration.getLocationGroupId().equals(stagingLocationGroupId)
                && match(pick, shippingStageAreaConfiguration)) {
                logger.debug(">>> Find Match!!!");
                return shippingStageAreaConfiguration;
            }

        }
        throw ShippingException.raiseException("Can't find matching shipping stage area");

    }
    public ShippingStageAreaConfiguration getShippingStageArea(Pick pick) {
        logger.debug("=====   Get ship stage area from pick ======");
        List<ShippingStageAreaConfiguration> shippingStageAreaConfigurations = findAll(pick.getWarehouseId());
        logger.debug(">> We have {} ship stage area configurations", shippingStageAreaConfigurations.size());
        if (shippingStageAreaConfigurations.size() == 0) {
            throw ShippingException.raiseException("no ship stage area configuration defined!");
        }
        for (ShippingStageAreaConfiguration shippingStageAreaConfiguration : shippingStageAreaConfigurations) {
            // Check if the pick match with the configuration, if so,
            // we will return the configuration
            logger.debug(">>> check ship stage configuration {} with pick {}",
                    shippingStageAreaConfiguration.getSequence(), pick.getNumber());
            if (match(pick, shippingStageAreaConfiguration)) {
                logger.debug(">>> Find Match!!!");
                return shippingStageAreaConfiguration;
            }

        }
        throw ShippingException.raiseException("Can't find matching shipping stage area");
    }

    private boolean match(Pick pick, ShippingStageAreaConfiguration shippingStageAreaConfiguration) {
        // TO-DO
        return true;
    }


    public ShippingStageAreaConfiguration addShippingStageAreaConfiguration(ShippingStageAreaConfiguration shippingStageAreaConfiguration) {
        return saveOrUpdate(shippingStageAreaConfiguration);
    }

    public ShippingStageAreaConfiguration changeShippingStageAreaConfiguration(ShippingStageAreaConfiguration shippingStageAreaConfiguration) {
        return saveOrUpdate(shippingStageAreaConfiguration);
    }
}

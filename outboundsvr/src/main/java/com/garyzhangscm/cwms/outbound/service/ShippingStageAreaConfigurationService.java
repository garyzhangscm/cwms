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

import javax.transaction.Transactional;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;


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

    @Value("${fileupload.test-data.shipping-stage-area-configuration:shipping-stage-area-configuration.csv}")
    String testDataFile;

    public ShippingStageAreaConfiguration findById(Long id, boolean loadDetails) {
        ShippingStageAreaConfiguration shippingStageAreaConfiguration = shippingStageAreaConfigurationRepository.findById(id).orElse(null);
        if (shippingStageAreaConfiguration != null && loadDetails) {
            loadAttribute(shippingStageAreaConfiguration);
        }
        return shippingStageAreaConfiguration;
    }

    public ShippingStageAreaConfiguration findBySequence(int sequence) {
        return findBySequence(sequence, true);
    }


    public ShippingStageAreaConfiguration findBySequence(int sequence, boolean loadDetails) {
        ShippingStageAreaConfiguration shippingStageAreaConfiguration = shippingStageAreaConfigurationRepository.findBySequence(sequence);
        if (shippingStageAreaConfiguration != null && loadDetails) {
            loadAttribute(shippingStageAreaConfiguration);
        }
        return shippingStageAreaConfiguration;
    }

    public ShippingStageAreaConfiguration findById(Long id) {
        return findById(id, true);
    }


    public List<ShippingStageAreaConfiguration> findAll(boolean loadDetails) {
        List<ShippingStageAreaConfiguration> shippingStageAreaConfigurations = shippingStageAreaConfigurationRepository.findAll();

        if (shippingStageAreaConfigurations.size() > 0 && loadDetails) {
            loadAttribute(shippingStageAreaConfigurations);
        }
        return shippingStageAreaConfigurations;
    }

    public List<ShippingStageAreaConfiguration> findAll() {
        return findAll(true);
    }

    private void loadAttribute(ShippingStageAreaConfiguration shippingStageAreaConfiguration) {

        if (shippingStageAreaConfiguration.getLocationGroupId() != null && shippingStageAreaConfiguration.getLocationGroup() == null) {
            shippingStageAreaConfiguration.setLocationGroup(warehouseLayoutServiceRestemplateClient.getLocationGroupById(shippingStageAreaConfiguration.getLocationGroupId()));
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
        if (shippingStageAreaConfiguration.getId() == null && findBySequence(shippingStageAreaConfiguration.getSequence()) != null) {
            shippingStageAreaConfiguration.setId(findBySequence(shippingStageAreaConfiguration.getSequence()).getId());
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
                addColumn("warehouse").
                addColumn("sequence").
                addColumn("locationGroup").
                addColumn("locationReserveStrategy").
                build().withHeader();

        return fileService.loadData(inputStream, schema, ShippingStageAreaConfigurationCSVWrapper.class);
    }

    public void initTestData() {
        try {
            InputStream inputStream = new ClassPathResource(testDataFile).getInputStream();
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
            Warehouse warehouse = warehouseLayoutServiceRestemplateClient.getWarehouseByName(shippingStageAreaConfigurationCSVWrapper.getWarehouse());
            if (warehouse != null) {
                shippingStageAreaConfiguration.setWarehouseId(warehouse.getId());
            }
        }
        if (!StringUtils.isBlank(shippingStageAreaConfigurationCSVWrapper.getLocationGroup())) {
            LocationGroup locationGroup =
                    warehouseLayoutServiceRestemplateClient.getLocationGroupByName(
                            shippingStageAreaConfigurationCSVWrapper.getWarehouse(), shippingStageAreaConfigurationCSVWrapper.getLocationGroup());
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
                reserveCode = pick.getShipmentLine().getWave().getNumber();
                break;
        }
        logger.debug(" will reserve ship stage with code: {}", reserveCode);

        if (StringUtils.isBlank(reserveCode)) {
            throw new GenericException(10000, "Shipping Stage Area Configuration is no correctly defined");
        }

        return warehouseLayoutServiceRestemplateClient.reserveLocationFromGroup(
                shippingStageAreaConfiguration.getLocationGroupId(), reserveCode, pick.getSize(), pick.getQuantity(), 1);


    }
    public ShippingStageAreaConfiguration getShippingStageArea(Pick pick, Long stagingLocationGroupId) {
        logger.debug("=====   Get ship stage area from pick / {} ======", stagingLocationGroupId);
        List<ShippingStageAreaConfiguration> shippingStageAreaConfigurations = findAll();
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
        throw new GenericException(10000, "Can't find matching shipping stage area");

    }
    public ShippingStageAreaConfiguration getShippingStageArea(Pick pick) {
        logger.debug("=====   Get ship stage area from pick ======");
        List<ShippingStageAreaConfiguration> shippingStageAreaConfigurations = findAll();
        logger.debug(">> We have {} ship stage area configurations", shippingStageAreaConfigurations.size());
        if (shippingStageAreaConfigurations.size() == 0) {
            throw new GenericException(10000, "no ship stage area configuration defined!");
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
        throw new GenericException(10000, "Can't find matching shipping stage area");
    }

    private boolean match(Pick pick, ShippingStageAreaConfiguration shippingStageAreaConfiguration) {
        // TO-DO
        return true;
    }




}

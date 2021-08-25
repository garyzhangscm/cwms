/**
 * Copyright 2019
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

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.garyzhangscm.cwms.inventory.clients.WarehouseLayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.inventory.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.inventory.model.Company;
import com.garyzhangscm.cwms.inventory.model.InventoryConfiguration;
import com.garyzhangscm.cwms.inventory.model.InventoryConfigurationType;
import com.garyzhangscm.cwms.inventory.model.Warehouse;
import com.garyzhangscm.cwms.inventory.repository.InventoryConfigurationRepository;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class InventoryConfigurationService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    @Autowired
    private InventoryConfigurationRepository inventoryConfigurationRepository;
    @Autowired
    private WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient;

    public InventoryConfiguration findById(Long id) {
        return inventoryConfigurationRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("inventory configuration  not found by id: " + id));
    }

    public List<InventoryConfiguration> findAll() {

        return inventoryConfigurationRepository.findAll();
    }
    public InventoryConfiguration save(InventoryConfiguration inventoryConfiguration) {
        return inventoryConfigurationRepository.save(inventoryConfiguration);
    }
    public InventoryConfiguration saveOrUpdate(InventoryConfiguration inventoryConfiguration) {
        if (Objects.nonNull(findByCompanyAndWarehouseAndType(
                inventoryConfiguration.getCompanyId(), inventoryConfiguration.getWarehouseId(),
                        inventoryConfiguration.getType()))) {
            inventoryConfiguration.setId(
                    findByCompanyAndWarehouseAndType(
                            inventoryConfiguration.getCompanyId(), inventoryConfiguration.getWarehouseId(),
                            inventoryConfiguration.getType()
                    ).getId()
            );
        }
        return inventoryConfigurationRepository.save(inventoryConfiguration);
    }
    public InventoryConfiguration findByCompanyAndWarehouseAndType(Long companyId, Long warehouseId,
                                                                   InventoryConfigurationType type) {
        // we will start with most specific until we reach the most generic
        // 1. company id + warehouse id + type
        // 2. company id + type
        // 3. type

        // get the configuration at warehouse level
        InventoryConfiguration inventoryConfiguration = inventoryConfigurationRepository.findByCompanyIdAndWarehouseIdAndType(
                companyId, warehouseId, type
        );
        if (Objects.nonNull(inventoryConfiguration)) {
            return inventoryConfiguration;
        }
        // get the configuration at company level
        inventoryConfiguration = inventoryConfigurationRepository.findByCompanyIdAndType(
                companyId, type
        );
        if (Objects.nonNull(inventoryConfiguration)) {
            return inventoryConfiguration;
        }

        // get the configuration at global level
        return inventoryConfigurationRepository.findByType(type);
    }
    public InventoryConfiguration findByCompanyIdAndWarehouseNameAndType(Long companyId, String warehouseName,
                                                                  InventoryConfigurationType type) {
        Company company = warehouseLayoutServiceRestemplateClient.getCompanyById(companyId);
        if (Objects.isNull(company)) {
            return null;
        }
        Warehouse warehouse = warehouseLayoutServiceRestemplateClient.getWarehouseByName(
                company.getCode(), warehouseName
        );
        if (Objects.isNull(warehouse)) {
            return null;
        }
        InventoryConfiguration inventoryConfiguration = findByCompanyAndWarehouseAndType(
                company.getId(), warehouse.getId(), type
        );
        return inventoryConfiguration;

    }

    public InventoryConfiguration getLPNValidationRule(Long companyId, Long warehouseId) {
        return findByCompanyAndWarehouseAndType(
                companyId, warehouseId, InventoryConfigurationType.LPN_VALIDATION_RULE
        );
    }




}

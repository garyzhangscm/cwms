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

package com.garyzhangscm.cwms.layout.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.garyzhangscm.cwms.layout.exception.RequestValidationFailException;
import com.garyzhangscm.cwms.layout.model.BillableEndpoint;
import com.garyzhangscm.cwms.layout.model.Warehouse;
import com.garyzhangscm.cwms.layout.model.WarehouseConfiguration;
import com.garyzhangscm.cwms.layout.service.WarehouseConfigurationService;
import com.garyzhangscm.cwms.layout.service.WarehouseService;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@RestController
public class WarehouseConfigurationController {

    private static final Logger logger = LoggerFactory.getLogger(WarehouseConfigurationController.class);
    @Autowired
    WarehouseConfigurationService warehouseConfigurationService;


    @RequestMapping(value="/warehouse-configuration", method=RequestMethod.GET)
    public List<WarehouseConfiguration> getWarehouseConfiguration(
            @RequestParam(name = "companyId", required = false, defaultValue = "") Long companyId,
            @RequestParam(name = "companyCode", required = false, defaultValue = "") String companyCode,
            @RequestParam(name = "warehouseId", required = false, defaultValue = "") Long warehouseId,
            @RequestParam(name = "warehouseName", required = false, defaultValue = "") String warehouseName) {

        return warehouseConfigurationService.findAll(companyId, companyCode, warehouseId, warehouseName);
    }

    @RequestMapping(value="/warehouse-configuration/by-warehouse/{warehouseId}", method=RequestMethod.GET)
    public WarehouseConfiguration getWarehouseConfiguration(
            @PathVariable Long warehouseId) {

        return warehouseConfigurationService.findByWarehouse(warehouseId);
    }

    @BillableEndpoint
    @RequestMapping(value="/warehouse-configuration", method=RequestMethod.POST)
    @Caching(
            evict = {
                    @CacheEvict(cacheNames = "inventory_warehouse_configuration", allEntries = true),
                    @CacheEvict(cacheNames = "common_warehouse_configuration", allEntries = true),
                    @CacheEvict(cacheNames = "work_order_warehouse_configuration", allEntries = true),
                    @CacheEvict(cacheNames = "admin_warehouse_configuration", allEntries = true)
            }
    )
    public WarehouseConfiguration changeWarehouseConfiguration(@RequestParam Long companyId,
                                   @RequestBody WarehouseConfiguration warehouseConfiguration) throws JsonProcessingException {
        return warehouseConfigurationService.changeWarehouseConfiguration(companyId, warehouseConfiguration);
    }




}

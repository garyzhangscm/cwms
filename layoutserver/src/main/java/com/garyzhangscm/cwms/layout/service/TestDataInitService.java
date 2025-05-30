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

package com.garyzhangscm.cwms.layout.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class TestDataInitService {

    private static final Logger logger = LoggerFactory.getLogger(TestDataInitService.class);
    @Autowired
    private JdbcTemplate jdbcTemplate;

    // WarehouseService warehouseService;

    LocationGroupTypeService locationGroupTypeService;

    LocationGroupService locationGroupService;

    LocationService locationService;

    Map<String, TestDataInitiableService> initiableServices = new HashMap<>();
    List<String> serviceNames = new ArrayList<>();
    @Autowired
    public TestDataInitService(LocationGroupTypeService locationGroupTypeService,
                               LocationGroupService locationGroupService,
                               LocationService locationService) {
        // this.warehouseService = warehouseService;
        this.locationGroupTypeService = locationGroupTypeService;
        this.locationGroupService = locationGroupService;
        this.locationService = locationService;

        // initiableServices.put("warehouse", warehouseService);
        // serviceNames.add("warehouse");
        initiableServices.put("location_group_type", locationGroupTypeService);
        serviceNames.add("location_group_type");
        initiableServices.put("location_group", locationGroupService);
        serviceNames.add("location_group");
    }
    public String[] getTestDataNames() {
        return serviceNames.toArray(new String[0]);
    }
    public void init(Long companyId, String warehouseName) {
        serviceNames.forEach(serviceName -> init(companyId, serviceName, warehouseName));
    }
    public void init(Long companyId, String name, String warehouseName) {
        initiableServices.get(name).initTestData(companyId, warehouseName);
    }
    public TestDataInitiableService getInitiableService(String name) {
        return initiableServices.get(name);
    }

    public void clear(Long warehouseId) {

        jdbcTemplate.update("delete from location where warehouse_id = ?", new Object[] { warehouseId });
        logger.debug("location records from warehouse ID {} removed!", warehouseId);

        jdbcTemplate.update("delete from location_group where warehouse_id = ?", new Object[] { warehouseId });
        logger.debug("location_group records from warehouse ID {} removed!", warehouseId);



        // We will keep the warehouse information
        // jdbcTemplate.update("delete from warehouse where warehouse_id = ?", new Object[] { warehouseId });
        // logger.debug("warehouse records from warehouse ID {} removed!", warehouseId);

    }
}

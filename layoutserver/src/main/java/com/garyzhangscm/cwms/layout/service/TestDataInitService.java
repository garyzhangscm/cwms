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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class TestDataInitService {

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
        initiableServices.put("location group type", locationGroupTypeService);
        serviceNames.add("location group type");
        initiableServices.put("location group", locationGroupService);
        serviceNames.add("location group");
        initiableServices.put("location", locationService);
        serviceNames.add("location");
    }
    public String[] getTestDataNames() {
        return serviceNames.toArray(new String[0]);
    }
    public void init(String warehouseName) {
        for(TestDataInitiableService testDataInitiableService : initiableServices.values()) {
            testDataInitiableService.initTestData(warehouseName);
        }
    }
    public void init(String name, String warehouseName) {
        initiableServices.get(name).initTestData(warehouseName);
    }
    public TestDataInitiableService getInitiableService(String name) {
        return initiableServices.get(name);
    }

}

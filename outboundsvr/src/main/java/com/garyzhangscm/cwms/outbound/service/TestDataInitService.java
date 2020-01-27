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

package com.garyzhangscm.cwms.outbound.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class TestDataInitService {

    OrderService orderService;

    OrderLineService orderLineService;

    AllocationConfigurationService allocationConfigurationService;

    ShippingStageAreaConfigurationService shippingStageAreaConfigurationService;

    EmergencyReplenishmentConfigurationService emergencyReplenishmentConfigurationService;

    TrailerTemplateService trailerTemplateService;

    @Autowired
    private KafkaTemplate kafkaTemplate;

    Map<String, TestDataInitiableService> initiableServices = new HashMap<>();
    List<String> serviceNames = new ArrayList<>();
    @Autowired
    public TestDataInitService(OrderService orderService,
                              OrderLineService orderLineService,
                               AllocationConfigurationService allocationConfigurationService,
                               ShippingStageAreaConfigurationService shippingStageAreaConfigurationService,
                               EmergencyReplenishmentConfigurationService emergencyReplenishmentConfigurationService,
                               TrailerTemplateService trailerTemplateService) {
        this.orderService = orderService;
        this.orderLineService = orderLineService;
        this.allocationConfigurationService = allocationConfigurationService;
        this.shippingStageAreaConfigurationService = shippingStageAreaConfigurationService;
        this.emergencyReplenishmentConfigurationService = emergencyReplenishmentConfigurationService;

        this.trailerTemplateService = trailerTemplateService;


        initiableServices.put("Order", orderService);
        serviceNames.add("Order");
        initiableServices.put("Order Line", orderLineService);
        serviceNames.add("Order Line");
        initiableServices.put("Allocation Configuration", allocationConfigurationService);
        serviceNames.add("Allocation Configuration");
        initiableServices.put("Shipping Stage Area Configuration", shippingStageAreaConfigurationService);
        serviceNames.add("Shipping Stage Area Configuration");
        initiableServices.put("Emergency Replenishment Configuration", emergencyReplenishmentConfigurationService);
        serviceNames.add("Emergency Replenishment Configuration");
        initiableServices.put("Trailer Template", trailerTemplateService);
        serviceNames.add("Trailer Template");

    }
    public String[] getTestDataNames() {
        return serviceNames.toArray(new String[0]);
    }
    public void init() {
        for(TestDataInitiableService testDataInitiableService : initiableServices.values()) {
            testDataInitiableService.initTestData();
        }
    }
    public void init(String name) {
        initiableServices.get(name).initTestData();
    }
    public TestDataInitiableService getInitiableService(String name) {
        return initiableServices.get(name);
    }

}

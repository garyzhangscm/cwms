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

package com.garyzhangscm.cwms.workorder.service;

import com.garyzhangscm.cwms.workorder.model.WorkOrderInstructionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class TestDataInitService {

    BillOfMaterialService billOfMaterialService;

    BillOfMaterialLineService billOfMaterialLineService;

    WorkOrderInstructionTemplateService workOrderInstructionTemplateService;

    ProductionLineService productionLineService;


    WorkOrderService workOrderService;

    WorkOrderLineService workOrderLineService;

    WorkOrderInstructionService workOrderInstructionService;

    Map<String, TestDataInitiableService> initiableServices = new HashMap<>();
    List<String> serviceNames = new ArrayList<>();

    @Autowired
    public TestDataInitService(BillOfMaterialService billOfMaterialService,
                               BillOfMaterialLineService billOfMaterialLineService,
                               WorkOrderInstructionTemplateService workOrderInstructionTemplateService,
                               ProductionLineService productionLineService,
                               WorkOrderService workOrderService,
                               WorkOrderLineService workOrderLineService,
                               WorkOrderInstructionService workOrderInstructionService) {
        this.billOfMaterialService = billOfMaterialService;
        this.billOfMaterialLineService = billOfMaterialLineService;
        this.workOrderInstructionTemplateService = workOrderInstructionTemplateService;

        this.productionLineService = productionLineService;

        this.workOrderService = workOrderService;
        this.workOrderLineService = workOrderLineService;
        this.workOrderInstructionService = workOrderInstructionService;



        initiableServices.put("Bill Of Material", billOfMaterialService);
        serviceNames.add("Bill Of Material");

        initiableServices.put("Bill Of Material Line", billOfMaterialLineService);
        serviceNames.add("Bill Of Material Line");

        initiableServices.put("Work Order Instruction Template", workOrderInstructionTemplateService);
        serviceNames.add("Work Order Instruction Template");

        initiableServices.put("Production Line", productionLineService);
        serviceNames.add("Production Line");

        initiableServices.put("Work Order", workOrderService);
        serviceNames.add("Work Order");
        initiableServices.put("Work Order Line", workOrderLineService);
        serviceNames.add("Work Order Line");
        initiableServices.put("Work Order Instruction", workOrderInstructionService);
        serviceNames.add("Work Order Instruction");
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

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



import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class TestDataInitService {

    private static final Logger logger = LoggerFactory.getLogger(TestDataInitService.class);
    @Autowired
    private JdbcTemplate jdbcTemplate;

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

    public void clear(Long warehouseId) {
        jdbcTemplate.update("delete from bill_of_material_line where bill_of_material_id in " +
                             "  (select bill_of_material_id from  bill_of_material where warehouse_id = ?)", new Object[] { warehouseId });
        logger.debug("bill_of_material_line records from warehouse ID {} removed!", warehouseId);

        jdbcTemplate.update("delete from work_order_instruction_template where bill_of_material_id in " +
                "  (select bill_of_material_id from  bill_of_material where warehouse_id = ?)", new Object[] { warehouseId });
        logger.debug("work_order_instruction_template records from warehouse ID {} removed!", warehouseId);

        jdbcTemplate.update("delete from bill_of_material where warehouse_id = ?", new Object[] { warehouseId });
        logger.debug("bill_of_material records from warehouse ID {} removed!", warehouseId);



        jdbcTemplate.update("delete from production_line_activity where work_order_id in " +
                "  (select work_order_id from  work_order where warehouse_id = ?)", new Object[] { warehouseId });
        logger.debug("production_line_activity records from warehouse ID {} removed!", warehouseId);

        jdbcTemplate.update("delete from  work_order_assignment where work_order_id in " +
                "  (select work_order_id from  work_order where warehouse_id = ?)", new Object[] { warehouseId });
        logger.debug("work_order_assignment records from warehouse ID {} removed!", warehouseId);

        jdbcTemplate.update("delete from work_order_instruction where work_order_id in " +
                "  (select work_order_id from  work_order where warehouse_id = ?)", new Object[] { warehouseId });
        logger.debug("work_order_instruction records from warehouse ID {} removed!", warehouseId);

        jdbcTemplate.update("delete from work_order_line where work_order_id in " +
                "  (select work_order_id from  work_order where warehouse_id = ?)", new Object[] { warehouseId });
        logger.debug("work_order_line records from warehouse ID {} removed!", warehouseId);

        jdbcTemplate.update("delete from work_order where warehouse_id = ?", new Object[] { warehouseId });
        logger.debug("work_order records from warehouse ID {} removed!", warehouseId);

        jdbcTemplate.update("delete from production_line where warehouse_id = ?", new Object[] { warehouseId });
        logger.debug("production_line records from warehouse ID {} removed!", warehouseId);

    }
}

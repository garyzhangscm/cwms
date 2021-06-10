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



import com.garyzhangscm.cwms.workorder.model.WorkOrderByProduct;
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

    ProductionPlanService productionPlanService;
    ProductionPlanLineService productionPlanLineService;


    BillOfMaterialService billOfMaterialService;

    BillOfMaterialLineService billOfMaterialLineService;

    BillOfMaterialByProductService billOfMaterialByProductService;

    WorkOrderInstructionTemplateService workOrderInstructionTemplateService;

    ProductionLineService productionLineService;


    WorkOrderService workOrderService;

    WorkOrderLineService workOrderLineService;

    WorkOrderByProductService workOrderByProductService;

    WorkOrderInstructionService workOrderInstructionService;

    Map<String, TestDataInitiableService> initiableServices = new HashMap<>();
    List<String> serviceNames = new ArrayList<>();

    @Autowired
    public TestDataInitService(BillOfMaterialService billOfMaterialService,
                               BillOfMaterialLineService billOfMaterialLineService,
                               BillOfMaterialByProductService billOfMaterialByProductService,
                               WorkOrderInstructionTemplateService workOrderInstructionTemplateService,
                               ProductionLineService productionLineService,
                               WorkOrderService workOrderService,
                               WorkOrderLineService workOrderLineService,
                               WorkOrderByProductService workOrderByProductService,
                               WorkOrderInstructionService workOrderInstructionService,
                               ProductionPlanService productionPlanService,
                               ProductionPlanLineService productionPlanLineService) {
        this.billOfMaterialService = billOfMaterialService;
        this.billOfMaterialLineService = billOfMaterialLineService;
        this.billOfMaterialByProductService = billOfMaterialByProductService;
        this.workOrderInstructionTemplateService = workOrderInstructionTemplateService;

        this.productionLineService = productionLineService;

        this.workOrderService = workOrderService;
        this.workOrderLineService = workOrderLineService;
        this.workOrderByProductService = workOrderByProductService;
        this.workOrderInstructionService = workOrderInstructionService;

        this.productionPlanService = productionPlanService;
        this.productionPlanLineService = productionPlanLineService;


        initiableServices.put("Bill_Of_Material", billOfMaterialService);
        serviceNames.add("Bill_Of_Material");

        initiableServices.put("Bill_Of_Material_Line", billOfMaterialLineService);
        serviceNames.add("Bill_Of_Material_Line");

        initiableServices.put("Bill_Of_Material_By_Product", billOfMaterialByProductService);
        serviceNames.add("Bill_Of_Material_By_Product");

        initiableServices.put("Work_Order_Instruction_Template", workOrderInstructionTemplateService);
        serviceNames.add("Work_Order_Instruction_Template");

        initiableServices.put("Production_Line", productionLineService);
        serviceNames.add("Production_Line");

        initiableServices.put("Work_Order", workOrderService);
        serviceNames.add("Work_Order");
        initiableServices.put("Work_Order_Line", workOrderLineService);
        serviceNames.add("Work_Order_Line");
        initiableServices.put("Work_Order_By_Product", workOrderByProductService);
        serviceNames.add("Work_Order_By_Product");
        initiableServices.put("Work_Order_Instruction", workOrderInstructionService);
        serviceNames.add("Work_Order_Instruction");
        initiableServices.put("Production_Plan", productionPlanService);
        serviceNames.add("Production_Plan");
        initiableServices.put("Production_Plan_Line", productionPlanLineService);
        serviceNames.add("Production_Plan_Line");

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


        jdbcTemplate.update("delete from production_line_assignment where work_order_id in " +
                "  (select work_order_id from  work_order where warehouse_id = ?)", new Object[] { warehouseId });
        logger.debug("production_line_assignment records from warehouse ID {} removed!", warehouseId);


        jdbcTemplate.update("delete from production_line_capacity where production_line_id in " +
                "  (select production_line_id from  production_line where warehouse_id = ?)", new Object[] { warehouseId });
        logger.debug("production_line_capacity records from warehouse ID {} removed!", warehouseId);

        jdbcTemplate.update("delete from bill_of_material_line where bill_of_material_id in " +
                             "  (select bill_of_material_id from  bill_of_material where warehouse_id = ?)", new Object[] { warehouseId });
        logger.debug("bill_of_material_line records from warehouse ID {} removed!", warehouseId);

        jdbcTemplate.update("delete from bill_of_material_by_product where bill_of_material_id in " +
                "  (select bill_of_material_id from  bill_of_material where warehouse_id = ?)", new Object[] { warehouseId });
        logger.debug("bill_of_material_by_product records from warehouse ID {} removed!", warehouseId);

        jdbcTemplate.update("delete from work_order_instruction_template where bill_of_material_id in " +
                "  (select bill_of_material_id from  bill_of_material where warehouse_id = ?)", new Object[] { warehouseId });
        logger.debug("work_order_instruction_template records from warehouse ID {} removed!", warehouseId);



        jdbcTemplate.update("delete from work_order_line_consume_transaction where work_order_line_id in " +
                "  (select work_order_line_id from  work_order join work_order_line on work_order.work_order_id = work_order_line.work_order_id " +
                "      where work_order.warehouse_id = ?)", new Object[] { warehouseId });
        logger.debug("work_order_line_consume_transaction records from warehouse ID {} removed!", warehouseId);

        jdbcTemplate.update("delete from work_order_kpi_transaction where 1=1 ");
        logger.debug("work_order_kpi_transaction records from warehouse ID {} removed!", warehouseId);

        jdbcTemplate.update("delete from work_order_produced_inventory where work_order_produce_transaction_id in " +
                                "  (select work_order_produce_transaction_id from  work_order join work_order_produce_transaction " +
                        "  on work_order.work_order_id = work_order_produce_transaction.work_order_id " +
                                "      where work_order.warehouse_id = ?)", new Object[] { warehouseId });
        logger.debug("work_order_produced_inventory records from warehouse ID {} removed!", warehouseId);


        jdbcTemplate.update("delete from work_order_produce_transaction where work_order_id in " +
                "  (select work_order_id from  work_order where warehouse_id = ?)", new Object[] { warehouseId });
        logger.debug("work_order_produce_transaction records from warehouse ID {} removed!", warehouseId);

        jdbcTemplate.update("delete from work_order_kpi where work_order_id in " +
                "  (select work_order_id from  work_order where warehouse_id = ?)", new Object[] { warehouseId });
        logger.debug("work_order_produce_transaction records from warehouse ID {} removed!", warehouseId);

        jdbcTemplate.update("delete from return_material_request where work_order_line_complete_transaction_id in " +
                "  (select work_order_line_complete_transaction_id from  work_order join work_order_line " +
                "       on work_order.work_order_id = work_order_line.work_order_id  " +
                "      join work_order_line_complete_transaction on work_order_line.work_order_line_id = work_order_line_complete_transaction.work_order_line_id " +
                "      where work_order.warehouse_id = ?)", new Object[] { warehouseId });
        logger.debug("return_material_request records from warehouse ID {} removed!", warehouseId);

        jdbcTemplate.update("delete from work_order_line_complete_transaction where work_order_line_id in " +
                "  (select work_order_line_id from  work_order join work_order_line on work_order.work_order_id = work_order_line.work_order_id " +
                "      where work_order.warehouse_id = ?)", new Object[] { warehouseId });
        logger.debug("work_order_line_complete_transaction records from warehouse ID {} removed!", warehouseId);



        jdbcTemplate.update("delete from work_order_complete_transaction where work_order_id in " +
                "  (select work_order_id from  work_order where warehouse_id = ?)", new Object[] { warehouseId });
        logger.debug("work_order_complete_transaction records from warehouse ID {} removed!", warehouseId);



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

        jdbcTemplate.update("delete from work_order_by_product where work_order_id in " +
                "  (select work_order_id from  work_order where warehouse_id = ?)", new Object[] { warehouseId });
        logger.debug("work_order_by_product records from warehouse ID {} removed!", warehouseId);

        jdbcTemplate.update("delete from work_order where warehouse_id = ?", new Object[] { warehouseId });
        logger.debug("work_order records from warehouse ID {} removed!", warehouseId);

        jdbcTemplate.update("delete from production_line where warehouse_id = ?", new Object[] { warehouseId });
        logger.debug("production_line records from warehouse ID {} removed!", warehouseId);


        jdbcTemplate.update("delete from production_plan_line where production_plan_id in " +
                "(select production_plan_id from production_plan where warehouse_id = ?)", new Object[] { warehouseId });
        logger.debug("production_plan_line records from warehouse ID {} removed!", warehouseId);

        jdbcTemplate.update("delete from production_plan where warehouse_id = ?", new Object[] { warehouseId });
        logger.debug("production_plan records from warehouse ID {} removed!", warehouseId);

        jdbcTemplate.update("delete from bill_of_material where warehouse_id = ?", new Object[] { warehouseId });
        logger.debug("bill_of_material records from warehouse ID {} removed!", warehouseId);

    }
}

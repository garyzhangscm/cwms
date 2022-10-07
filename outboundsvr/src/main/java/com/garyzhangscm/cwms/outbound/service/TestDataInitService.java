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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
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


    OrderService orderService;

    OrderLineService orderLineService;

    AllocationConfigurationService allocationConfigurationService;

    ShippingStageAreaConfigurationService shippingStageAreaConfigurationService;

    EmergencyReplenishmentConfigurationService emergencyReplenishmentConfigurationService;

    ListPickConfigurationService listPickConfigurationService;

    TrailerTemplateService trailerTemplateService;

    CartonizationConfigurationService cartonizationConfigurationService;

    CartonService cartonService;

    GridConfigurationService gridConfigurationService;

    GridLocationConfigurationService gridLocationConfigurationService;

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
                               ListPickConfigurationService listPickConfigurationService,
                               TrailerTemplateService trailerTemplateService,
                               CartonService cartonService,
                               CartonizationConfigurationService cartonizationConfigurationService,
                               GridConfigurationService gridConfigurationService,
                               GridLocationConfigurationService gridLocationConfigurationService) {
        this.orderService = orderService;
        this.orderLineService = orderLineService;
        this.allocationConfigurationService = allocationConfigurationService;
        this.shippingStageAreaConfigurationService = shippingStageAreaConfigurationService;
        this.emergencyReplenishmentConfigurationService = emergencyReplenishmentConfigurationService;
        this.listPickConfigurationService = listPickConfigurationService;
        this.cartonService = cartonService;
        this.cartonizationConfigurationService = cartonizationConfigurationService;
        this.gridConfigurationService = gridConfigurationService;
        this.gridLocationConfigurationService = gridLocationConfigurationService;

        this.trailerTemplateService = trailerTemplateService;


        initiableServices.put("Order", orderService);
        serviceNames.add("Order");
        initiableServices.put("Order_Line", orderLineService);
        serviceNames.add("Order_Line");
        initiableServices.put("Allocation_Configuration", allocationConfigurationService);
        serviceNames.add("Allocation_Configuration");
        initiableServices.put("Shipping_Stage_Area_Configuration", shippingStageAreaConfigurationService);
        serviceNames.add("Shipping_Stage_Area_Configuration");
        initiableServices.put("Emergency_Replenishment_Configuration", emergencyReplenishmentConfigurationService);
        serviceNames.add("Emergency_Replenishment_Configuration");
        initiableServices.put("List_Picking_Configuration", listPickConfigurationService);
        serviceNames.add("List_Picking_Configuration");
        initiableServices.put("Carton", cartonService);
        serviceNames.add("Carton");
        initiableServices.put("Cartonization_Configuration", cartonizationConfigurationService);
        serviceNames.add("Cartonization_Configuration");
        initiableServices.put("Trailer_Template", trailerTemplateService);
        serviceNames.add("Trailer_Template");

        initiableServices.put("Grid_Configuration", gridConfigurationService);
        serviceNames.add("Grid_Configuration");

        initiableServices.put("Grid_Location_Configuration", gridLocationConfigurationService);
        serviceNames.add("Grid_Location_Configuration");

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

        jdbcTemplate.update("delete from pick_movement where warehouse_id = ?", new Object[] { warehouseId });
        logger.debug("pick_movement records from warehouse ID {} removed!", warehouseId);

        jdbcTemplate.update("delete from pick where warehouse_id = ?", new Object[] { warehouseId });
        logger.debug("pick records from warehouse ID {} removed!", warehouseId);

        jdbcTemplate.update("delete from cancelled_pick where warehouse_id = ?", new Object[] { warehouseId });
        logger.debug("cancelled_pick records from warehouse ID {} removed!", warehouseId);

        jdbcTemplate.update("delete from cartonization where warehouse_id = ?", new Object[] { warehouseId });
        logger.debug("cartonization records from warehouse ID {} removed!", warehouseId);

        jdbcTemplate.update("delete from shipping_cartonization where warehouse_id = ?", new Object[] { warehouseId });
        logger.debug("shipping_cartonization records from warehouse ID {} removed!", warehouseId);

        jdbcTemplate.update("delete from carton where warehouse_id = ?", new Object[] { warehouseId });
        logger.debug("carton records from warehouse ID {} removed!", warehouseId);

        jdbcTemplate.update("delete from pick_list where warehouse_id = ?", new Object[] { warehouseId });
        logger.debug("pick_list records from warehouse ID {} removed!", warehouseId);

        jdbcTemplate.update("delete from short_allocation where warehouse_id = ?", new Object[] { warehouseId });
        logger.debug("short_allocation records from warehouse ID {} removed!", warehouseId);

        jdbcTemplate.update("delete from cancelled_short_allocation where warehouse_id = ?", new Object[] { warehouseId });
        logger.debug("cancelled_short_allocation records from warehouse ID {} removed!", warehouseId);

        jdbcTemplate.update("delete from shipment_line where warehouse_id = ?", new Object[] { warehouseId });
        logger.debug("shipment_line records from warehouse ID {} removed!", warehouseId);

        jdbcTemplate.update("delete from shipment where warehouse_id = ?", new Object[] { warehouseId });
        logger.debug("shipment records from warehouse ID {} removed!", warehouseId);

        jdbcTemplate.update("delete from stop where warehouse_id = ?", new Object[] { warehouseId });
        logger.debug("stop records from warehouse ID {} removed!", warehouseId);

        jdbcTemplate.update("delete from trailer where warehouse_id = ?", new Object[] { warehouseId });
        logger.debug("trailer records from warehouse ID {} removed!", warehouseId);

        jdbcTemplate.update("delete from wave where warehouse_id = ?", new Object[] { warehouseId });
        logger.debug("wave records from warehouse ID {} removed!", warehouseId);


        jdbcTemplate.update("delete from outbound_order_line where warehouse_id = ?", new Object[] { warehouseId });
        logger.debug("outbound_order_line records from warehouse ID {} removed!", warehouseId);

        jdbcTemplate.update("delete from outbound_order where warehouse_id = ?", new Object[] { warehouseId });
        logger.debug("outbound_order records from warehouse ID {} removed!", warehouseId);


        jdbcTemplate.update("delete from allocation_configuration_pickable_unit_of_measure where warehouse_id = ?", new Object[] { warehouseId });
        logger.debug("allocation_configuration_pickable_unit_of_measure records from warehouse ID {} removed!", warehouseId);

        jdbcTemplate.update("delete from allocation_configuration where warehouse_id = ?", new Object[] { warehouseId });
        logger.debug("allocation_configuration records from warehouse ID {} removed!", warehouseId);

        jdbcTemplate.update("delete from shipping_stage_area_configuration where warehouse_id = ?", new Object[] { warehouseId });
        logger.debug("shipping_stage_area_configuration records from warehouse ID {} removed!", warehouseId);

        jdbcTemplate.update("delete from emergency_replenishment_configuration where warehouse_id = ?", new Object[] { warehouseId });
        logger.debug("emergency_replenishment_configuration records from warehouse ID {} removed!", warehouseId);


        jdbcTemplate.update("delete from trailer_template where warehouse_id = ?", new Object[] { warehouseId });
        logger.debug("trailer_template records from warehouse ID {} removed!", warehouseId);


        jdbcTemplate.update("delete from list_picking_configuration where warehouse_id = ?", new Object[] { warehouseId });
        logger.debug("list_picking_configuration records from warehouse ID {} removed!", warehouseId);


        jdbcTemplate.update("delete from cartonization_configuration_group_rule " +
                " where cartonization_configuration_id in (select  cartonization_configuration_id from cartonization_configuration where warehouse_id = ?)",
                new Object[] { warehouseId });
        logger.debug("cartonization_configuration_group_rule records from warehouse ID {} removed!", warehouseId);

        jdbcTemplate.update("delete from cartonization_configuration where warehouse_id = ?", new Object[] { warehouseId });
        logger.debug("cartonization_configuration records from warehouse ID {} removed!", warehouseId);


        jdbcTemplate.update("delete from grid_configuration where warehouse_id = ?", new Object[] { warehouseId });
        logger.debug("grid_configuration records from warehouse ID {} removed!", warehouseId);

        jdbcTemplate.update("delete from grid_location_configuration where warehouse_id = ?", new Object[] { warehouseId });
        logger.debug("grid_location_configuration records from warehouse ID {} removed!", warehouseId);
    }
}

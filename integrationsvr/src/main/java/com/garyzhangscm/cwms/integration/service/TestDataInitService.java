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

package com.garyzhangscm.cwms.integration.service;

import com.garyzhangscm.cwms.integration.clients.WarehouseLayoutServiceRestemplateClient;
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

    @Autowired
    private WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient;


    public String[] getTestDataNames() {
        return  new String[]{"Integration"};
    }
    public void init(String warehouseName) {

        // Do nothing
    }
    public void init(String name, String warehouseName) {

        // Do nothing
    }


    public void clear(Long warehouseId) {

        String warehouseName = warehouseLayoutServiceRestemplateClient.getWarehouseById(warehouseId).getName();

        jdbcTemplate.update("delete from integration_client");
        logger.debug("integration_client records from warehouse ID {} removed!", warehouseId);

        jdbcTemplate.update("delete from integration_customer");
        logger.debug("integration_customer records from warehouse ID {} removed!", warehouseId);

        jdbcTemplate.update("delete from integration_supplier");
        logger.debug("integration_supplier records from warehouse ID {} removed!", warehouseId);

        jdbcTemplate.update("delete from integration_item_unit_of_measure where warehouse_id = ?", new Object[] { warehouseId });
        jdbcTemplate.update("delete from integration_item_unit_of_measure where warehouse_name = ?", new Object[] { warehouseName });
        logger.debug("integration_item_unit_of_measure records from warehouse ID {} / {} removed!", warehouseId, warehouseName);

        jdbcTemplate.update("delete from integration_item_package_type where warehouse_id = ?", new Object[] { warehouseId });
        jdbcTemplate.update("delete from integration_item_package_type where warehouse_name = ?", new Object[] { warehouseName });
        logger.debug("integration_item_package_type records from warehouse ID {} / {} removed!", warehouseId, warehouseName);

        jdbcTemplate.update("delete from integration_item where warehouse_id = ?", new Object[] { warehouseId });
        jdbcTemplate.update("delete from integration_item where warehouse_name = ?", new Object[] { warehouseName });
        logger.debug("integration_item records from warehouse ID {} / {} removed!", warehouseId, warehouseName);

        jdbcTemplate.update("delete from integration_item_family where warehouse_id = ?", new Object[] { warehouseId });
        jdbcTemplate.update("delete from integration_item_family where warehouse_name = ?", new Object[] { warehouseName });
        logger.debug("integration_item_family records from warehouse ID {} / {} removed!", warehouseId, warehouseName);


        jdbcTemplate.update("delete from integration_inventory_adjustment_confirmation where warehouse_id = ?", new Object[] { warehouseId });
        jdbcTemplate.update("delete from integration_inventory_adjustment_confirmation where warehouse_name = ?", new Object[] { warehouseName });
        logger.debug("integration_inventory_adjustment_confirmation records from warehouse ID {} / {} removed!", warehouseId, warehouseName);

        jdbcTemplate.update("delete from integration_inventory_attribute_change_confirmation where warehouse_id = ?", new Object[] { warehouseId });
        jdbcTemplate.update("delete from integration_inventory_attribute_change_confirmation where warehouse_name = ?", new Object[] { warehouseName });
        logger.debug("integration_inventory_attribute_change_confirmation records from warehouse ID {} / {} removed!", warehouseId, warehouseName);

        jdbcTemplate.update("delete from integration_inventory_shippping_confirmation where warehouse_id = ?", new Object[] { warehouseId });
        jdbcTemplate.update("delete from integration_inventory_shippping_confirmation where warehouse_name = ?", new Object[] { warehouseName });
        logger.debug("integration_inventory_shippping_confirmation records from warehouse ID {} / {} removed!", warehouseId, warehouseName);


        jdbcTemplate.update("delete from integration_order_line where warehouse_id = ?", new Object[] { warehouseId });
        jdbcTemplate.update("delete from integration_order_line where warehouse_name = ?", new Object[] { warehouseName });
        logger.debug("integration_order_line records from warehouse ID {} / {} removed!", warehouseId, warehouseName);

        jdbcTemplate.update("delete from integration_order where warehouse_id = ?", new Object[] { warehouseId });
        jdbcTemplate.update("delete from integration_order where warehouse_name = ?", new Object[] { warehouseName });
        logger.debug("integration_order records from warehouse ID {} / {} removed!", warehouseId, warehouseName);

        jdbcTemplate.update("delete from integration_order_line_confirmation where warehouse_id = ?", new Object[] { warehouseId });
        jdbcTemplate.update("delete from integration_order_line_confirmation where warehouse_name = ?", new Object[] { warehouseName });
        logger.debug("integration_order_line_confirmation records from warehouse ID {} / {} removed!", warehouseId, warehouseName);

        jdbcTemplate.update("delete from integration_order_confirmation where warehouse_id = ?", new Object[] { warehouseId });
        jdbcTemplate.update("delete from integration_order_confirmation where warehouse_name = ?", new Object[] { warehouseName });
        logger.debug("integration_order_confirmation records from warehouse ID {} / {} removed!", warehouseId, warehouseName);


        jdbcTemplate.update("delete from integration_work_order_line_confirmation where integration_work_order_confirmation_id in (" +
                "select integration_work_order_confirmation_id from integration_work_order_confirmation where warehouse_id = ?)",
                new Object[] { warehouseId });
        jdbcTemplate.update("delete from integration_work_order_line_confirmation where integration_work_order_confirmation_id in (" +
                        "select integration_work_order_confirmation_id from integration_work_order_confirmation where warehouse_name = ?)",
                new Object[] { warehouseName });
        logger.debug("integration_work_order_line_confirmation records from warehouse ID {} / {} removed!", warehouseId, warehouseName);

        jdbcTemplate.update("delete from integration_work_order_confirmation where warehouse_id = ?", new Object[] { warehouseId });
        jdbcTemplate.update("delete from integration_work_order_confirmation where warehouse_name = ?", new Object[] { warehouseName });
        logger.debug("integration_work_order_confirmation records from warehouse ID {} / {} removed!", warehouseId, warehouseName);

        jdbcTemplate.update("delete from integration_receipt_line where warehouse_id = ?", new Object[] { warehouseId });
        jdbcTemplate.update("delete from integration_receipt_line where warehouse_name = ?", new Object[] { warehouseName });
        logger.debug("integration_receipt_line records from warehouse ID {} / {} removed!", warehouseId, warehouseName);

        jdbcTemplate.update("delete from integration_receipt where warehouse_id = ?", new Object[] { warehouseId });
        jdbcTemplate.update("delete from integration_receipt where warehouse_name = ?", new Object[] { warehouseName });
        logger.debug("integration_receipt records from warehouse ID {} / {} removed!", warehouseId, warehouseName);

        jdbcTemplate.update("delete from integration_receipt_line_confirmation where warehouse_id = ?", new Object[] { warehouseId });
        jdbcTemplate.update("delete from integration_receipt_line_confirmation where warehouse_name = ?", new Object[] { warehouseName });
        logger.debug("integration_receipt_line_confirmation records from warehouse ID {} / {} removed!", warehouseId, warehouseName);

        jdbcTemplate.update("delete from integration_receipt_confirmation where warehouse_id = ?", new Object[] { warehouseId });
        jdbcTemplate.update("delete from integration_receipt_confirmation where warehouse_name = ?", new Object[] { warehouseName });
        logger.debug("integration_receipt_confirmation records from warehouse ID {} / {} removed!", warehouseId, warehouseName);

        jdbcTemplate.update("delete from integration_shipment_line_confirmation where warehouse_id = ?", new Object[] { warehouseId });
        jdbcTemplate.update("delete from integration_shipment_line_confirmation where warehouse_name = ?", new Object[] { warehouseName });
        logger.debug("integration_shipment_line_confirmation records from warehouse ID {} / {} removed!", warehouseId, warehouseName);


    }
}

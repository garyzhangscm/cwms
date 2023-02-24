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

package com.garyzhangscm.cwms.inbound.service;

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

    ReceiptService receiptService;

    ReceiptLineService receiptLineService;

    PutawayConfigurationService putawayConfigurationService;

    Map<String, TestDataInitiableService> initiableServices = new HashMap<>();
    List<String> serviceNames = new ArrayList<>();
    @Autowired
    public TestDataInitService(ReceiptService receiptService,
                               ReceiptLineService receiptLineService,
                               PutawayConfigurationService putawayConfigurationService) {
        this.receiptService = receiptService;
        this.receiptLineService = receiptLineService;
        this.putawayConfigurationService = putawayConfigurationService;

        initiableServices.put("Receipt", receiptService);
        serviceNames.add("Receipt");

        initiableServices.put("Putaway_Configuration", putawayConfigurationService);
        serviceNames.add("Putaway_Configuration");
    }
    public String[] getTestDataNames() {
        return serviceNames.toArray(new String[0]);
    }
    public void init(Long companyId,String warehouseName) {
        serviceNames.forEach(serviceName -> init(companyId, serviceName, warehouseName));
    }
    public void init(Long companyId, String name, String warehouseName) {
        initiableServices.get(name).initTestData(companyId, warehouseName);
    }
    public TestDataInitiableService getInitiableService(String name) {
        return initiableServices.get(name);
    }

    public void clear(Long warehouseId) {

        jdbcTemplate.update("delete from receipt_line where warehouse_id = ?", new Object[] { warehouseId });
        logger.debug("receipt_line records from warehouse ID {} removed!", warehouseId);

        jdbcTemplate.update("delete from receipt where warehouse_id = ?", new Object[] { warehouseId });
        logger.debug("receipt records from warehouse ID {} removed!", warehouseId);

        jdbcTemplate.update("delete from putaway_configuration where warehouse_id = ?", new Object[] { warehouseId });
        logger.debug("putaway_configuration records from warehouse ID {} removed!", warehouseId);

    }
}

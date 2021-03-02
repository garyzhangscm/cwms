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

package com.garyzhangscm.cwms.common.service;

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



    ClientService clientService;

    SupplierService supplierService;

    CustomerService customerService;

    CarrierService carrierService;

    CarrierServiceLevelService carrierServiceLevelService;

    UnitOfMeasureService unitOfMeasureService;

    ReasonCodeService reasonCodeService;

    SystemControlledNumberService systemControlledNumberService;

    PolicyService policyService;

    Map<String, TestDataInitiableService> initiableServices = new HashMap<>();
    List<String> serviceNames = new ArrayList<>();

    @Autowired
    public TestDataInitService(

            ClientService clientService,
            SupplierService supplierService,
            CustomerService customerService,
            CarrierService carrierService,
            CarrierServiceLevelService carrierServiceLevelService,
            UnitOfMeasureService unitOfMeasureService,
            ReasonCodeService reasonCodeService,
            SystemControlledNumberService systemControlledNumberService,
            PolicyService policyService) {

        this.clientService = clientService;
        this.supplierService = supplierService;
        this.customerService = customerService;
        this.carrierService = carrierService;
        this.carrierServiceLevelService = carrierServiceLevelService;
        this.unitOfMeasureService = unitOfMeasureService;
        this.reasonCodeService = reasonCodeService;
        this.systemControlledNumberService = systemControlledNumberService;
        this.policyService = policyService;


        initiableServices.put("Client", clientService);
        serviceNames.add("Client");
        initiableServices.put("Supplier", supplierService);
        serviceNames.add("Supplier");
        initiableServices.put("Customer", customerService);
        serviceNames.add("Customer");
        initiableServices.put("Carrier", carrierService);
        serviceNames.add("Carrier");
        initiableServices.put("Carrier_Service_Level", carrierServiceLevelService);
        serviceNames.add("Carrier_Service_Level");
        initiableServices.put("Unit_of_Measure", unitOfMeasureService);
        serviceNames.add("Unit_of_Measure");
        initiableServices.put("Reason_Code", reasonCodeService);
        serviceNames.add("Reason_Code");
        initiableServices.put("System_Controlled_Number", systemControlledNumberService);
        serviceNames.add("System_Controlled_Number");
        initiableServices.put("Policy", policyService);
        serviceNames.add("Policy");
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

        // Since all those records are not warehouse related, we will
        // remove them here
        jdbcTemplate.update("delete from client where warehouse_id = ?", new Object[] { warehouseId });
        logger.debug("client records removed!");

        jdbcTemplate.update("delete from supplier where warehouse_id = ?", new Object[] { warehouseId });
        logger.debug("supplier records removed!");

        jdbcTemplate.update("delete from customer where warehouse_id = ?", new Object[] { warehouseId });
        logger.debug("customer records removed!");

        jdbcTemplate.update("delete from unit_of_measure where warehouse_id = ?", new Object[] { warehouseId });
        logger.debug("unit_of_measure records removed!");

        jdbcTemplate.update("delete from reason_code where warehouse_id = ?", new Object[] { warehouseId });
        logger.debug("reason_code records removed!");

        jdbcTemplate.update("delete from system_controlled_number where warehouse_id = ?", new Object[] { warehouseId });
        logger.debug("system_controlled_number records removed!");

        jdbcTemplate.update("delete from policy where warehouse_id = ?", new Object[] { warehouseId });
        logger.debug("policy records removed!");

        jdbcTemplate.update("delete from carrier_service_level where warehouse_id = ?", new Object[] { warehouseId });
        logger.debug("carrier_service_level records removed!");
        jdbcTemplate.update("delete from carrier where warehouse_id = ?", new Object[] { warehouseId });
        logger.debug("carrier records removed!");


        jdbcTemplate.update("delete from work_task where warehouse_id = ?", new Object[] { warehouseId });
        logger.debug("work_task records removed!");

    }
}

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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.TreeMap;

@Service
public class TestDataInitService {
    ClientService clientService;

    SupplierService supplierService;

    CustomerService customerService;

    UnitOfMeasureService unitOfMeasureService;

    ReasonCodeService reasonCodeService;

    SystemControlledNumberService systemControlledNumberService;

    PolicyService policyService;

    Map<String, TestDataInitiableService> initiableServices = new TreeMap<>();
    @Autowired
    public TestDataInitService(
            ClientService clientService,
            SupplierService supplierService,
            CustomerService customerService,
            UnitOfMeasureService unitOfMeasureService,
            ReasonCodeService reasonCodeService,
            SystemControlledNumberService systemControlledNumberService,
            PolicyService policyService) {
        this.clientService = clientService;
        this.supplierService = supplierService;
        this.customerService = customerService;
        this.unitOfMeasureService = unitOfMeasureService;
        this.reasonCodeService = reasonCodeService;
        this.systemControlledNumberService = systemControlledNumberService;
        this.policyService = policyService;

        initiableServices.put("Client", clientService);
        initiableServices.put("Supplier", supplierService);
        initiableServices.put("Customer", customerService);
        initiableServices.put("Unit of Measure", unitOfMeasureService);
        initiableServices.put("Reason Code", reasonCodeService);
        initiableServices.put("System Controlled Number", systemControlledNumberService);
        initiableServices.put("Policy", policyService);
    }
    public String[] getTestDataNames() {
        return initiableServices.keySet().toArray(new String[0]);
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

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

import java.util.*;

@Service
public class TestDataInitService {
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
        initiableServices.put("Carrier Service Level", carrierServiceLevelService);
        serviceNames.add("Carrier Service Level");
        initiableServices.put("Unit of Measure", unitOfMeasureService);
        serviceNames.add("Unit of Measure");
        initiableServices.put("Reason Code", reasonCodeService);
        serviceNames.add("Reason Code");
        initiableServices.put("System Controlled Number", systemControlledNumberService);
        serviceNames.add("System Controlled Number");
        initiableServices.put("Policy", policyService);
        serviceNames.add("Policy");
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

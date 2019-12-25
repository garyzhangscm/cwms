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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class TestDataInitService {

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
        initiableServices.put("Receipt Line", receiptLineService);
        serviceNames.add("Receipt Line");

        initiableServices.put("Putaway Configuration", putawayConfigurationService);
        serviceNames.add("Putaway Configuration");
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

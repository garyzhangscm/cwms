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

package com.garyzhangscm.cwms.resources.service;

import com.garyzhangscm.cwms.resources.ResponseBodyWrapper;
import com.garyzhangscm.cwms.resources.clients.*;
import javafx.collections.transformation.SortedList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

@Service
public class InitTestDataService {

    CommonServiceRestemplateClient commonServiceRestemplateClient;

    InventoryServiceRestemplateClient inventoryServiceRestemplateClient;

    LayoutServiceRestemplateClient layoutServiceRestemplateClient;

    InboundServiceRestemplateClient inboundServiceRestemplateClient;

    OutboundServiceRestemplateClient outboundServiceRestemplateClient;




    List<InitiableServiceRestemplateClient> initiableServiceRestemplateClients = new LinkedList<>();
    @Autowired
    public InitTestDataService(CommonServiceRestemplateClient commonServiceRestemplateClient,
                               InventoryServiceRestemplateClient inventoryServiceRestemplateClient,
                               LayoutServiceRestemplateClient layoutServiceRestemplateClient,
                               InboundServiceRestemplateClient inboundServiceRestemplateClient,
                               OutboundServiceRestemplateClient outboundServiceRestemplateClient) {
        this.commonServiceRestemplateClient = commonServiceRestemplateClient;
        this.layoutServiceRestemplateClient  = layoutServiceRestemplateClient;
        this.inventoryServiceRestemplateClient = inventoryServiceRestemplateClient;
        this.inboundServiceRestemplateClient = inboundServiceRestemplateClient;
        this.outboundServiceRestemplateClient = outboundServiceRestemplateClient;

        initiableServiceRestemplateClients.add(commonServiceRestemplateClient);
        initiableServiceRestemplateClients.add(layoutServiceRestemplateClient);
        initiableServiceRestemplateClients.add(inventoryServiceRestemplateClient);
        initiableServiceRestemplateClients.add(inboundServiceRestemplateClient);
        initiableServiceRestemplateClients.add(outboundServiceRestemplateClient);

    }

    public String[] getTestDataNames() {
        List<String> testDataNames = new ArrayList<>();
        for(InitiableServiceRestemplateClient initiableServiceRestemplateClient : initiableServiceRestemplateClients) {
            testDataNames.addAll(Arrays.asList(initiableServiceRestemplateClient.getTestDataNames()));
        }
        return testDataNames.toArray(new String[0]);
    }

    public void init() {
        initAll();
    }
    public void init(String name) {
        if (name.isEmpty() || name.equals("ALL")) {
            initAll();
        }
        else {

            for(InitiableServiceRestemplateClient initiableServiceRestemplateClient : initiableServiceRestemplateClients) {
                if (initiableServiceRestemplateClient.contains(name)) {
                    initiableServiceRestemplateClient.initTestData(name);
                }
            }
        }

    }
    private void initAll() {
        for(InitiableServiceRestemplateClient initiableServiceRestemplateClient : initiableServiceRestemplateClients) {

            initiableServiceRestemplateClient.initTestData();
        }
    }
}

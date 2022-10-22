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

package com.garyzhangscm.cwms.inbound.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.garyzhangscm.cwms.inbound.ResponseBodyWrapper;
import com.garyzhangscm.cwms.inbound.model.*;
import com.garyzhangscm.cwms.inbound.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class InboundController {
    private static final Logger logger = LoggerFactory.getLogger(InboundController.class);

    @Autowired
    ReceiptLineService receiptLineService;

    @Autowired
    CustomerReturnOrderLineService customerReturnOrderLineService;
    @Autowired
    InboundQCConfigurationService inboundQCConfigurationService;
    @Autowired
    PutawayConfigurationService putawayConfigurationService;
    @Autowired
    PurchaseOrderService purchaseOrderService;



    @RequestMapping(value="/inbound-configuration/item-override", method = RequestMethod.POST)
    public ResponseBodyWrapper<String> handleItemOverride(
            @RequestParam Long warehouseId,
            @RequestParam Long oldItemId,
            @RequestParam Long newItemId
    ) {
        receiptLineService.handleItemOverride(warehouseId,
                oldItemId, newItemId);

        customerReturnOrderLineService.handleItemOverride(warehouseId,
                oldItemId, newItemId);

        inboundQCConfigurationService.handleItemOverride(warehouseId,
                oldItemId, newItemId);

        putawayConfigurationService.handleItemOverride(warehouseId,
                oldItemId, newItemId);

        purchaseOrderService.handleItemOverride(warehouseId,
                oldItemId, newItemId);

        return ResponseBodyWrapper.success("success");
    }
}

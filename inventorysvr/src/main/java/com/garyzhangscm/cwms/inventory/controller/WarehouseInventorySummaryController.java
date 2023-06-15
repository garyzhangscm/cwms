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

package com.garyzhangscm.cwms.inventory.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.garyzhangscm.cwms.inventory.ResponseBodyWrapper;
import com.garyzhangscm.cwms.inventory.clients.WarehouseLayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.inventory.exception.RequestValidationFailException;
import com.garyzhangscm.cwms.inventory.model.*;
import com.garyzhangscm.cwms.inventory.service.FileService;
import com.garyzhangscm.cwms.inventory.service.InventoryService;
import com.garyzhangscm.cwms.inventory.service.WarehouseInventorySummaryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Objects;

@RestController
public class WarehouseInventorySummaryController {
    private static final Logger logger = LoggerFactory.getLogger(WarehouseInventorySummaryController.class);
    @Autowired
    WarehouseInventorySummaryService warehouseInventorySummaryService;



    @RequestMapping(value="/inventories/warehouse-inventory-summary", method = RequestMethod.GET)
    @ClientValidationEndpoint
    public List<WarehouseInventorySummary> getWarehouseInventorySummaries(
            @RequestParam Long warehouseId,
            @RequestParam(name = "itemNameList", defaultValue = "", required = false) String itemNameList,
            ClientRestriction clientRestriction) {

        return warehouseInventorySummaryService.getWarehouseInventorySummaries(warehouseId, itemNameList, clientRestriction);
    }
}

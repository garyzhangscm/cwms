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


import com.garyzhangscm.cwms.inventory.model.ClientRestriction;
import com.garyzhangscm.cwms.inventory.model.ClientValidationEndpoint;
import com.garyzhangscm.cwms.inventory.model.InventoryActivity;

import com.garyzhangscm.cwms.inventory.service.InventoryActivityService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
public class InventoryActivityController {
    @Autowired
    InventoryActivityService inventoryActivityService;


    @ClientValidationEndpoint
    @RequestMapping(value="/inventory-activities", method = RequestMethod.GET)
    public List<InventoryActivity> findAllInventoryActivities(@RequestParam Long warehouseId,
                                                              @RequestParam(name="itemName", required = false, defaultValue = "") String itemName,
                                                              @RequestParam(name="clients", required = false, defaultValue = "") String clientIds,
                                                              @RequestParam(name="itemFamilies", required = false, defaultValue = "") String itemFamilyIds,
                                                              @RequestParam(name="inventoryStatusId", required = false, defaultValue = "") Long inventoryStatusId,
                                                              @RequestParam(name="location", required = false, defaultValue = "") String locationName,
                                                              @RequestParam(name="locationId", required = false, defaultValue = "") Long locationId,
                                                              @RequestParam(name="locationGroupId", required = false, defaultValue = "") Long locationGroupId,
                                                              @RequestParam(name="receiptId", required = false, defaultValue = "") String receiptId,
                                                              @RequestParam(name="pickIds", required = false, defaultValue = "") String pickIds,
                                                              @RequestParam(name="lpn", required = false, defaultValue = "") String lpn,
                                                              @RequestParam(name="inventoryActivityType", required = false, defaultValue = "") String inventoryActivityType,
                                                              @RequestParam(name="beginDateTime", required = false, defaultValue = "") String beginDateTime,
                                                              @RequestParam(name="endDateTime", required = false, defaultValue = "") String endDateTime,
                                                              // begin date will be in the format of YYYYMMDD
                                                              @RequestParam(name="beginDate", required = false, defaultValue = "") String beginDate,
                                                              // end date will be in the format of YYYYMMDD
                                                              @RequestParam(name="endDate", required = false, defaultValue = "") String endDate,
                                                              @RequestParam(name="date", required = false, defaultValue = "") String date,
                                                              @RequestParam(name="username", required = false, defaultValue = "") String username,
                                                              @RequestParam(name="rfCode", required = false, defaultValue = "") String rfCode,
                                                              @RequestParam(name = "includeDetails", defaultValue = "true", required = false) Boolean includeDetails,
                                                              ClientRestriction clientRestriction) {
        return inventoryActivityService.findAll(warehouseId, itemName, clientIds, itemFamilyIds,inventoryStatusId,
                locationName, locationId, locationGroupId, receiptId, pickIds, lpn,
                inventoryActivityType,  beginDateTime, endDateTime, beginDate, endDate, date, username,
                rfCode, clientRestriction, includeDetails
        );
    }
    @ClientValidationEndpoint
    @RequestMapping(value="/inventory-activities/pagination", method = RequestMethod.GET)
    public Page<InventoryActivity> findPaginatedInventoryActivities(@RequestParam Long warehouseId,
                                                                    @RequestParam(name="itemName", required = false, defaultValue = "") String itemName,
                                                                    @RequestParam(name="clients", required = false, defaultValue = "") String clientIds,
                                                                    @RequestParam(name="itemFamilies", required = false, defaultValue = "") String itemFamilyIds,
                                                                    @RequestParam(name="inventoryStatusId", required = false, defaultValue = "") Long inventoryStatusId,
                                                                    @RequestParam(name="location", required = false, defaultValue = "") String locationName,
                                                                    @RequestParam(name="locationId", required = false, defaultValue = "") Long locationId,
                                                                    @RequestParam(name="locationGroupId", required = false, defaultValue = "") Long locationGroupId,
                                                                    @RequestParam(name="receiptId", required = false, defaultValue = "") String receiptId,
                                                                    @RequestParam(name="pickIds", required = false, defaultValue = "") String pickIds,
                                                                    @RequestParam(name="lpn", required = false, defaultValue = "") String lpn,
                                                                    @RequestParam(name="inventoryActivityType", required = false, defaultValue = "") String inventoryActivityType,
                                                                    @RequestParam(name="beginDateTime", required = false, defaultValue = "") String beginDateTime,
                                                                    @RequestParam(name="endDateTime", required = false, defaultValue = "") String endDateTime,
                                                                    // begin date will be in the format of YYYYMMDD
                                                                    @RequestParam(name="beginDate", required = false, defaultValue = "") String beginDate,
                                                                    // end date will be in the format of YYYYMMDD
                                                                    @RequestParam(name="endDate", required = false, defaultValue = "") String endDate,
                                                                    @RequestParam(name="date", required = false, defaultValue = "") String date,
                                                                    @RequestParam(name="username", required = false, defaultValue = "") String username,
                                                                    @RequestParam(name="rfCode", required = false, defaultValue = "") String rfCode,
                                                                    @RequestParam(name = "pageIndex", required = false, defaultValue = "0") int pageIndex,
                                                                    @RequestParam(name = "pageSize", required = false, defaultValue = "20") int pageSize,
                                                                    ClientRestriction clientRestriction) {

        Pageable paging = PageRequest.of(pageIndex, pageSize, Sort.by(Sort.Direction.DESC, "transactionId"));

        return inventoryActivityService.findPaginatedInventoryActivities(warehouseId, itemName, clientIds, itemFamilyIds,inventoryStatusId,
                locationName, locationId, locationGroupId, receiptId, pickIds, lpn,
                inventoryActivityType,  beginDateTime, endDateTime, beginDate, endDate, date, username,
                rfCode, clientRestriction, paging
        );
    }


}

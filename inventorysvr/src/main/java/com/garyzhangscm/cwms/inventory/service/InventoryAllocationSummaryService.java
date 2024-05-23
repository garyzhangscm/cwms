/**
 * Copyright 2018
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

package com.garyzhangscm.cwms.inventory.service;


import com.garyzhangscm.cwms.inventory.clients.CommonServiceRestemplateClient;
import com.garyzhangscm.cwms.inventory.clients.KafkaSender;
import com.garyzhangscm.cwms.inventory.clients.OutbuondServiceRestemplateClient;
import com.garyzhangscm.cwms.inventory.clients.WarehouseLayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.inventory.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.inventory.model.*;
import com.garyzhangscm.cwms.inventory.repository.InventoryActivityRepository;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.persistence.criteria.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 *
 */
@Service
public class InventoryAllocationSummaryService {
    private static final Logger logger = LoggerFactory.getLogger(InventoryAllocationSummaryService.class);

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private OutbuondServiceRestemplateClient outbuondServiceRestemplateClient;

    public Collection<InventoryAllocationSummary> getInventoryAllocationSummary(Long warehouseId,
                                                                                  Long itemId,
                                                                                  String itemName,
                                                                                  Long locationId,
                                                                                String locationName) {
        //get all the inventory based on the criteria
        List<Inventory> inventories = inventoryService.findAll(warehouseId,
                itemId, itemName,null, null,null,null,null, null,locationName,
                locationId,null,
                null, null,null,null,null, null,
                null,null,null,null,null, null,null,null,null,
                null,null,null,null,null,
                false, null,
                null);
        // key: location id + item id  + inventory status id
        Map<String, InventoryAllocationSummary> inventoryAllocationSummaryMap = new HashMap<>();
        inventories.forEach(
                inventory -> {
                    String key = inventory.getLocationId() + "|" + inventory.getItem().getId() + "|" + inventory.getInventoryStatus().getId();
                    InventoryAllocationSummary inventoryAllocationSummary;
                    if (inventoryAllocationSummaryMap.containsKey(key)) {
                        inventoryAllocationSummary = inventoryAllocationSummaryMap.get(key);
                        inventoryAllocationSummary.setTotalQuantity(
                                inventoryAllocationSummary.getTotalQuantity() + inventory.getQuantity()
                        );
                        inventoryAllocationSummary.setAvailableQuantity(
                                inventoryAllocationSummary.getAvailableQuantity() + inventory.getQuantity()
                        );
                    }
                    else {
                        inventoryAllocationSummary = new InventoryAllocationSummary(inventory);
                    }
                    inventoryAllocationSummaryMap.put(key, inventoryAllocationSummary);
                }
        );

        return inventoryAllocationSummaryMap.values();
        /***
         * We will calculate the quantites on the client to save some time
         */

        /***
        // let's get the open picks from the location so we can calculate the allocated quantity
        Collection<InventoryAllocationSummary> inventoryAllocationSummaryList
                = inventoryAllocationSummaryMap.values();
        inventoryAllocationSummaryList.forEach(
                inventoryAllocationSummary -> {
                    List<Pick> picks = outbuondServiceRestemplateClient.getOpenPicksBySourceLocationIdAndItemId(
                            inventoryAllocationSummary.getWarehouseId(),
                            inventoryAllocationSummary.getLocationId(),
                            inventoryAllocationSummary.getItem().getId(),
                            inventoryAllocationSummary.getInventoryStatus().getId()
                    );
                    Long totalOpenQuantity =
                            picks.stream().mapToLong(
                                    pick -> pick.getQuantity() - pick.getPickedQuantity()
                            ).sum();
                    inventoryAllocationSummary.setAllocatedQuantity(
                            inventoryAllocationSummary.getAllocatedQuantity() + totalOpenQuantity
                    );
                    inventoryAllocationSummary.setAvailableQuantity(
                            inventoryAllocationSummary.getTotalQuantity() - totalOpenQuantity
                    );
                }
        );
        return inventoryAllocationSummaryList;
         **/
    }

}

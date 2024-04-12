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
import com.garyzhangscm.cwms.inventory.clients.OutbuondServiceRestemplateClient;
import com.garyzhangscm.cwms.inventory.clients.WarehouseLayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.inventory.model.*;
import com.garyzhangscm.cwms.inventory.repository.InventoryStatusRepository;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class WarehouseInventorySummaryService {
    private static final Logger logger = LoggerFactory.getLogger(WarehouseInventorySummaryService.class);

    @Autowired
    private WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient;
    @Autowired
    private CommonServiceRestemplateClient commonServiceRestemplateClient;
    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private OutbuondServiceRestemplateClient outbuondServiceRestemplateClient;


    /**
     *
     * @param warehouseId
     * @return
     */
    public List<WarehouseInventorySummary> getWarehouseInventorySummaries(Long warehouseId, String itemNameList,
                                                                          ClientRestriction clientRestriction) {
        List<Inventory> inventories = inventoryService.findAll(warehouseId,
                null,
                null,
                itemNameList,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                clientRestriction, false,
                null);

        Warehouse warehouse = warehouseLayoutServiceRestemplateClient.getWarehouseById(warehouseId);
        List<Client> clients = commonServiceRestemplateClient.getAllClients(warehouseId);
        Map<Long, String> clientNameMap = new HashMap<>();
        clients.forEach(
                client -> clientNameMap.put(client.getId(), client.getName())
        );

        // key:  clientName-itemName-itemPackageTypeName-inventoryStatusName-color-productSize-style
        // value: WarehouseInventorySummary
        Map<String, WarehouseInventorySummary> warehouseInventorySummaryMap = new HashMap<>();

        logger.debug("Get {} inventories record", inventories.size());

        for (Inventory inventory : inventories) {
            String key = new StringBuilder()
                    .append(Objects.isNull(inventory.getClientId()) ? "" :
                            clientNameMap.containsKey(inventory.getClientId()) ? clientNameMap.get(inventory.getClientId()) : "").append("-")
                    .append(inventory.getItem().getName()).append("-")
                    .append(inventory.getItemPackageType().getName()).append("-")
                    .append(inventory.getInventoryStatus().getName()).append("-")
                    .append(Strings.isBlank(inventory.getColor()) ? "" : inventory.getColor()).append("-")
                    .append(Strings.isBlank(inventory.getProductSize()) ? "" : inventory.getProductSize()).append("-")
                    .append(Strings.isBlank(inventory.getStyle()) ? "" : inventory.getStyle()).append("-")
                    .toString();
            logger.debug("start to process inventory summary with key: {}", key);
            WarehouseInventorySummary warehouseInventorySummary =
                    warehouseInventorySummaryMap.getOrDefault(key,
                            new WarehouseInventorySummary(
                                    warehouse.getCompany().getCode(),
                                    warehouse.getName(),
                                    Objects.isNull(inventory.getClientId()) ? "" :
                                            clientNameMap.containsKey(inventory.getClientId()) ? clientNameMap.get(inventory.getClientId()) : "",
                                    inventory.getItem().getName(),
                                    inventory.getItemPackageType().getName(),
                                    inventory.getInventoryStatus().getName(),
                                    Strings.isBlank(inventory.getColor()) ? "" : inventory.getColor(),
                                    Strings.isBlank(inventory.getProductSize()) ? "" : inventory.getProductSize(),
                                    Strings.isBlank(inventory.getStyle()) ? "" : inventory.getStyle(),
                                    Strings.isBlank(inventory.getAttribute1()) ? "" : inventory.getAttribute1(),
                                    Strings.isBlank(inventory.getAttribute2()) ? "" : inventory.getAttribute2(),
                                    Strings.isBlank(inventory.getAttribute3()) ? "" : inventory.getAttribute3(),
                                    Strings.isBlank(inventory.getAttribute4()) ? "" : inventory.getAttribute4(),
                                    Strings.isBlank(inventory.getAttribute5()) ? "" : inventory.getAttribute5(),
                                    0l
                            ));
            if (warehouseInventorySummary.getTotalQuantity() == 0) {
                // this is a new inventory summary, let's get the quantity in order / work  order / picks
                warehouseInventorySummary.setQuantityInExactMatchedOrder(
                        getQuantityInOrder(
                                warehouseId, inventory.getClientId(), inventory.getItem().getId(),
                                inventory.getInventoryStatus().getId(), inventory.getColor(),
                                inventory.getProductSize(), inventory.getStyle(),
                                inventory.getAttribute1(),
                                inventory.getAttribute2(),
                                inventory.getAttribute3(),
                                inventory.getAttribute4(),
                                inventory.getAttribute5(),
                                true
                        )
                );
                warehouseInventorySummary.setQuantityInMatchedOrder(
                        getQuantityInOrder(
                                warehouseId, inventory.getClientId(), inventory.getItem().getId(),
                                inventory.getInventoryStatus().getId(), inventory.getColor(),
                                inventory.getProductSize(), inventory.getStyle(),
                                inventory.getAttribute1(),
                                inventory.getAttribute2(),
                                inventory.getAttribute3(),
                                inventory.getAttribute4(),
                                inventory.getAttribute5(),
                                false
                        )
                );
                warehouseInventorySummary.setQuantityInExactMatchedOrderPick(
                        getQuantityInOrderPick(
                                warehouseId, inventory.getClientId(), inventory.getItem().getId(),
                                inventory.getInventoryStatus().getId(), inventory.getColor(),
                                inventory.getProductSize(), inventory.getStyle(),
                                true
                        )
                );
                warehouseInventorySummary.setQuantityInMatchedOrderPick(
                        getQuantityInOrderPick(
                                warehouseId, inventory.getClientId(), inventory.getItem().getId(),
                                inventory.getInventoryStatus().getId(), inventory.getColor(),
                                inventory.getProductSize(), inventory.getStyle(),
                                false
                        )
                );
            }

            warehouseInventorySummary.addQuantity(inventory.getQuantity());
            warehouseInventorySummaryMap.put(key, warehouseInventorySummary);


        }

        logger.debug("==========    warehouse inventory summary ===============");
        logger.debug("size: {}", warehouseInventorySummaryMap.size());
        warehouseInventorySummaryMap.entrySet().forEach(
                entry -> {
                    logger.debug(">> key: {}", entry.getKey());
                    logger.debug(">> value: {}", entry.getValue());
                }
        );
        return new ArrayList<>(warehouseInventorySummaryMap.values());

    }

    private long getQuantityInOrder(Long warehouseId,
                                    Long clientId,
                                    Long itemId,
                                    Long inventoryStatusId,
                                    String color,
                                    String productSize,
                                    String style,
                                    String inventoryAttribute1,
                                    String inventoryAttribute2,
                                    String inventoryAttribute3,
                                    String inventoryAttribute4,
                                    String inventoryAttribute5,
                                    boolean exactMatch) {
        return outbuondServiceRestemplateClient.getQuantityInOrder(
                warehouseId, clientId, itemId,
                inventoryStatusId, color, productSize, style,
                inventoryAttribute1, inventoryAttribute2, inventoryAttribute3, inventoryAttribute4, inventoryAttribute5,
                exactMatch
        );
    }

    private long getQuantityInOrderPick(Long warehouseId,
                                    Long clientId,
                                    Long itemId,
                                    Long inventoryStatusId,
                                    String color,
                                    String productSize,
                                    String style,
                                    boolean exactMatch) {
        return outbuondServiceRestemplateClient.getQuantityInOrderPick(
                warehouseId, clientId, itemId,
                inventoryStatusId, color, productSize, style, exactMatch
        );
    }
}

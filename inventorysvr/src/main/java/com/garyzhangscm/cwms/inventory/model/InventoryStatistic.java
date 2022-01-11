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

package com.garyzhangscm.cwms.inventory.model;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.Serializable;

public class InventoryStatistic  implements Serializable {


    private Long itemId;
    private Item item;


    private Long quantity;

    private Long inventoryStatusId;
    private InventoryStatus inventoryStatus;

    private Long totalOrderQuantity;
    private Long totalAllocatedQuantity;
    // open order quantity = order quantity - allocated quantity
    private Long totalOpenOrderQuantity;

    private Long totalInboundQuantity;
    private Long totalReceivedQuantity;
    // open inbound quantity = inbound quantity - received quantity
    private Long totalOpenInboundQuantity;

    @Override
    public String toString() {
        try {
            return new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(this);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return null;
    }

    public Long getItemId() {
        return itemId;
    }

    public void setItemId(Long itemId) {
        this.itemId = itemId;
    }

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public Long getQuantity() {
        return quantity;
    }

    public void setQuantity(Long quantity) {
        this.quantity = quantity;
    }

    public Long getInventoryStatusId() {
        return inventoryStatusId;
    }

    public void setInventoryStatusId(Long inventoryStatusId) {
        this.inventoryStatusId = inventoryStatusId;
    }

    public InventoryStatus getInventoryStatus() {
        return inventoryStatus;
    }

    public void setInventoryStatus(InventoryStatus inventoryStatus) {
        this.inventoryStatus = inventoryStatus;
    }

    public Long getTotalOrderQuantity() {
        return totalOrderQuantity;
    }

    public void setTotalOrderQuantity(Long totalOrderQuantity) {
        this.totalOrderQuantity = totalOrderQuantity;
    }

    public Long getTotalAllocatedQuantity() {
        return totalAllocatedQuantity;
    }

    public void setTotalAllocatedQuantity(Long totalAllocatedQuantity) {
        this.totalAllocatedQuantity = totalAllocatedQuantity;
    }

    public Long getTotalOpenOrderQuantity() {
        return totalOpenOrderQuantity;
    }

    public void setTotalOpenOrderQuantity(Long totalOpenOrderQuantity) {
        this.totalOpenOrderQuantity = totalOpenOrderQuantity;
    }

    public Long getTotalInboundQuantity() {
        return totalInboundQuantity;
    }

    public void setTotalInboundQuantity(Long totalInboundQuantity) {
        this.totalInboundQuantity = totalInboundQuantity;
    }

    public Long getTotalReceivedQuantity() {
        return totalReceivedQuantity;
    }

    public void setTotalReceivedQuantity(Long totalReceivedQuantity) {
        this.totalReceivedQuantity = totalReceivedQuantity;
    }

    public Long getTotalOpenInboundQuantity() {
        return totalOpenInboundQuantity;
    }

    public void setTotalOpenInboundQuantity(Long totalOpenInboundQuantity) {
        this.totalOpenInboundQuantity = totalOpenInboundQuantity;
    }
}

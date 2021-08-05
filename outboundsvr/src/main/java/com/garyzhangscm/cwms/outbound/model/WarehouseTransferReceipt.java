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

package com.garyzhangscm.cwms.outbound.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.Serializable;
import java.util.Map;

public class WarehouseTransferReceipt implements Serializable {

    // destination warehouse
    private Long sourceWarehouseId;
    private Long destinationWarehouseId;

    // item id and shipped quantity
    Map<Long, Long> shippedItem;

    private String receiptNumber;
    private String orderNumber;

    public WarehouseTransferReceipt() {}

    public WarehouseTransferReceipt(Long sourceWarehouseId, Long destinationWarehouseId, Map<Long, Long> shippedItem, String receiptNumber, String orderNumber) {
        this.sourceWarehouseId = sourceWarehouseId;
        this.destinationWarehouseId = destinationWarehouseId;
        this.shippedItem = shippedItem;
        this.receiptNumber = receiptNumber;
        this.orderNumber = orderNumber;
    }

    @Override
    public String toString() {
        try {
            return new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(this);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return null;
    }

    public Long getSourceWarehouseId() {
        return sourceWarehouseId;
    }

    public void setSourceWarehouseId(Long sourceWarehouseId) {
        this.sourceWarehouseId = sourceWarehouseId;
    }

    public Long getDestinationWarehouseId() {
        return destinationWarehouseId;
    }

    public void setDestinationWarehouseId(Long destinationWarehouseId) {
        this.destinationWarehouseId = destinationWarehouseId;
    }

    public Map<Long, Long> getShippedItem() {
        return shippedItem;
    }

    public void setShippedItem(Map<Long, Long> shippedItem) {
        this.shippedItem = shippedItem;
    }

    public String getReceiptNumber() {
        return receiptNumber;
    }

    public void setReceiptNumber(String receiptNumber) {
        this.receiptNumber = receiptNumber;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }
}

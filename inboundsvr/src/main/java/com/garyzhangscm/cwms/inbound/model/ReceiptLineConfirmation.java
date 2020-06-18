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

package com.garyzhangscm.cwms.inbound.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;


public class ReceiptLineConfirmation implements Serializable {


    private String number;

    private Long warehouseId;
    private String warehouseName;

    private Long itemId;
    private String itemName;

    private Long expectedQuantity;

    private Long receivedQuantity;

    @JsonIgnore
    private ReceiptConfirmation receipt;

    private Long overReceivingQuantity;
    private Double overReceivingPercent;

    public ReceiptLineConfirmation() {

    }

    public ReceiptLineConfirmation(ReceiptLine receiptLine) {
        setNumber(receiptLine.getNumber());

        setWarehouseId(receiptLine.getWarehouseId());
        if (Objects.nonNull(receiptLine.getWarehouse())) {
            setWarehouseName(receiptLine.getWarehouse().getName());
        }

        setItemId(receiptLine.getItemId());
        if (Objects.nonNull(receiptLine.getItem())) {
            setItemName(receiptLine.getItem().getName());
        }

        setExpectedQuantity(receiptLine.getExpectedQuantity());

        setReceivedQuantity(receiptLine.getReceivedQuantity());


        setOverReceivingQuantity(receiptLine.getOverReceivingQuantity());
        setOverReceivingPercent(receiptLine.getOverReceivingPercent());


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

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public Long getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(Long warehouseId) {
        this.warehouseId = warehouseId;
    }

    public String getWarehouseName() {
        return warehouseName;
    }

    public void setWarehouseName(String warehouseName) {
        this.warehouseName = warehouseName;
    }

    public Long getItemId() {
        return itemId;
    }

    public void setItemId(Long itemId) {
        this.itemId = itemId;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public Long getExpectedQuantity() {
        return expectedQuantity;
    }

    public void setExpectedQuantity(Long expectedQuantity) {
        this.expectedQuantity = expectedQuantity;
    }

    public Long getReceivedQuantity() {
        return receivedQuantity;
    }

    public void setReceivedQuantity(Long receivedQuantity) {
        this.receivedQuantity = receivedQuantity;
    }

    public ReceiptConfirmation getReceipt() {
        return receipt;
    }

    public void setReceipt(ReceiptConfirmation receipt) {
        this.receipt = receipt;
    }

    public Long getOverReceivingQuantity() {
        return overReceivingQuantity;
    }

    public void setOverReceivingQuantity(Long overReceivingQuantity) {
        this.overReceivingQuantity = overReceivingQuantity;
    }

    public Double getOverReceivingPercent() {
        return overReceivingPercent;
    }

    public void setOverReceivingPercent(Double overReceivingPercent) {
        this.overReceivingPercent = overReceivingPercent;
    }

}

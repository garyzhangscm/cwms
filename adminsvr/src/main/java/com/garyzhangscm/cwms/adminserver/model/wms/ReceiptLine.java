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

package com.garyzhangscm.cwms.adminserver.model.wms;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;



@JsonIgnoreProperties(ignoreUnknown = true)
public class ReceiptLine {

    private Long id;

    private String number;

    private Long warehouseId;
    private String warehouseName;

    private Warehouse warehouse;

    private Long itemId;
    private String itemName;

    private Item item;

    private Long expectedQuantity;


    private Long receivedQuantity;

    @JsonIgnore
    private  Receipt receipt;


    private Long overReceivingQuantity;
    private Double overReceivingPercent;

    public ReceiptLine(){}

    public ReceiptLine(Warehouse warehouse, Receipt receipt, String number,
                       Item item, Long expectedQuantity, Long receivedQuantity,
                       Long overReceivingQuantity, Double overReceivingPercent) {
        this.number = number;

        this.warehouse = warehouse;
        this.warehouseId = warehouse.getId();
        this.warehouseName = warehouse.getName();

        this.itemId = item.getId();
        this.itemName = item.getName();
        this.item = item;

        this.expectedQuantity = expectedQuantity;
        this.receivedQuantity = receivedQuantity;
        this.overReceivingQuantity = overReceivingQuantity;
        this.overReceivingPercent = overReceivingPercent;

        this.receipt = receipt;

    }

    public ReceiptLine(Warehouse warehouse, Receipt receipt, String number,
                       Item item, Long expectedQuantity) {
        this(warehouse, receipt, number, item, expectedQuantity,
                0L, 0L, 0.0);

    }

    public ReceiptLine(Warehouse warehouse, Receipt receipt, String number,
                       Item item, Long expectedQuantity,
                       Long overReceivingQuantity, Double overReceivingPercent) {
        this(warehouse, receipt, number, item, expectedQuantity,
                0L, overReceivingQuantity, overReceivingPercent);

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

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
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

    public Receipt getReceipt() {
        return receipt;
    }

    public void setReceipt(Receipt receipt) {
        this.receipt = receipt;
    }

    public Long getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(Long warehouseId) {
        this.warehouseId = warehouseId;
    }

    public Warehouse getWarehouse() {
        return warehouse;
    }

    public void setWarehouse(Warehouse warehouse) {
        this.warehouse = warehouse;
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

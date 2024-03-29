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

package com.garyzhangscm.cwms.inbound.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.util.Strings;

public class ReceiptLineCSVWrapper {
    private String client;
    private String supplier;
    private String receipt;
    private String line;
    private String item;
    private String inventoryStatus;
    private Long expectedQuantity;
    private String unitOfMeasure;

    private Long overReceivingQuantity;
    private Double overReceivingPercent;

    private String allowUnexpectedItem;

    public ReceiptLineCSVWrapper trim() {

        client = Strings.isBlank(client) ? "" : client.trim();
        supplier = Strings.isBlank(supplier) ? "" : supplier.trim();
        receipt = Strings.isBlank(receipt) ? "" : receipt.trim();
        line = Strings.isBlank(line) ? "" : line.trim();
        item = Strings.isBlank(item) ? "" : item.trim();
        inventoryStatus = Strings.isBlank(inventoryStatus) ? "" : inventoryStatus.trim();
        unitOfMeasure = Strings.isBlank(unitOfMeasure) ? "" : unitOfMeasure.trim();

        allowUnexpectedItem = Strings.isBlank(allowUnexpectedItem) ? "" : allowUnexpectedItem.trim();

        return this;
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

    public String getReceipt() {
        return receipt;
    }

    public void setReceipt(String receipt) {
        this.receipt = receipt;
    }


    public String getItem() {
        return item;
    }

    public void setItem(String item) {
        this.item = item;
    }

    public Long getExpectedQuantity() {
        return expectedQuantity;
    }

    public void setExpectedQuantity(Long expectedQuantity) {
        this.expectedQuantity = expectedQuantity;
    }

    public String getInventoryStatus() {
        return inventoryStatus;
    }

    public void setInventoryStatus(String inventoryStatus) {
        this.inventoryStatus = inventoryStatus;
    }

    public Long getOverReceivingQuantity() {
        return overReceivingQuantity;
    }

    public void setOverReceivingQuantity(Long overReceivingQuantity) {
        this.overReceivingQuantity = overReceivingQuantity;
    }

    public String getUnitOfMeasure() {
        return unitOfMeasure;
    }

    public void setUnitOfMeasure(String unitOfMeasure) {
        this.unitOfMeasure = unitOfMeasure;
    }

    public String getAllowUnexpectedItem() {
        return allowUnexpectedItem;
    }

    public void setAllowUnexpectedItem(String allowUnexpectedItem) {
        this.allowUnexpectedItem = allowUnexpectedItem;
    }

    public Double getOverReceivingPercent() {
        return overReceivingPercent;
    }

    public void setOverReceivingPercent(Double overReceivingPercent) {
        this.overReceivingPercent = overReceivingPercent;
    }

    public String getClient() {
        return client;
    }

    public void setClient(String client) {
        this.client = client;
    }

    public String getSupplier() {
        return supplier;
    }

    public void setSupplier(String supplier) {
        this.supplier = supplier;
    }

    public String getLine() {
        return line;
    }

    public void setLine(String line) {
        this.line = line;
    }
}

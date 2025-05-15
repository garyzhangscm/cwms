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

import javax.persistence.Column;

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

    private String color;
    private String productSize;
    private String style;

    private String itemPackageType;

    private Double cubicMeter;

    private String inventoryAttribute1;
    private String inventoryAttribute2;
    private String inventoryAttribute3;
    private String inventoryAttribute4;
    private String inventoryAttribute5;

    public ReceiptLineCSVWrapper trim() {

        client = Strings.isBlank(client) ? "" : client.trim();
        supplier = Strings.isBlank(supplier) ? "" : supplier.trim();
        receipt = Strings.isBlank(receipt) ? "" : receipt.trim();
        line = Strings.isBlank(line) ? "" : line.trim();
        item = Strings.isBlank(item) ? "" : item.trim();
        inventoryStatus = Strings.isBlank(inventoryStatus) ? "" : inventoryStatus.trim();
        unitOfMeasure = Strings.isBlank(unitOfMeasure) ? "" : unitOfMeasure.trim();

        allowUnexpectedItem = Strings.isBlank(allowUnexpectedItem) ? "" : allowUnexpectedItem.trim();

        color = Strings.isBlank(color) ? "" : color.trim();
        productSize = Strings.isBlank(productSize) ? "" : productSize.trim();
        style = Strings.isBlank(style) ? "" : style.trim();


        itemPackageType = Strings.isBlank(itemPackageType) ? "" : itemPackageType.trim();


        inventoryAttribute1 = Strings.isBlank(inventoryAttribute1) ? "" : inventoryAttribute1.trim();
        inventoryAttribute2 = Strings.isBlank(inventoryAttribute2) ? "" : inventoryAttribute2.trim();
        inventoryAttribute3 = Strings.isBlank(inventoryAttribute3) ? "" : inventoryAttribute3.trim();
        inventoryAttribute4 = Strings.isBlank(inventoryAttribute4) ? "" : inventoryAttribute4.trim();
        inventoryAttribute5 = Strings.isBlank(inventoryAttribute5) ? "" : inventoryAttribute5.trim();

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

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getProductSize() {
        return productSize;
    }

    public void setProductSize(String productSize) {
        this.productSize = productSize;
    }

    public String getStyle() {
        return style;
    }

    public void setStyle(String style) {
        this.style = style;
    }

    public String getItemPackageType() {
        return itemPackageType;
    }

    public void setItemPackageType(String itemPackageType) {
        this.itemPackageType = itemPackageType;
    }

    public Double getCubicMeter() {
        return cubicMeter;
    }

    public void setCubicMeter(Double cubicMeter) {
        this.cubicMeter = cubicMeter;
    }

    public String getInventoryAttribute1() {
        return inventoryAttribute1;
    }

    public void setInventoryAttribute1(String inventoryAttribute1) {
        this.inventoryAttribute1 = inventoryAttribute1;
    }

    public String getInventoryAttribute2() {
        return inventoryAttribute2;
    }

    public void setInventoryAttribute2(String inventoryAttribute2) {
        this.inventoryAttribute2 = inventoryAttribute2;
    }

    public String getInventoryAttribute3() {
        return inventoryAttribute3;
    }

    public void setInventoryAttribute3(String inventoryAttribute3) {
        this.inventoryAttribute3 = inventoryAttribute3;
    }

    public String getInventoryAttribute4() {
        return inventoryAttribute4;
    }

    public void setInventoryAttribute4(String inventoryAttribute4) {
        this.inventoryAttribute4 = inventoryAttribute4;
    }

    public String getInventoryAttribute5() {
        return inventoryAttribute5;
    }

    public void setInventoryAttribute5(String inventoryAttribute5) {
        this.inventoryAttribute5 = inventoryAttribute5;
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

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

public class WarehouseInventorySummary implements Serializable {

    private String companyCode;

    private String warehouseName;

    private String clientName;

    private String itemName;

    private String itemPackageTypeName;

    private String inventoryStatusName;


    private String color;

    private String productSize;

    private String style;

    private String attribute1;
    private String attribute2;
    private String attribute3;
    private String attribute4;
    private String attribute5;

    private Long totalQuantity = 0l;

    private Long quantityInExactMatchedOrder = 0l;
    private Long quantityInMatchedOrder = 0l;
    private Long quantityInExactMatchedOrderPick = 0l;
    private Long quantityInMatchedOrderPick = 0l;

    private Long quantityInExactMatchedWorkOrder = 0l;
    private Long quantityInMatchedWorkOrder = 0l;
    private Long quantityInExactMatchedWorkOrderPick = 0l;
    private Long quantityInMatchedWorkOrderPick = 0l;

    public WarehouseInventorySummary(){}

    public WarehouseInventorySummary(String companyCode, String warehouseName,
                                     String clientName, String itemName,
                                     String itemPackageTypeName, String inventoryStatusName,
                                     String color, String productSize, String style,
                                     String attribute1, String attribute2,String attribute3,
                                     String attribute4, String attribute5,
                                     Long totalQuantity) {
        this.companyCode = companyCode;
        this.warehouseName = warehouseName;
        this.clientName = clientName;
        this.itemName = itemName;
        this.itemPackageTypeName = itemPackageTypeName;
        this.inventoryStatusName = inventoryStatusName;
        this.color = color;
        this.productSize = productSize;
        this.style = style;

        this.attribute1 = attribute1;
        this.attribute2 = attribute2;
        this.attribute3 = attribute3;
        this.attribute4 = attribute4;
        this.attribute5 = attribute5;

        this.totalQuantity = totalQuantity;
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

    public void addQuantity(Long quantity) {
        this.totalQuantity += quantity;
    }

    public String getCompanyCode() {
        return companyCode;
    }

    public void setCompanyCode(String companyCode) {
        this.companyCode = companyCode;
    }

    public String getWarehouseName() {
        return warehouseName;
    }

    public void setWarehouseName(String warehouseName) {
        this.warehouseName = warehouseName;
    }

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public String getItemPackageTypeName() {
        return itemPackageTypeName;
    }

    public void setItemPackageTypeName(String itemPackageTypeName) {
        this.itemPackageTypeName = itemPackageTypeName;
    }

    public String getInventoryStatusName() {
        return inventoryStatusName;
    }

    public void setInventoryStatusName(String inventoryStatusName) {
        this.inventoryStatusName = inventoryStatusName;
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

    public String getAttribute1() {
        return attribute1;
    }

    public void setAttribute1(String attribute1) {
        this.attribute1 = attribute1;
    }

    public String getAttribute2() {
        return attribute2;
    }

    public void setAttribute2(String attribute2) {
        this.attribute2 = attribute2;
    }

    public String getAttribute3() {
        return attribute3;
    }

    public void setAttribute3(String attribute3) {
        this.attribute3 = attribute3;
    }

    public String getAttribute4() {
        return attribute4;
    }

    public void setAttribute4(String attribute4) {
        this.attribute4 = attribute4;
    }

    public String getAttribute5() {
        return attribute5;
    }

    public void setAttribute5(String attribute5) {
        this.attribute5 = attribute5;
    }

    public Long getTotalQuantity() {
        return totalQuantity;
    }

    public void setTotalQuantity(Long totalQuantity) {
        this.totalQuantity = totalQuantity;
    }


    public Long getQuantityInExactMatchedOrder() {
        return quantityInExactMatchedOrder;
    }

    public void setQuantityInExactMatchedOrder(Long quantityInExactMatchedOrder) {
        this.quantityInExactMatchedOrder = quantityInExactMatchedOrder;
    }

    public Long getQuantityInMatchedOrder() {
        return quantityInMatchedOrder;
    }

    public void setQuantityInMatchedOrder(Long quantityInMatchedOrder) {
        this.quantityInMatchedOrder = quantityInMatchedOrder;
    }

    public Long getQuantityInExactMatchedOrderPick() {
        return quantityInExactMatchedOrderPick;
    }

    public void setQuantityInExactMatchedOrderPick(Long quantityInExactMatchedOrderPick) {
        this.quantityInExactMatchedOrderPick = quantityInExactMatchedOrderPick;
    }

    public Long getQuantityInMatchedOrderPick() {
        return quantityInMatchedOrderPick;
    }

    public void setQuantityInMatchedOrderPick(Long quantityInMatchedOrderPick) {
        this.quantityInMatchedOrderPick = quantityInMatchedOrderPick;
    }

    public Long getQuantityInExactMatchedWorkOrder() {
        return quantityInExactMatchedWorkOrder;
    }

    public void setQuantityInExactMatchedWorkOrder(Long quantityInExactMatchedWorkOrder) {
        this.quantityInExactMatchedWorkOrder = quantityInExactMatchedWorkOrder;
    }

    public Long getQuantityInMatchedWorkOrder() {
        return quantityInMatchedWorkOrder;
    }

    public void setQuantityInMatchedWorkOrder(Long quantityInMatchedWorkOrder) {
        this.quantityInMatchedWorkOrder = quantityInMatchedWorkOrder;
    }

    public Long getQuantityInExactMatchedWorkOrderPick() {
        return quantityInExactMatchedWorkOrderPick;
    }

    public void setQuantityInExactMatchedWorkOrderPick(Long quantityInExactMatchedWorkOrderPick) {
        this.quantityInExactMatchedWorkOrderPick = quantityInExactMatchedWorkOrderPick;
    }

    public Long getQuantityInMatchedWorkOrderPick() {
        return quantityInMatchedWorkOrderPick;
    }

    public void setQuantityInMatchedWorkOrderPick(Long quantityInMatchedWorkOrderPick) {
        this.quantityInMatchedWorkOrderPick = quantityInMatchedWorkOrderPick;
    }
}

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
import org.apache.logging.log4j.util.Strings;


import java.io.Serializable;

public class InventoryCSVWrapper implements Serializable {

    private String lpn;

    private String location;

    private String item;

    private String itemPackageType;

    private Long quantity;
    private String unitOfMeasure;

    private String inventoryStatus;

    private String client;

    private String fifoDate;
    private String inWarehouseDatetime;

    private String color;
    private String productSize;
    private String style;

    private String attribute1;
    private String attribute2;
    private String attribute3;
    private String attribute4;
    private String attribute5;

    public InventoryCSVWrapper trim() {

        lpn = Strings.isBlank(lpn) ? "" : lpn.trim();

        location = Strings.isBlank(location) ? "" : location.trim();

        item = Strings.isBlank(item) ? "" : item.trim();

        itemPackageType = Strings.isBlank(itemPackageType) ? "" : itemPackageType.trim();

        unitOfMeasure = Strings.isBlank(unitOfMeasure) ? "" : unitOfMeasure.trim();

        inventoryStatus = Strings.isBlank(inventoryStatus) ? "" : inventoryStatus.trim();

        client = Strings.isBlank(client) ? "" : client.trim();

        fifoDate = Strings.isBlank(fifoDate) ? "" : fifoDate.trim();
        inWarehouseDatetime = Strings.isBlank(inWarehouseDatetime) ? "" : inWarehouseDatetime.trim();

        color = Strings.isBlank(color) ? "" : color.trim();
        productSize = Strings.isBlank(productSize) ? "" : productSize.trim();
        style = Strings.isBlank(style) ? "" : style.trim();

        attribute1 = Strings.isBlank(attribute1) ? "" : attribute1.trim();
        attribute2 = Strings.isBlank(attribute2) ? "" : attribute2.trim();
        attribute3 = Strings.isBlank(attribute3) ? "" : attribute3.trim();
        attribute4 = Strings.isBlank(attribute4) ? "" : attribute4.trim();
        attribute5 = Strings.isBlank(attribute5) ? "" : attribute5.trim();

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

    public String getLpn() {
        return lpn;
    }

    public void setLpn(String lpn) {
        this.lpn = lpn;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getItem() {
        return item;
    }

    public void setItem(String item) {
        this.item = item;
    }

    public String getItemPackageType() {
        return itemPackageType;
    }

    public void setItemPackageType(String itemPackageType) {
        this.itemPackageType = itemPackageType;
    }

    public Long getQuantity() {
        return quantity;
    }

    public void setQuantity(Long quantity) {
        this.quantity = quantity;
    }

    public String getInventoryStatus() {
        return inventoryStatus;
    }

    public void setInventoryStatus(String inventoryStatus) {
        this.inventoryStatus = inventoryStatus;
    }

    public String getFifoDate() {
        return fifoDate;
    }

    public void setFifoDate(String fifoDate) {
        this.fifoDate = fifoDate;
    }

    public String getUnitOfMeasure() {
        return unitOfMeasure;
    }

    public void setUnitOfMeasure(String unitOfMeasure) {
        this.unitOfMeasure = unitOfMeasure;
    }

    public String getClient() {
        return client;
    }

    public void setClient(String client) {
        this.client = client;
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

    public String getInWarehouseDatetime() {
        return inWarehouseDatetime;
    }

    public void setInWarehouseDatetime(String inWarehouseDatetime) {
        this.inWarehouseDatetime = inWarehouseDatetime;
    }
}

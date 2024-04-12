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

public class InventoryPutawayCSVWrapper implements Serializable {

    private String client;

    private String lpn;

    private String location;

    private String item;

    private String itemPackageType;

    private Long quantity;
    private String unitOfMeasure;

    private String inventoryStatus;

    private String color;
    private String productSize;
    private String style;

    private String inventoryAttribute1;
    private String inventoryAttribute2;
    private String inventoryAttribute3;
    private String inventoryAttribute4;
    private String inventoryAttribute5;

    private String destinationLocation;

    public InventoryPutawayCSVWrapper trim() {

        client = Strings.isBlank(client) ? "" :  client.trim();

        lpn = Strings.isBlank(lpn) ? "" :  lpn.trim();

        location = Strings.isBlank(location) ? "" :  location.trim();

        item = Strings.isBlank(item) ? "" :  item.trim();

        itemPackageType = Strings.isBlank(itemPackageType) ? "" :  itemPackageType.trim();

        unitOfMeasure = Strings.isBlank(unitOfMeasure) ? "" :  unitOfMeasure.trim();

        inventoryStatus = Strings.isBlank(inventoryStatus) ? "" :  inventoryStatus.trim();

        color = Strings.isBlank(color) ? "" :  color.trim();
        productSize = Strings.isBlank(productSize) ? "" :  productSize.trim();
        style = Strings.isBlank(style) ? "" :  style.trim();

        inventoryAttribute1 = Strings.isBlank(inventoryAttribute1) ? "" :  inventoryAttribute1.trim();
        inventoryAttribute2 = Strings.isBlank(inventoryAttribute2) ? "" :  inventoryAttribute2.trim();
        inventoryAttribute3 = Strings.isBlank(inventoryAttribute3) ? "" :  inventoryAttribute3.trim();
        inventoryAttribute4 = Strings.isBlank(inventoryAttribute4) ? "" :  inventoryAttribute4.trim();
        inventoryAttribute5 = Strings.isBlank(inventoryAttribute5) ? "" :  inventoryAttribute5.trim();

        destinationLocation = Strings.isBlank(destinationLocation) ? "" :  destinationLocation.trim();

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

    public String getClient() {
        return client;
    }

    public void setClient(String client) {
        this.client = client;
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

    public String getUnitOfMeasure() {
        return unitOfMeasure;
    }

    public void setUnitOfMeasure(String unitOfMeasure) {
        this.unitOfMeasure = unitOfMeasure;
    }

    public String getInventoryStatus() {
        return inventoryStatus;
    }

    public void setInventoryStatus(String inventoryStatus) {
        this.inventoryStatus = inventoryStatus;
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

    public String getDestinationLocation() {
        return destinationLocation;
    }

    public void setDestinationLocation(String destinationLocation) {
        this.destinationLocation = destinationLocation;
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
}

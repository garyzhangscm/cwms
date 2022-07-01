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

public class ItemUnitOfMeasureCSVWrapper implements Serializable {

    private String company;
    private String warehouse;

    private String client;
    private String item;

    private String itemPackageType;
    private String itemPackageTypeDescription;
    private Boolean defaultItemPackageType;

    private String unitOfMeasure;
    private Integer quantity;
    private Double weight;
    private Double length;
    private Double width;
    private Double height;


    private Boolean defaultForInboundReceiving;
    private Boolean defaultForWorkOrderReceiving;
    private Boolean trackingLpn;
    private Boolean caseFlag;


    @Override
    public String toString() {
        try {
            return new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(this);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return null;
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
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

    public String getUnitOfMeasure() {
        return unitOfMeasure;
    }

    public void setUnitOfMeasure(String unitOfMeasure) {
        this.unitOfMeasure = unitOfMeasure;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public Double getWeight() {
        return weight;
    }

    public void setWeight(Double weight) {
        this.weight = weight;
    }

    public Double getLength() {
        return length;
    }

    public void setLength(Double length) {
        this.length = length;
    }

    public Double getWidth() {
        return width;
    }

    public void setWidth(Double width) {
        this.width = width;
    }

    public Double getHeight() {
        return height;
    }

    public void setHeight(Double height) {
        this.height = height;
    }

    public String getClient() {
        return client;
    }

    public void setClient(String client) {
        this.client = client;
    }

    public String getWarehouse() {
        return warehouse;
    }

    public void setWarehouse(String warehouse) {
        this.warehouse = warehouse;
    }

    public String getItemPackageTypeDescription() {
        return itemPackageTypeDescription;
    }

    public void setItemPackageTypeDescription(String itemPackageTypeDescription) {
        this.itemPackageTypeDescription = itemPackageTypeDescription;
    }

    public Boolean getDefaultItemPackageType() {
        return defaultItemPackageType;
    }

    public void setDefaultItemPackageType(Boolean defaultItemPackageType) {
        this.defaultItemPackageType = defaultItemPackageType;
    }

    public Boolean getDefaultForInboundReceiving() {
        return defaultForInboundReceiving;
    }

    public void setDefaultForInboundReceiving(Boolean defaultForInboundReceiving) {
        this.defaultForInboundReceiving = defaultForInboundReceiving;
    }

    public Boolean getDefaultForWorkOrderReceiving() {
        return defaultForWorkOrderReceiving;
    }

    public void setDefaultForWorkOrderReceiving(Boolean defaultForWorkOrderReceiving) {
        this.defaultForWorkOrderReceiving = defaultForWorkOrderReceiving;
    }

    public Boolean getTrackingLpn() {
        return trackingLpn;
    }

    public void setTrackingLpn(Boolean trackingLpn) {
        this.trackingLpn = trackingLpn;
    }

    public Boolean getCaseFlag() {
        return caseFlag;
    }

    public void setCaseFlag(Boolean caseFlag) {
        this.caseFlag = caseFlag;
    }
}

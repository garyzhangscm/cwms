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


    private String client;
    private String item;
    private String itemDescription;
    private String itemFamily;
    private String trackingColorFlag;
    private String defaultColor;
    private String trackingProductSizeFlag;
    private String defaultProductSize;
    private String trackingStyleFlag;
    private String defaultStyle;

    private String itemPackageType;
    private String itemPackageTypeDescription;
    private String defaultItemPackageType;

    private String unitOfMeasure;
    private Integer quantity;
    private Double weight;
    private Double length;
    private Double width;
    private Double height;


    private String defaultForInboundReceiving;
    private String defaultForWorkOrderReceiving;
    private String trackingLpn;
    private String caseFlag;
    private String defaultForDisplay;


    @Override
    public String toString() {
        try {
            return new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(this);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return null;
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


    public String getItemPackageTypeDescription() {
        return itemPackageTypeDescription;
    }

    public void setItemPackageTypeDescription(String itemPackageTypeDescription) {
        this.itemPackageTypeDescription = itemPackageTypeDescription;
    }

    public String getItemFamily() {
        return itemFamily;
    }

    public void setItemFamily(String itemFamily) {
        this.itemFamily = itemFamily;
    }

    public String getDefaultItemPackageType() {
        return defaultItemPackageType;
    }

    public void setDefaultItemPackageType(String defaultItemPackageType) {
        this.defaultItemPackageType = defaultItemPackageType;
    }

    public String getDefaultForInboundReceiving() {
        return defaultForInboundReceiving;
    }

    public void setDefaultForInboundReceiving(String defaultForInboundReceiving) {
        this.defaultForInboundReceiving = defaultForInboundReceiving;
    }

    public String getDefaultForWorkOrderReceiving() {
        return defaultForWorkOrderReceiving;
    }

    public void setDefaultForWorkOrderReceiving(String defaultForWorkOrderReceiving) {
        this.defaultForWorkOrderReceiving = defaultForWorkOrderReceiving;
    }

    public String getTrackingLpn() {
        return trackingLpn;
    }

    public void setTrackingLpn(String trackingLpn) {
        this.trackingLpn = trackingLpn;
    }

    public String getCaseFlag() {
        return caseFlag;
    }

    public void setCaseFlag(String caseFlag) {
        this.caseFlag = caseFlag;
    }

    public String getItemDescription() {
        return itemDescription;
    }

    public void setItemDescription(String itemDescription) {
        this.itemDescription = itemDescription;
    }

    public String getTrackingColorFlag() {
        return trackingColorFlag;
    }

    public void setTrackingColorFlag(String trackingColorFlag) {
        this.trackingColorFlag = trackingColorFlag;
    }

    public String getDefaultColor() {
        return defaultColor;
    }

    public void setDefaultColor(String defaultColor) {
        this.defaultColor = defaultColor;
    }

    public String getTrackingProductSizeFlag() {
        return trackingProductSizeFlag;
    }

    public void setTrackingProductSizeFlag(String trackingProductSizeFlag) {
        this.trackingProductSizeFlag = trackingProductSizeFlag;
    }

    public String getDefaultProductSize() {
        return defaultProductSize;
    }

    public void setDefaultProductSize(String defaultProductSize) {
        this.defaultProductSize = defaultProductSize;
    }

    public String getTrackingStyleFlag() {
        return trackingStyleFlag;
    }

    public void setTrackingStyleFlag(String trackingStyleFlag) {
        this.trackingStyleFlag = trackingStyleFlag;
    }

    public String getDefaultStyle() {
        return defaultStyle;
    }

    public void setDefaultStyle(String defaultStyle) {
        this.defaultStyle = defaultStyle;
    }

    public String getDefaultForDisplay() {
        return defaultForDisplay;
    }

    public void setDefaultForDisplay(String defaultForDisplay) {
        this.defaultForDisplay = defaultForDisplay;
    }
}

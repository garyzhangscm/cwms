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

    private String trackingInventoryAttribute1Flag;
    private String defaultInventoryAttribute1;
    private String trackingInventoryAttribute2Flag;
    private String defaultInventoryAttribute2;
    private String trackingInventoryAttribute3Flag;
    private String defaultInventoryAttribute3;
    private String trackingInventoryAttribute4Flag;
    private String defaultInventoryAttribute4;
    private String trackingInventoryAttribute5Flag;
    private String defaultInventoryAttribute5;

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

    private Double receivingRateByUnit;

    private Double shippingRateByUnit;

    private Double handlingRateByUnit;

    public ItemUnitOfMeasureCSVWrapper trim() {

        client = Strings.isBlank(client) ? "" : client.trim();
        item = Strings.isBlank(item) ? "" : item.trim();
        itemDescription = Strings.isBlank(itemDescription) ? "" : itemDescription.trim();
        itemFamily = Strings.isBlank(itemFamily) ? "" : itemFamily.trim();
        trackingColorFlag = Strings.isBlank(trackingColorFlag) ? "" : trackingColorFlag.trim();
        defaultColor = Strings.isBlank(defaultColor) ? "" : defaultColor.trim();
        trackingProductSizeFlag = Strings.isBlank(trackingProductSizeFlag) ? "" : trackingProductSizeFlag.trim();
        defaultProductSize = Strings.isBlank(defaultProductSize) ? "" : defaultProductSize.trim();
        trackingStyleFlag = Strings.isBlank(trackingStyleFlag) ? "" : trackingStyleFlag.trim();
        defaultStyle = Strings.isBlank(defaultStyle) ? "" : defaultStyle.trim();

        trackingInventoryAttribute1Flag = Strings.isBlank(trackingInventoryAttribute1Flag) ? "" : trackingInventoryAttribute1Flag.trim();
        defaultInventoryAttribute1 = Strings.isBlank(defaultInventoryAttribute1) ? "" : defaultInventoryAttribute1.trim();

        itemPackageType = Strings.isBlank(itemPackageType) ? "" : itemPackageType.trim();
        itemPackageTypeDescription = Strings.isBlank(itemPackageTypeDescription) ? "" : itemPackageTypeDescription.trim();
        defaultItemPackageType = Strings.isBlank(defaultItemPackageType) ? "" : defaultItemPackageType.trim();

        unitOfMeasure = Strings.isBlank(unitOfMeasure) ? "" : unitOfMeasure.trim();

        defaultForInboundReceiving = Strings.isBlank(defaultForInboundReceiving) ? "" : defaultForInboundReceiving.trim();
        defaultForWorkOrderReceiving = Strings.isBlank(defaultForWorkOrderReceiving) ? "" : defaultForWorkOrderReceiving.trim();
        trackingLpn = Strings.isBlank(trackingLpn) ? "" : trackingLpn.trim();
        caseFlag = Strings.isBlank(caseFlag) ? "" : caseFlag.trim();
        defaultForDisplay = Strings.isBlank(defaultForDisplay) ? "" : defaultForDisplay.trim();

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

    public String getTrackingInventoryAttribute1Flag() {
        return trackingInventoryAttribute1Flag;
    }

    public void setTrackingInventoryAttribute1Flag(String trackingInventoryAttribute1Flag) {
        this.trackingInventoryAttribute1Flag = trackingInventoryAttribute1Flag;
    }

    public String getDefaultInventoryAttribute1() {
        return defaultInventoryAttribute1;
    }

    public void setDefaultInventoryAttribute1(String defaultInventoryAttribute1) {
        this.defaultInventoryAttribute1 = defaultInventoryAttribute1;
    }

    public String getTrackingInventoryAttribute2Flag() {
        return trackingInventoryAttribute2Flag;
    }

    public void setTrackingInventoryAttribute2Flag(String trackingInventoryAttribute2Flag) {
        this.trackingInventoryAttribute2Flag = trackingInventoryAttribute2Flag;
    }

    public String getDefaultInventoryAttribute2() {
        return defaultInventoryAttribute2;
    }

    public void setDefaultInventoryAttribute2(String defaultInventoryAttribute2) {
        this.defaultInventoryAttribute2 = defaultInventoryAttribute2;
    }

    public String getTrackingInventoryAttribute3Flag() {
        return trackingInventoryAttribute3Flag;
    }

    public void setTrackingInventoryAttribute3Flag(String trackingInventoryAttribute3Flag) {
        this.trackingInventoryAttribute3Flag = trackingInventoryAttribute3Flag;
    }

    public String getDefaultInventoryAttribute3() {
        return defaultInventoryAttribute3;
    }

    public void setDefaultInventoryAttribute3(String defaultInventoryAttribute3) {
        this.defaultInventoryAttribute3 = defaultInventoryAttribute3;
    }

    public String getTrackingInventoryAttribute4Flag() {
        return trackingInventoryAttribute4Flag;
    }

    public void setTrackingInventoryAttribute4Flag(String trackingInventoryAttribute4Flag) {
        this.trackingInventoryAttribute4Flag = trackingInventoryAttribute4Flag;
    }

    public String getDefaultInventoryAttribute4() {
        return defaultInventoryAttribute4;
    }

    public void setDefaultInventoryAttribute4(String defaultInventoryAttribute4) {
        this.defaultInventoryAttribute4 = defaultInventoryAttribute4;
    }

    public String getTrackingInventoryAttribute5Flag() {
        return trackingInventoryAttribute5Flag;
    }

    public void setTrackingInventoryAttribute5Flag(String trackingInventoryAttribute5Flag) {
        this.trackingInventoryAttribute5Flag = trackingInventoryAttribute5Flag;
    }

    public String getDefaultInventoryAttribute5() {
        return defaultInventoryAttribute5;
    }

    public void setDefaultInventoryAttribute5(String defaultInventoryAttribute5) {
        this.defaultInventoryAttribute5 = defaultInventoryAttribute5;
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

    public Double getReceivingRateByUnit() {
        return receivingRateByUnit;
    }

    public void setReceivingRateByUnit(Double receivingRateByUnit) {
        this.receivingRateByUnit = receivingRateByUnit;
    }

    public Double getShippingRateByUnit() {
        return shippingRateByUnit;
    }

    public void setShippingRateByUnit(Double shippingRateByUnit) {
        this.shippingRateByUnit = shippingRateByUnit;
    }

    public Double getHandlingRateByUnit() {
        return handlingRateByUnit;
    }

    public void setHandlingRateByUnit(Double handlingRateByUnit) {
        this.handlingRateByUnit = handlingRateByUnit;
    }
}

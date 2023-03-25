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

public class ItemPackageTypeCSVWrapper implements Serializable {

    private String name;
    private String description;

    private String item;
    private String itemDescription;
    private String itemFamily;
    private String trackingColorFlag;
    private String defaultColor;
    private String trackingProductSizeFlag;
    private String defaultProductSize;
    private String trackingStyleFlag;
    private String defaultStyle;


    private String client;
    private String supplier;

    private Double receivingRateByUnit;

    private Double shippingRateByUnit;

    private Double handlingRateByUnit;

    @Override
    public String toString() {
        try {
            return new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(this);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return null;
    }


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getItem() {
        return item;
    }

    public void setItem(String item) {
        this.item = item;
    }

    public String getClient() {
        return client;
    }

    public void setClient(String client) {
        this.client = client;
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

    public String getSupplier() {
        return supplier;
    }

    public void setSupplier(String supplier) {
        this.supplier = supplier;
    }


    public String getItemDescription() {
        return itemDescription;
    }

    public void setItemDescription(String itemDescription) {
        this.itemDescription = itemDescription;
    }

    public String getItemFamily() {
        return itemFamily;
    }

    public void setItemFamily(String itemFamily) {
        this.itemFamily = itemFamily;
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

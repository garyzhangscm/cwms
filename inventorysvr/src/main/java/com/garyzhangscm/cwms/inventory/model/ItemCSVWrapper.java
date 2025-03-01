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
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.persistence.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class ItemCSVWrapper implements Serializable {
    private String name;

    private String description;

    private String client;
    private String itemFamily;
    private Double unitCost;
    private Boolean allowCartonization;

    private Boolean allowAllocationByLPN;
    private String allocationRoundUpStrategyType;
    private Double allocationRoundUpStrategyValue;



    private boolean trackingVolumeFlag;
    private boolean trackingLotNumberFlag;
    private boolean trackingManufactureDateFlag;
    private Integer shelfLifeDays;
    private boolean trackingExpirationDateFlag;

    private boolean trackingColorFlag;
    private String defaultColor;

    private boolean trackingProductSizeFlag ;
    private String defaultProductSize;

    private boolean trackingStyleFlag;
    private String defaultStyle;

    private boolean trackingInventoryAttribute1Flag = false;
    private String defaultInventoryAttribute1;

    private boolean trackingInventoryAttribute2Flag = false;
    private String defaultInventoryAttribute2;

    private boolean trackingInventoryAttribute3Flag = false;
    private String defaultInventoryAttribute3;

    private boolean trackingInventoryAttribute4Flag = false;
    private String defaultInventoryAttribute4;

    private boolean trackingInventoryAttribute5Flag = false;
    private String defaultInventoryAttribute5;


    private String imageUrl;

    private String thumbnailUrl;

    private Double receivingRateByUnit;

    private Double shippingRateByUnit;

    private Double handlingRateByUnit;

    public ItemCSVWrapper trim() {

        name = Strings.isBlank(name) ? "" : name.trim();

        description = Strings.isBlank(description) ? "" : description.trim();

        client = Strings.isBlank(client) ? "" : client.trim();
        itemFamily = Strings.isBlank(itemFamily) ? "" : itemFamily.trim();

        allocationRoundUpStrategyType = Strings.isBlank(allocationRoundUpStrategyType) ? "" : allocationRoundUpStrategyType.trim();

        defaultColor = Strings.isBlank(defaultColor) ? "" : defaultColor.trim();

        defaultProductSize = Strings.isBlank(defaultProductSize) ? "" : defaultProductSize.trim();

        defaultStyle = Strings.isBlank(defaultStyle) ? "" : defaultStyle.trim();

        imageUrl = Strings.isBlank(imageUrl) ? "" : imageUrl.trim();

        thumbnailUrl = Strings.isBlank(thumbnailUrl) ? "" : thumbnailUrl.trim();

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

    public String getClient() {
        return client;
    }

    public void setClient(String client) {
        this.client = client;
    }

    public String getItemFamily() {
        return itemFamily;
    }

    public void setItemFamily(String itemFamily) {
        this.itemFamily = itemFamily;
    }

    public Double getUnitCost() {
        return unitCost;
    }

    public void setUnitCost(Double unitCost) {
        this.unitCost = unitCost;
    }


    public Boolean getAllowCartonization() {
        return allowCartonization;
    }

    public void setAllowCartonization(Boolean allowCartonization) {
        this.allowCartonization = allowCartonization;
    }

    public Boolean getAllowAllocationByLPN() {
        return allowAllocationByLPN;
    }

    public void setAllowAllocationByLPN(Boolean allowAllocationByLPN) {
        this.allowAllocationByLPN = allowAllocationByLPN;
    }

    public boolean isTrackingColorFlag() {
        return trackingColorFlag;
    }

    public void setTrackingColorFlag(boolean trackingColorFlag) {
        this.trackingColorFlag = trackingColorFlag;
    }

    public String getDefaultColor() {
        return defaultColor;
    }

    public void setDefaultColor(String defaultColor) {
        this.defaultColor = defaultColor;
    }

    public boolean isTrackingProductSizeFlag() {
        return trackingProductSizeFlag;
    }

    public void setTrackingProductSizeFlag(boolean trackingProductSizeFlag) {
        this.trackingProductSizeFlag = trackingProductSizeFlag;
    }

    public String getDefaultProductSize() {
        return defaultProductSize;
    }

    public void setDefaultProductSize(String defaultProductSize) {
        this.defaultProductSize = defaultProductSize;
    }

    public boolean isTrackingStyleFlag() {
        return trackingStyleFlag;
    }

    public void setTrackingStyleFlag(boolean trackingStyleFlag) {
        this.trackingStyleFlag = trackingStyleFlag;
    }

    public String getDefaultStyle() {
        return defaultStyle;
    }

    public void setDefaultStyle(String defaultStyle) {
        this.defaultStyle = defaultStyle;
    }

    public String getAllocationRoundUpStrategyType() {
        return allocationRoundUpStrategyType;
    }

    public void setAllocationRoundUpStrategyType(String allocationRoundUpStrategyType) {
        this.allocationRoundUpStrategyType = allocationRoundUpStrategyType;
    }

    public Double getAllocationRoundUpStrategyValue() {
        return allocationRoundUpStrategyValue;
    }

    public void setAllocationRoundUpStrategyValue(Double allocationRoundUpStrategyValue) {
        this.allocationRoundUpStrategyValue = allocationRoundUpStrategyValue;
    }

    public boolean isTrackingVolumeFlag() {
        return trackingVolumeFlag;
    }

    public void setTrackingVolumeFlag(boolean trackingVolumeFlag) {
        this.trackingVolumeFlag = trackingVolumeFlag;
    }

    public boolean isTrackingLotNumberFlag() {
        return trackingLotNumberFlag;
    }

    public void setTrackingLotNumberFlag(boolean trackingLotNumberFlag) {
        this.trackingLotNumberFlag = trackingLotNumberFlag;
    }

    public boolean isTrackingManufactureDateFlag() {
        return trackingManufactureDateFlag;
    }

    public void setTrackingManufactureDateFlag(boolean trackingManufactureDateFlag) {
        this.trackingManufactureDateFlag = trackingManufactureDateFlag;
    }

    public Integer getShelfLifeDays() {
        return shelfLifeDays;
    }

    public void setShelfLifeDays(Integer shelfLifeDays) {
        this.shelfLifeDays = shelfLifeDays;
    }

    public boolean isTrackingExpirationDateFlag() {
        return trackingExpirationDateFlag;
    }

    public void setTrackingExpirationDateFlag(boolean trackingExpirationDateFlag) {
        this.trackingExpirationDateFlag = trackingExpirationDateFlag;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
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


    public boolean isTrackingInventoryAttribute1Flag() {
        return trackingInventoryAttribute1Flag;
    }

    public void setTrackingInventoryAttribute1Flag(boolean trackingInventoryAttribute1Flag) {
        this.trackingInventoryAttribute1Flag = trackingInventoryAttribute1Flag;
    }

    public String getDefaultInventoryAttribute1() {
        return defaultInventoryAttribute1;
    }

    public void setDefaultInventoryAttribute1(String defaultInventoryAttribute1) {
        this.defaultInventoryAttribute1 = defaultInventoryAttribute1;
    }

    public boolean isTrackingInventoryAttribute2Flag() {
        return trackingInventoryAttribute2Flag;
    }

    public void setTrackingInventoryAttribute2Flag(boolean trackingInventoryAttribute2Flag) {
        this.trackingInventoryAttribute2Flag = trackingInventoryAttribute2Flag;
    }

    public String getDefaultInventoryAttribute2() {
        return defaultInventoryAttribute2;
    }

    public void setDefaultInventoryAttribute2(String defaultInventoryAttribute2) {
        this.defaultInventoryAttribute2 = defaultInventoryAttribute2;
    }

    public boolean isTrackingInventoryAttribute3Flag() {
        return trackingInventoryAttribute3Flag;
    }

    public void setTrackingInventoryAttribute3Flag(boolean trackingInventoryAttribute3Flag) {
        this.trackingInventoryAttribute3Flag = trackingInventoryAttribute3Flag;
    }

    public String getDefaultInventoryAttribute3() {
        return defaultInventoryAttribute3;
    }

    public void setDefaultInventoryAttribute3(String defaultInventoryAttribute3) {
        this.defaultInventoryAttribute3 = defaultInventoryAttribute3;
    }

    public boolean isTrackingInventoryAttribute4Flag() {
        return trackingInventoryAttribute4Flag;
    }

    public void setTrackingInventoryAttribute4Flag(boolean trackingInventoryAttribute4Flag) {
        this.trackingInventoryAttribute4Flag = trackingInventoryAttribute4Flag;
    }

    public String getDefaultInventoryAttribute4() {
        return defaultInventoryAttribute4;
    }

    public void setDefaultInventoryAttribute4(String defaultInventoryAttribute4) {
        this.defaultInventoryAttribute4 = defaultInventoryAttribute4;
    }

    public boolean isTrackingInventoryAttribute5Flag() {
        return trackingInventoryAttribute5Flag;
    }

    public void setTrackingInventoryAttribute5Flag(boolean trackingInventoryAttribute5Flag) {
        this.trackingInventoryAttribute5Flag = trackingInventoryAttribute5Flag;
    }

    public String getDefaultInventoryAttribute5() {
        return defaultInventoryAttribute5;
    }

    public void setDefaultInventoryAttribute5(String defaultInventoryAttribute5) {
        this.defaultInventoryAttribute5 = defaultInventoryAttribute5;
    }
}

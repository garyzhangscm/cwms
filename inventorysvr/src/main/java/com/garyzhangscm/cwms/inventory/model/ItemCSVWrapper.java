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
import org.codehaus.jackson.annotate.JsonProperty;

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

    private String warehouse;
    private String company;

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



    private String imageUrl;

    private String thumbnailUrl;

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

    public String getWarehouse() {
        return warehouse;
    }

    public void setWarehouse(String warehouse) {
        this.warehouse = warehouse;
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
}

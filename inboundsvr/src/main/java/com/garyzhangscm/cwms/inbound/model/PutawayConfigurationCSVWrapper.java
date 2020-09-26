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

public class PutawayConfigurationCSVWrapper {


    private Integer sequence;

    private String item;

    private String itemFamily;

    private String inventoryStatus;

    private String location;
    private String locationGroup;
    private String locationGroupType;
    private String strategies;

    private String warehouse;
    private String company;

    public Integer getSequence() {
        return sequence;
    }

    public void setSequence(Integer sequence) {
        this.sequence = sequence;
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

    public String getItemFamily() {
        return itemFamily;
    }

    public void setItemFamily(String itemFamily) {
        this.itemFamily = itemFamily;
    }

    public String getInventoryStatus() {
        return inventoryStatus;
    }

    public void setInventoryStatus(String inventoryStatus) {
        this.inventoryStatus = inventoryStatus;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getLocationGroup() {
        return locationGroup;
    }

    public void setLocationGroup(String locationGroup) {
        this.locationGroup = locationGroup;
    }

    public String getLocationGroupType() {
        return locationGroupType;
    }

    public void setLocationGroupType(String locationGroupType) {
        this.locationGroupType = locationGroupType;
    }

    public String getStrategies() {
        return strategies;
    }

    public void setStrategies(String strategies) {
        this.strategies = strategies;
    }

    public String getWarehouse() {
        return warehouse;
    }

    public void setWarehouse(String warehouse) {
        this.warehouse = warehouse;
    }
}

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

package com.garyzhangscm.cwms.outbound.model;


import java.io.Serializable;

public class PickConfirmStrategyCSVWrapper  implements Serializable {



    private String company;
    private String warehouse;
    private Integer sequence;
    private String item;
    private String itemFamily;
    private String location;
    private String locationGroup;
    private String locationGroupType;
    private String unitOfMeasure;
    private Boolean confirmItemFlag;
    private Boolean confirmLocationFlag;
    private Boolean confirmLocationCodeFlag;

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public String getWarehouse() {
        return warehouse;
    }

    public void setWarehouse(String warehouse) {
        this.warehouse = warehouse;
    }

    public Integer getSequence() {
        return sequence;
    }

    public void setSequence(Integer sequence) {
        this.sequence = sequence;
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

    public String getUnitOfMeasure() {
        return unitOfMeasure;
    }

    public void setUnitOfMeasure(String unitOfMeasure) {
        this.unitOfMeasure = unitOfMeasure;
    }

    public Boolean getConfirmItemFlag() {
        return confirmItemFlag;
    }

    public void setConfirmItemFlag(Boolean confirmItemFlag) {
        this.confirmItemFlag = confirmItemFlag;
    }

    public Boolean getConfirmLocationFlag() {
        return confirmLocationFlag;
    }

    public void setConfirmLocationFlag(Boolean confirmLocationFlag) {
        this.confirmLocationFlag = confirmLocationFlag;
    }

    public Boolean getConfirmLocationCodeFlag() {
        return confirmLocationCodeFlag;
    }

    public void setConfirmLocationCodeFlag(Boolean confirmLocationCodeFlag) {
        this.confirmLocationCodeFlag = confirmLocationCodeFlag;
    }
}

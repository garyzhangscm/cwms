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


public class GridConfigurationCSVWrapper implements Serializable {

    private String warehouse;
    private String company;


    private String locationGroup;

    private Boolean preAssignedLocation;

    private Boolean allowConfirmByGroup;

    private Boolean depositOnConfirm;

    @Override
    public String toString() {
        return "GridConfigurationCSVWrapper{" +
                "warehouse='" + warehouse + '\'' +
                ", locationGroup='" + locationGroup + '\'' +
                ", preAssignedLocation=" + preAssignedLocation +
                ", allowConfirmByGroup=" + allowConfirmByGroup +
                ", depositOnConfirm=" + depositOnConfirm +
                '}';
    }

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

    public String getLocationGroup() {
        return locationGroup;
    }

    public void setLocationGroup(String locationGroup) {
        this.locationGroup = locationGroup;
    }

    public Boolean getPreAssignedLocation() {
        return preAssignedLocation;
    }

    public void setPreAssignedLocation(Boolean preAssignedLocation) {
        this.preAssignedLocation = preAssignedLocation;
    }

    public Boolean getAllowConfirmByGroup() {
        return allowConfirmByGroup;
    }

    public void setAllowConfirmByGroup(Boolean allowConfirmByGroup) {
        this.allowConfirmByGroup = allowConfirmByGroup;
    }

    public Boolean getDepositOnConfirm() {
        return depositOnConfirm;
    }

    public void setDepositOnConfirm(Boolean depositOnConfirm) {
        this.depositOnConfirm = depositOnConfirm;
    }
}

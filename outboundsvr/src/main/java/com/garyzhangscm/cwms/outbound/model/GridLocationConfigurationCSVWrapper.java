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


public class GridLocationConfigurationCSVWrapper implements Serializable {

    private String warehouse;
    private String company;


    private String location;

    private Integer rowNumber;

    private Integer columnSpan;
    private Integer sequence;
    private Long pendingQuantity;


    private boolean permanentLPNFlag;
    private String permanentLPN;

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

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public Integer getRowNumber() {
        return rowNumber;
    }

    public void setRowNumber(Integer rowNumber) {
        this.rowNumber = rowNumber;
    }

    public Integer getColumnSpan() {
        return columnSpan;
    }

    public void setColumnSpan(Integer columnSpan) {
        this.columnSpan = columnSpan;
    }

    public Integer getSequence() {
        return sequence;
    }

    public void setSequence(Integer sequence) {
        this.sequence = sequence;
    }

    public Long getPendingQuantity() {
        return pendingQuantity;
    }

    public void setPendingQuantity(Long pendingQuantity) {
        this.pendingQuantity = pendingQuantity;
    }

    public String getPermanentLPN() {
        return permanentLPN;
    }

    public void setPermanentLPN(String permanentLPN) {
        this.permanentLPN = permanentLPN;
    }

    public boolean isPermanentLPNFlag() {
        return permanentLPNFlag;
    }

    public void setPermanentLPNFlag(boolean permanentLPNFlag) {
        this.permanentLPNFlag = permanentLPNFlag;
    }
}

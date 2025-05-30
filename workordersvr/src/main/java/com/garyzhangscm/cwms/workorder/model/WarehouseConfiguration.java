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

package com.garyzhangscm.cwms.workorder.model;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;

@JsonIgnoreProperties(ignoreUnknown = true)
public class WarehouseConfiguration extends AuditibleEntity<String> implements Serializable {

    private Long id;

    private Warehouse warehouse;

    private Boolean threePartyLogisticsFlag;

    private Boolean newLPNPrintLabelAtReceivingFlag;
    private Boolean newLPNPrintLabelAtProducingFlag;
    private Boolean newLPNPrintLabelAtAdjustmentFlag;

    private String timeZone;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Warehouse getWarehouse() {
        return warehouse;
    }

    public void setWarehouse(Warehouse warehouse) {
        this.warehouse = warehouse;
    }

    public Boolean getThreePartyLogisticsFlag() {
        return threePartyLogisticsFlag;
    }

    public void setThreePartyLogisticsFlag(Boolean threePartyLogisticsFlag) {
        this.threePartyLogisticsFlag = threePartyLogisticsFlag;
    }

    public Boolean getNewLPNPrintLabelAtReceivingFlag() {
        return newLPNPrintLabelAtReceivingFlag;
    }

    public void setNewLPNPrintLabelAtReceivingFlag(Boolean newLPNPrintLabelAtReceivingFlag) {
        this.newLPNPrintLabelAtReceivingFlag = newLPNPrintLabelAtReceivingFlag;
    }

    public Boolean getNewLPNPrintLabelAtProducingFlag() {
        return newLPNPrintLabelAtProducingFlag;
    }

    public void setNewLPNPrintLabelAtProducingFlag(Boolean newLPNPrintLabelAtProducingFlag) {
        this.newLPNPrintLabelAtProducingFlag = newLPNPrintLabelAtProducingFlag;
    }

    public Boolean getNewLPNPrintLabelAtAdjustmentFlag() {
        return newLPNPrintLabelAtAdjustmentFlag;
    }

    public void setNewLPNPrintLabelAtAdjustmentFlag(Boolean newLPNPrintLabelAtAdjustmentFlag) {
        this.newLPNPrintLabelAtAdjustmentFlag = newLPNPrintLabelAtAdjustmentFlag;
    }

    public String getTimeZone() {
        return timeZone;
    }

    public void setTimeZone(String timeZone) {
        this.timeZone = timeZone;
    }
}

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

package com.garyzhangscm.cwms.adminserver.model.wms;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.persistence.*;
import java.io.Serializable;

public class CycleCountBatch implements Serializable {

    private Long id;

    private String batchId;

    private Long warehouseId;

    private Warehouse warehouse;

    private int requestLocationCount;
    private int openLocationCount;
    private int finishedLocationCount;
    private int cancelledLocationCount;
    private int openAuditLocationCount;
    private int finishedAuditLocationCount;

    @Override
    public String toString() {
        try {
            return new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(this);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return null;
    }

    public String getBatchId() {
        return batchId;
    }

    public void setBatchId(String batchId) {
        this.batchId = batchId;
    }

    public int getRequestLocationCount() {
        return requestLocationCount;
    }

    public void setRequestLocationCount(int requestLocationCount) {
        this.requestLocationCount = requestLocationCount;
    }

    public int getOpenLocationCount() {
        return openLocationCount;
    }

    public void setOpenLocationCount(int openLocationCount) {
        this.openLocationCount = openLocationCount;
    }

    public int getFinishedLocationCount() {
        return finishedLocationCount;
    }

    public void setFinishedLocationCount(int finishedLocationCount) {
        this.finishedLocationCount = finishedLocationCount;
    }

    public int getCancelledLocationCount() {
        return cancelledLocationCount;
    }

    public void setCancelledLocationCount(int cancelledLocationCount) {
        this.cancelledLocationCount = cancelledLocationCount;
    }

    public int getOpenAuditLocationCount() {
        return openAuditLocationCount;
    }

    public void setOpenAuditLocationCount(int openAuditLocationCount) {
        this.openAuditLocationCount = openAuditLocationCount;
    }

    public int getFinishedAuditLocationCount() {
        return finishedAuditLocationCount;
    }

    public void setFinishedAuditLocationCount(int finishedAuditLocationCount) {
        this.finishedAuditLocationCount = finishedAuditLocationCount;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(Long warehouseId) {
        this.warehouseId = warehouseId;
    }

    public Warehouse getWarehouse() {
        return warehouse;
    }

    public void setWarehouse(Warehouse warehouse) {
        this.warehouse = warehouse;
    }
}

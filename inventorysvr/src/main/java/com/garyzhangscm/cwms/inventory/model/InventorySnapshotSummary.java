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


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.codehaus.jackson.annotate.JsonProperty;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

public class InventorySnapshotSummary extends AuditibleEntity<String> implements Serializable {

    // inventory snapshot
    String batchNumber;
    LocalDateTime completeTime;

    InventorySnapshotSummaryGroupBy groupBy;
    String groupByValue;

    long inventoryQuantity;

    public InventorySnapshotSummary(String batchNumber, LocalDateTime completeTime, InventorySnapshotSummaryGroupBy groupBy, String groupByValue, long inventoryQuantity) {
        this.batchNumber = batchNumber;
        this.completeTime = completeTime;
        this.groupBy = groupBy;
        this.groupByValue = groupByValue;
        this.inventoryQuantity = inventoryQuantity;
    }

    public InventorySnapshotSummary( ) {
    }

    public InventorySnapshotSummaryGroupBy getGroupBy() {
        return groupBy;
    }

    public void setGroupBy(InventorySnapshotSummaryGroupBy groupBy) {
        this.groupBy = groupBy;
    }


    public String getBatchNumber() {
        return batchNumber;
    }

    public void setBatchNumber(String batchNumber) {
        this.batchNumber = batchNumber;
    }

    public LocalDateTime getCompleteTime() {
        return completeTime;
    }

    public void setCompleteTime(LocalDateTime completeTime) {
        this.completeTime = completeTime;
    }

    public String getGroupByValue() {
        return groupByValue;
    }

    public void setGroupByValue(String groupByValue) {
        this.groupByValue = groupByValue;
    }

    public long getInventoryQuantity() {
        return inventoryQuantity;
    }

    public void setInventoryQuantity(long inventoryQuantity) {
        this.inventoryQuantity = inventoryQuantity;
    }
}

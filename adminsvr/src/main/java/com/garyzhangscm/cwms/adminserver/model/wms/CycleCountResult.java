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
import org.codehaus.jackson.annotate.JsonProperty;

import javax.persistence.*;
import java.io.Serializable;

public class CycleCountResult implements Serializable {

    private Long id;

    private String batchId;

    private Long locationId;

    private Location location;

    private Item item;

    private Long quantity;

    private Long countQuantity;


    private AuditCountRequest auditCountRequest;

    private Long warehouseId;

    private Warehouse warehouse;

    public CycleCountResult() {

    }

    public CycleCountResult(CycleCountRequest cycleCountRequest) {
        this.batchId = cycleCountRequest.getBatchId();
        this.locationId = cycleCountRequest.getLocationId();
        this.location = cycleCountRequest.getLocation();
        this.warehouseId = cycleCountRequest.getWarehouseId();
    }

    public CycleCountResult(CycleCountRequest cycleCountRequest, Item item, Long quantity) {
        this.batchId = cycleCountRequest.getBatchId();
        this.locationId = cycleCountRequest.getLocationId();
        this.location = cycleCountRequest.getLocation();
        this.warehouseId = cycleCountRequest.getWarehouseId();
        this.item = item;
        this.quantity = quantity;
        this.countQuantity = 0L;
    }
    public CycleCountResult(CycleCountRequest cycleCountRequest, Item item, Long quantity, Long countQuantity) {
        this.batchId = cycleCountRequest.getBatchId();
        this.locationId = cycleCountRequest.getLocationId();
        this.location = cycleCountRequest.getLocation();
        this.warehouseId = cycleCountRequest.getWarehouseId();
        this.item = item;
        this.quantity = quantity;
        this.countQuantity = countQuantity;
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

    public static CycleCountResult cycleCountResultForEmptyLocation(CycleCountRequest cycleCountRequest) {
        return new CycleCountResult(cycleCountRequest, null, 0L);
    }
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getBatchId() {
        return batchId;
    }

    public void setBatchId(String batchId) {
        this.batchId = batchId;
    }

    public Long getLocationId() {
        return locationId;
    }

    public void setLocationId(Long locationId) {
        this.locationId = locationId;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public Long getQuantity() {
        return quantity;
    }

    public void setQuantity(Long quantity) {
        this.quantity = quantity;
    }

    public Long getCountQuantity() {
        return countQuantity;
    }

    public void setCountQuantity(Long countQuantity) {
        this.countQuantity = countQuantity;
    }

    public AuditCountRequest getAuditCountRequest() {
        return auditCountRequest;
    }

    public void setAuditCountRequest(AuditCountRequest auditCountRequest) {
        this.auditCountRequest = auditCountRequest;
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

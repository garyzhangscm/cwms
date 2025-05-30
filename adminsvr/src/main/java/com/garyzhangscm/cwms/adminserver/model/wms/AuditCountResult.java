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
import java.util.Objects;

public class AuditCountResult implements Serializable {

    private Long id;
    private String batchId;

    private Long locationId;

    private Location location;

    private Inventory inventory;

    private Long quantity;

    private Long countQuantity;

    private Long warehouseId;

    private Warehouse warehouse;

    public AuditCountResult() {}
    public AuditCountResult(Long warehouseId, String batchId, Long locationId, Inventory inventory,
                            Long quantity, Long countQuantity) {
        this.warehouseId = warehouseId;
        this.batchId = batchId;
        this.locationId = locationId;
        this.location = inventory.getLocation();
        this.inventory = inventory;
        this.quantity = quantity;
        this.countQuantity = countQuantity;
    }

    public AuditCountResult(Long warehouseId, String batchId, Long locationId,Location location, Inventory inventory,
                            Long quantity, Long countQuantity) {
        this.warehouseId = warehouseId;
        this.batchId = batchId;
        this.locationId = locationId;
        this.location = location;
        this.inventory = inventory;
        this.quantity = quantity;
        this.countQuantity = countQuantity;
    }
    public AuditCountResult(AuditCountRequest auditCountRequest, Inventory inventory,
                            Long countQuantity) {
        this.warehouseId = auditCountRequest.getWarehouseId();
        this.warehouse = auditCountRequest.getWarehouse();
        this.batchId = auditCountRequest.getBatchId();
        this.locationId = auditCountRequest.getLocationId();
        this.location = Objects.isNull(auditCountRequest.getLocation()) ?
                inventory.getLocation() : auditCountRequest.getLocation();
        this.inventory = inventory;
        this.quantity = inventory.getQuantity();
        this.countQuantity = countQuantity;
    }
    public static AuditCountResult emptyLocationAuditCountResult(Long warehouseId, String batchId, Long locationId) {
        return new AuditCountResult(warehouseId, batchId, locationId, null, 0L, 0L);
    }
    public static AuditCountResult emptyLocationAuditCountResult(Long warehouseId, String batchId, Long locationId, Location location) {
        return new AuditCountResult(warehouseId, batchId, locationId, location,null, 0L, 0L);
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
        if (location.getId() != null) {
            setLocationId(location.getId());
        }
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

    public Inventory getInventory() {
        return inventory;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
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

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
import java.util.Set;

@Entity
@Table(name = "audit_count_result")
public class AuditCountResult implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "audit_count_result_id")
    @JsonProperty(value="id")
    private Long id;

    @Column(name = "batch_id")
    private String batchId;

    @Column(name = "location_id")
    private Long locationId;

    @Transient
    private Location location;

    @ManyToOne
    @JoinColumn(name="inventory_id")
    private Inventory inventory;


    @Column(name = "lpn")
    private String lpn;

    @ManyToOne
    @JoinColumn(name="item_id")
    private Item item;

    @Column(name = "quantity")
    private Long quantity;

    @Column(name = "count_quantity")
    private Long countQuantity;


    @Column(name = "warehouse_id")
    private Long warehouseId;

    @Transient
    private Warehouse warehouse;

    public AuditCountResult() {}
    public AuditCountResult(Long warehouseId, String batchId, Long locationId, Inventory inventory,
                            Long quantity, Long countQuantity) {
        this.warehouseId = warehouseId;
        this.batchId = batchId;
        this.locationId = locationId;
        this.inventory = inventory;
        this.lpn = inventory.getLpn();
        this.item = inventory.getItem();
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
        this.lpn = inventory == null ? "" : inventory.getLpn();
        this.item = inventory == null ? null : inventory.getItem();
        this.quantity = quantity;
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

    public String getLpn() {
        return lpn;
    }

    public void setLpn(String lpn) {
        this.lpn = lpn;
    }

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }
}

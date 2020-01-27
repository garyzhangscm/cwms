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
import org.codehaus.jackson.annotate.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "inventory_movement")
public class InventoryMovement implements Serializable {

    private static final Logger logger = LoggerFactory.getLogger(InventoryMovement.class);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "inventory_movement_id")
    @JsonProperty(value="id")
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inventory_id")
    private Inventory inventory;

    @Column(name = "location_id")
    private Long locationId;

    @Transient
    private Location location;


    @Column(name = "sequence")
    private Integer sequence;


    @Column(name = "warehouse_id")
    private Long warehouseId;

    @Transient
    private Warehouse warehouse;

    @Override
    public Object clone() {
        InventoryMovement inventoryMovement = null;
        try {
            inventoryMovement = (InventoryMovement) super.clone();
        }
        catch(CloneNotSupportedException e) {
            inventoryMovement = new InventoryMovement();
            inventoryMovement.setLocationId(getLocationId());
            inventoryMovement.setSequence(getSequence());
        }
        inventoryMovement.setLocation((Location)(getLocation().clone()));
        return inventoryMovement;
    }
    public String toString() {
        return "id: " + id + "\n"
                + "Inventory: " + "\n"
                + " >> LPN: " + (inventory == null ? "" : inventory.getLpn())  + "\n"
                + " >> item: " + (inventory == null ? "" : inventory.getItem().getName() ) + "\n"
                + " >> item description : " + (inventory == null ? "" : inventory.getItem().getDescription() ) + "\n"
                + " >> inventory status : " + (inventory == null ? "" : inventory.getInventoryStatus().getName() ) + "\n"
                + " >> quantity : " + (inventory == null ? "" : inventory.getQuantity())  + "\n"
                + "Sequence : " + sequence   + "\n"
                + "Location: " + (location == null ? "" :  location.getName())  + "\n"
                + "Location ID: " + locationId;
    }
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Inventory getInventory() {
        return inventory;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
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

    public Integer getSequence() {
        return sequence;
    }

    public void setSequence(Integer sequence) {
        this.sequence = sequence;
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

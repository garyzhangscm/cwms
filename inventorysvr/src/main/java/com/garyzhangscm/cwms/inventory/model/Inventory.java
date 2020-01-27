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


import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.annotate.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "inventory")
public class Inventory implements Serializable {

    private static final Logger logger = LoggerFactory.getLogger(Inventory.class);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "inventory_id")
    @JsonProperty(value="id")
    private Long id;

    @Column(name = "lpn")
    private String lpn;

    @Column(name = "location_id")
    private Long locationId;

    @Transient
    private Location location;

    @Column(name = "pick_id")
    private Long pickId;

    @Transient
    private Pick pick;

    @Column(name = "receipt_id")
    private Long receiptId;

    @ManyToOne
    @JoinColumn(name="item_id")
    private Item item;

    @ManyToOne
    @JoinColumn(name="item_package_type_id")
    private ItemPackageType itemPackageType;

    @Column(name = "quantity")
    private Long quantity;

    @Column(name = "virtual_inventory")
    private Boolean virtual;

    @ManyToOne
    @JoinColumn(name="inventory_status_id")
    private InventoryStatus inventoryStatus;

    @Column(name = "warehouse_id")
    private Long warehouseId;

    @Transient
    private Warehouse warehouse;

    @OneToMany(
            mappedBy = "inventory",
            cascade = CascadeType.REMOVE,
            // orphanRemoval = true, // We will process the movement manually from InventoryMovementService
            fetch = FetchType.LAZY
    )
    List<InventoryMovement> inventoryMovements = new ArrayList<>();

    public Inventory split(String newLpn, Long newQuantity) {
        Inventory inventory = new Inventory();
        if (StringUtils.isBlank(newLpn)) {
            newLpn = getLpn();
        }
        inventory.setLpn(newLpn);
        inventory.setPickId(getPickId());
        // Copy inventory movement
        List<InventoryMovement> inventoryMovements = new ArrayList<>();
        getInventoryMovements().stream().forEach(inventoryMovement -> {
            InventoryMovement newInventoryMovement = (InventoryMovement)inventoryMovement.clone();
            newInventoryMovement.setInventory(inventory);
            inventoryMovements.add(newInventoryMovement);
        });

        inventory.setInventoryMovements(inventoryMovements);

        inventory.setInventoryStatus(getInventoryStatus());
        inventory.setItem(getItem());
        inventory.setItemPackageType(getItemPackageType());
        inventory.setLocationId(getLocationId());
        inventory.setQuantity(newQuantity);
        inventory.setReceiptId(getReceiptId());
        inventory.setVirtual(getVirtual());

        setQuantity(getQuantity() - newQuantity);

        return inventory;

    }
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getLpn() {
        return lpn;
    }

    public void setLpn(String lpn) {
        this.lpn = lpn;
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

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public ItemPackageType getItemPackageType() {
        return itemPackageType;
    }

    public void setItemPackageType(ItemPackageType itemPackageType) {
        this.itemPackageType = itemPackageType;
    }

    public Long getQuantity() {
        return quantity;
    }

    public void setQuantity(Long quantity) {
        this.quantity = quantity;
    }

    public InventoryStatus getInventoryStatus() {
        return inventoryStatus;
    }

    public void setInventoryStatus(InventoryStatus inventoryStatus) {
        this.inventoryStatus = inventoryStatus;
    }

    public Double getSize() {

        ItemUnitOfMeasure stockItemUnitOfMeasure = itemPackageType.getStockItemUnitOfMeasures();

        return (quantity / stockItemUnitOfMeasure.getQuantity())
                * stockItemUnitOfMeasure.getLength()
                * stockItemUnitOfMeasure.getWidth()
                * stockItemUnitOfMeasure.getHeight();
    }

    public Boolean getVirtual() {
        return virtual;
    }

    public void setVirtual(Boolean virtual) {
        this.virtual = virtual;
    }

    public Long getReceiptId() {
        return receiptId;
    }

    public void setReceiptId(Long receiptId) {
        this.receiptId = receiptId;
    }

    public List<InventoryMovement> getInventoryMovements() {
        return inventoryMovements;
    }

    public void setInventoryMovements(List<InventoryMovement> inventoryMovements) {
        this.inventoryMovements = inventoryMovements;
    }

    public Long getPickId() {
        return pickId;
    }

    public void setPickId(Long pickId) {
        this.pickId = pickId;
    }

    public Pick getPick() {
        return pick;
    }

    public void setPick(Pick pick) {
        this.pick = pick;
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

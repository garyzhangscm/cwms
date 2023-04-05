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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;

import javax.persistence.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "bulk_pick")
public class BulkPick extends AuditibleEntity<String> implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "bulk_pick_id")
    @JsonProperty(value="id")
    private Long id;

    @Column(name = "number")
    private String number;

    // right now we will only allow bulk
    // at wave level
    @Column(name = "wave_number")
    private String waveNumber;

    @Column(name = "warehouse_id")
    private Long warehouseId;

    @Transient
    private Warehouse warehouse;

    @Column(name = "source_location_id")
    private Long sourceLocationId;

    @Transient
    private Location sourceLocation;

    @Column(name = "item_id")
    private Long itemId;

    @Transient
    private Item item;

    @OneToMany(
            mappedBy = "bulkPick",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    private List<Pick> picks = new ArrayList<>();

    @Column(name = "quantity")
    private Long quantity;

    @Column(name = "picked_quantity")
    private Long pickedQuantity;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private PickStatus status;

    @Column(name = "inventory_status_id")
    private Long inventoryStatusId;

    @Transient
    private InventoryStatus inventoryStatus;

    @Column(name = "unit_of_measure_id")
    private Long unitOfMeasureId;

    // how to confirm the pick. THe flag
    // will be setup according to the area/ location's attribute
    @Column(name = "confirm_item_flag")
    private boolean confirmItemFlag;
    @Column(name = "confirm_location_flag")
    private boolean confirmLocationFlag;
    @Column(name = "confirm_location_code_flag")
    private boolean confirmLocationCodeFlag;
    @Column(name = "confirm_lpn_flag")
    private boolean confirmLpnFlag;

    @Column(name="color")
    private String color;

    @Column(name="product_size")
    private String productSize;

    @Column(name="style")
    private String style;


    // if the pick is currently picking by certain user
    @Column(name = "picking_by_user_id")
    private Long pickingByUserId;

    // if the user is assigned to certain user
    @Column(name = "assigned_to_user_id")
    private Long assignedToUserId;

    @JsonIgnore
    public Double getSize() {

        if (item == null) {
            return 0.0;
        }
        ItemUnitOfMeasure stockItemUnitOfMeasure = item.getItemPackageTypes().get(0).getStockItemUnitOfMeasures();

        return (quantity / stockItemUnitOfMeasure.getQuantity())
                * stockItemUnitOfMeasure.getLength()
                * stockItemUnitOfMeasure.getWidth()
                * stockItemUnitOfMeasure.getHeight();
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

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public Long getSourceLocationId() {
        return sourceLocationId;
    }

    public void setSourceLocationId(Long sourceLocationId) {
        this.sourceLocationId = sourceLocationId;
    }

    public Location getSourceLocation() {
        return sourceLocation;
    }

    public void setSourceLocation(Location sourceLocation) {
        this.sourceLocation = sourceLocation;
    }

    public Long getItemId() {
        return itemId;
    }

    public void setItemId(Long itemId) {
        this.itemId = itemId;
    }

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public List<Pick> getPicks() {
        return picks;
    }

    public void setPicks(List<Pick> picks) {
        this.picks = picks;
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

    public Long getQuantity() {
        return quantity;
    }

    public void setQuantity(Long quantity) {
        this.quantity = quantity;
    }

    public Long getPickedQuantity() {
        return pickedQuantity;
    }

    public void setPickedQuantity(Long pickedQuantity) {
        this.pickedQuantity = pickedQuantity;
    }

    public PickStatus getStatus() {
        return status;
    }

    public void setStatus(PickStatus status) {
        this.status = status;
    }

    public Long getInventoryStatusId() {
        return inventoryStatusId;
    }

    public void setInventoryStatusId(Long inventoryStatusId) {
        this.inventoryStatusId = inventoryStatusId;
    }

    public InventoryStatus getInventoryStatus() {
        return inventoryStatus;
    }

    public void setInventoryStatus(InventoryStatus inventoryStatus) {
        this.inventoryStatus = inventoryStatus;
    }

    public Long getUnitOfMeasureId() {
        return unitOfMeasureId;
    }

    public void setUnitOfMeasureId(Long unitOfMeasureId) {
        this.unitOfMeasureId = unitOfMeasureId;
    }

    public boolean isConfirmItemFlag() {
        return confirmItemFlag;
    }

    public void setConfirmItemFlag(boolean confirmItemFlag) {
        this.confirmItemFlag = confirmItemFlag;
    }

    public boolean isConfirmLocationFlag() {
        return confirmLocationFlag;
    }

    public void setConfirmLocationFlag(boolean confirmLocationFlag) {
        this.confirmLocationFlag = confirmLocationFlag;
    }

    public boolean isConfirmLocationCodeFlag() {
        return confirmLocationCodeFlag;
    }

    public void setConfirmLocationCodeFlag(boolean confirmLocationCodeFlag) {
        this.confirmLocationCodeFlag = confirmLocationCodeFlag;
    }

    public boolean isConfirmLpnFlag() {
        return confirmLpnFlag;
    }

    public void setConfirmLpnFlag(boolean confirmLpnFlag) {
        this.confirmLpnFlag = confirmLpnFlag;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getProductSize() {
        return productSize;
    }

    public void setProductSize(String productSize) {
        this.productSize = productSize;
    }

    public String getStyle() {
        return style;
    }

    public void setStyle(String style) {
        this.style = style;
    }

    public Long getPickingByUserId() {
        return pickingByUserId;
    }

    public void setPickingByUserId(Long pickingByUserId) {
        this.pickingByUserId = pickingByUserId;
    }

    public Long getAssignedToUserId() {
        return assignedToUserId;
    }

    public void setAssignedToUserId(Long assignedToUserId) {
        this.assignedToUserId = assignedToUserId;
    }

    public String getWaveNumber() {
        return waveNumber;
    }

    public void setWaveNumber(String waveNumber) {
        this.waveNumber = waveNumber;
    }
}

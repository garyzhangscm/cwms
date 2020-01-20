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
import org.codehaus.jackson.annotate.JsonProperty;

import javax.persistence.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "pick")
public class Pick implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pick_id")
    @JsonProperty(value="id")
    private Long id;

    @Column(name = "number")
    private String number;

    @Column(name = "source_location_id")
    private Long sourceLocationId;

    @Transient
    private Location sourceLocation;

    @Column(name = "destination_location_id")
    private Long destinationLocationId;

    @Transient
    private Location destinationLocation;

    @Column(name = "item_id")
    private Long itemId;

    @Transient
    private Item item;

    @ManyToOne
    @JoinColumn(name = "shipment_line_id")
    @JsonIgnore
    private ShipmentLine shipmentLine;

    @Column(name = "quantity")
    private Long quantity;

    @Column(name = "picked_quantity")
    private Long pickedQuantity;

    @Column(name = "status")
    private PickStatus status;

    @OneToMany(
            mappedBy = "pick",
            cascade = CascadeType.REMOVE,
            // orphanRemoval = true, // We will process the movement manually from InventoryMovementService
            fetch = FetchType.LAZY
    )
    List<PickMovement> pickMovements = new ArrayList<>();

    public Double getSize() {

        ItemUnitOfMeasure stockItemUnitOfMeasure = item.getItemPackageTypes().get(0).getStockItemUnitOfMeasures();

        return (quantity / stockItemUnitOfMeasure.getQuantity())
                * stockItemUnitOfMeasure.getLength()
                * stockItemUnitOfMeasure.getWidth()
                * stockItemUnitOfMeasure.getHeight();
    }

    @Override
    public String toString() {
        return new StringBuilder()
                .append("{ id: ").append(id).append(",")
                .append("number: ").append(number).append(",")
                .append("sourceLocationId: ").append(sourceLocationId).append(",")
                .append("sourceLocation: ").append(sourceLocation).append(",")
                .append("destinationLocationId: ").append(destinationLocationId).append(",")
                .append("destinationLocation: ").append(destinationLocation).append(",")
                .append("itemId: ").append(itemId).append(",")
                .append("item: ").append(item).append(",")
                .append("shipmentLine: ").append(shipmentLine).append(",")
                .append("quantity: ").append(quantity).append(",")
                .append("pickedQuantity: ").append(pickedQuantity).append(",")
                .append("status: ").append(status).append("}").toString();

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

    public ShipmentLine getShipmentLine() {
        return shipmentLine;
    }

    public void setShipmentLine(ShipmentLine shipmentLine) {
        this.shipmentLine = shipmentLine;
    }

    public Long getDestinationLocationId() {
        return destinationLocationId;
    }

    public void setDestinationLocationId(Long destinationLocationId) {
        this.destinationLocationId = destinationLocationId;
    }

    public Location getDestinationLocation() {
        return destinationLocation;
    }

    public void setDestinationLocation(Location destinationLocation) {
        this.destinationLocation = destinationLocation;
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

    public String getOrderNumber() {
        return shipmentLine.getOrderNumber();
    }

    public List<PickMovement> getPickMovements() {
        return pickMovements;
    }

    public void setPickMovements(List<PickMovement> pickMovements) {
        this.pickMovements = pickMovements;
    }

    public InventoryStatus getInventoryStatus() {
        return shipmentLine.getOrderLine().getInventoryStatus();
    }
}

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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.codehaus.jackson.annotate.JsonProperty;

import javax.persistence.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "shipment_line")
public class ShipmentLine  extends AuditibleEntity<String> implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "shipment_line_id")
    @JsonProperty(value="id")
    private Long id;

    @Column(name = "warehouse_id")
    private Long warehouseId;

    @Transient
    private Warehouse warehouse;

    @Column(name = "number")
    private String number;

    @ManyToOne
    @JoinColumn(name = "shipment_id")
    @JsonIgnore
    private Shipment shipment;

    @ManyToOne
    @JoinColumn(name = "wave_id")
    @JsonIgnore
    private Wave wave;

    @ManyToOne
    @JoinColumn(name = "outbound_order_line_id")
    private OrderLine orderLine;

    @Column(name = "quantity")
    private Long quantity;

    @Column(name = "open_quantity")
    private Long openQuantity;

    @Column(name = "inprocess_quantity")
    private Long inprocessQuantity;


    @Column(name = "loaded_quantity")
    private Long loadedQuantity;

    @Column(name = "shipped_quantity")
    private Long shippedQuantity;


    @OneToMany(
            mappedBy = "shipmentLine",
            cascade = CascadeType.REMOVE,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    private List<Pick> picks = new ArrayList<>();

    @OneToMany(
            mappedBy = "shipmentLine",
            cascade = CascadeType.REMOVE,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    private List<ShortAllocation> shortAllocations = new ArrayList<>();

    @Column(name="status")
    @Enumerated(EnumType.STRING)
    private ShipmentLineStatus status = ShipmentLineStatus.PENDING;


    @Override
    public String toString() {
        try {
            return new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(this);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ShipmentLine that = (ShipmentLine) o;
        if (id != null && that.id != null) {
            return Objects.equals(id, that.id);
        }
        return  Objects.equals(warehouseId, that.warehouseId) &&
                Objects.equals(number, that.number) &&
                Objects.equals(shipment, that.shipment);
    }

    @Override
    public int hashCode() {
        return Objects.hash(warehouseId, number, shipment);
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

    public Shipment getShipment() {
        return shipment;
    }

    public void setShipment(Shipment shipment) {
        this.shipment = shipment;
    }

    public OrderLine getOrderLine() {
        return orderLine;
    }

    public void setOrderLine(OrderLine orderLine) {
        this.orderLine = orderLine;
    }

    public Long getQuantity() {
        return quantity;
    }

    public void setQuantity(Long quantity) {
        this.quantity = quantity;
    }

    public Long getShippedQuantity() {
        return shippedQuantity;
    }

    public void setShippedQuantity(Long shippedQuantity) {
        this.shippedQuantity = shippedQuantity;
    }

    public Wave getWave() {
        return wave;
    }

    public void setWave(Wave wave) {
        this.wave = wave;
    }

    public String getOrderNumber() {
        return orderLine.getOrder().getNumber();
    }

    public String getShipmentNumber() {return shipment.getNumber();}

    public Long getOpenQuantity() {
        return openQuantity;
    }

    public void setOpenQuantity(Long openQuantity) {
        this.openQuantity = openQuantity;
    }

    public Long getInprocessQuantity() {
        return inprocessQuantity;
    }

    public void setInprocessQuantity(Long inprocessQuantity) {
        this.inprocessQuantity = inprocessQuantity;
    }

    public List<Pick> getPicks() {
        return picks;
    }

    public void setPicks(List<Pick> picks) {
        this.picks = picks;
    }

    public List<ShortAllocation> getShortAllocations() {
        return shortAllocations;
    }

    public void setShortAllocations(List<ShortAllocation> shortAllocations) {
        this.shortAllocations = shortAllocations;
    }

    public Long getLoadedQuantity() {
        return loadedQuantity;
    }

    public void setLoadedQuantity(Long loadedQuantity) {
        this.loadedQuantity = loadedQuantity;
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

    public ShipmentLineStatus getStatus() {
        return status;
    }

    public void setStatus(ShipmentLineStatus status) {
        this.status = status;
    }
}

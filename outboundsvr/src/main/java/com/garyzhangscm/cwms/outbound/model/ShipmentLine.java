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
@Table(name = "shipment_line")
public class ShipmentLine implements Serializable {

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

    @OneToOne(
            mappedBy = "shipmentLine",
            cascade = CascadeType.REMOVE,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    private ShortAllocation shortAllocation;

    @Override
    public String toString() {
        return new StringBuilder()
                .append("{ id: ").append(id).append(",")
                .append("number: ").append(number).append(",")
                .append("shipment number: ").append(shipment.getNumber()).append(",")
                .append("wave number: ").append(wave.getNumber()).append(",")
                .append("order: ").append(orderLine.getOrder().getNumber()).append(",")
                .append("orderLine: ").append(orderLine).append(",")
                .append("quantity: ").append(quantity).append(",")
                .append("openQuantity: ").append(openQuantity).append(",")
                .append("inprocessQuantity: ").append(inprocessQuantity).append(",")
                .append("shippedQuantity: ").append(shippedQuantity).append("}")
                .toString();

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

    public ShortAllocation getShortAllocation() {
        return shortAllocation;
    }

    public void setShortAllocation(ShortAllocation shortAllocation) {
        this.shortAllocation = shortAllocation;
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
}

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

@Entity
@Table(name = "outbound_order_line")
public class OrderLine implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "outbound_order_line_id")
    @JsonProperty(value="id")
    private Long id;

    @Column(name = "number")
    private String number;

    @Column(name = "item_id")
    private Long itemId;

    @Transient
    private Item item;
    @Column(name = "warehouse_id")
    private Long warehouseId;

    @Transient
    private Warehouse warehouse;

    @Column(name = "expected_quantity")
    private Long expectedQuantity;


    @Column(name = "open_quantity")
    private Long openQuantity;

    @Column(name = "inprocess_quantity")
    private Long inprocessQuantity;

    @Column(name = "shipped_quantity")
    private Long shippedQuantity;


    // Specific the inventory status that
    // user ordered. For example, when return
    // to vendor, we may return DAMAGED inventory
    @Column(name = "inventory_status_id")
    private Long inventoryStatusId;

    @Transient
    private InventoryStatus inventoryStatus;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "outbound_order_id")
    private Order order;

    @Column(name = "carrier_id")
    private Long carrierId;

    @Transient
    private Carrier carrier;

    @Column(name = "carrier_service_level_id")
    private Long carrierServiceLevelId;

    @Transient
    private CarrierServiceLevel carrierServiceLevel;

    @Override
    public String toString() {
        return new StringBuilder()
                .append("{ id: ").append(id).append(",")
                .append("number: ").append(number).append(",")
                .append("order number: ").append(order == null ? "" : order.getNumber()).append(",")
                .append("itemId: ").append(itemId).append(",")
                .append("item number: ").append(item == null ? "" : item.getName()).append(",")
                .append("carrier ID: ").append(carrierId).append(",")
                .append("carrier: ").append(carrier == null ? "" : carrier.getName()).append(",")
                .append("Carrier Service Level Id: ").append(carrierServiceLevelId).append(",")
                .append("Carrier Service Level: ").append(carrierServiceLevel == null ? "" : carrierServiceLevel.getName()).append(",")
                .append("expectedQuantity: ").append(expectedQuantity).append(",")
                .append("openQuantity: ").append(openQuantity).append(",")
                .append("inprocessQuantity: ").append(inprocessQuantity).append(",")
                .append("shippedQuantity: ").append(shippedQuantity).append(",")
                .append("inventoryStatusId: ").append(inventoryStatusId).append(",")
                .append("inventoryStatus: ").append(inventoryStatus == null? "" : inventoryStatus.getName()).append("}")
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

    public Long getExpectedQuantity() {
        return expectedQuantity;
    }

    public void setExpectedQuantity(Long expectedQuantity) {
        this.expectedQuantity = expectedQuantity;
    }

    public Long getShippedQuantity() {
        return shippedQuantity;
    }

    public void setShippedQuantity(Long shippedQuantity) {
        this.shippedQuantity = shippedQuantity;
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

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }

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

    public String getOrderNumber() {
        return order.getNumber();
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

    public Long getCarrierId() {
        return carrierId;
    }

    public void setCarrierId(Long carrierId) {
        this.carrierId = carrierId;
    }

    public Carrier getCarrier() {
        return carrier;
    }

    public void setCarrier(Carrier carrier) {
        this.carrier = carrier;
    }

    public Long getCarrierServiceLevelId() {
        return carrierServiceLevelId;
    }

    public void setCarrierServiceLevelId(Long carrierServiceLevelId) {
        this.carrierServiceLevelId = carrierServiceLevelId;
    }

    public CarrierServiceLevel getCarrierServiceLevel() {
        return carrierServiceLevel;
    }

    public void setCarrierServiceLevel(CarrierServiceLevel carrierServiceLevel) {
        this.carrierServiceLevel = carrierServiceLevel;
    }
}

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
@Table(name = "outbound_order_line")
public class OrderLine  extends AuditibleEntity<String> implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "outbound_order_line_id")
    @JsonProperty(value="id")
    private Long id;

    @Column(name = "number")
    private String number;


    @Transient
    private String orderNumber;

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
    private Long inprocessQuantity = 0L;

    @Column(name = "production_plan_inprocess_quantity")
    private Long productionPlanInprocessQuantity = 0L;
    @Column(name = "production_plan_produced_quantity")
    private Long productionPlanProducedQuantity = 0L;

    @Column(name = "shipped_quantity")
    private Long shippedQuantity = 0L;


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


    @JsonIgnore
    @OneToMany(
            mappedBy = "orderLine",
            cascade = CascadeType.REMOVE,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    private List<ShipmentLine> shipmentLines = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(name="allocation_strategy_type")
    private AllocationStrategyType allocationStrategyType;

    @Column(name = "carrier_id")
    private Long carrierId;

    @Transient
    private Carrier carrier;

    @Column(name = "carrier_service_level_id")
    private Long carrierServiceLevelId;

    @Transient
    private CarrierServiceLevel carrierServiceLevel;


    // return quantity
    // request and actual
    @Column(name = "requested_return_quantity")
    private Long requestedReturnQuantity = 0L;
    @Column(name = "actual_return_quantity")
    private Long actualReturnQuantity = 0L;


    @Column(name = "quickbook_txnlineid")
    private String quickbookTxnLineID;

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
        return Objects.isNull(order) ? "" : order.getNumber();
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

    public List<ShipmentLine> getShipmentLines() {
        return shipmentLines;
    }

    public void setShipmentLines(List<ShipmentLine> shipmentLines) {
        this.shipmentLines = shipmentLines;
    }

    public Long getProductionPlanInprocessQuantity() {
        return productionPlanInprocessQuantity;
    }

    public void setProductionPlanInprocessQuantity(Long productionPlanInprocessQuantity) {
        this.productionPlanInprocessQuantity = productionPlanInprocessQuantity;
    }

    public Long getProductionPlanProducedQuantity() {
        return productionPlanProducedQuantity;
    }

    public void setProductionPlanProducedQuantity(Long productionPlanProducedQuantity) {
        this.productionPlanProducedQuantity = productionPlanProducedQuantity;
    }

    public AllocationStrategyType getAllocationStrategyType() {
        return allocationStrategyType;
    }

    public void setAllocationStrategyType(AllocationStrategyType allocationStrategyType) {
        this.allocationStrategyType = allocationStrategyType;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public Long getRequestedReturnQuantity() {
        return requestedReturnQuantity;
    }

    public void setRequestedReturnQuantity(Long requestedReturnQuantity) {
        this.requestedReturnQuantity = requestedReturnQuantity;
    }

    public Long getActualReturnQuantity() {
        return actualReturnQuantity;
    }

    public void setActualReturnQuantity(Long actualReturnQuantity) {
        this.actualReturnQuantity = actualReturnQuantity;
    }

    public String getQuickbookTxnLineID() {
        return quickbookTxnLineID;
    }

    public void setQuickbookTxnLineID(String quickbookTxnLineID) {
        this.quickbookTxnLineID = quickbookTxnLineID;
    }
}

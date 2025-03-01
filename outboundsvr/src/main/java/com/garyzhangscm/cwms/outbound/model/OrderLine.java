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
import com.fasterxml.jackson.annotation.JsonProperty;

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

    @OneToMany(
            mappedBy = "orderLine",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    private List<OrderLineBillableActivity> orderLineBillableActivities = new ArrayList<>();

    @JsonIgnore
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "outbound_order_id")
    private Order order;

    @Column(name = "non_allocatable")
    private Boolean nonAllocatable;


    // product id if shipped by hualei. This is
    // related to the carrier that ship the package
    @Column(name="hualei_product_id")
    private String hualeiProductId;

    @Column(name = "auto_request_shipping_label")
    private Boolean autoRequestShippingLabel;


    @JsonIgnore
    @OneToMany(
            mappedBy = "orderLine",
            cascade = CascadeType.REMOVE,
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


    @Column(name="color")
    private String color;

    @Column(name="product_size")
    private String productSize;

    @Column(name="style")
    private String style;

    // required inventory attribute
    @Column(name="inventory_attribute_1")
    private String inventoryAttribute1;
    @Column(name="inventory_attribute_2")
    private String inventoryAttribute2;
    @Column(name="inventory_attribute_3")
    private String inventoryAttribute3;
    @Column(name="inventory_attribute_4")
    private String inventoryAttribute4;
    @Column(name="inventory_attribute_5")
    private String inventoryAttribute5;

    // only allocate inventory that received by certain receipt
    @Column(name = "allocate_by_receipt_number")
    private String allocateByReceiptNumber;

    // when ship by parcel, whether we will need to
    // insure the package
    @Column(name = "parcel_insured")
    private Boolean parcelInsured;
    @Column(name = "parcel_insured_amount_per_unit")
    private Double parcelInsuredAmountPerUnit;
    // when ship by parcel, whether we will need to
    // insure the package
    @Column(name = "parcel_signature_required")
    private Boolean parcelSignatureRequired;


    // Right now we will get the client information from
    // the order, which means the order can
    // only allocate the inventory from the same owner of
    // the order. We may consider to change logic to allow
    // different owner for order and order line so that
    // the owner of the order is who get paid from the customer
    // while the owner of the order line ships the inventory
    // this may requires support from ERP as well
    @Transient
    private Long clientId;

    @Transient
    private Client client;

    // for report purpose
    @Transient
    private Long quantity;
    @Transient
    private Long quantityPerCase;
    @Transient
    private Double caseQuantity;

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
        OrderLine orderLine = (OrderLine) o;

        if (Objects.nonNull(id) && Objects.nonNull(orderLine.id) &&
                Objects.equals(id, orderLine.id)){
            return true;
        }
        return Objects.equals(order, orderLine.order) &&
                number.equalsIgnoreCase(orderLine.getNumber());
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, order, number);
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

    public String getInventoryAttribute1() {
        return inventoryAttribute1;
    }

    public void setInventoryAttribute1(String inventoryAttribute1) {
        this.inventoryAttribute1 = inventoryAttribute1;
    }

    public String getInventoryAttribute2() {
        return inventoryAttribute2;
    }

    public void setInventoryAttribute2(String inventoryAttribute2) {
        this.inventoryAttribute2 = inventoryAttribute2;
    }

    public String getInventoryAttribute3() {
        return inventoryAttribute3;
    }

    public void setInventoryAttribute3(String inventoryAttribute3) {
        this.inventoryAttribute3 = inventoryAttribute3;
    }

    public String getInventoryAttribute4() {
        return inventoryAttribute4;
    }

    public void setInventoryAttribute4(String inventoryAttribute4) {
        this.inventoryAttribute4 = inventoryAttribute4;
    }

    public String getInventoryAttribute5() {
        return inventoryAttribute5;
    }

    public void setInventoryAttribute5(String inventoryAttribute5) {
        this.inventoryAttribute5 = inventoryAttribute5;
    }

    public Warehouse getWarehouse() {
        return warehouse;
    }

    public void setWarehouse(Warehouse warehouse) {
        this.warehouse = warehouse;
    }

    public Boolean getAutoRequestShippingLabel() {
        return autoRequestShippingLabel;
    }

    public void setAutoRequestShippingLabel(Boolean autoRequestShippingLabel) {
        this.autoRequestShippingLabel = autoRequestShippingLabel;
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

    public Boolean getNonAllocatable() {
        return nonAllocatable;
    }

    public void setNonAllocatable(Boolean nonAllocatable) {
        this.nonAllocatable = nonAllocatable;
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

    public String getHualeiProductId() {
        return hualeiProductId;
    }

    public void setHualeiProductId(String hualeiProductId) {
        this.hualeiProductId = hualeiProductId;
    }

    public String getStyle() {
        return style;
    }

    public void setStyle(String style) {
        this.style = style;
    }

    public List<OrderLineBillableActivity> getOrderLineBillableActivities() {
        return orderLineBillableActivities;
    }

    public void setOrderLineBillableActivities(List<OrderLineBillableActivity> orderLineBillableActivities) {
        this.orderLineBillableActivities = orderLineBillableActivities;
    }

    public String getAllocateByReceiptNumber() {
        return allocateByReceiptNumber;
    }

    public void setAllocateByReceiptNumber(String allocateByReceiptNumber) {
        this.allocateByReceiptNumber = allocateByReceiptNumber;
    }

    public Boolean getParcelInsured() {
        return parcelInsured;
    }

    public void setParcelInsured(Boolean parcelInsured) {
        this.parcelInsured = parcelInsured;
    }

    public Double getParcelInsuredAmountPerUnit() {
        return parcelInsuredAmountPerUnit;
    }

    public void setParcelInsuredAmountPerUnit(Double parcelInsuredAmountPerUnit) {
        this.parcelInsuredAmountPerUnit = parcelInsuredAmountPerUnit;
    }

    public Boolean getParcelSignatureRequired() {
        return parcelSignatureRequired;
    }

    public void setParcelSignatureRequired(Boolean parcelSignatureRequired) {
        this.parcelSignatureRequired = parcelSignatureRequired;
    }

    public Long getClientId() {
        if (Objects.isNull(clientId) && Objects.nonNull(order)) {
            return order.getClientId();
        }
        return clientId;
    }

    public void setClientId(Long clientId) {
        this.clientId = clientId;
    }

    public Client getClient() {
        if (Objects.isNull(client) && Objects.nonNull(order)) {
            return order.getClient();
        }
        return client;
    }

    public void setClient(Client client) {
        this.client = client;
    }

    public long getQuantity() {
        if(Objects.nonNull(quantity)) {
            return quantity;
        }

        long quantity = 0;
        if (Objects.isNull(getShipmentLines()) || getShipmentLines().isEmpty()) {
            return 0;
        }
        for (ShipmentLine shipmentLine : shipmentLines) {
            quantity += shipmentLine.getPicks().stream().mapToLong(Pick::getQuantity).sum();
        }
        return quantity;
    }

    public void setQuantity(Long quantity) {
        this.quantity = quantity;
    }

    public long getQuantityPerCase() {
        return Objects.isNull(quantityPerCase) ? 0l : quantityPerCase;
    }

    public void setQuantityPerCase(Long quantityPerCase) {
        this.quantityPerCase = quantityPerCase;
    }

    public Double getCaseQuantity() {
        return Objects.isNull(caseQuantity) ? 0.0 : caseQuantity;
    }

    public void setCaseQuantity(Double caseQuantity) {
        this.caseQuantity = caseQuantity;
    }
}

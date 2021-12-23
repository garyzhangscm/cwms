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

package com.garyzhangscm.cwms.inbound.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.codehaus.jackson.annotate.JsonProperty;

import javax.persistence.*;


@Entity
@Table(name = "customer_return_order_line")
public class CustomerReturnOrderLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "customer_return_order_line_id")
    @JsonProperty(value="id")
    private Long id;

    @Column(name = "number")
    private String number;

    @Column(name = "warehouse_id")
    private Long warehouseId;

    @Transient
    private Warehouse warehouse;

    @Column(name = "item_id")
    private Long itemId;

    @Transient
    private Item item;

    @Column(name = "expected_quantity")
    private Long expectedQuantity;


    @Column(name = "received_quantity")
    private Long receivedQuantity = 0L;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_return_order_id")
    private CustomerReturnOrder customerReturnOrder;


    @Column(name = "over_receiving_quantity")
    private Long overReceivingQuantity = 0L;
    @Column(name = "over_receiving_percent")
    private Double overReceivingPercent = 0.0;

    @Column(name = "qc_quantity")
    private Long qcQuantity = 0l;

    @Column(name = "qc_percentage")
    private Double qcPercentage = 0.0;

    @Column(name = "qc_quantity_requested")
    private Long qcQuantityRequested = 0L;

    // filed for customer return
    @Column(name = "outbound_order_line_id")
    private Long outboundOrderLineId;


    @Transient
    private Long customerReturnOrderId;
    @Transient
    private String customerReturnOrderNumber;

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

    public Long getReceivedQuantity() {
        return receivedQuantity;
    }

    public void setReceivedQuantity(Long receivedQuantity) {
        this.receivedQuantity = receivedQuantity;
    }

    public CustomerReturnOrder getCustomerReturnOrder() {
        return customerReturnOrder;
    }

    public void setCustomerReturnOrder(CustomerReturnOrder customerReturnOrder) {
        this.customerReturnOrder = customerReturnOrder;
    }

    public Long getOverReceivingQuantity() {
        return overReceivingQuantity;
    }

    public void setOverReceivingQuantity(Long overReceivingQuantity) {
        this.overReceivingQuantity = overReceivingQuantity;
    }

    public Double getOverReceivingPercent() {
        return overReceivingPercent;
    }

    public void setOverReceivingPercent(Double overReceivingPercent) {
        this.overReceivingPercent = overReceivingPercent;
    }

    public Long getQcQuantity() {
        return qcQuantity;
    }

    public void setQcQuantity(Long qcQuantity) {
        this.qcQuantity = qcQuantity;
    }

    public Double getQcPercentage() {
        return qcPercentage;
    }

    public void setQcPercentage(Double qcPercentage) {
        this.qcPercentage = qcPercentage;
    }

    public Long getQcQuantityRequested() {
        return qcQuantityRequested;
    }

    public void setQcQuantityRequested(Long qcQuantityRequested) {
        this.qcQuantityRequested = qcQuantityRequested;
    }

    public Long getOutboundOrderLineId() {
        return outboundOrderLineId;
    }

    public void setOutboundOrderLineId(Long outboundOrderLineId) {
        this.outboundOrderLineId = outboundOrderLineId;
    }

    public Long getCustomerReturnOrderId() {
        return customerReturnOrderId;
    }

    public void setCustomerReturnOrderId(Long customerReturnOrderId) {
        this.customerReturnOrderId = customerReturnOrderId;
    }

    public String getCustomerReturnOrderNumber() {
        return customerReturnOrderNumber;
    }

    public void setCustomerReturnOrderNumber(String customerReturnOrderNumber) {
        this.customerReturnOrderNumber = customerReturnOrderNumber;
    }
}

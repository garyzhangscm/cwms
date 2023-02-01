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

package com.garyzhangscm.cwms.outbound.model;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.Serializable;
import java.util.Objects;


public class OrderLineConfirmation implements Serializable {

    private String number;

    private Long itemId;
    private String itemName;


    private Long warehouseId;

    private String warehouseName;

    private Long expectedQuantity;

    private Long openQuantity;

    private Long inprocessQuantity;

    private Long shippedQuantity;
    private Long orderShippedQuantity;

    private Long inventoryStatusId;
    private String inventoryStatusName;

    @JsonIgnore
    private OrderConfirmation order;

    private Long carrierId;

    private String carrierName;

    private Long carrierServiceLevelId;

    private String carrierServiceLevelName;

    private String quickbookTxnLineID;


    public OrderLineConfirmation() {}

    public OrderLineConfirmation(OrderLine orderLine) {
        this.number = orderLine.getNumber();

        this.itemId = orderLine.getItemId();
        if (Objects.nonNull(orderLine.getItem())) {
            this.itemName = orderLine.getItem().getName();
        }


        this.warehouseId = orderLine.getWarehouseId();
        if (Objects.nonNull(orderLine.getWarehouse())) {
            this.warehouseName = orderLine.getWarehouse().getName();
        }

        this.expectedQuantity = orderLine.getExpectedQuantity();

        this.openQuantity = orderLine.getOpenQuantity();

        this.inprocessQuantity = orderLine.getInprocessQuantity();

        this.shippedQuantity = orderLine.getShippedQuantity();

        this.inventoryStatusId = orderLine.getInventoryStatusId();
        if (Objects.nonNull(orderLine.getInventoryStatus())) {

            this.inventoryStatusName = orderLine.getInventoryStatus().getName();
        }


        this.carrierId = orderLine.getCarrierId();
        if (Objects.nonNull(orderLine.getCarrier())) {

            this.carrierName = orderLine.getCarrier().getName();
        }

        this.carrierServiceLevelId = orderLine.getCarrierServiceLevelId();
        if (Objects.nonNull(orderLine.getCarrierServiceLevel())) {

            this.carrierServiceLevelName = orderLine.getCarrierServiceLevel().getName();
        }

        setQuickbookTxnLineID(orderLine.getQuickbookTxnLineID());

    }

    public OrderLineConfirmation(ShipmentLine shipmentLine) {
        OrderLine orderLine = shipmentLine.getOrderLine();

        this.number = orderLine.getNumber();
        setQuickbookTxnLineID(orderLine.getQuickbookTxnLineID());

        this.itemId = orderLine.getItemId();
        if (Objects.nonNull(orderLine.getItem())) {
            this.itemName = orderLine.getItem().getName();
        }


        this.warehouseId = orderLine.getWarehouseId();
        if (Objects.nonNull(orderLine.getWarehouse())) {
            this.warehouseName = orderLine.getWarehouse().getName();
        }

        this.expectedQuantity = orderLine.getExpectedQuantity();

        this.openQuantity = orderLine.getOpenQuantity();

        this.inprocessQuantity = orderLine.getInprocessQuantity();

        this.shippedQuantity = shipmentLine.getShippedQuantity();
        this.orderShippedQuantity = orderLine.getShippedQuantity();

        this.inventoryStatusId = orderLine.getInventoryStatusId();
        if (Objects.nonNull(orderLine.getInventoryStatus())) {

            this.inventoryStatusName = orderLine.getInventoryStatus().getName();
        }


        this.carrierId = orderLine.getCarrierId();
        if (Objects.nonNull(orderLine.getCarrier())) {

            this.carrierName = orderLine.getCarrier().getName();
        }

        this.carrierServiceLevelId = orderLine.getCarrierServiceLevelId();
        if (Objects.nonNull(orderLine.getCarrierServiceLevel())) {

            this.carrierServiceLevelName = orderLine.getCarrierServiceLevel().getName();
        }

        setQuickbookTxnLineID(orderLine.getQuickbookTxnLineID());
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

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public Long getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(Long warehouseId) {
        this.warehouseId = warehouseId;
    }

    public String getWarehouseName() {
        return warehouseName;
    }

    public void setWarehouseName(String warehouseName) {
        this.warehouseName = warehouseName;
    }

    public Long getExpectedQuantity() {
        return expectedQuantity;
    }

    public void setExpectedQuantity(Long expectedQuantity) {
        this.expectedQuantity = expectedQuantity;
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

    public String getInventoryStatusName() {
        return inventoryStatusName;
    }

    public void setInventoryStatusName(String inventoryStatusName) {
        this.inventoryStatusName = inventoryStatusName;
    }

    public OrderConfirmation getOrder() {
        return order;
    }

    public void setOrder(OrderConfirmation order) {
        this.order = order;
    }

    public Long getCarrierId() {
        return carrierId;
    }

    public void setCarrierId(Long carrierId) {
        this.carrierId = carrierId;
    }

    public String getCarrierName() {
        return carrierName;
    }

    public void setCarrierName(String carrierName) {
        this.carrierName = carrierName;
    }

    public Long getCarrierServiceLevelId() {
        return carrierServiceLevelId;
    }

    public void setCarrierServiceLevelId(Long carrierServiceLevelId) {
        this.carrierServiceLevelId = carrierServiceLevelId;
    }

    public String getCarrierServiceLevelName() {
        return carrierServiceLevelName;
    }

    public void setCarrierServiceLevelName(String carrierServiceLevelName) {
        this.carrierServiceLevelName = carrierServiceLevelName;
    }

    public Long getOrderShippedQuantity() {
        return orderShippedQuantity;
    }

    public void setOrderShippedQuantity(Long orderShippedQuantity) {
        this.orderShippedQuantity = orderShippedQuantity;
    }

    public String getQuickbookTxnLineID() {
        return quickbookTxnLineID;
    }

    public void setQuickbookTxnLineID(String quickbookTxnLineID) {
        this.quickbookTxnLineID = quickbookTxnLineID;
    }
}

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

package com.garyzhangscm.cwms.integration.model;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.persistence.*;
import java.io.Serializable;
import java.time.ZoneId;
import java.time.ZonedDateTime;

@Entity
@Table(name = "integration_order_line_confirmation")
public class DBBasedOrderLineConfirmation extends AuditibleEntity<String> implements Serializable, IntegrationOrderLineConfirmationData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "integration_order_line_confirmation_id")
    @JsonProperty(value="id")
    private Long id;

    @Column(name = "number")
    private String number;

    @Column(name = "item_id")
    private Long itemId;
    @Column(name = "item_name")
    private String itemName;


    @Column(name = "warehouse_id")
    private Long warehouseId;

    @Column(name = "warehouse_name")
    private String warehouseName;

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

    @Column(name = "inventory_status_name")
    private String inventoryStatusName;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "integration_order_confirmation_id")
    private DBBasedOrderConfirmation order;



    @Column(name = "carrier_id")
    private Long carrierId;

    @Column(name = "carrier_name")
    private String carrierName;

    @Column(name = "carrier_service_level_id")
    private Long carrierServiceLevelId;

    @Column(name = "carrier_service_level_name")
    private String carrierServiceLevelName;


    @Column(name = "quickbook_txnlineid")
    private String quickbookTxnLineID;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private IntegrationStatus status;

    @Column(name = "error_message")
    private String errorMessage;


    public DBBasedOrderLineConfirmation(){}

    public DBBasedOrderLineConfirmation(OrderLineConfirmation orderLineConfirmation){

        setNumber(orderLineConfirmation.getNumber());
        setQuickbookTxnLineID(orderLineConfirmation.getQuickbookTxnLineID());

        setItemId(orderLineConfirmation.getItemId());
        setItemName(orderLineConfirmation.getItemName());

        setWarehouseId(orderLineConfirmation.getWarehouseId());
        setWarehouseName(orderLineConfirmation.getWarehouseName());

        setExpectedQuantity(orderLineConfirmation.getExpectedQuantity());
        setOpenQuantity(orderLineConfirmation.getOpenQuantity());
        setInprocessQuantity(orderLineConfirmation.getInprocessQuantity());
        setShippedQuantity(orderLineConfirmation.getShippedQuantity());

        setInventoryStatusId(orderLineConfirmation.getInventoryStatusId());
        setInventoryStatusName(orderLineConfirmation.getInventoryStatusName());

        setCarrierId(orderLineConfirmation.getCarrierId());
        setCarrierName(orderLineConfirmation.getCarrierName());

        setCarrierServiceLevelId(orderLineConfirmation.getCarrierServiceLevelId());
        setCarrierServiceLevelName(orderLineConfirmation.getCarrierServiceLevelName());


        setCreatedTime(ZonedDateTime.now(ZoneId.of("UTC")));
        setStatus(IntegrationStatus.PENDING);
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

    @Override
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    @Override
    public Long getItemId() {
        return itemId;
    }

    public void setItemId(Long itemId) {
        this.itemId = itemId;
    }

    @Override
    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    @Override
    public Long getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(Long warehouseId) {
        this.warehouseId = warehouseId;
    }

    @Override
    public String getWarehouseName() {
        return warehouseName;
    }

    public void setWarehouseName(String warehouseName) {
        this.warehouseName = warehouseName;
    }

    @Override
    public Long getExpectedQuantity() {
        return expectedQuantity;
    }

    public void setExpectedQuantity(Long expectedQuantity) {
        this.expectedQuantity = expectedQuantity;
    }

    @Override
    public Long getOpenQuantity() {
        return openQuantity;
    }

    public void setOpenQuantity(Long openQuantity) {
        this.openQuantity = openQuantity;
    }

    @Override
    public Long getInprocessQuantity() {
        return inprocessQuantity;
    }

    public void setInprocessQuantity(Long inprocessQuantity) {
        this.inprocessQuantity = inprocessQuantity;
    }

    @Override
    public Long getShippedQuantity() {
        return shippedQuantity;
    }

    public void setShippedQuantity(Long shippedQuantity) {
        this.shippedQuantity = shippedQuantity;
    }

    @Override
    public Long getInventoryStatusId() {
        return inventoryStatusId;
    }

    public void setInventoryStatusId(Long inventoryStatusId) {
        this.inventoryStatusId = inventoryStatusId;
    }

    @Override
    public String getInventoryStatusName() {
        return inventoryStatusName;
    }

    public void setInventoryStatusName(String inventoryStatusName) {
        this.inventoryStatusName = inventoryStatusName;
    }

    @Override
    public DBBasedOrderConfirmation getOrder() {
        return order;
    }

    public void setOrder(DBBasedOrderConfirmation order) {
        this.order = order;
    }

    @Override
    public Long getCarrierId() {
        return carrierId;
    }

    public void setCarrierId(Long carrierId) {
        this.carrierId = carrierId;
    }

    @Override
    public String getCarrierName() {
        return carrierName;
    }

    public void setCarrierName(String carrierName) {
        this.carrierName = carrierName;
    }

    @Override
    public Long getCarrierServiceLevelId() {
        return carrierServiceLevelId;
    }

    public void setCarrierServiceLevelId(Long carrierServiceLevelId) {
        this.carrierServiceLevelId = carrierServiceLevelId;
    }

    @Override
    public String getCarrierServiceLevelName() {
        return carrierServiceLevelName;
    }

    public void setCarrierServiceLevelName(String carrierServiceLevelName) {
        this.carrierServiceLevelName = carrierServiceLevelName;
    }

    public String getQuickbookTxnLineID() {
        return quickbookTxnLineID;
    }

    public void setQuickbookTxnLineID(String quickbookTxnLineID) {
        this.quickbookTxnLineID = quickbookTxnLineID;
    }

    @Override
    public IntegrationStatus getStatus() {
        return status;
    }

    public void setStatus(IntegrationStatus status) {
        this.status = status;
    }


    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}

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



import org.codehaus.jackson.annotate.JsonProperty;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "integration_inventory_attribute_change_confirmation")
public class DBBasedInventoryAttributeChangeConfirmation implements Serializable, IntegrationInventoryAttributeChangeConfirmationData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "integration_inventory_attribute_change_confirmation_id")
    @JsonProperty(value="id")
    private Long id;

    @Column(name = "warehouse_id")
    private Long warehouseId;

    @Column(name = "warehouse_name")
    private String warehouseName;

    @Column(name = "client_id")
    private Long clientId;

    @Column(name = "client_name")
    private String clientName;


    @Column(name = "item_id")
    private Long itemId;
    @Column(name = "item_name")
    private String itemName;

    // Specific the inventory status that
    // user ordered. For example, when return
    // to vendor, we may return DAMAGED inventory
    @Column(name = "inventory_status_id")
    private Long inventoryStatusId;

    @Column(name = "inventory_status_name")
    private String inventoryStatusName;

    @Column(name = "quantity")
    private Long quantity;




    @Column(name = "attribute_name")
    private String attributeName;
    @Column(name = "original_value")
    private String originalValue;
    @Column(name = "new_value")
    private String newValue;



    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private IntegrationStatus status;
    @Column(name = "insert_time")
    private LocalDateTime insertTime;
    @Column(name = "last_update_time")
    private LocalDateTime lastUpdateTime;
    @Column(name = "error_message")
    private String errorMessage;


    public DBBasedInventoryAttributeChangeConfirmation(){}

    public DBBasedInventoryAttributeChangeConfirmation(InventoryAttributeChangeConfirmation inventoryAttributeChangeConfirmation){

        setItemId(inventoryAttributeChangeConfirmation.getItem().getId());
        setItemName(inventoryAttributeChangeConfirmation.getItem().getName());

        setWarehouseId(inventoryAttributeChangeConfirmation.getWarehouse().getId());
        setWarehouseName(inventoryAttributeChangeConfirmation.getWarehouse().getName());

        setQuantity(inventoryAttributeChangeConfirmation.getQuantity());

        setInventoryStatusId(inventoryAttributeChangeConfirmation.getInventoryStatus().getId());
        setInventoryStatusName(inventoryAttributeChangeConfirmation.getInventoryStatus().getName());

        // Client is optional
        if (Objects.nonNull(inventoryAttributeChangeConfirmation.getClient())) {

            setClientId(inventoryAttributeChangeConfirmation.getClient().getId());
            setClientName(inventoryAttributeChangeConfirmation.getClient().getName());
        }

        setAttributeName(inventoryAttributeChangeConfirmation.getAttributeName());
        setOriginalValue(inventoryAttributeChangeConfirmation.getOriginalValue());
        setNewValue(inventoryAttributeChangeConfirmation.getNewValue());
    }

    @Override
    public String toString() {
        return "DBBasedInventoryAttributeChangeConfirmation{" +
                "id=" + id +
                ", itemId=" + itemId +
                ", itemName='" + itemName + '\'' +
                ", warehouseId=" + warehouseId +
                ", warehouseName='" + warehouseName + '\'' +
                ", quantity=" + quantity +
                ", inventoryStatusId=" + inventoryStatusId +
                ", inventoryStatusName='" + inventoryStatusName + '\'' +
                ", clientId=" + clientId +
                ", clientName='" + clientName + '\'' +
                ", attributeName='" + attributeName + '\'' +
                ", originalValue='" + originalValue + '\'' +
                ", newValue='" + newValue + '\'' +
                ", status=" + status +
                ", insertTime=" + insertTime +
                ", lastUpdateTime=" + lastUpdateTime +
                '}';
    }

    @Override
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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
    public Long getQuantity() {
        return quantity;
    }

    public void setQuantity(Long quantity) {
        this.quantity = quantity;
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
    public Long getClientId() {
        return clientId;
    }

    public void setClientId(Long clientId) {
        this.clientId = clientId;
    }

    @Override
    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    @Override
    public String getAttributeName() {
        return attributeName;
    }

    public void setAttributeName(String attributeName) {
        this.attributeName = attributeName;
    }

    @Override
    public String getOriginalValue() {
        return originalValue;
    }

    public void setOriginalValue(String originalValue) {
        this.originalValue = originalValue;
    }

    @Override
    public String getNewValue() {
        return newValue;
    }

    public void setNewValue(String newValue) {
        this.newValue = newValue;
    }

    @Override
    public IntegrationStatus getStatus() {
        return status;
    }

    public void setStatus(IntegrationStatus status) {
        this.status = status;
    }

    @Override
    public LocalDateTime getInsertTime() {
        return insertTime;
    }

    public void setInsertTime(LocalDateTime insertTime) {
        this.insertTime = insertTime;
    }

    @Override
    public LocalDateTime getLastUpdateTime() {
        return lastUpdateTime;
    }

    public void setLastUpdateTime(LocalDateTime lastUpdateTime) {
        this.lastUpdateTime = lastUpdateTime;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}

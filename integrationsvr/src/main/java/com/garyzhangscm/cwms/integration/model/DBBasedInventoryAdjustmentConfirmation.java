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



import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.codehaus.jackson.annotate.JsonProperty;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "integration_inventory_adjustment_confirmation")
public class DBBasedInventoryAdjustmentConfirmation extends AuditibleEntity<String> implements Serializable, IntegrationInventoryAdjustmentConfirmationData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "integration_inventory_adjustment_confirmation_id")
    @JsonProperty(value="id")
    private Long id;

    @Column(name = "item_id")
    private Long itemId;
    @Column(name = "item_name")
    private String itemName;

    @Column(name = "warehouse_id")
    private Long warehouseId;

    @Column(name = "warehouse_name")
    private String warehouseName;

    @Column(name = "company_id")
    private Long companyId;

    @Column(name = "company_code")
    private String companyCode;

    @Column(name = "adjust_quantity")
    private Long adjustQuantity;

    // Specific the inventory status that
    // user ordered. For example, when return
    // to vendor, we may return DAMAGED inventory
    @Column(name = "inventory_status_id")
    private Long inventoryStatusId;

    @Column(name = "inventory_status_name")
    private String inventoryStatusName;


    @Column(name = "client_id")
    private Long clientId;

    @Column(name = "client_name")
    private String clientName;



    @Column(name = "inventory_quantity_change_type")
    @Enumerated(EnumType.STRING)
    private InventoryQuantityChangeType inventoryQuantityChangeType;

    @Column(name = "document_number")
    private String documentNumber;

    @Column(name = "comment")
    private String comment;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private IntegrationStatus status;

    @Column(name = "error_message")
    private String errorMessage;


    public DBBasedInventoryAdjustmentConfirmation(){}

    public DBBasedInventoryAdjustmentConfirmation(InventoryAdjustmentConfirmation inventoryAdjustmentConfirmation){

        setItemId(inventoryAdjustmentConfirmation.getItem().getId());
        setItemName(inventoryAdjustmentConfirmation.getItem().getName());

        setWarehouseId(inventoryAdjustmentConfirmation.getWarehouse().getId());
        setWarehouseName(inventoryAdjustmentConfirmation.getWarehouse().getName());

        setAdjustQuantity(inventoryAdjustmentConfirmation.getAdjustQuantity());

        setInventoryStatusId(inventoryAdjustmentConfirmation.getInventoryStatus().getId());
        setInventoryStatusName(inventoryAdjustmentConfirmation.getInventoryStatus().getName());
        setInventoryQuantityChangeType(inventoryAdjustmentConfirmation.getInventoryQuantityChangeType());
        setDocumentNumber(inventoryAdjustmentConfirmation.getDocumentNumber());
        setComment(inventoryAdjustmentConfirmation.getComment());

        // Client is optional
        if (Objects.nonNull(inventoryAdjustmentConfirmation.getClient())) {

            setClientId(inventoryAdjustmentConfirmation.getClient().getId());
            setClientName(inventoryAdjustmentConfirmation.getClient().getName());
        }

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
    public Long getAdjustQuantity() {
        return adjustQuantity;
    }

    public void setAdjustQuantity(Long adjustQuantity) {
        this.adjustQuantity = adjustQuantity;
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
    public IntegrationStatus getStatus() {
        return status;
    }

    public void setStatus(IntegrationStatus status) {
        this.status = status;
    }


    public Long getCompanyId() {
        return companyId;
    }

    public void setCompanyId(Long companyId) {
        this.companyId = companyId;
    }

    public String getCompanyCode() {
        return companyCode;
    }

    public void setCompanyCode(String companyCode) {
        this.companyCode = companyCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public InventoryQuantityChangeType getInventoryQuantityChangeType() {
        return inventoryQuantityChangeType;
    }

    public void setInventoryQuantityChangeType(InventoryQuantityChangeType inventoryQuantityChangeType) {
        this.inventoryQuantityChangeType = inventoryQuantityChangeType;
    }

    public String getDocumentNumber() {
        return documentNumber;
    }

    public void setDocumentNumber(String documentNumber) {
        this.documentNumber = documentNumber;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}

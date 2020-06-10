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

package com.garyzhangscm.cwms.adminserver.model.wms;


import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.codehaus.jackson.annotate.JsonProperty;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;

public class InventoryAdjustmentRequest implements Serializable {
    private Long id;

    private Long inventoryId;

    private String lpn;

    private Long locationId;

    private Location location;

    private Item item;

    private ItemPackageType itemPackageType;

    private Long quantity;

    private Long newQuantity;

    private Boolean virtual;

    private InventoryStatus inventoryStatus;
    private Long warehouseId;

    private InventoryQuantityChangeType inventoryQuantityChangeType;

    private InventoryAdjustmentRequestStatus status = InventoryAdjustmentRequestStatus.PENDING;


    private String requestedByUsername;

    private LocalDateTime requestedByDateTime;


    private String processedByUsername;

    private LocalDateTime processedByDateTime;

    private String documentNumber;
    private String comment;


    public InventoryAdjustmentRequest() {

    }

    public InventoryAdjustmentRequest(Inventory inventory, Long newQuantity,
                                      InventoryQuantityChangeType inventoryQuantityChangeType,
                                      String username,
                                      String documentNumber, String comment) {
        setInventoryId(inventory.getId());
        setLpn(inventory.getLpn());
        setLocationId(inventory.getLocationId());
        setItem(inventory.getItem());
        setItemPackageType(inventory.getItemPackageType());
        setQuantity(inventory.getQuantity());
        setNewQuantity(newQuantity);
        setVirtual(inventory.getVirtual());
        setInventoryStatus(inventory.getInventoryStatus());
        setWarehouseId(inventory.getWarehouseId());
        setInventoryQuantityChangeType(inventoryQuantityChangeType);
        setStatus(InventoryAdjustmentRequestStatus.PENDING);
        setRequestedByUsername(username);
        setRequestedByDateTime(LocalDateTime.now());
        setDocumentNumber(documentNumber);
        setComment(comment);
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

    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }

    public String getLpn() {
        return lpn;
    }

    public void setLpn(String lpn) {
        this.lpn = lpn;
    }

    public Long getLocationId() {
        return locationId;
    }

    public void setLocationId(Long locationId) {
        this.locationId = locationId;
    }

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public ItemPackageType getItemPackageType() {
        return itemPackageType;
    }

    public void setItemPackageType(ItemPackageType itemPackageType) {
        this.itemPackageType = itemPackageType;
    }

    public Long getQuantity() {
        return quantity;
    }

    public void setQuantity(Long quantity) {
        this.quantity = quantity;
    }

    public Long getNewQuantity() {
        return newQuantity;
    }

    public void setNewQuantity(Long newQuantity) {
        this.newQuantity = newQuantity;
    }

    public Boolean getVirtual() {
        return virtual;
    }

    public void setVirtual(Boolean virtual) {
        this.virtual = virtual;
    }

    public InventoryStatus getInventoryStatus() {
        return inventoryStatus;
    }

    public void setInventoryStatus(InventoryStatus inventoryStatus) {
        this.inventoryStatus = inventoryStatus;
    }

    public Long getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(Long warehouseId) {
        this.warehouseId = warehouseId;
    }

    public InventoryQuantityChangeType getInventoryQuantityChangeType() {
        return inventoryQuantityChangeType;
    }

    public void setInventoryQuantityChangeType(InventoryQuantityChangeType inventoryQuantityChangeType) {
        this.inventoryQuantityChangeType = inventoryQuantityChangeType;
    }

    public InventoryAdjustmentRequestStatus getStatus() {
        return status;
    }

    public void setStatus(InventoryAdjustmentRequestStatus status) {
        this.status = status;
    }

    public String getRequestedByUsername() {
        return requestedByUsername;
    }

    public void setRequestedByUsername(String requestedByUsername) {
        this.requestedByUsername = requestedByUsername;
    }

    public LocalDateTime getRequestedByDateTime() {
        return requestedByDateTime;
    }

    public void setRequestedByDateTime(LocalDateTime requestedByDateTime) {
        this.requestedByDateTime = requestedByDateTime;
    }

    public String getProcessedByUsername() {
        return processedByUsername;
    }

    public void setProcessedByUsername(String processedByUsername) {
        this.processedByUsername = processedByUsername;
    }

    public LocalDateTime getProcessedByDateTime() {
        return processedByDateTime;
    }

    public void setProcessedByDateTime(LocalDateTime processedByDateTime) {
        this.processedByDateTime = processedByDateTime;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public Long getInventoryId() {
        return inventoryId;
    }

    public void setInventoryId(Long inventoryId) {
        this.inventoryId = inventoryId;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getDocumentNumber() {
        return documentNumber;
    }

    public void setDocumentNumber(String documentNumber) {
        this.documentNumber = documentNumber;
    }
}

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

package com.garyzhangscm.cwms.inventory.model;


import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.codehaus.jackson.annotate.JsonProperty;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

@Entity
@Table(name = "inventory_adjustment_request")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class InventoryAdjustmentRequest extends AuditibleEntity<String> implements Serializable {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "inventory_adjustment_request_id")
    @JsonProperty(value="id")
    private Long id;

    @Column(name = "inventory_id")
    private Long inventoryId;

    @Column(name = "lpn")
    private String lpn;

    @Column(name = "location_id")
    private Long locationId;


    @Transient
    private Location location;

    @ManyToOne
    @JoinColumn(name="item_id")
    private Item item;

    @ManyToOne
    @JoinColumn(name="item_package_type_id")
    private ItemPackageType itemPackageType;

    @Column(name = "quantity")
    private Long quantity;

    @Column(name = "new_quantity")
    private Long newQuantity;

    @Column(name = "virtual_inventory")
    private Boolean virtual;

    @ManyToOne
    @JoinColumn(name="inventory_status_id")
    private InventoryStatus inventoryStatus;

    @Column(name = "warehouse_id")
    private Long warehouseId;

    @Column(name = "inventory_quantity_change_type")
    private InventoryQuantityChangeType inventoryQuantityChangeType;

    @Column(name = "status")
    private InventoryAdjustmentRequestStatus status = InventoryAdjustmentRequestStatus.PENDING;


    // Requested by
    @Column(name = "requested_by_username")
    private String requestedByUsername;

    // Request Date
    @Column(name = "requested_by_datetime")
    @JsonDeserialize(using = CustomZonedDateTimeDeserializer.class)
    @JsonSerialize(using = CustomZonedDateTimeSerializer.class)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private ZonedDateTime requestedByDateTime;
    // private LocalDateTime requestedByDateTime;


    // Approved or Denied by
    @Column(name = "processed_by_username")
    private String processedByUsername;

    // Approved or Denied Date
    @Column(name = "processed_by_datetime")
    @JsonDeserialize(using = CustomZonedDateTimeDeserializer.class)
    @JsonSerialize(using = CustomZonedDateTimeSerializer.class)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private ZonedDateTime processedByDateTime;
    // private LocalDateTime processedByDateTime;

    // Approved or Denied comment
    @Column(name = "document_number")
    private String documentNumber;
    // Approved or Denied comment
    @Column(name = "comment")
    private String comment;



    @Column(name = "reason_code_id")
    private Long reasonCodeId;

    @Transient
    private ReasonCode reasonCode;


    public Long getId() {
        return id;
    }

    public InventoryAdjustmentRequest() {

    }

    public InventoryAdjustmentRequest(Inventory inventory,  Long newQuantity,
                                      InventoryQuantityChangeType inventoryQuantityChangeType,
                                      String username,
                                      String documentNumber, String comment, Long reasonCodeId) {
        this(inventory, inventory.getQuantity(),newQuantity,
                inventoryQuantityChangeType,
                username,
                documentNumber,
                comment, reasonCodeId);
    }
    public InventoryAdjustmentRequest(Inventory inventory, Long oldQuantity, Long newQuantity,
                                      InventoryQuantityChangeType inventoryQuantityChangeType,
                                      String username,
                                      String documentNumber, String comment, Long reasonCodeId) {
        setInventoryId(inventory.getId());
        setLpn(inventory.getLpn());
        setLocationId(inventory.getLocationId());
        setItem(inventory.getItem());
        setItemPackageType(inventory.getItemPackageType());
        setQuantity(oldQuantity);
        setNewQuantity(newQuantity);
        setVirtual(inventory.getVirtual());
        setInventoryStatus(inventory.getInventoryStatus());
        setWarehouseId(inventory.getWarehouseId());
        setInventoryQuantityChangeType(inventoryQuantityChangeType);
        setStatus(InventoryAdjustmentRequestStatus.PENDING);
        setRequestedByUsername(username);
        setRequestedByDateTime(ZonedDateTime.now(ZoneOffset.UTC));
        setDocumentNumber(documentNumber);
        setComment(comment);
        this.reasonCodeId = reasonCodeId;
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

    public ZonedDateTime getRequestedByDateTime() {
        return requestedByDateTime;
    }

    public void setRequestedByDateTime(ZonedDateTime requestedByDateTime) {
        this.requestedByDateTime = requestedByDateTime;
    }

    public String getProcessedByUsername() {
        return processedByUsername;
    }

    public void setProcessedByUsername(String processedByUsername) {
        this.processedByUsername = processedByUsername;
    }

    public ZonedDateTime getProcessedByDateTime() {
        return processedByDateTime;
    }

    public void setProcessedByDateTime(ZonedDateTime processedByDateTime) {
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

    public Long getReasonCodeId() {
        return reasonCodeId;
    }

    public void setReasonCodeId(Long reasonCodeId) {
        this.reasonCodeId = reasonCodeId;
    }

    public ReasonCode getReasonCode() {
        return reasonCode;
    }

    public void setReasonCode(ReasonCode reasonCode) {
        this.reasonCode = reasonCode;
    }
}

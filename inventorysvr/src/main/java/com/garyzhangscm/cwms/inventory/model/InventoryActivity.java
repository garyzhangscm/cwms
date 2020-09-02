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


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.annotate.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


/**
 * A class to hold the activities history that related to inventory
 * examples:
 * 1. inventory move
 * 2. inventory quantity change
 * 3. inventory status change
 * 4. inventory attribute change
 * 5. inventory consolidation
 * 6. receiving
 * 7. picking
 * 8. loading / shipping
 * 9. count up / down
 */
@Entity
@Table(name = "inventory_activity")
public class InventoryActivity extends AuditibleEntity<String> implements Serializable {

    private static final Logger logger = LoggerFactory.getLogger(InventoryActivity.class);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "inventory_activity_id")
    @JsonProperty(value="id")
    private Long id;

    @Column(name = "transaction_id")
    private String transactionId;

    @Column(name = "transaction_group_id")
    private String transactionGroupId;

    /**
     * All the inventory attribute
     */
    @Column(name = "lpn")
    private String lpn;

    @Column(name = "location_id")
    private Long locationId;

    @Transient
    private Location location;

    @Column(name = "pick_id")
    private Long pickId;

    @Transient
    private Pick pick;

    @Column(name = "receipt_id")
    private Long receiptId;

    @ManyToOne
    @JoinColumn(name="item_id")
    private Item item;

    @ManyToOne
    @JoinColumn(name="item_package_type_id")
    private ItemPackageType itemPackageType;

    @Column(name = "quantity")
    private Long quantity;

    @Column(name = "virtual_inventory")
    private Boolean virtual;

    @ManyToOne
    @JoinColumn(name="inventory_status_id")
    private InventoryStatus inventoryStatus;

    @Column(name = "warehouse_id")
    private Long warehouseId;

    @Transient
    private Warehouse warehouse;


    /**
     * Activity type: Movement / Adjustment / Receiving / Picking / etc
     */
    @Column(name = "type")
    @com.fasterxml.jackson.annotation.JsonProperty(value="type")
    private InventoryActivityType inventoryActivityType;

    @Column(name = "activity_datetime")
    private LocalDateTime activityDateTime;

    /**
     * User who carry out this activity
     */
    @Column(name = "username")
    private String username;

    /**
     * value type: which attribute is changed for the inventory.
     *     for example: location / inventory attribute / inventory status
     * from_value & to_value:
     *     the value of the key is changed from from_value to to_value
     */
    @Column(name = "value_type")
    private String valueType;

    @Column(name = "from_value")
    private String fromValue;
    @Column(name = "to_value")
    private String toValue;

    /**
     * In case it is a document related activity, save the number here
     * For example, if it is a pick, this will be the pick number.
     * if it is a receiving activity, this will be the receipt number
     */
    @Column(name = "document_number")
    private String documentNumber;

    @Column(name = "comment")
    private String comment;

    public InventoryActivity(){}

    public InventoryActivity(Inventory inventory, InventoryActivityType inventoryActivityType,
                             String transactionId,
                             String transactionGroupId,
                             LocalDateTime activityDateTime, String username,
                             String valueType, String fromValue, String toValue,
                             String documentNumber, String comment) {
        setLpn(inventory.getLpn());
        setTransactionId(transactionId);
        setTransactionGroupId(transactionGroupId);
        setLocationId(inventory.getLocationId());
        setLocation(inventory.getLocation());
        setPickId(inventory.getPickId());
        setPick(inventory.getPick());
        setReceiptId(inventory.getReceiptId());
        setItem(inventory.getItem());
        setItemPackageType(inventory.getItemPackageType());
        setQuantity(inventory.getQuantity());
        setVirtual(inventory.getVirtual());
        setInventoryStatus(inventory.getInventoryStatus());
        setWarehouseId(inventory.getWarehouseId());
        setWarehouse(inventory.getWarehouse());

        this.inventoryActivityType = inventoryActivityType;
        this.activityDateTime = activityDateTime;
        this.username = username;
        this.valueType = valueType;
        this.fromValue = fromValue;
        this.toValue = toValue;
        this.documentNumber = documentNumber;
        this.comment = comment;
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

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public Long getPickId() {
        return pickId;
    }

    public void setPickId(Long pickId) {
        this.pickId = pickId;
    }

    public Pick getPick() {
        return pick;
    }

    public void setPick(Pick pick) {
        this.pick = pick;
    }

    public Long getReceiptId() {
        return receiptId;
    }

    public void setReceiptId(Long receiptId) {
        this.receiptId = receiptId;
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

    public Warehouse getWarehouse() {
        return warehouse;
    }

    public void setWarehouse(Warehouse warehouse) {
        this.warehouse = warehouse;
    }

    public InventoryActivityType getInventoryActivityType() {
        return inventoryActivityType;
    }

    public void setInventoryActivityType(InventoryActivityType inventoryActivityType) {
        this.inventoryActivityType = inventoryActivityType;
    }

    public LocalDateTime getActivityDateTime() {
        return activityDateTime;
    }

    public void setActivityDateTime(LocalDateTime activityDateTime) {
        this.activityDateTime = activityDateTime;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getDocumentNumber() {
        return documentNumber;
    }

    public void setDocumentNumber(String documentNumber) {
        this.documentNumber = documentNumber;
    }

    public String getValueType() {
        return valueType;
    }

    public void setValueType(String valueType) {
        this.valueType = valueType;
    }

    public String getFromValue() {
        return fromValue;
    }

    public void setFromValue(String fromValue) {
        this.fromValue = fromValue;
    }

    public String getToValue() {
        return toValue;
    }

    public void setToValue(String toValue) {
        this.toValue = toValue;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getTransactionGroupId() {
        return transactionGroupId;
    }

    public void setTransactionGroupId(String transactionGroupId) {
        this.transactionGroupId = transactionGroupId;
    }
}

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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.persistence.*;
import java.io.Serializable;
import java.time.ZonedDateTime;


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


    @Column(name = "client_id")
    private Long clientId;

    @Transient
    private Client client;


    @Column(name = "reason_code_id")
    private Long reasonCodeId;

    @Transient
    private ReasonCode reasonCode;

    /**
     * Activity type: Movement / Adjustment / Receiving / Picking / etc
     */
    @Column(name = "type")
    @com.fasterxml.jackson.annotation.JsonProperty(value="type")
    @Enumerated(EnumType.STRING)
    private InventoryActivityType inventoryActivityType;

    @Column(name = "activity_datetime")
    @JsonDeserialize(using = CustomZonedDateTimeDeserializer.class)
    // @JsonSerialize(using = ZonedDateTimeSerializer.class)
    @JsonSerialize(using = CustomZonedDateTimeSerializer.class)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private ZonedDateTime activityDateTime;

    /**
     * User who carry out this activity
     */
    @Column(name = "username")
    private String username;

    @Column(name = "rf_code")
    private String rfCode;


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

    @Column(name="color")
    private String color;

    @Column(name="product_size")
    private String productSize;

    @Column(name="style")
    private String style;

    @Column(name="attribute_1")
    private String attribute1;
    @Column(name="attribute_2")
    private String attribute2;
    @Column(name="attribute_3")
    private String attribute3;
    @Column(name="attribute_4")
    private String attribute4;
    @Column(name="attribute_5")
    private String attribute5;

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
                             ZonedDateTime activityDateTime, String username,
                             String valueType, String fromValue, String toValue,
                             String documentNumber, String comment,
                             String rfCode, Long reasonCodeId) {
        setLpn(inventory.getLpn());
        setClientId(inventory.getClientId());
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

        setColor(inventory.getColor());
        setStyle(inventory.getStyle());
        setProductSize(inventory.getProductSize());
        setAttribute1(inventory.getAttribute1());
        setAttribute2(inventory.getAttribute2());
        setAttribute3(inventory.getAttribute3());
        setAttribute4(inventory.getAttribute4());
        setAttribute5(inventory.getAttribute5());
        this.rfCode = rfCode;

        this.inventoryActivityType = inventoryActivityType;
        this.activityDateTime = activityDateTime;
        this.username = username;
        this.valueType = valueType;
        this.fromValue = fromValue;
        this.toValue = toValue;
        this.documentNumber = documentNumber;
        this.comment = comment;

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

    public String getRfCode() {
        return rfCode;
    }

    public void setRfCode(String rfCode) {
        this.rfCode = rfCode;
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

    public ZonedDateTime getActivityDateTime() {
        return activityDateTime;
    }

    public void setActivityDateTime(ZonedDateTime activityDateTime) {
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

    public Long getClientId() {
        return clientId;
    }

    public void setClientId(Long clientId) {
        this.clientId = clientId;
    }

    public Client getClient() {
        return client;
    }

    public void setClient(Client client) {
        this.client = client;
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

    public String getStyle() {
        return style;
    }

    public void setStyle(String style) {
        this.style = style;
    }

    public String getAttribute1() {
        return attribute1;
    }

    public void setAttribute1(String attribute1) {
        this.attribute1 = attribute1;
    }

    public String getAttribute2() {
        return attribute2;
    }

    public void setAttribute2(String attribute2) {
        this.attribute2 = attribute2;
    }

    public String getAttribute3() {
        return attribute3;
    }

    public void setAttribute3(String attribute3) {
        this.attribute3 = attribute3;
    }

    public String getAttribute4() {
        return attribute4;
    }

    public void setAttribute4(String attribute4) {
        this.attribute4 = attribute4;
    }

    public String getAttribute5() {
        return attribute5;
    }

    public void setAttribute5(String attribute5) {
        this.attribute5 = attribute5;
    }
}

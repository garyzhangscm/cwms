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
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.persistence.*;


@Entity
@Table(name = "receiving_transaction")
public class ReceivingTransaction extends AuditibleEntity<String>{


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "receiving_transaction_id")
    @JsonProperty(value="id")
    private Long id;

    @Column(name = "warehouse_id")
    private Long warehouseId;


    @ManyToOne
    @JoinColumn(name = "receipt_id")
    @JsonIgnore
    private Receipt receipt;


    @ManyToOne
    @JoinColumn(name = "receipt_line_id")
    @JsonIgnore
    private ReceiptLine receiptLine;


    @Column(name = "type")
    @Enumerated(EnumType.STRING)
    private ReceivingTransactionType type;


    @Column(name = "lpn")
    private String lpn;


    @Column(name = "item_id")
    private Long itemId;

    @Transient
    private Item item;

    @Column(name = "quantity")
    private Long quantity;

    @Column(name = "username")
    private String username;
    @Column(name = "rf_code")
    private String rfCode;

    @Column(name="color")
    private String color;

    @Column(name="product_size")
    private String productSize;

    @Column(name="style")
    private String style;


    @Column(name="item_package_type_id")
    private Long itemPackageTypeId;
    @Transient
    private ItemPackageType itemPackageType;

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

    @Column(name="inventory_status_id")
    private Long inventoryStatusId;


    @Transient
    private InventoryStatus inventoryStatus;

    public ReceivingTransaction(){}
    public ReceivingTransaction(ReceiptLine receiptLine, Inventory inventory,
                                ReceivingTransactionType type,
                                String username,
                                String rfCode){
        setWarehouseId(receiptLine.getWarehouseId());
        setReceipt(receiptLine.getReceipt());
        setReceiptLine(receiptLine);
        setType(type);
        setLpn(inventory.getLpn());
        setItemId(inventory.getItem().getId());
        setQuantity(inventory.getQuantity());
        setUsername(username);
        setRfCode(rfCode);

        setColor(inventory.getColor());
        setStyle(inventory.getStyle());
        setProductSize(inventory.getProductSize());
        setItemPackageTypeId(inventory.getItemPackageType().getId());

        setInventoryAttribute1(inventory.getAttribute1());
        setInventoryAttribute2(inventory.getAttribute2());
        setInventoryAttribute3(inventory.getAttribute3());
        setInventoryAttribute4(inventory.getAttribute4());
        setInventoryAttribute5(inventory.getAttribute5());

        setInventoryStatusId(inventory.getInventoryStatus().getId());
    }
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(Long warehouseId) {
        this.warehouseId = warehouseId;
    }

    public Receipt getReceipt() {
        return receipt;
    }

    public void setReceipt(Receipt receipt) {
        this.receipt = receipt;
    }

    public ReceiptLine getReceiptLine() {
        return receiptLine;
    }

    public void setReceiptLine(ReceiptLine receiptLine) {
        this.receiptLine = receiptLine;
    }

    public ReceivingTransactionType getType() {
        return type;
    }

    public void setType(ReceivingTransactionType type) {
        this.type = type;
    }

    public String getLpn() {
        return lpn;
    }

    public void setLpn(String lpn) {
        this.lpn = lpn;
    }

    public Long getItemId() {
        return itemId;
    }

    public void setItemId(Long itemId) {
        this.itemId = itemId;
    }

    public Long getQuantity() {
        return quantity;
    }

    public void setQuantity(Long quantity) {
        this.quantity = quantity;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getRfCode() {
        return rfCode;
    }

    public void setRfCode(String rfCode) {
        this.rfCode = rfCode;
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

    public Long getItemPackageTypeId() {
        return itemPackageTypeId;
    }

    public void setItemPackageTypeId(Long itemPackageTypeId) {
        this.itemPackageTypeId = itemPackageTypeId;
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

    public Long getInventoryStatusId() {
        return inventoryStatusId;
    }

    public void setInventoryStatusId(Long inventoryStatusId) {
        this.inventoryStatusId = inventoryStatusId;
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

    public InventoryStatus getInventoryStatus() {
        return inventoryStatus;
    }

    public void setInventoryStatus(InventoryStatus inventoryStatus) {
        this.inventoryStatus = inventoryStatus;
    }
}

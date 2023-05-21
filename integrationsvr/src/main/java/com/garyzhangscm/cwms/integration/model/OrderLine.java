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

package com.garyzhangscm.cwms.integration.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import javax.persistence.Column;
import java.io.Serializable;



@JsonIgnoreProperties(ignoreUnknown = true)
public class OrderLine implements Serializable {

    private Long id;
    private String number;

    private Long itemId;
    private String itemName;
    private String itemQuickbookListId;

    private Long warehouseId;
    private String warehouseName;
    private String companyCode;
    private Long companyId;


    private Long expectedQuantity;



    private Long inventoryStatusId;
    private String inventoryStatusName;

    private Long carrierId;
    private String carrierName;

    private Long carrierServiceLevelId;
    private String carrierServiceLevelName;
    private String quickbookTxnLineID;
    private Boolean nonAllocatable = false;

    private String color;
    private String productSize;
    private String style;

    private String hualeiProductId;

    private Boolean autoRequestShippingLabel = false;

    private String allocateByReceiptNumber;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCompanyCode() {
        return companyCode;
    }

    public void setCompanyCode(String companyCode) {
        this.companyCode = companyCode;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public void setCompanyId(Long companyId) {
        this.companyId = companyId;
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

    public Long getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(Long warehouseId) {
        this.warehouseId = warehouseId;
    }

    public Long getExpectedQuantity() {
        return expectedQuantity;
    }

    public void setExpectedQuantity(Long expectedQuantity) {
        this.expectedQuantity = expectedQuantity;
    }

    public String getInventoryStatusName() {
        return inventoryStatusName;
    }

    public void setInventoryStatusName(String inventoryStatusName) {
        this.inventoryStatusName = inventoryStatusName;
    }

    public Long getInventoryStatusId() {
        return inventoryStatusId;
    }

    public void setInventoryStatusId(Long inventoryStatusId) {
        this.inventoryStatusId = inventoryStatusId;
    }

    public String getAllocateByReceiptNumber() {
        return allocateByReceiptNumber;
    }

    public void setAllocateByReceiptNumber(String allocateByReceiptNumber) {
        this.allocateByReceiptNumber = allocateByReceiptNumber;
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

    public Long getCarrierId() {
        return carrierId;
    }

    public void setCarrierId(Long carrierId) {
        this.carrierId = carrierId;
    }

    public Long getCarrierServiceLevelId() {
        return carrierServiceLevelId;
    }

    public void setCarrierServiceLevelId(Long carrierServiceLevelId) {
        this.carrierServiceLevelId = carrierServiceLevelId;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public String getWarehouseName() {
        return warehouseName;
    }

    public void setWarehouseName(String warehouseName) {
        this.warehouseName = warehouseName;
    }

    public String getCarrierName() {
        return carrierName;
    }

    public void setCarrierName(String carrierName) {
        this.carrierName = carrierName;
    }

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

    public String getItemQuickbookListId() {
        return itemQuickbookListId;
    }

    public void setItemQuickbookListId(String itemQuickbookListId) {
        this.itemQuickbookListId = itemQuickbookListId;
    }

    public Boolean getNonAllocatable() {
        return nonAllocatable;
    }

    public void setNonAllocatable(Boolean nonAllocatable) {
        this.nonAllocatable = nonAllocatable;
    }

    public String getHualeiProductId() {
        return hualeiProductId;
    }

    public void setHualeiProductId(String hualeiProductId) {
        this.hualeiProductId = hualeiProductId;
    }

    public Boolean getAutoRequestShippingLabel() {
        return autoRequestShippingLabel;
    }

    public void setAutoRequestShippingLabel(Boolean autoRequestShippingLabel) {
        this.autoRequestShippingLabel = autoRequestShippingLabel;
    }
}

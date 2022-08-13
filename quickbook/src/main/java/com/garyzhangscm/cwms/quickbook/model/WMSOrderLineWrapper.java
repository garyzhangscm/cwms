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

package com.garyzhangscm.cwms.quickbook.model;

import java.io.Serializable;
import java.util.Map;


public class WMSOrderLineWrapper implements Serializable {

    private String number;

    private Long itemId;
    private String itemName;

    private Long warehouseId;
    private String warehouseName;

    private Long companyId;
    private String companyName;

    private Long expectedQuantity;

    public WMSOrderLineWrapper() {}
    public WMSOrderLineWrapper(InvoiceLine invoiceLine, Long companyId, Long warehouseId) {
        setNumber(String.valueOf(invoiceLine.getLineNum()));
        // We will use the item's value(which is the item's id in quickbook)
        // as the item name
        setItemName(invoiceLine.getSalesItemLineDetail().getItemRef().getValue());
        setWarehouseId(warehouseId);
        setCompanyId(companyId);
        setExpectedQuantity((long) Math.ceil(invoiceLine.getSalesItemLineDetail().getQty()));
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

    public Long getCompanyId() {
        return companyId;
    }

    public void setCompanyId(Long companyId) {
        this.companyId = companyId;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public Long getExpectedQuantity() {
        return expectedQuantity;
    }

    public void setExpectedQuantity(Long expectedQuantity) {
        this.expectedQuantity = expectedQuantity;
    }
}

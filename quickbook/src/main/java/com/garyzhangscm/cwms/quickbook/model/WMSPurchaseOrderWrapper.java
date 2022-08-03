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


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;


public class WMSPurchaseOrderWrapper {

    private String number;

    private Long warehouseId;
    private String warehouseName;

    private Warehouse warehouse;

    private Long clientId;
    private String clientName;

    private Long supplierId;
    private String supplierName;

    private List<WMSPurchaseOrderLineWrapper> purchaseOrderLines = new ArrayList<>();

    private Boolean allowUnexpectedItem;

    public WMSPurchaseOrderWrapper() {}

    public WMSPurchaseOrderWrapper(PurchaseOrder purchaseOrder) {
        setWarehouseId(purchaseOrder.getWarehouseId());
        setNumber(purchaseOrder.getDocNumber());
        setSupplierName(purchaseOrder.getVendorRef().getName());
        setAllowUnexpectedItem(false);

        for (PurchaseOrderLine purchaseOrderLine : purchaseOrder.getLine()) {
            addPurchaseOrderLines(new WMSPurchaseOrderLineWrapper(warehouseId, purchaseOrderLine));

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
    public Warehouse getWarehouse() {
        return warehouse;
    }

    public void setWarehouse(Warehouse warehouse) {
        this.warehouse = warehouse;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public Long getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(Long warehouseId) {
        this.warehouseId = warehouseId;
    }

    public Long getClientId() {
        return clientId;
    }

    public void setClientId(Long clientId) {
        this.clientId = clientId;
    }

    public Long getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(Long supplierId) {
        this.supplierId = supplierId;
    }


    public Boolean getAllowUnexpectedItem() {
        return allowUnexpectedItem;
    }

    public void setAllowUnexpectedItem(Boolean allowUnexpectedItem) {
        this.allowUnexpectedItem = allowUnexpectedItem;
    }

    public String getWarehouseName() {
        return warehouseName;
    }

    public void setWarehouseName(String warehouseName) {
        this.warehouseName = warehouseName;
    }

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public String getSupplierName() {
        return supplierName;
    }

    public void setSupplierName(String supplierName) {
        this.supplierName = supplierName;
    }

    public List<WMSPurchaseOrderLineWrapper> getPurchaseOrderLines() {
        return purchaseOrderLines;
    }

    public void setPurchaseOrderLines(List<WMSPurchaseOrderLineWrapper> purchaseOrderLines) {
        this.purchaseOrderLines = purchaseOrderLines;
    }
    public void addPurchaseOrderLines(WMSPurchaseOrderLineWrapper purchaseOrderLine) {
        this.purchaseOrderLines.add(purchaseOrderLine);
    }
}

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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;


public class ItemPackageType implements Serializable {


    private String name;

    private String description;

    private Long clientId;
    private String clientName;

    private Long supplierId;
    private String supplierName;

    private List<ItemUnitOfMeasure> itemUnitOfMeasures= new ArrayList<>();

    private Long warehouseId;
    private String warehouseName;

    @Override
    public String toString() {
        return "ItemPackageType{" +
                "name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", clientId=" + clientId +
                ", clientName='" + clientName + '\'' +
                ", supplierId=" + supplierId +
                ", supplierName='" + supplierName + '\'' +
                ", itemUnitOfMeasures=" + itemUnitOfMeasures +
                ", warehouseId=" + warehouseId +
                ", warehouseName='" + warehouseName + '\'' +
                '}';
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

    public String getWarehouseName() {
        return warehouseName;
    }

    public void setWarehouseName(String warehouseName) {
        this.warehouseName = warehouseName;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    public List<ItemUnitOfMeasure> getItemUnitOfMeasures() {
        return itemUnitOfMeasures;
    }

    public void addItemUnitOfMeasure(ItemUnitOfMeasure itemUnitOfMeasure) {
        getItemUnitOfMeasures().add(itemUnitOfMeasure);
    }

    public void setItemUnitOfMeasures(List<ItemUnitOfMeasure> itemUnitOfMeasures) {
        this.itemUnitOfMeasures = itemUnitOfMeasures;
    }

    public Long getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(Long warehouseId) {
        this.warehouseId = warehouseId;
    }
}

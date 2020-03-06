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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;


public class ItemPackageType implements Serializable {


    private String name;

    private String description;

    private Long clientId;

    private Long supplierId;

    private List<ItemUnitOfMeasure> itemUnitOfMeasures= new ArrayList<>();

    private Long warehouseId;

    @Override
    public String toString() {
        return "ItemPackageType{" +
                "name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", clientId=" + clientId +
                ", supplierId=" + supplierId +
                ", itemUnitOfMeasures=" + itemUnitOfMeasures +
                ", warehouseId=" + warehouseId +
                '}';
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

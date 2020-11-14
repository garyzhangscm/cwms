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

package com.garyzhangscm.cwms.common.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class ItemPackageType implements Serializable {

    private Long id;

    private String name;
    private String description;

    private Long clientId;
    private Client client;

    private Long supplierId;
    private Supplier supplier;
    private Item item;

    private List<ItemUnitOfMeasure> itemUnitOfMeasures= new ArrayList<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public Client getClient() {
        return client;
    }

    public void setClient(Client client) {
        this.client = client;
    }

    public Long getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(Long supplierId) {
        this.supplierId = supplierId;
    }

    public Supplier getSupplier() {
        return supplier;
    }

    public void setSupplier(Supplier supplier) {
        this.supplier = supplier;
    }

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public List<ItemUnitOfMeasure> getItemUnitOfMeasures() {
        return itemUnitOfMeasures;
    }

    public void setItemUnitOfMeasures(List<ItemUnitOfMeasure> itemUnitOfMeasures) {
        this.itemUnitOfMeasures = itemUnitOfMeasures;
    }

    public ItemUnitOfMeasure getStockItemUnitOfMeasures() {
        if (itemUnitOfMeasures.size() == 0) {
            return null;
        }

        ItemUnitOfMeasure stockItemUnitOfMeasure = itemUnitOfMeasures.get(0);
        for (ItemUnitOfMeasure itemUnitOfMeasure : itemUnitOfMeasures) {
            if (itemUnitOfMeasure.getQuantity() < stockItemUnitOfMeasure.getQuantity()) {
                stockItemUnitOfMeasure = itemUnitOfMeasure;
            }
        }
        return stockItemUnitOfMeasure;
    }
}

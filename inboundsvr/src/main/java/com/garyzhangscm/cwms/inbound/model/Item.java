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

package com.garyzhangscm.cwms.inbound.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Item implements Serializable {

    private Long id;

    private String name;
    private String description;

    private Long clientId;

    private Client client;

    private ItemFamily itemFamily;

    private List<ItemPackageType> itemPackageTypes= new ArrayList<>();

    private double unitCost;

    @Override
    public boolean equals(Object anotherItem) {
        if (this == anotherItem) {
            return true;
        }
        if (!(anotherItem instanceof Item)) {
            return false;
        }
        return this.getName().equals(((Item)anotherItem).getName());
    }
    @Override
    public int hashCode() {
        return this.getName().hashCode();
    }

    @Override
    public String toString() {
        return "name: " + getName() + "\n"
                + "description: " + getDescription() + "\n";
    }

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

    public ItemFamily getItemFamily() {
        return itemFamily;
    }

    public void setItemFamily(ItemFamily itemFamily) {
        this.itemFamily = itemFamily;
    }

    public List<ItemPackageType> getItemPackageTypes() {
        return itemPackageTypes;
    }

    public void setItemPackageTypes(List<ItemPackageType> itemPackageTypes) {
        this.itemPackageTypes = itemPackageTypes;
    }

    public double getUnitCost() {
        return unitCost;
    }

    public void setUnitCost(double unitCost) {
        this.unitCost = unitCost;
    }
}

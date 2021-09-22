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

package com.garyzhangscm.cwms.outbound.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.persistence.Column;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class Item implements Serializable {

    private Long id;
    private Long warehouseId;

    private String name;
    private String description;

    private Long clientId;

    private Client client;

    private ItemFamily itemFamily;

    private Boolean allowCartonization;

    private List<ItemPackageType> itemPackageTypes= new ArrayList<>();

    private ItemPackageType defaultItemPackageType;

    private double unitCost;

    private Boolean allowAllocationByLPN;
    private AllocationRoundUpStrategyType allocationRoundUpStrategyType;
    private Double allocationRoundUpStrategyValue;

    @Override
    public boolean equals(Object anotherItem) {
        if (this == anotherItem) {
            return true;
        }
        if (!(anotherItem instanceof Item)) {
            return false;
        }
        if (Objects.equals(getId(), ((Item) anotherItem).getId())) {
            return true;
        }
        return this.getName().equals(((Item)anotherItem).getName()) &&
                this.getWarehouseId().equals(((Item) anotherItem).getWarehouseId());
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


    @Override
    public int hashCode() {
        return this.getName().hashCode();
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

    public Boolean getAllowCartonization() {
        return allowCartonization;
    }

    public void setAllowCartonization(Boolean allowCartonization) {
        this.allowCartonization = allowCartonization;
    }

    public Boolean getAllowAllocationByLPN() {
        return allowAllocationByLPN;
    }

    public void setAllowAllocationByLPN(Boolean allowAllocationByLPN) {
        this.allowAllocationByLPN = allowAllocationByLPN;
    }

    public AllocationRoundUpStrategyType getAllocationRoundUpStrategyType() {
        return allocationRoundUpStrategyType;
    }

    public void setAllocationRoundUpStrategyType(AllocationRoundUpStrategyType allocationRoundUpStrategyType) {
        this.allocationRoundUpStrategyType = allocationRoundUpStrategyType;
    }

    public Double getAllocationRoundUpStrategyValue() {
        return allocationRoundUpStrategyValue;
    }

    public void setAllocationRoundUpStrategyValue(Double allocationRoundUpStrategyValue) {
        this.allocationRoundUpStrategyValue = allocationRoundUpStrategyValue;
    }

    public Long getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(Long warehouseId) {
        this.warehouseId = warehouseId;
    }

    public ItemPackageType getDefaultItemPackageType() {
        if (itemPackageTypes.size() == 0) {
            return null;
        }
        if (Objects.nonNull(defaultItemPackageType)) {
            return defaultItemPackageType;
        }
        // see if we have any item package types that marked as default
        Optional<ItemPackageType> defaultItemPackageType = itemPackageTypes.stream().filter(
                itemPackageType -> Boolean.TRUE.equals(itemPackageType.getDefaultFlag())
        ).findFirst();
        if (defaultItemPackageType.isPresent()) {
            return defaultItemPackageType.get();
        }
        // we can't find any default item package type defined for this item
        // let's return the first one we find in the list
        return itemPackageTypes.get(0);
    }

    public void setDefaultItemPackageType(ItemPackageType defaultItemPackageType) {
        this.defaultItemPackageType = defaultItemPackageType;
    }
}

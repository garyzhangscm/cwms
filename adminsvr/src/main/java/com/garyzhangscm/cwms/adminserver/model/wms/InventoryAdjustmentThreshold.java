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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.garyzhangscm.cwms.adminserver.model.User;


import javax.persistence.*;
import java.io.Serializable;



public class InventoryAdjustmentThreshold implements Serializable {

    private Long id;

    // criteria

    private Item item;

    private Long clientId;

    private Client client;

    private ItemFamily itemFamily;

    private Long warehouseId;
    private Warehouse warehouse;


    private InventoryQuantityChangeType type;

    private Long userId;
    private User user;


    private Long roleId;

    private Role role;

    // Threshold by
    // 1. quantity
    // 2. cost / price
    private Long quantityThreshold;

    private Double costThreshold;
    private Boolean enabled = false;

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

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
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

    public InventoryQuantityChangeType getType() {
        return type;
    }

    public void setType(InventoryQuantityChangeType type) {
        this.type = type;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Long getRoleId() {
        return roleId;
    }

    public void setRoleId(Long roleId) {
        this.roleId = roleId;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public Long getQuantityThreshold() {
        return quantityThreshold;
    }

    public void setQuantityThreshold(Long quantityThreshold) {
        this.quantityThreshold = quantityThreshold;
    }

    public Double getCostThreshold() {
        return costThreshold;
    }

    public void setCostThreshold(Double costThreshold) {
        this.costThreshold = costThreshold;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }
}

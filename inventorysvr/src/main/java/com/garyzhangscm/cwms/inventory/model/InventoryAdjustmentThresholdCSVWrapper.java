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

package com.garyzhangscm.cwms.inventory.model;

import java.io.Serializable;


public class InventoryAdjustmentThresholdCSVWrapper implements Serializable {



    private String item;

    private String client;

    private String itemFamily;

    private String warehouse;


    private String type;

    private String user;

    private String role;

    private Long quantityThreshold;


    private Double costThreshold;


    private Boolean enabled;

    @Override
    public String toString() {
        return "InventoryAdjustmentThresholdCSVWrapper{" +
                "item='" + item + '\'' +
                ", client='" + client + '\'' +
                ", itemFamily='" + itemFamily + '\'' +
                ", warehouse='" + warehouse + '\'' +
                ", type='" + type + '\'' +
                ", user='" + user + '\'' +
                ", role='" + role + '\'' +
                ", quantityThreshold=" + quantityThreshold +
                ", costThreshold=" + costThreshold +
                ", enabled=" + enabled +
                '}';
    }

    public String getItem() {
        return item;
    }

    public void setItem(String item) {
        this.item = item;
    }

    public String getClient() {
        return client;
    }

    public void setClient(String client) {
        this.client = client;
    }

    public String getItemFamily() {
        return itemFamily;
    }

    public void setItemFamily(String itemFamily) {
        this.itemFamily = itemFamily;
    }

    public String getWarehouse() {
        return warehouse;
    }

    public void setWarehouse(String warehouse) {
        this.warehouse = warehouse;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
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

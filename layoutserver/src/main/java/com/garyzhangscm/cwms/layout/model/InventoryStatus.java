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

package com.garyzhangscm.cwms.layout.model;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.persistence.*;
import java.io.Serializable;

@JsonIgnoreProperties(ignoreUnknown = true)
public class InventoryStatus extends AuditibleEntity<String> implements Serializable {

    private Long id;

    private String name;

    private String description;

    private Long warehouseId;

    private Boolean availableStatusFlag;

    @Transient
    private Warehouse warehouse;

    public InventoryStatus(){}
    public InventoryStatus(String name, String description, Long warehouseId, Warehouse warehouse,
                           Boolean availableStatusFlag) {
        this.name = name;
        this.description = description;
        this.warehouseId = warehouseId;
        this.warehouse = warehouse;
        this.availableStatusFlag = Boolean.TRUE.equals(availableStatusFlag);
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

    public Boolean getAvailableStatusFlag() {
        return availableStatusFlag;
    }

    public void setAvailableStatusFlag(Boolean availableStatusFlag) {
        this.availableStatusFlag = availableStatusFlag;
    }
}

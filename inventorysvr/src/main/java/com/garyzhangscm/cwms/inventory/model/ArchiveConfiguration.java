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

package com.garyzhangscm.cwms.inventory.model;


import javax.persistence.*;

public class ArchiveConfiguration extends AuditibleEntity<String>  {

    private Long id;

    private Long warehouseId;

    private Boolean inventoryArchiveEnabled;


    // archive removed inventory after certain days
    private Integer removedInventoryArchiveDays;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(Long warehouseId) {
        this.warehouseId = warehouseId;
    }

    public Boolean getInventoryArchiveEnabled() {
        return inventoryArchiveEnabled;
    }

    public void setInventoryArchiveEnabled(Boolean inventoryArchiveEnabled) {
        this.inventoryArchiveEnabled = inventoryArchiveEnabled;
    }

    public Integer getRemovedInventoryArchiveDays() {
        return removedInventoryArchiveDays;
    }

    public void setRemovedInventoryArchiveDays(Integer removedInventoryArchiveDays) {
        this.removedInventoryArchiveDays = removedInventoryArchiveDays;
    }
}
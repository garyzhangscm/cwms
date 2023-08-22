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


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.codehaus.jackson.annotate.JsonProperty;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "inventory_status")
public class InventoryStatus extends AuditibleEntity<String> implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "inventory_status_id")
    @JsonProperty(value="id")
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(name = "description")
    private String description;


    @Column(name = "warehouse_id")
    private Long warehouseId;

    @Transient
    private Warehouse warehouse;

    // WHETHER this status stands for a 'available' status.
    // the available status will be displayed by default when
    // receiving / adjust in / produce new inventory
    @Column(name = "available_status_flag")
    private Boolean availableStatusFlag;


    @Column(name = "reason_required_when_receiving")
    private Boolean reasonRequiredWhenReceiving;
    @Column(name = "reason_required_when_producing")
    private Boolean reasonRequiredWhenProducing;
    @Column(name = "reason_required_when_adjusting")
    private Boolean reasonRequiredWhenAdjusting;

    @Column(name = "reason_optional_when_receiving")
    private Boolean reasonOptionalWhenReceiving;
    @Column(name = "reason_optional_when_producing")
    private Boolean reasonOptionalWhenProducing;
    @Column(name = "reason_optional_when_adjusting")
    private Boolean reasonOptionalWhenAdjusting;


    @Override
    public Object clone() {
        InventoryStatus inventoryStatus = null;
        try {
            inventoryStatus = (InventoryStatus) super.clone();
        } catch (CloneNotSupportedException e) {
            inventoryStatus = new InventoryStatus();
            inventoryStatus.setName(name);

            inventoryStatus.setDescription(description);
        }
        return inventoryStatus;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        InventoryStatus that = (InventoryStatus) o;

        // If both record has ID set, make sure the IDs are the same,
        // otherwise, make sure the names are the same in the same warehouse
        if (Objects.nonNull(id) && Objects.nonNull(that.id)) {
            return Objects.equals(id, that.id);
        }
        return Objects.equals(name, that.name) &&
                Objects.equals(warehouseId, that.warehouseId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, description, warehouseId, warehouse);
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

    public Boolean getReasonRequiredWhenReceiving() {
        return reasonRequiredWhenReceiving;
    }

    public void setReasonRequiredWhenReceiving(Boolean reasonRequiredWhenReceiving) {
        this.reasonRequiredWhenReceiving = reasonRequiredWhenReceiving;
    }

    public Boolean getReasonRequiredWhenProducing() {
        return reasonRequiredWhenProducing;
    }

    public void setReasonRequiredWhenProducing(Boolean reasonRequiredWhenProducing) {
        this.reasonRequiredWhenProducing = reasonRequiredWhenProducing;
    }

    public Boolean getReasonRequiredWhenAdjusting() {
        return reasonRequiredWhenAdjusting;
    }

    public void setReasonRequiredWhenAdjusting(Boolean reasonRequiredWhenAdjusting) {
        this.reasonRequiredWhenAdjusting = reasonRequiredWhenAdjusting;
    }

    public Boolean getReasonOptionalWhenReceiving() {
        return reasonOptionalWhenReceiving;
    }

    public void setReasonOptionalWhenReceiving(Boolean reasonOptionalWhenReceiving) {
        this.reasonOptionalWhenReceiving = reasonOptionalWhenReceiving;
    }

    public Boolean getReasonOptionalWhenProducing() {
        return reasonOptionalWhenProducing;
    }

    public void setReasonOptionalWhenProducing(Boolean reasonOptionalWhenProducing) {
        this.reasonOptionalWhenProducing = reasonOptionalWhenProducing;
    }

    public Boolean getReasonOptionalWhenAdjusting() {
        return reasonOptionalWhenAdjusting;
    }

    public void setReasonOptionalWhenAdjusting(Boolean reasonOptionalWhenAdjusting) {
        this.reasonOptionalWhenAdjusting = reasonOptionalWhenAdjusting;
    }
}

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


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang.StringUtils;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "inventory_snapshot_detail")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class InventorySnapshotDetail extends AuditibleEntity<String> implements Serializable {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "inventory_snapshot_detail_id")
    @JsonProperty(value="id")
    private Long id;


    @JsonIgnore
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "inventory_snapshot_id")
    private InventorySnapshot inventorySnapshot;

    // we will group the quantity of item based on the
    // item number / item package type and inventory status
    // and the location type group
    @ManyToOne
    @JoinColumn(name="item_id")
    private Item item;

    @ManyToOne
    @JoinColumn(name="item_package_type_id")
    private ItemPackageType itemPackageType;

    @ManyToOne
    @JoinColumn(name="inventory_status_id")
    private InventoryStatus inventoryStatus;

    @Column(name = "location_group_type_id")
    private Long locationGroupTypeId;

    @Transient
    private String locationGroupTypeName;

    @Column(name = "quantity")
    private Long quantity;


    public InventorySnapshotDetail(){}

    public InventorySnapshotDetail(InventorySnapshot inventorySnapshot, Inventory inventory) {
        this.inventorySnapshot = inventorySnapshot;
        item = inventory.getItem();
        itemPackageType = inventory.getItemPackageType();
        inventoryStatus = inventory.getInventoryStatus();
        locationGroupTypeId = inventory.getLocation().getLocationGroup().getLocationGroupType().getId();
        quantity = inventory.getQuantity();

    }
    @Override
    public int hashCode() {
        return Objects.hash(id);
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

    public InventorySnapshot getInventorySnapshot() {
        return inventorySnapshot;
    }

    public void setInventorySnapshot(InventorySnapshot inventorySnapshot) {
        this.inventorySnapshot = inventorySnapshot;
    }

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public ItemPackageType getItemPackageType() {
        return itemPackageType;
    }

    public void setItemPackageType(ItemPackageType itemPackageType) {
        this.itemPackageType = itemPackageType;
    }

    public InventoryStatus getInventoryStatus() {
        return inventoryStatus;
    }

    public void setInventoryStatus(InventoryStatus inventoryStatus) {
        this.inventoryStatus = inventoryStatus;
    }

    public Long getLocationGroupTypeId() {
        return locationGroupTypeId;
    }

    public void setLocationGroupTypeId(Long locationGroupTypeId) {
        this.locationGroupTypeId = locationGroupTypeId;
    }

    public Long getQuantity() {
        return quantity;
    }

    public void setQuantity(Long quantity) {
        this.quantity = quantity;
    }

    public String getLocationGroupTypeName() {
        return locationGroupTypeName;
    }

    public void setLocationGroupTypeName(String locationGroupTypeName) {
        this.locationGroupTypeName = locationGroupTypeName;
    }
}

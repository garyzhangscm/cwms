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
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.persistence.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "item_package_type")
public class ItemPackageType extends AuditibleEntity<String> implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "item_package_type_id")
    @JsonProperty(value="id")
    private Long id;

    @Column(name = "name")
    private String name;
    @Column(name = "description")
    private String description;

    @Column(name = "client_id")
    private Long clientId;

    @Transient
    private Client client;

    @Column(name = "supplier_id")
    private Long supplierId;

    @Transient
    private Supplier supplier;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id")
    private Item item;

    @Transient
    private ItemUnitOfMeasure stockItemUnitOfMeasure;

    @OneToMany(
            mappedBy = "itemPackageType",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.EAGER
    )
    private List<ItemUnitOfMeasure> itemUnitOfMeasures= new ArrayList<>();

    @Column(name = "warehouse_id")
    private Long warehouseId;

    @Transient
    private Warehouse warehouse;

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
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ItemPackageType that = (ItemPackageType) o;
        if (Objects.nonNull(id) && Objects.nonNull(that.id)) {
            return Objects.equals(id, that.id);
        }
        return Objects.equals(item, that.item) &&
                Objects.equals(name, that.name) &&
                Objects.equals(warehouseId, that.warehouseId) ;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, description, clientId, client, supplierId, supplier, item, itemUnitOfMeasures, warehouseId, warehouse);
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
    public void addItemUnitOfMeasure(ItemUnitOfMeasure itemUnitOfMeasure) {
        this.itemUnitOfMeasures.add(itemUnitOfMeasure);
    }

    public ItemUnitOfMeasure getStockItemUnitOfMeasure() {
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

    public void setStockItemUnitOfMeasure(ItemUnitOfMeasure stockItemUnitOfMeasure) {
        this.stockItemUnitOfMeasure = stockItemUnitOfMeasure;
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
}

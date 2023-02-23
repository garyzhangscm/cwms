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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

@JsonIgnoreProperties(ignoreUnknown = true)
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

    private ItemUnitOfMeasure defaultInboundReceivingUOM;

    private ItemUnitOfMeasure defaultWorkOrderReceivingUOM;

    private ItemUnitOfMeasure trackingLpnUOM;

    private ItemUnitOfMeasure stockItemUnitOfMeasure;

    public ItemUnitOfMeasure getDefaultInboundReceivingUOM() {
        if (itemUnitOfMeasures.size() == 0) {
            return null;
        }
        if (Objects.nonNull(defaultInboundReceivingUOM)) {
            return defaultInboundReceivingUOM;
        }

        return itemUnitOfMeasures.stream().filter(
                itemUnitOfMeasure -> Boolean.TRUE.equals(itemUnitOfMeasure.getDefaultForInboundReceiving())
        ).findFirst().orElse(getStockItemUnitOfMeasure());
    }
    public void setDefaultInboundReceivingUOM(ItemUnitOfMeasure defaultInboundReceivingUOM) {
        this.defaultInboundReceivingUOM = defaultInboundReceivingUOM;
    }

    public ItemUnitOfMeasure getDefaultWorkOrderReceivingUOM() {
        if (itemUnitOfMeasures.size() == 0) {
            return null;
        }
        if (Objects.nonNull(defaultWorkOrderReceivingUOM)) {
            return defaultWorkOrderReceivingUOM;
        }

        return itemUnitOfMeasures.stream().filter(
                itemUnitOfMeasure -> Boolean.TRUE.equals(itemUnitOfMeasure.getDefaultForWorkOrderReceiving())
        ).findFirst().orElse(getStockItemUnitOfMeasure());
    }
    public void setDefaultWorkOrderReceivingUOM(ItemUnitOfMeasure defaultWorkOrderReceivingUOM) {
        this.defaultWorkOrderReceivingUOM = defaultWorkOrderReceivingUOM;
    }

    public ItemUnitOfMeasure getTrackingLpnUOM() {
        if (itemUnitOfMeasures.size() == 0) {
            return null;
        }
        if (Objects.nonNull(trackingLpnUOM)) {
            return trackingLpnUOM;
        }

        // let's find the smallest uom marked as tracking UOM
        List<ItemUnitOfMeasure> trackingLPNUoms = itemUnitOfMeasures.stream().filter(
                itemUnitOfMeasure -> Boolean.TRUE.equals(itemUnitOfMeasure.getTrackingLpn())
        ).collect(Collectors.toList());
        if (trackingLPNUoms.size() == 0) {
            return null;
        }
        Collections.sort(trackingLPNUoms, (Comparator.comparing(ItemUnitOfMeasure::getQuantity)));

        return trackingLPNUoms.get(0);
    }

    public void setTrackingLpnUOM(ItemUnitOfMeasure trackingLpnUOM) {
        this.trackingLpnUOM = trackingLpnUOM;
    }


    public ItemUnitOfMeasure getStockItemUnitOfMeasure() {
        if (itemUnitOfMeasures.size() == 0) {
            return null;
        }
        if (Objects.nonNull(stockItemUnitOfMeasure)) {
            return stockItemUnitOfMeasure;
        }

        ItemUnitOfMeasure stockItemUnitOfMeasure = itemUnitOfMeasures.get(0);
        for (ItemUnitOfMeasure itemUnitOfMeasure : itemUnitOfMeasures) {
            if (itemUnitOfMeasure.getQuantity() < stockItemUnitOfMeasure.getQuantity()) {
                stockItemUnitOfMeasure = itemUnitOfMeasure;
            }
        }
        return stockItemUnitOfMeasure;
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


    public void setStockItemUnitOfMeasure(ItemUnitOfMeasure stockItemUnitOfMeasure) {
        this.stockItemUnitOfMeasure = stockItemUnitOfMeasure;
    }
}

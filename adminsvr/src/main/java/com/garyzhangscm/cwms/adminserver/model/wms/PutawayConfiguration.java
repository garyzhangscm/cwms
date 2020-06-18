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

package com.garyzhangscm.cwms.adminserver.model.wms;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.annotate.JsonProperty;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class PutawayConfiguration {

    private Long id;

    private Integer sequence;

    // criteria: item / item group / inventory status

    private Long warehouseId;

    private Warehouse warehouse;

    private Long itemId;

    private Item item;

    private Long itemFamilyId;

    private ItemFamily itemFamily;

    private Long inventoryStatusId;

    private InventoryStatus inventoryStatus;

    private Long locationId;

    private Location location;

    private Long locationGroupId;
    private LocationGroup locationGroup;

    private Long locationGroupTypeId;

    private LocationGroupType locationGroupType;

    private String strategies;

    private List<PutawayConfigurationStrategy> putawayConfigurationStrategies = new ArrayList<>();

    public static PutawayConfiguration byItemFamilyAndLocationGroup(
                            int sequence, Warehouse warehouse,
                            ItemFamily itemFamily, LocationGroup locationGroup,
                            String strategies) {
        PutawayConfiguration putawayConfiguration = new PutawayConfiguration();
        putawayConfiguration.setSequence(sequence);
        putawayConfiguration.setWarehouseId(warehouse.getId());
        putawayConfiguration.setWarehouse(warehouse);
        putawayConfiguration.setItemFamilyId(itemFamily.getId());
        putawayConfiguration.setItemFamily(itemFamily);
        putawayConfiguration.setLocationGroupId(locationGroup.getId());
        putawayConfiguration.setLocationGroup(locationGroup);
        putawayConfiguration.setStrategies(strategies);
        for (String strategy : strategies.split(",")) {

            putawayConfiguration.addPutawayConfigurationStrategy(PutawayConfigurationStrategy.valueOf(strategy));
        }
        return putawayConfiguration;
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

    public Integer getSequence() {
        return sequence;
    }

    public void setSequence(Integer sequence) {
        this.sequence = sequence;
    }

    public Long getItemId() {
        return itemId;
    }

    public void setItemId(Long itemId) {
        this.itemId = itemId;
    }

    public Long getItemFamilyId() {
        return itemFamilyId;
    }

    public void setItemFamilyId(Long itemFamilyId) {
        this.itemFamilyId = itemFamilyId;
    }

    public Long getInventoryStatusId() {
        return inventoryStatusId;
    }

    public void setInventoryStatusId(Long inventoryStatusId) {
        this.inventoryStatusId = inventoryStatusId;
    }

    public Long getLocationId() {
        return locationId;
    }

    public void setLocationId(Long locationId) {
        this.locationId = locationId;
    }

    public Long getLocationGroupId() {
        return locationGroupId;
    }

    public void setLocationGroupId(Long locationGroupId) {
        this.locationGroupId = locationGroupId;
    }

    public Long getLocationGroupTypeId() {
        return locationGroupTypeId;
    }

    public void setLocationGroupTypeId(Long locationGroupTypeId) {
        this.locationGroupTypeId = locationGroupTypeId;
    }

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public ItemFamily getItemFamily() {
        return itemFamily;
    }

    public void setItemFamily(ItemFamily itemFamily) {
        this.itemFamily = itemFamily;
    }

    public InventoryStatus getInventoryStatus() {
        return inventoryStatus;
    }

    public void setInventoryStatus(InventoryStatus inventoryStatus) {
        this.inventoryStatus = inventoryStatus;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public LocationGroup getLocationGroup() {
        return locationGroup;
    }

    public void setLocationGroup(LocationGroup locationGroup) {
        this.locationGroup = locationGroup;
    }

    public LocationGroupType getLocationGroupType() {
        return locationGroupType;
    }

    public void setLocationGroupType(LocationGroupType locationGroupType) {
        this.locationGroupType = locationGroupType;
    }


    public String getStrategies() {
        return strategies;
    }

    public void setStrategies(String strategies) {
        this.strategies = strategies;
    }

    public List<PutawayConfigurationStrategy> getPutawayConfigurationStrategies() {
        if (putawayConfigurationStrategies.size()  == 0 &&
               !StringUtils.isBlank(strategies)){
            putawayConfigurationStrategies =
                    Arrays.stream(strategies.split(","))
                            .map(strategy -> PutawayConfigurationStrategy.valueOf(strategy))
                            .collect(Collectors.toList());

        }
        return putawayConfigurationStrategies;
    }

    public void setPutawayConfigurationStrategies(List<PutawayConfigurationStrategy> putawayConfigurationStrategies) {
        this.putawayConfigurationStrategies = putawayConfigurationStrategies;
    }
    public void addPutawayConfigurationStrategy(PutawayConfigurationStrategy putawayConfigurationStrategy) {
        this.putawayConfigurationStrategies.add(putawayConfigurationStrategy);
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

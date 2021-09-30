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

package com.garyzhangscm.cwms.layout.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class PutawayConfiguration {

    private Long id;

    private Integer sequence;

    private Long warehouseId;


    private Long itemId;


    private Long itemFamilyId;


    private Long inventoryStatusId;


    private Long locationId;


    private Long locationGroupId;

    private Long locationGroupTypeId;

    private String strategies;

    public PutawayConfiguration(){}

    public PutawayConfiguration(Integer sequence, Long warehouseId,
                                Long itemId, Long itemFamilyId,
                                Long inventoryStatusId, Long locationId,
                                Long locationGroupId, Long locationGroupTypeId, String strategies) {
        this.sequence = sequence;
        this.warehouseId = warehouseId;
        this.itemId = itemId;
        this.itemFamilyId = itemFamilyId;
        this.inventoryStatusId = inventoryStatusId;
        this.locationId = locationId;
        this.locationGroupId = locationGroupId;
        this.locationGroupTypeId = locationGroupTypeId;
        this.strategies = strategies;
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

    public Long getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(Long warehouseId) {
        this.warehouseId = warehouseId;
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

    public String getStrategies() {
        return strategies;
    }

    public void setStrategies(String strategies) {
        this.strategies = strategies;
    }
}

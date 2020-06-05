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



public class MovementPathDetail {

    private Long id;

    private Long hopLocationId;
    private Location hopLocation;

    private Long hopLocationGroupId;

    private LocationGroup hopLocationGroup;

    private Integer sequence;

    private MovementPathStrategy strategy;


    private Long warehouseId;

    private Warehouse warehouse;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getHopLocationId() {
        return hopLocationId;
    }

    public void setHopLocationId(Long hopLocationId) {
        this.hopLocationId = hopLocationId;
    }

    public Location getHopLocation() {
        return hopLocation;
    }

    public void setHopLocation(Location hopLocation) {
        this.hopLocation = hopLocation;
    }

    public Long getHopLocationGroupId() {
        return hopLocationGroupId;
    }

    public void setHopLocationGroupId(Long hopLocationGroupId) {
        this.hopLocationGroupId = hopLocationGroupId;
    }

    public LocationGroup getHopLocationGroup() {
        return hopLocationGroup;
    }

    public void setHopLocationGroup(LocationGroup hopLocationGroup) {
        this.hopLocationGroup = hopLocationGroup;
    }

    public Integer getSequence() {
        return sequence;
    }

    public void setSequence(Integer sequence) {
        this.sequence = sequence;
    }

    public MovementPathStrategy getStrategy() {
        return strategy;
    }

    public void setStrategy(MovementPathStrategy strategy) {
        this.strategy = strategy;
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

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


import com.fasterxml.jackson.annotation.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;

import javax.persistence.*;

@Entity
@Table(name = "movement_path_detail")
public class MovementPathDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "movement_path_detail_id")
    @JsonProperty(value="id")
    private Long id;

    @Column(name = "hop_location_id")
    private Long hopLocationId;

    @Transient
    private Location hopLocation;


    @Column(name = "hop_location_group_id")
    private Long hopLocationGroupId;

    @Transient
    private LocationGroup hopLocationGroup;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "movement_path_id")
    private MovementPath movementPath;

    @Column(name = "sequence")
    private Integer sequence;

    /**
     * When the movement path detail is defined by location group, then
     * the strategy defines how we reserve a location from the group.
     * For example, if the strategy is BY_SHIPMENT, then all the
     * inventory picked for the same shipment will reserve the same
     * hop location.
     */
    @Column(name = "strategy")
    @Enumerated(EnumType.STRING)
    private MovementPathStrategy strategy;

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("MovementPathDetail: { ")
                .append("sequence: ").append(sequence)
                .append("hopLocation: ").append(hopLocation).append(",")
                .append("hopLocationGroup: ").append(hopLocationGroup).append(",")
                .append("strategy: ").append(strategy).append("}");

        return stringBuilder.toString();
    }

    @Column(name = "warehouse_id")
    private Long warehouseId;

    @Transient
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

    public MovementPath getMovementPath() {
        return movementPath;
    }

    public void setMovementPath(MovementPath movementPath) {
        this.movementPath = movementPath;
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

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


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.codehaus.jackson.annotate.JsonProperty;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "movement_path")
public class MovementPath extends AuditibleEntity<String>  {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "movement_path_id")
    @JsonProperty(value="id")
    private Long id;


    @Column(name = "from_location_id")
    private Long fromLocationId;

    @Transient
    private Location fromLocation;

    @Column(name = "from_location_group_id")
    private Long fromLocationGroupId;

    @Transient
    private LocationGroup fromLocationGroup;

    @Column(name = "to_location_id")
    private Long toLocationId;

    @Transient
    private Location toLocation;

    @Column(name = "to_location_group_id")
    private Long toLocationGroupId;

    @Transient
    private LocationGroup toLocationGroup;


    @Column(name = "sequence", unique = true)
    private Integer sequence;

    @OneToMany(
            mappedBy = "movementPath",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    private List<MovementPathDetail> movementPathDetails = new ArrayList<>();


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
    public boolean equals(Object anotherObject) {
        if (anotherObject == this) {
            return true;
        }
        if (!(anotherObject instanceof MovementPath)) {
            return false;
        }
        MovementPath anotherMovementPath = (MovementPath) anotherObject;
        if (id != null && anotherMovementPath.getId() != null && id.equals(anotherMovementPath.getId())) {
            return true;
        }

        return (sequence.equals(anotherMovementPath.getSequence()));

    }


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getFromLocationId() {
        return fromLocationId;
    }

    public void setFromLocationId(Long fromLocationId) {
        this.fromLocationId = fromLocationId;
    }

    public Location getFromLocation() {
        return fromLocation;
    }

    public void setFromLocation(Location fromLocation) {
        this.fromLocation = fromLocation;
    }

    public Long getFromLocationGroupId() {
        return fromLocationGroupId;
    }

    public void setFromLocationGroupId(Long fromLocationGroupId) {
        this.fromLocationGroupId = fromLocationGroupId;
    }

    public LocationGroup getFromLocationGroup() {
        return fromLocationGroup;
    }

    public void setFromLocationGroup(LocationGroup fromLocationGroup) {
        this.fromLocationGroup = fromLocationGroup;
    }

    public Long getToLocationId() {
        return toLocationId;
    }

    public void setToLocationId(Long toLocationId) {
        this.toLocationId = toLocationId;
    }

    public Location getToLocation() {
        return toLocation;
    }

    public void setToLocation(Location toLocation) {
        this.toLocation = toLocation;
    }

    public Long getToLocationGroupId() {
        return toLocationGroupId;
    }

    public void setToLocationGroupId(Long toLocationGroupId) {
        this.toLocationGroupId = toLocationGroupId;
    }

    public LocationGroup getToLocationGroup() {
        return toLocationGroup;
    }

    public void setToLocationGroup(LocationGroup toLocationGroup) {
        this.toLocationGroup = toLocationGroup;
    }

    public List<MovementPathDetail> getMovementPathDetails() {
        return movementPathDetails;
    }

    public void setMovementPathDetails(List<MovementPathDetail> movementPathDetails) {
        this.movementPathDetails = movementPathDetails;
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

    public Warehouse getWarehouse() {
        return warehouse;
    }

    public void setWarehouse(Warehouse warehouse) {
        this.warehouse = warehouse;
    }
}

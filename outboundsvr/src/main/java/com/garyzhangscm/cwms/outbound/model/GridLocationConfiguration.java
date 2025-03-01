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

package com.garyzhangscm.cwms.outbound.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.persistence.*;
import java.io.Serializable;

/**
 *
 */
@Entity
@Table(name = "grid_location_configuration")
public class GridLocationConfiguration  extends AuditibleEntity<String> implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "grid_location_configuration_id")
    @JsonProperty(value="id")
    private Long id;

    @Column(name = "warehouse_id")
    private Long warehouseId;

    @Transient
    private Warehouse warehouse;


    @Column(name = "location_group_id")
    private Long locationGroupId;

    @Transient
    private LocationGroup locationGroup;

    @Column(name = "location_id")
    private Long locationId;

    @Transient
    private Location location;

    // row number / column span are 2
    // attribute for displaying
    // grid location
    @Column(name = "row_num")
    private Integer rowNumber;

    @Column(name = "column_span")
    private Integer columnSpan;

    @Column(name = "sequence")
    private Integer sequence;

    /**
     * The quantity the will be / already being
     * directed into the grid location
     */
    @Column(name = "pending_quantity")
    private Long pendingQuantity = 0L;


    @Column(name = "arrived_quantity")
    private Long arrivedQuantity = 0L;

    /**
     * Whether there's a permanent LPN assign
     * to this location.
     */
    @Column(name = "permanent_lpn_flag")
    private boolean permanentLPNFlag;
    @Column(name = "permanent_lpn")
    private String permanentLPN;
    @Column(name = "current_lpn")
    private String currentLPN;


    public void increasePendingQuantity(Long increasedQuantity) {
        pendingQuantity += increasedQuantity;
    }
    public void decreasePendingQuantity(Long decreasedQuantity) {
        pendingQuantity -= decreasedQuantity;
    }

    public void increaseArrivedQuantity(Long increasedQuantity) {
        arrivedQuantity += increasedQuantity;
    }
    public void decreaseArrivedQuantity(Long decreasedQuantity) {
        arrivedQuantity -= decreasedQuantity;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getLocationId() {
        return locationId;
    }

    public void setLocationId(Long locationId) {
        this.locationId = locationId;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public Integer getRowNumber() {
        return rowNumber;
    }

    public void setRowNumber(Integer rowNumber) {
        this.rowNumber = rowNumber;
    }

    public Integer getColumnSpan() {
        return columnSpan;
    }

    public void setColumnSpan(Integer columnSpan) {
        this.columnSpan = columnSpan;
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

    public Long getLocationGroupId() {
        return locationGroupId;
    }

    public void setLocationGroupId(Long locationGroupId) {
        this.locationGroupId = locationGroupId;
    }

    public LocationGroup getLocationGroup() {
        return locationGroup;
    }

    public void setLocationGroup(LocationGroup locationGroup) {
        this.locationGroup = locationGroup;
    }

    public Integer getSequence() {
        return sequence;
    }

    public void setSequence(Integer sequence) {
        this.sequence = sequence;
    }

    public Long getPendingQuantity() {
        return pendingQuantity;
    }

    public void setPendingQuantity(Long pendingQuantity) {
        this.pendingQuantity = pendingQuantity;
    }

    public Long getArrivedQuantity() {
        return arrivedQuantity;
    }

    public void setArrivedQuantity(Long arrivedQuantity) {
        this.arrivedQuantity = arrivedQuantity;
    }

    public boolean isPermanentLPNFlag() {
        return permanentLPNFlag;
    }

    public void setPermanentLPNFlag(boolean permanentLPNFlag) {
        this.permanentLPNFlag = permanentLPNFlag;
    }

    public String getPermanentLPN() {
        return permanentLPN;
    }

    public void setPermanentLPN(String permanentLPN) {
        this.permanentLPN = permanentLPN;
    }

    public String getCurrentLPN() {
        return currentLPN;
    }

    public void setCurrentLPN(String currentLPN) {
        this.currentLPN = currentLPN;
    }
}

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


import com.fasterxml.jackson.annotation.JsonInclude;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.*;

import java.io.Serializable;


@Entity
@Table(name = "inventory_snapshot_configuration")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class InventorySnapshotConfiguration extends AuditibleEntity<String> implements Serializable {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "inventory_snapshot_configuration_id")
    @JsonProperty(value="id")
    private Long id;

    @Column(name = "warehouse_id")
    private Long warehouseId;

    @Transient
    private Warehouse warehouse;


    @Column(name = "cron")
    private String cron;

    @Column(name = "location_utilization_snapshot_cron")
    private String locationUtilizationSnapshotCron;

    // We will have 3 type of snapshot:
    // 1. inventory snapshot
    // 2. location utilization snapshot: used by billing system to charge the
    //    customer by location usage
    // 3. inventory aging snapshot: used by billing system to charge the customer
    //    by the inventory aging(charge X dollars daily for inventory that already
    //    in warehouse for N days)
    // for each type, we allow the user to set up to 3 times per day to capture the
    // snapshot. In most case, the user may only need one snapshot per day

    @Column(name = "inventory_snapshot_timing_1")
    private Integer inventorySnapshotTiming1;
    @Column(name = "inventory_snapshot_timing_2")
    private Integer inventorySnapshotTiming2;
    @Column(name = "inventory_snapshot_timing_3")
    private Integer inventorySnapshotTiming3;

    @Column(name = "location_utilization_snapshot_timing_1")
    private Integer locationUtilizationSnapshotTiming1;
    @Column(name = "location_utilization_snapshot_timing_2")
    private Integer locationUtilizationSnapshotTiming2;
    @Column(name = "location_utilization_snapshot_timing_3")
    private Integer locationUtilizationSnapshotTiming3;


    @Column(name = "inventory_aging_snapshot_timing_1")
    private Integer inventoryAgingSnapshotTiming1;
    @Column(name = "inventory_aging_snapshot_timing_2")
    private Integer inventoryAgingSnapshotTiming2;
    @Column(name = "inventory_aging_snapshot_timing_3")
    private Integer inventoryAgingSnapshotTiming3;


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getCron() {
        return cron;
    }

    public void setCron(String cron) {
        this.cron = cron;
    }

    public String getLocationUtilizationSnapshotCron() {
        return locationUtilizationSnapshotCron;
    }

    public void setLocationUtilizationSnapshotCron(String locationUtilizationSnapshotCron) {
        this.locationUtilizationSnapshotCron = locationUtilizationSnapshotCron;
    }

    public Integer getInventorySnapshotTiming1() {
        return inventorySnapshotTiming1;
    }

    public void setInventorySnapshotTiming1(Integer inventorySnapshotTiming1) {
        this.inventorySnapshotTiming1 = inventorySnapshotTiming1;
    }

    public Integer getInventorySnapshotTiming2() {
        return inventorySnapshotTiming2;
    }

    public void setInventorySnapshotTiming2(Integer inventorySnapshotTiming2) {
        this.inventorySnapshotTiming2 = inventorySnapshotTiming2;
    }

    public Integer getInventorySnapshotTiming3() {
        return inventorySnapshotTiming3;
    }

    public void setInventorySnapshotTiming3(Integer inventorySnapshotTiming3) {
        this.inventorySnapshotTiming3 = inventorySnapshotTiming3;
    }

    public Integer getLocationUtilizationSnapshotTiming1() {
        return locationUtilizationSnapshotTiming1;
    }

    public void setLocationUtilizationSnapshotTiming1(Integer locationUtilizationSnapshotTiming1) {
        this.locationUtilizationSnapshotTiming1 = locationUtilizationSnapshotTiming1;
    }

    public Integer getLocationUtilizationSnapshotTiming2() {
        return locationUtilizationSnapshotTiming2;
    }

    public void setLocationUtilizationSnapshotTiming2(Integer locationUtilizationSnapshotTiming2) {
        this.locationUtilizationSnapshotTiming2 = locationUtilizationSnapshotTiming2;
    }

    public Integer getLocationUtilizationSnapshotTiming3() {
        return locationUtilizationSnapshotTiming3;
    }

    public void setLocationUtilizationSnapshotTiming3(Integer locationUtilizationSnapshotTiming3) {
        this.locationUtilizationSnapshotTiming3 = locationUtilizationSnapshotTiming3;
    }

    public Integer getInventoryAgingSnapshotTiming1() {
        return inventoryAgingSnapshotTiming1;
    }

    public void setInventoryAgingSnapshotTiming1(Integer inventoryAgingSnapshotTiming1) {
        this.inventoryAgingSnapshotTiming1 = inventoryAgingSnapshotTiming1;
    }

    public Integer getInventoryAgingSnapshotTiming2() {
        return inventoryAgingSnapshotTiming2;
    }

    public void setInventoryAgingSnapshotTiming2(Integer inventoryAgingSnapshotTiming2) {
        this.inventoryAgingSnapshotTiming2 = inventoryAgingSnapshotTiming2;
    }

    public Integer getInventoryAgingSnapshotTiming3() {
        return inventoryAgingSnapshotTiming3;
    }

    public void setInventoryAgingSnapshotTiming3(Integer inventoryAgingSnapshotTiming3) {
        this.inventoryAgingSnapshotTiming3 = inventoryAgingSnapshotTiming3;
    }
}

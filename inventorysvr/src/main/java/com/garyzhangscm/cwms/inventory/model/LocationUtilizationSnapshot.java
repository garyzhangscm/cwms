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
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;


@Entity
@Table(name = "location_utilization_snapshot")
public class LocationUtilizationSnapshot extends AuditibleEntity<String>{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "location_utilization_snapshot_id")
    @JsonProperty(value="id")
    private Long id;

    @Column(name = "warehouse_id")
    private Long warehouseId;

    @ManyToOne
    @JoinColumn(name="item_id")
    private Item item;

    @Column(name = "client_id")
    private Long clientId;
    @Transient
    private Client client;

    @Column(name = "net_volume")
    private Double netVolume;

    @Column(name = "gross_volume")
    private Double grossVolume;


    @Column(name = "capacity_unit")
    private String capacityUnit;

    @Column(name = "total_locations")
    private Integer totalLocations;

    @OneToMany(
            mappedBy = "locationUtilizationSnapshot",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    @JsonIgnore
    private List<LocationUtilizationSnapshotDetail> locationUtilizationSnapshotDetails= new ArrayList<>();


    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_location_utilization_snapshot_batch_id")
    private ClientLocationUtilizationSnapshotBatch clientLocationUtilizationSnapshotBatch;

    public LocationUtilizationSnapshot() {}
    public LocationUtilizationSnapshot(Long warehouseId, Item item, Long clientId, Double netVolume, Double grossVolume,
                                       Integer totalLocations,
                                       String capacityUnit) {
        this.warehouseId = warehouseId;
        this.item = item;
        this.clientId = clientId;
        this.netVolume = netVolume;
        this.grossVolume = grossVolume;
        this.totalLocations = totalLocations;
        this.capacityUnit = capacityUnit;
    }

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


    public Long getClientId() {
        return clientId;
    }

    public void setClientId(Long clientId) {
        this.clientId = clientId;
    }

    public Double getNetVolume() {
        return netVolume;
    }

    public void setNetVolume(Double netVolume) {
        this.netVolume = netVolume;
    }

    public Double getGrossVolume() {
        return grossVolume;
    }

    public void setGrossVolume(Double grossVolume) {
        this.grossVolume = grossVolume;
    }

    public Integer getTotalLocations() {
        return totalLocations;
    }

    public void setTotalLocations(Integer totalLocations) {
        this.totalLocations = totalLocations;
    }

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public Client getClient() {
        return client;
    }

    public void setClient(Client client) {
        this.client = client;
    }

    public List<LocationUtilizationSnapshotDetail> getLocationUtilizationSnapshotDetails() {
        return locationUtilizationSnapshotDetails;
    }

    public void setLocationUtilizationSnapshotDetails(List<LocationUtilizationSnapshotDetail> locationUtilizationSnapshotDetails) {
        this.locationUtilizationSnapshotDetails = locationUtilizationSnapshotDetails;
    }

    public void addLocationUtilizationSnapshotDetail(LocationUtilizationSnapshotDetail locationUtilizationSnapshotDetail) {
        this.locationUtilizationSnapshotDetails.add(locationUtilizationSnapshotDetail);
    }

    public ClientLocationUtilizationSnapshotBatch getClientLocationUtilizationSnapshotBatch() {
        return clientLocationUtilizationSnapshotBatch;
    }

    public void setClientLocationUtilizationSnapshotBatch(ClientLocationUtilizationSnapshotBatch clientLocationUtilizationSnapshotBatch) {
        this.clientLocationUtilizationSnapshotBatch = clientLocationUtilizationSnapshotBatch;
    }

    public String getCapacityUnit() {
        return capacityUnit;
    }

    public void setCapacityUnit(String capacityUnit) {
        this.capacityUnit = capacityUnit;
    }
}

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
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "client_location_utilization_snapshot_batch")
public class ClientLocationUtilizationSnapshotBatch extends AuditibleEntity<String>{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "client_location_utilization_snapshot_batch_id")
    @JsonProperty(value="id")
    private Long id;

    @Column(name = "warehouse_id")
    private Long warehouseId;

    @Column(name = "client_id")
    private Long clientId;
    @Transient
    private Client client;

    @Column(name = "net_volume")
    private Double netVolume;

    @Column(name = "gross_volume")
    private Double grossVolume;


    @Column(name = "total_locations")
    private Integer totalLocations;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_utilization_snapshot_batch_id")
    private LocationUtilizationSnapshotBatch locationUtilizationSnapshotBatch;

    @JsonIgnore
    @OneToMany(
            mappedBy = "clientLocationUtilizationSnapshotBatch",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    private List<LocationUtilizationSnapshot> locationUtilizationSnapshots= new ArrayList<>();


    public ClientLocationUtilizationSnapshotBatch(){}
    public ClientLocationUtilizationSnapshotBatch(Long warehouseId, Long clientId, Double netVolume, Double grossVolume, Integer totalLocations) {
        this.warehouseId = warehouseId;
        this.clientId = clientId;
        this.netVolume = netVolume;
        this.grossVolume = grossVolume;
        this.totalLocations = totalLocations;
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

    public List<LocationUtilizationSnapshot> getLocationUtilizationSnapshots() {
        return locationUtilizationSnapshots;
    }

    public void setLocationUtilizationSnapshots(List<LocationUtilizationSnapshot> locationUtilizationSnapshots) {
        this.locationUtilizationSnapshots = locationUtilizationSnapshots;
    }
    public void addLocationUtilizationSnapshot(LocationUtilizationSnapshot locationUtilizationSnapshot) {
        this.locationUtilizationSnapshots.add(locationUtilizationSnapshot);
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

    public LocationUtilizationSnapshotBatch getLocationUtilizationSnapshotBatch() {
        return locationUtilizationSnapshotBatch;
    }

    public void setLocationUtilizationSnapshotBatch(LocationUtilizationSnapshotBatch locationUtilizationSnapshotBatch) {
        this.locationUtilizationSnapshotBatch = locationUtilizationSnapshotBatch;
    }
}
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
import java.util.Objects;

@Entity
@Table(name = "location_utilization_snapshot_detail")
public class LocationUtilizationSnapshotDetail extends AuditibleEntity<String>{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "location_utilization_snapshot_detail_id")
    @JsonProperty(value="id")
    private Long id;


    @Transient
    private Long warehouseId;

    @Transient
    private Long itemId;

    @Transient
    private Long clientId;


    @Column(name = "net_volume")
    private Double netVolume;

    @Column(name = "gross_volume")
    private Double grossVolume;


    @Column(name = "location_id")
    private Long locationId;

    @Column(name = "location_size")
    private Double locationSize = 0.0;



    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_utilization_snapshot_id")
    private LocationUtilizationSnapshot locationUtilizationSnapshot;

    public LocationUtilizationSnapshotDetail(Long warehouseId, Long itemId, Long clientId,
                                             Double netVolume, Double grossVolume, Long locationId, Double locationSize) {
        this.warehouseId = warehouseId;
        this.itemId = itemId;
        this.clientId = clientId;
        this.netVolume = netVolume;
        this.grossVolume = grossVolume;
        this.locationId = locationId;
        this.locationSize = locationSize;
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

    public Long getItemId() {
        return itemId;
    }

    public void setItemId(Long itemId) {
        this.itemId = itemId;
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

    public Long getLocationId() {
        return locationId;
    }

    public void setLocationId(Long locationId) {
        this.locationId = locationId;
    }

    public Double getLocationSize() {
        return locationSize;
    }

    public void setLocationSize(Double locationSize) {
        this.locationSize = locationSize;
    }

    public LocationUtilizationSnapshot getLocationUtilizationSnapshot() {
        return locationUtilizationSnapshot;
    }

    public void setLocationUtilizationSnapshot(LocationUtilizationSnapshot locationUtilizationSnapshot) {
        this.locationUtilizationSnapshot = locationUtilizationSnapshot;
    }
}

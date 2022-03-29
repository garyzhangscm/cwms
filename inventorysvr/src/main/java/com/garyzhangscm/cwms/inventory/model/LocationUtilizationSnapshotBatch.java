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

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.codehaus.jackson.annotate.JsonProperty;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "location_utilization_snapshot_batch")
public class LocationUtilizationSnapshotBatch extends AuditibleEntity<String>{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "location_utilization_snapshot_batch_id")
    @JsonProperty(value="id")
    private Long id;

    @Column(name = "warehouse_id")
    private Long warehouseId;

    @Column(name = "number")
    private String number;


    @Column(name = "net_volume")
    private Double netVolume;

    @Column(name = "gross_volume")
    private Double grossVolume;


    @Column(name = "total_locations")
    private Integer totalLocations;

    @Column(name = "start_time")
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private LocalDateTime startTime;

    @Column(name = "complete_time")
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private LocalDateTime completeTime;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    LocationUtilizationSnapshotStatus status;

    @OneToMany(
            mappedBy = "locationUtilizationSnapshotBatch",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    private List<ClientLocationUtilizationSnapshotBatch> clientLocationUtilizationSnapshotBatches = new ArrayList<>();
    public LocationUtilizationSnapshotBatch(){}

    public LocationUtilizationSnapshotBatch(Long warehouseId, String number) {
        this(warehouseId, number, 0.0, 0.0, 0, LocationUtilizationSnapshotStatus.PROCESSING,
                LocalDateTime.now());
    }

    public LocationUtilizationSnapshotBatch(Long warehouseId, String number, Double netVolume,
                                            Double grossVolume, Integer totalLocations,
                                            LocationUtilizationSnapshotStatus status,
                                            LocalDateTime startTime) {
        this.warehouseId = warehouseId;
        this.number = number;
        this.netVolume = netVolume;
        this.grossVolume = grossVolume;
        this.totalLocations = totalLocations;
        this.status = status;
        this.startTime = startTime;
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

    public List<ClientLocationUtilizationSnapshotBatch> getClientLocationUtilizationSnapshotBatches() {
        return clientLocationUtilizationSnapshotBatches;
    }

    public void setClientLocationUtilizationSnapshotBatches(List<ClientLocationUtilizationSnapshotBatch> clientLocationUtilizationSnapshotBatches) {
        this.clientLocationUtilizationSnapshotBatches = clientLocationUtilizationSnapshotBatches;
    }
    public void addClientLocationUtilizationSnapshotBatch(ClientLocationUtilizationSnapshotBatch clientLocationUtilizationSnapshotBatch) {
        this.clientLocationUtilizationSnapshotBatches.add(clientLocationUtilizationSnapshotBatch);
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getCompleteTime() {
        return completeTime;
    }

    public void setCompleteTime(LocalDateTime completeTime) {
        this.completeTime = completeTime;
    }

    public LocationUtilizationSnapshotStatus getStatus() {
        return status;
    }

    public void setStatus(LocationUtilizationSnapshotStatus status) {
        this.status = status;
    }
}

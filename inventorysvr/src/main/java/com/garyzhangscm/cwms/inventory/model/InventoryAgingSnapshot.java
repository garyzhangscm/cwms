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


import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.codehaus.jackson.annotate.JsonProperty;

import javax.persistence.*;
import java.io.Serializable;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;


@Entity
@Table(name = "inventory_aging_snapshot")
public class InventoryAgingSnapshot extends AuditibleEntity<String> implements Serializable {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "inventory_aging_snapshot_id")
    @JsonProperty(value="id")
    private Long id;

    @Column(name = "warehouse_id")
    private Long warehouseId;

    @Column(name = "number")
    private String number;


    @OneToMany(
            mappedBy = "inventoryAgingSnapshot",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    private List<ClientInventoryAgingSnapshot> clientInventoryAgingSnapshots = new ArrayList<>();


    @Column(name = "start_time")
    @JsonDeserialize(using = CustomZonedDateTimeDeserializer.class)
    @JsonSerialize(using = CustomZonedDateTimeSerializer.class)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private ZonedDateTime startTime;

    @Column(name = "complete_time")
    @JsonDeserialize(using = CustomZonedDateTimeDeserializer.class)
    @JsonSerialize(using = CustomZonedDateTimeSerializer.class)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private ZonedDateTime completeTime;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private InventoryAgingSnapshotStatus status;

    public InventoryAgingSnapshot(Long warehouseId, String number) {
        this(warehouseId, number, InventoryAgingSnapshotStatus.PROCESSING,
                ZonedDateTime.now(ZoneOffset.UTC));
    }

    public InventoryAgingSnapshot(Long warehouseId, String number,
                                  InventoryAgingSnapshotStatus status,
                                  ZonedDateTime startTime) {
        this.warehouseId = warehouseId;
        this.number = number;
        this.status = status;
        this.startTime = startTime;
    }


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public List<ClientInventoryAgingSnapshot> getClientInventoryAgingSnapshots() {
        return clientInventoryAgingSnapshots;
    }

    public void setClientInventoryAgingSnapshots(List<ClientInventoryAgingSnapshot> clientInventoryAgingSnapshots) {
        this.clientInventoryAgingSnapshots = clientInventoryAgingSnapshots;
    }

    public ZonedDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(ZonedDateTime startTime) {
        this.startTime = startTime;
    }

    public ZonedDateTime getCompleteTime() {
        return completeTime;
    }

    public void setCompleteTime(ZonedDateTime completeTime) {
        this.completeTime = completeTime;
    }

    public InventoryAgingSnapshotStatus getStatus() {
        return status;
    }

    public void setStatus(InventoryAgingSnapshotStatus status) {
        this.status = status;
    }

    public Long getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(Long warehouseId) {
        this.warehouseId = warehouseId;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }
}

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
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.codehaus.jackson.annotate.JsonProperty;

import javax.persistence.*;
import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Entity
@Table(name = "client_inventory_aging_snapshot")
public class ClientInventoryAgingSnapshot extends AuditibleEntity<String> implements Serializable {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "client_inventory_aging_snapshot_id")
    @JsonProperty(value="id")
    private Long id;

    @Column(name = "warehouse_id")
    private Long warehouseId;

    @Column(name = "client_id")
    private Long clientId;
    @Transient
    private Client client;

    @Column(name = "average_age_in_days")
    private Long averageAgeInDays;

    @Column(name = "average_age_in_weeks")
    private Long averageAgeInWeeks;


    @Transient
    private List<InventoryAgingByLPN> inventoryAgingByLPNS = new ArrayList<>();

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inventory_aging_snapshot_id")
    private InventoryAgingSnapshot inventoryAgingSnapshot;

    @OneToMany(
            mappedBy = "clientInventoryAgingSnapshot",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    private List<InventoryAgingSnapshotDetail> inventoryAgingSnapshotDetails = new ArrayList<>();

    public ClientInventoryAgingSnapshot(){}

    public ClientInventoryAgingSnapshot(Long warehouseId, Long clientId,
                                        InventoryAgingSnapshot inventoryAgingSnapshot) {
        this.warehouseId = warehouseId;
        this.clientId = clientId;
        this.averageAgeInDays = 0l;
        this.averageAgeInWeeks = 0l;
        this.inventoryAgingSnapshot = inventoryAgingSnapshot;
        this.inventoryAgingSnapshotDetails = new ArrayList<>();

    }

    public void setupInventoryAgingByLPN() {

        Map<String, InventoryAgingByLPN> inventoryAgingByLPNMap = new HashMap<>();

        getInventoryAgingSnapshotDetails().forEach(
                inventoryAgingSnapshotDetail -> {
                    InventoryAgingByLPN inventoryAgingByLPN = inventoryAgingByLPNMap.getOrDefault(inventoryAgingSnapshotDetail.getLpn(),
                            new InventoryAgingByLPN(inventoryAgingSnapshotDetail.getLpn()));

                    inventoryAgingByLPN.addInventoryAgingSnapshotDetail(inventoryAgingSnapshotDetail);
                    inventoryAgingByLPNMap.put(
                            inventoryAgingSnapshotDetail.getLpn(),
                            inventoryAgingByLPN
                    );
                }
        );
        inventoryAgingByLPNS = new ArrayList<>(inventoryAgingByLPNMap.values());

    }
    public void recalculateAverageAge() {
        this.averageAgeInDays = (long)
                this.getInventoryAgingSnapshotDetails().stream().map(
                        InventoryAgingSnapshotDetail::getAgeInDays
                ).mapToLong(Long::longValue).average().orElse(0.0);
        this.averageAgeInWeeks = (long)
                this.getInventoryAgingSnapshotDetails().stream().map(
                        InventoryAgingSnapshotDetail::getAgeInWeeks
                ).mapToLong(Long::longValue).average().orElse(0.0);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public InventoryAgingSnapshot getInventoryAgingSnapshot() {
        return inventoryAgingSnapshot;
    }

    public void setInventoryAgingSnapshot(InventoryAgingSnapshot inventoryAgingSnapshot) {
        this.inventoryAgingSnapshot = inventoryAgingSnapshot;
    }

    public List<InventoryAgingSnapshotDetail> getInventoryAgingSnapshotDetails() {
        return inventoryAgingSnapshotDetails;
    }

    public void setInventoryAgingSnapshotDetails(List<InventoryAgingSnapshotDetail> inventoryAgingSnapshotDetails) {
        this.inventoryAgingSnapshotDetails = inventoryAgingSnapshotDetails;
        // recalculate the average age in days and weeks once we changed the details
        recalculateAverageAge();
    }
    public void addInventoryAgingSnapshotDetail(InventoryAgingSnapshotDetail inventoryAgingSnapshotDetail) {
        this.inventoryAgingSnapshotDetails.add(inventoryAgingSnapshotDetail);
        inventoryAgingSnapshotDetail.setClientInventoryAgingSnapshot(this);
        // recalculate the average age in days and weeks once we changed the details
        recalculateAverageAge();
    }

    public Long getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(Long warehouseId) {
        this.warehouseId = warehouseId;
    }

    public Long getAverageAgeInDays() {
        return averageAgeInDays;
    }

    public void setAverageAgeInDays(Long averageAgeInDays) {
        this.averageAgeInDays = averageAgeInDays;
    }

    public Long getAverageAgeInWeeks() {
        return averageAgeInWeeks;
    }

    public void setAverageAgeInWeeks(Long averageAgeInWeeks) {
        this.averageAgeInWeeks = averageAgeInWeeks;
    }

    public List<InventoryAgingByLPN> getInventoryAgingByLPNS() {
        return inventoryAgingByLPNS;
    }

    public void setInventoryAgingByLPNS(List<InventoryAgingByLPN> inventoryAgingByLPNS) {
        this.inventoryAgingByLPNS = inventoryAgingByLPNS;
    }
}

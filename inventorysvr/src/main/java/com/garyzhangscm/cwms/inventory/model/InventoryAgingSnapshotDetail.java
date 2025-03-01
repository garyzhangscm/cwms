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


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Objects;


@Entity
@Table(name = "inventory_aging_snapshot_detail")
public class InventoryAgingSnapshotDetail extends AuditibleEntity<String> implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "inventory_aging_snapshot_detail_id")
    @JsonProperty(value="id")
    private Long id;

    @ManyToOne
    @JoinColumn(name="inventory_id")
    private Inventory inventory;

    @Column(name = "lpn")
    private String lpn;

    @Column(name = "client_id")
    private Long clientId;

    @Column(name = "quantity")
    private Long quantity;

    @Column(name = "age_in_days")
    private Long ageInDays;

    @Column(name = "age_in_weeks")
    private Long ageInWeeks;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_inventory_aging_snapshot_id")
    private ClientInventoryAgingSnapshot clientInventoryAgingSnapshot;

    public InventoryAgingSnapshotDetail() {}
    public InventoryAgingSnapshotDetail(Inventory inventory) {
        this.inventory = inventory;
        this.lpn = inventory.getLpn();
        this.quantity = inventory.getQuantity();
        this.clientId = inventory.getClientId();

        if (Objects.isNull(inventory.getCreatedTime())) {
            this.ageInDays = 0l;
            this.ageInWeeks = 0l;
        }
        else {
            this.ageInDays = ChronoUnit.DAYS.between(
                    inventory.getCreatedTime().toLocalDate(),
                    LocalDateTime.now().toLocalDate());
            this.ageInWeeks = (long)Math.ceil(this.ageInDays * 1.0 / 7);
        }
    }


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Inventory getInventory() {
        return inventory;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    public Long getAgeInDays() {
        return ageInDays;
    }

    public void setAgeInDays(Long ageInDays) {
        this.ageInDays = ageInDays;
    }

    public Long getAgeInWeeks() {
        return ageInWeeks;
    }

    public void setAgeInWeeks(Long ageInWeeks) {
        this.ageInWeeks = ageInWeeks;
    }

    public ClientInventoryAgingSnapshot getClientInventoryAgingSnapshot() {
        return clientInventoryAgingSnapshot;
    }

    public void setClientInventoryAgingSnapshot(ClientInventoryAgingSnapshot clientInventoryAgingSnapshot) {
        this.clientInventoryAgingSnapshot = clientInventoryAgingSnapshot;
    }

    public String getLpn() {
        return lpn;
    }

    public void setLpn(String lpn) {
        this.lpn = lpn;
    }

    public Long getClientId() {
        return clientId;
    }

    public void setClientId(Long clientId) {
        this.clientId = clientId;
    }

    public Long getQuantity() {
        return quantity;
    }

    public void setQuantity(Long quantity) {
        this.quantity = quantity;
    }
}

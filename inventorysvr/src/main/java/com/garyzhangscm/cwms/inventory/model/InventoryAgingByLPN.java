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


import java.io.Serializable; 

public class InventoryAgingByLPN implements Serializable {


    private String lpn;


    private Long quantity;

    private Long ageInDays;

    private Long ageInWeeks;

    public InventoryAgingByLPN() {}

    public InventoryAgingByLPN(String lpn) {
        this(lpn, 0l, 0l, 0l);

    }

    public InventoryAgingByLPN(String lpn, Long quantity, Long ageInDays, Long ageInWeeks) {
        this.lpn = lpn;
        this.quantity = quantity;
        this.ageInDays = ageInDays;
        this.ageInWeeks = ageInWeeks;
    }

    public String getLpn() {
        return lpn;
    }

    public void setLpn(String lpn) {
        this.lpn = lpn;
    }

    public Long getQuantity() {
        return quantity;
    }

    public void setQuantity(Long quantity) {
        this.quantity = quantity;
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

    public void addInventoryAgingSnapshotDetail(InventoryAgingSnapshotDetail inventoryAgingSnapshotDetail) {
        if (getLpn().equals(inventoryAgingSnapshotDetail.getLpn())) {
            this.quantity += inventoryAgingSnapshotDetail.getQuantity();
            // age in days is the max of the age in days of the inventory on the LPN
            this.ageInDays = Math.max(this.ageInDays, inventoryAgingSnapshotDetail.getAgeInDays());
            this.ageInWeeks = (long)Math.ceil(this.ageInDays * 1.0 / 7);
        }
    }
}

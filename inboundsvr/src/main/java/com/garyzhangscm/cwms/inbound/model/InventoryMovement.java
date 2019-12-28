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

package com.garyzhangscm.cwms.inbound.model;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;

public class InventoryMovement implements Serializable {

    private static final Logger logger = LoggerFactory.getLogger(InventoryMovement.class);

    private Long id;

    private Inventory inventory;

    private Long locationId;

    private Location location;

    private Integer sequence;

    public String toString() {
        return "id: " + id + "\n"
                + "Inventory: " + "\n"
                + " >> LPN: " + inventory.getLpn()  + "\n"
                + " >> item: " + inventory.getItem().getName()  + "\n"
                + " >> item description : " + inventory.getItem().getDescription()  + "\n"
                + " >> inventory status : " + inventory.getInventoryStatus().getName()  + "\n"
                + " >> quantity : " + inventory.getQuantity()  + "\n"
                + "Sequence : " + sequence   + "\n"
                + "Location: " + (location == null ? "" :  location.getName())  + "\n"
                + "Location ID: " + locationId;
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

    public Integer getSequence() {
        return sequence;
    }

    public void setSequence(Integer sequence) {
        this.sequence = sequence;
    }
}

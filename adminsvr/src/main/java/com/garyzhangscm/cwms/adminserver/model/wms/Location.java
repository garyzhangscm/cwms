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

package com.garyzhangscm.cwms.adminserver.model.wms;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Location {
    private Long id;
    private String name;

    private String aisle;

    private Double x;
    private Double y;
    private Double z;

    private Double length;
    private Double width;
    private Double height;

    private Long pickSequence;
    private Long putawaySequence;
    private Long countSequence;

    private Double capacity;
    private Double fillPercentage;

    private Double currentVolume;

    private Double pendingVolume;

    private LocationGroup locationGroup;

    private Boolean enabled;

    private String reservedCode;

    private Boolean locked;

    private Warehouse warehouse;

    public Location() {}
    public Location(Warehouse warehouse, LocationGroup locationGroup,
                    String name, int sequence) {
        this.warehouse = warehouse;
        this.locationGroup = locationGroup;
        this.name = name;

        this.aisle = "";

        this.x = 0.0;
        this.y = 0.0;
        this.z = 0.0;

        this.length = 64.0;
        this.width = 64.0;
        this.height = 64.0;

        this.pickSequence = (long)sequence;
        this.putawaySequence = (long)sequence;
        this.countSequence = (long)sequence;

        this.capacity = 262144.0;
        this.fillPercentage= 100.0;

        this.currentVolume = 0.0;

        this.pendingVolume = 0.0;
        this.enabled = true;

        this.reservedCode = "";

        this.locked = false;

    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAisle() {
        return aisle;
    }

    public void setAisle(String aisle) {
        this.aisle = aisle;
    }

    public Double getX() {
        return x;
    }

    public void setX(Double x) {
        this.x = x;
    }

    public Double getY() {
        return y;
    }

    public void setY(Double y) {
        this.y = y;
    }

    public Double getZ() {
        return z;
    }

    public void setZ(Double z) {
        this.z = z;
    }

    public Double getLength() {
        return length;
    }

    public void setLength(Double length) {
        this.length = length;
    }

    public Double getWidth() {
        return width;
    }

    public void setWidth(Double width) {
        this.width = width;
    }

    public Double getHeight() {
        return height;
    }

    public void setHeight(Double height) {
        this.height = height;
    }

    public Long getPickSequence() {
        return pickSequence;
    }

    public void setPickSequence(Long pickSequence) {
        this.pickSequence = pickSequence;
    }

    public Long getPutawaySequence() {
        return putawaySequence;
    }

    public void setPutawaySequence(Long putawaySequence) {
        this.putawaySequence = putawaySequence;
    }

    public Long getCountSequence() {
        return countSequence;
    }

    public void setCountSequence(Long countSequence) {
        this.countSequence = countSequence;
    }

    public Double getCapacity() {
        return capacity;
    }

    public void setCapacity(Double capacity) {
        this.capacity = capacity;
    }

    public Double getFillPercentage() {
        return fillPercentage;
    }

    public void setFillPercentage(Double fillPercentage) {
        this.fillPercentage = fillPercentage;
    }

    public Double getCurrentVolume() {
        return currentVolume;
    }

    public void setCurrentVolume(Double currentVolume) {
        this.currentVolume = currentVolume;
    }

    public Double getPendingVolume() {
        return pendingVolume;
    }

    public void setPendingVolume(Double pendingVolume) {
        this.pendingVolume = pendingVolume;
    }

    public LocationGroup getLocationGroup() {
        return locationGroup;
    }

    public void setLocationGroup(LocationGroup locationGroup) {
        this.locationGroup = locationGroup;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public String getReservedCode() {
        return reservedCode;
    }

    public void setReservedCode(String reservedCode) {
        this.reservedCode = reservedCode;
    }

    public Boolean getLocked() {
        return locked;
    }

    public void setLocked(Boolean locked) {
        this.locked = locked;
    }

    public Warehouse getWarehouse() {
        return warehouse;
    }

    public void setWarehouse(Warehouse warehouse) {
        this.warehouse = warehouse;
    }
}

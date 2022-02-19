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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

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

    private Warehouse warehouse;
    private Boolean locked;

    @Override
    public Object clone() {
        Location location = null;
        try {
            location = (Location) super.clone();
        }
        catch(CloneNotSupportedException e) {
            location = new Location();
            location.setName(name);

            location.setAisle(aisle);

            location.setX(x);
            location.setY(y);
            location.setZ(z);

            location.setLength(length);
            location.setWidth(width);
            location.setHeight(height);

            location.setPickSequence(pickSequence);
            location.setPutawaySequence(putawaySequence);
            location.setCountSequence(countSequence);

            location.setCapacity(capacity);
            location.setFillPercentage(fillPercentage);

            location.setCurrentVolume(currentVolume);

            location.setPendingVolume(pendingVolume);
            location.setLocationGroup(locationGroup);

            location.setEnabled(enabled);

            location.setReservedCode(reservedCode);
        }
        return location;
    }

    @Override
    public String toString() {
        try {
            return new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(this);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return null;
    }

    @JsonIgnore
    public boolean hasInventory() {
        return getCurrentVolume() > 0.0;
    }

    @JsonIgnore
    public boolean isEmpty() {
        return getCurrentVolume() == 0 && getPendingVolume() == 0;
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

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
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

    public String getReservedCode() {
        return reservedCode;
    }

    public void setReservedCode(String reservedCode) {
        this.reservedCode = reservedCode;
    }

    public Warehouse getWarehouse() {
        return warehouse;
    }

    public void setWarehouse(Warehouse warehouse) {
        this.warehouse = warehouse;
    }

    public Boolean getLocked() {
        return locked;
    }

    public void setLocked(Boolean locked) {
        this.locked = locked;
    }
}

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

package com.garyzhangscm.cwms.resources.model;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Location extends AuditibleEntity<String>{
    private Long id;

    private String name;
    private String code;

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


    private Boolean enabled = true;

    private String reservedCode;

    private Boolean locked = false;

    private LocationGroup locationGroup;

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

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
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

    public LocationGroup getLocationGroup() {
        return locationGroup;
    }

    public void setLocationGroup(LocationGroup locationGroup) {
        this.locationGroup = locationGroup;
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
}

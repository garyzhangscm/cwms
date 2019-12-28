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

package com.garyzhangscm.cwms.layout.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;

import javax.persistence.*;

@Entity
@Table(name = "location")
public class Location {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "location_id")
    @JsonProperty(value="id")
    private Long id;

    @Column(name = "name", unique = true)
    private String name;

    @Column(name = "aisle")
    private String aisle;

    @Column(name = "x")
    private Double x;
    @Column(name = "y")
    private Double y;
    @Column(name = "z")
    private Double z;

    @Column(name = "length")
    private Double length;
    @Column(name = "width")
    private Double width;
    @Column(name = "height")
    private Double height;

    @Column(name = "pick_sequence")
    private Long pickSequence;
    @Column(name = "putaway_sequence")
    private Long putawaySequence;
    @Column(name = "count_sequence")
    private Long countSequence;

    @Column(name = "capacity")
    private Double capacity;
    @Column(name = "fill_percentage")
    private Double fillPercentage;

    @Column(name = "current_volume")
    private Double currentVolume;

    @Column(name = "pending_volume")
    private Double pendingVolume;

    @ManyToOne
    @JoinColumn(name="location_group_id")
    private LocationGroup locationGroup;

    @Column(name = "enabled")
    private Boolean enabled;

    @Column(name = "reserved_code")
    private String reservedCode;

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

    public String getReservedCode() {
        return reservedCode;
    }

    public void setReservedCode(String reservedCode) {
        this.reservedCode = reservedCode;
    }

}

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

package com.garyzhangscm.cwms.common.model;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@JsonIgnoreProperties(ignoreUnknown = true)
public class LocationGroup {

    private Long id;

    private String name;

    private String description;

    private LocationGroupType locationGroupType;

    private Boolean pickable;
    private Boolean storable;
    private Boolean countable;
    private Boolean allowCartonization;
    private Boolean trackingVolume;
    private Boolean consolidateLpn;

    private LocationVolumeTrackingPolicy volumeTrackingPolicy;

    @Override
    public String toString() {
        try {
            return new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(this);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return null;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocationGroupType getLocationGroupType() {
        return locationGroupType;
    }

    public void setLocationGroupType(LocationGroupType locationGroupType) {
        this.locationGroupType = locationGroupType;
    }

    public Boolean getPickable() {
        return pickable;
    }

    public void setPickable(Boolean pickable) {
        this.pickable = pickable;
    }

    public Boolean getStorable() {
        return storable;
    }

    public void setStorable(Boolean storable) {
        this.storable = storable;
    }

    public Boolean getCountable() {
        return countable;
    }

    public void setCountable(Boolean countable) {
        this.countable = countable;
    }

    public Boolean getTrackingVolume() {
        return trackingVolume;
    }

    public void setTrackingVolume(Boolean trackingVolume) {
        this.trackingVolume = trackingVolume;
    }

    public Boolean getConsolidateLpn() {
        return consolidateLpn;
    }

    public void setConsolidateLpn(Boolean consolidateLpn) {
        this.consolidateLpn = consolidateLpn;
    }

    public Boolean getAllowCartonization() {
        return allowCartonization;
    }

    public void setAllowCartonization(Boolean allowCartonization) {
        this.allowCartonization = allowCartonization;
    }

    public LocationVolumeTrackingPolicy getVolumeTrackingPolicy() {
        return volumeTrackingPolicy;
    }

    public void setVolumeTrackingPolicy(LocationVolumeTrackingPolicy volumeTrackingPolicy) {
        this.volumeTrackingPolicy = volumeTrackingPolicy;
    }
}

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

package com.garyzhangscm.cwms.workorder.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.Serializable;

public class SiloDevice implements Serializable {

    @JsonProperty("LOCATION_NAME")
    private String locationName;
    @JsonProperty("NAME")
    private String name;
    @JsonProperty("DEVICE_ID")
    private Integer deviceId;
    @JsonProperty("MATERIAL")
    private String material;
    @JsonProperty("itemName")
    private String itemName;
    @JsonProperty("DISTANCE")
    private Double distance;
    @JsonProperty("SMU_TIMESTAMP")
    private Long timeStamp;
    @JsonProperty("STATUS_CDE")
    private String statusCode;


    public SiloDevice() {

    }
    public SiloDevice(SiloDeviceAPICallHistory siloDeviceAPICallHistory) {

        setLocationName(siloDeviceAPICallHistory.getLocationName());
        setName(siloDeviceAPICallHistory.getName());
        setDeviceId(siloDeviceAPICallHistory.getDeviceId());
        setMaterial(siloDeviceAPICallHistory.getMaterial());
        setItemName(siloDeviceAPICallHistory.getItemName());
        setDistance(siloDeviceAPICallHistory.getDistance());
        setTimeStamp(siloDeviceAPICallHistory.getTimeStamp());
        setStatusCode(siloDeviceAPICallHistory.getStatusCode());
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


    public String getLocationName() {
        return locationName;
    }

    public void setLocationName(String locationName) {
        this.locationName = locationName;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(Integer deviceId) {
        this.deviceId = deviceId;
    }

    public String getMaterial() {
        return material;
    }

    public void setMaterial(String material) {
        this.material = material;
    }

    public Double getDistance() {
        return distance;
    }

    public void setDistance(Double distance) {
        this.distance = distance;
    }

    public Long getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(Long timeStamp) {
        this.timeStamp = timeStamp;
    }

    public String getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(String statusCode) {
        this.statusCode = statusCode;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }
}

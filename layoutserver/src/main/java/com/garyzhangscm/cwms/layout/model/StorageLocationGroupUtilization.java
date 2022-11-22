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


public class StorageLocationGroupUtilization {

    private String locationGroupName;

    private Integer emptyLocation;

    private Integer partialLocation;

    private Integer fullLocation;

    public StorageLocationGroupUtilization() {}

    public StorageLocationGroupUtilization(String locationGroupName, Integer emptyLocation, Integer partialLocation, Integer fullLocation) {
        this.locationGroupName = locationGroupName;
        this.emptyLocation = emptyLocation;
        this.partialLocation = partialLocation;
        this.fullLocation = fullLocation;
    }

    public String getLocationGroupName() {
        return locationGroupName;
    }

    public void setLocationGroupName(String locationGroupName) {
        this.locationGroupName = locationGroupName;
    }

    public Integer getEmptyLocation() {
        return emptyLocation;
    }

    public void setEmptyLocation(Integer emptyLocation) {
        this.emptyLocation = emptyLocation;
    }

    public Integer getPartialLocation() {
        return partialLocation;
    }

    public void setPartialLocation(Integer partialLocation) {
        this.partialLocation = partialLocation;
    }

    public Integer getFullLocation() {
        return fullLocation;
    }

    public void setFullLocation(Integer fullLocation) {
        this.fullLocation = fullLocation;
    }
}

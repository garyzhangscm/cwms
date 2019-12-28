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

public class MovementPathCSVWrapper {

    private String fromLocation;
    private String toLocation;
    private String fromLocationGroup;
    private String toLocationGroup;
    private String sequence;
    private String hopLocation;
    private String hopLocationGroup;
    private String strategy;

    @Override
    public String toString() {
        return "MovementPathCSVWrapper: { "
                + "fromLocation: " + fromLocation + ","
                + "toLocation: " + toLocation+ ","
                + "fromLocationGroup: " + fromLocationGroup+ ","
                + "toLocationGroup: " + toLocationGroup+ ","
                + "sequence: " + sequence+ ","
                + "hopLocation: " + hopLocation+ ","
                + "hopLocationGroup: " + hopLocationGroup+ ","
                + "strategy: " + strategy + "}";
    }

    public String getFromLocation() {
        return fromLocation;
    }

    public void setFromLocation(String fromLocation) {
        this.fromLocation = fromLocation;
    }

    public String getToLocation() {
        return toLocation;
    }

    public void setToLocation(String toLocation) {
        this.toLocation = toLocation;
    }

    public String getFromLocationGroup() {
        return fromLocationGroup;
    }

    public void setFromLocationGroup(String fromLocationGroup) {
        this.fromLocationGroup = fromLocationGroup;
    }

    public String getToLocationGroup() {
        return toLocationGroup;
    }

    public void setToLocationGroup(String toLocationGroup) {
        this.toLocationGroup = toLocationGroup;
    }

    public String getSequence() {
        return sequence;
    }

    public void setSequence(String sequence) {
        this.sequence = sequence;
    }

    public String getHopLocation() {
        return hopLocation;
    }

    public void setHopLocation(String hopLocation) {
        this.hopLocation = hopLocation;
    }

    public String getHopLocationGroup() {
        return hopLocationGroup;
    }

    public void setHopLocationGroup(String hopLocationGroup) {
        this.hopLocationGroup = hopLocationGroup;
    }

    public String getStrategy() {
        return strategy;
    }

    public void setStrategy(String strategy) {
        this.strategy = strategy;
    }
}

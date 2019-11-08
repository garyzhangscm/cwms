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

import org.codehaus.jackson.annotate.JsonProperty;

import javax.persistence.*;

@Entity
@Table(name = "location_group_type")
public class LocationGroupType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "location_group_type_id")
    @JsonProperty(value="id")
    private Long id;

    @Column(name = "name")
    private String name;


    @Column(name = "description")
    private String description;


    @Column(name = "four_wall_inventory")
    private Boolean fourWallInventory;


    @Column(name = "virtual_locations")
    private Boolean virtual;

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

    public Boolean getFourWallInventory() {
        return fourWallInventory;
    }

    public void setFourWallInventory(Boolean fourWallInventory) {
        this.fourWallInventory = fourWallInventory;
    }

    public Boolean getVirtual() {
        return virtual;
    }

    public void setVirtual(Boolean virtual) {
        this.virtual = virtual;
    }

}

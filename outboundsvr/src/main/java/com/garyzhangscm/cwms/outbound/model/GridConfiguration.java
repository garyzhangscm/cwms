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

package com.garyzhangscm.cwms.outbound.model;

import org.codehaus.jackson.annotate.JsonProperty;

import javax.persistence.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "grid_configuration")
public class GridConfiguration implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "grid_configuration_id")
    @JsonProperty(value="id")
    private Long id;

    @Column(name = "warehouse_id")
    private Long warehouseId;

    @Transient
    private Warehouse warehouse;

    @Column(name = "location_group_id")
    private Long locationGroupId;

    @Transient
    private LocationGroup locationGroup;

    // Whether user choose a location to distribute
    // the inventory, or the system decides the
    // location
    @Column(name = "pre_assigned_location")
    private Boolean preAssignedLocation;

    // If we allow the user to deposit by
    // list / carton / any other type of group
    // or only allow deposit item by item
    @Column(name = "allow_confirm_by_group")
    private Boolean allowConfirmByGroup;

    // Only move the inventory to the grid
    // when get a confirmation.
    // If not setup, we will confirm the movement
    // when the user scan in the item number
    // or some group id(carton number / list id)
    @Column(name = "deposit_on_confirm")
    private Boolean depositOnConfirm;

    @Override
    public String toString() {
        return "GridConfiguration{" +
                "id=" + id +
                ", warehouseId=" + warehouseId +
                ", warehouse=" + warehouse +
                ", locationGroupId=" + locationGroupId +
                ", locationGroup=" + locationGroup +
                ", preAssignedLocation=" + preAssignedLocation +
                ", allowConfirmByGroup=" + allowConfirmByGroup +
                ", depositOnConfirm=" + depositOnConfirm +
                '}';
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getLocationGroupId() {
        return locationGroupId;
    }

    public void setLocationGroupId(Long locationGroupId) {
        this.locationGroupId = locationGroupId;
    }

    public LocationGroup getLocationGroup() {
        return locationGroup;
    }

    public void setLocationGroup(LocationGroup locationGroup) {
        this.locationGroup = locationGroup;
    }

    public Boolean getPreAssignedLocation() {
        return preAssignedLocation;
    }

    public void setPreAssignedLocation(Boolean preAssignedLocation) {
        this.preAssignedLocation = preAssignedLocation;
    }

    public Boolean getAllowConfirmByGroup() {
        return allowConfirmByGroup;
    }

    public void setAllowConfirmByGroup(Boolean allowConfirmByGroup) {
        this.allowConfirmByGroup = allowConfirmByGroup;
    }

    public Boolean getDepositOnConfirm() {
        return depositOnConfirm;
    }

    public void setDepositOnConfirm(Boolean depositOnConfirm) {
        this.depositOnConfirm = depositOnConfirm;
    }

    public Long getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(Long warehouseId) {
        this.warehouseId = warehouseId;
    }

    public Warehouse getWarehouse() {
        return warehouse;
    }

    public void setWarehouse(Warehouse warehouse) {
        this.warehouse = warehouse;
    }
}

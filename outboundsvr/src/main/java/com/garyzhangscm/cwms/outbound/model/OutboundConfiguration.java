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

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.persistence.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "outbound_configuration")
public class OutboundConfiguration extends AuditibleEntity<String> implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "outbound_configuration_id")
    @JsonProperty(value="id")
    private Long id;

    @Column(name = "warehouse_id")
    private Long warehouseId;

    @Transient
    private Warehouse warehouse;

    @Column(name = "short_auto_reallocation")
    private Boolean shortAutoReallocation;

    @Column(name = "asynchronous_allocation")
    private Boolean asynchronousAllocation;

    // only allow asynchronous allocation when the overall quantity
    // is greater than the threshold
    @Column(name = "asynchronous_allocation_pallet_threshold")
    private Integer asynchronousAllocationPalletThreshold;


    @Column(name = "max_pallet_size")
    private Double maxPalletSize;

    @Column(name = "max_pallet_height")
    private Double maxPalletHeight;


    // before which status do we allow the user to change the order
    // if it is empty, then we are not allow the user to change the
    // order once it is created
    // we have different configuration for
    // upload files
    // integration
    // manually change from web UI
    @Column(name = "status_allow_order_change_when_upload_file")
    @Enumerated(EnumType.STRING)
    private OrderStatus statusAllowOrderChangeWhenUploadFile;
    @Column(name = "status_allow_order_change_from_integration")
    @Enumerated(EnumType.STRING)
    private OrderStatus statusAllowOrderChangeFromIntegration;
    @Column(name = "status_allow_order_change_from_web_ui")
    @Enumerated(EnumType.STRING)
    private OrderStatus statusAllowOrderChangeFromWebUI;


    @Column(name = "complete_order_and_shipment_when_complete_wave")
    private Boolean completeOrderAndShipmentWhenCompleteWave;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(Long warehouseId) {
        this.warehouseId = warehouseId;
    }

    public Boolean getShortAutoReallocation() {
        return shortAutoReallocation;
    }

    public void setShortAutoReallocation(Boolean shortAutoReallocation) {
        this.shortAutoReallocation = shortAutoReallocation;
    }

    public Warehouse getWarehouse() {
        return warehouse;
    }

    public void setWarehouse(Warehouse warehouse) {
        this.warehouse = warehouse;
    }

    public Boolean getAsynchronousAllocation() {
        return asynchronousAllocation;
    }

    public void setAsynchronousAllocation(Boolean asynchronousAllocation) {
        this.asynchronousAllocation = asynchronousAllocation;
    }

    public Integer getAsynchronousAllocationPalletThreshold() {
        return asynchronousAllocationPalletThreshold;
    }

    public void setAsynchronousAllocationPalletThreshold(Integer asynchronousAllocationPalletThreshold) {
        this.asynchronousAllocationPalletThreshold = asynchronousAllocationPalletThreshold;
    }

    public Double getMaxPalletSize() {
        return maxPalletSize;
    }

    public void setMaxPalletSize(Double maxPalletSize) {
        this.maxPalletSize = maxPalletSize;
    }

    public Double getMaxPalletHeight() {
        return maxPalletHeight;
    }

    public void setMaxPalletHeight(Double maxPalletHeight) {
        this.maxPalletHeight = maxPalletHeight;
    }

    public OrderStatus getStatusAllowOrderChangeWhenUploadFile() {
        return statusAllowOrderChangeWhenUploadFile;
    }

    public void setStatusAllowOrderChangeWhenUploadFile(OrderStatus statusAllowOrderChangeWhenUploadFile) {
        this.statusAllowOrderChangeWhenUploadFile = statusAllowOrderChangeWhenUploadFile;
    }

    public OrderStatus getStatusAllowOrderChangeFromIntegration() {
        return statusAllowOrderChangeFromIntegration;
    }

    public void setStatusAllowOrderChangeFromIntegration(OrderStatus statusAllowOrderChangeFromIntegration) {
        this.statusAllowOrderChangeFromIntegration = statusAllowOrderChangeFromIntegration;
    }

    public OrderStatus getStatusAllowOrderChangeFromWebUI() {
        return statusAllowOrderChangeFromWebUI;
    }

    public void setStatusAllowOrderChangeFromWebUI(OrderStatus statusAllowOrderChangeFromWebUI) {
        this.statusAllowOrderChangeFromWebUI = statusAllowOrderChangeFromWebUI;
    }

    public Boolean getCompleteOrderAndShipmentWhenCompleteWave() {
        return completeOrderAndShipmentWhenCompleteWave;
    }

    public void setCompleteOrderAndShipmentWhenCompleteWave(Boolean completeOrderAndShipmentWhenCompleteWave) {
        this.completeOrderAndShipmentWhenCompleteWave = completeOrderAndShipmentWhenCompleteWave;
    }
}

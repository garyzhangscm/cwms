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


import javax.persistence.*;

/**
 * Default configuration on RFs
 */
@Entity
@Table(name = "rf_configuration")
public class RFConfiguration extends AuditibleEntity<String>  {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "rf_configuration_id")
    private Long id;

    @Column(name = "warehouse_id")
    private Long warehouseId;

    // can be override by RF
    @Column(name = "rf_code")
    private String rfCode;

    // work order - manual pick, whether
    // check the partial LPN if whole LPN is too much
    // for the current pick
    @Column(name = "work_order_validate_partial_lpn_pick")
    private Boolean workOrderValidatePartialLPNPick = false;

    // work order
    // whether pick directly to the production line, or
    // pick to the RF then ask the user to drop to the stage
    @Column(name = "work_order_pick_to_production_line_in_stage")
    private Boolean pickToProductionLineInStage = false;


    // receiving
    // whether receive directly to the receive stage, or
    // pick to the RF then ask the user to drop to somewhere
    // Note, if this is enabled, it will choose the first location
    // from the location groups that marked as receiving stage
    @Column(name = "receiving_receive_to_stage")
    private Boolean receiveToStage = false;


    // Outbound
    // list pick - batch pick when picking from same
    //    location and attribute
    @Column(name = "list_pick_batch_picking")
    private Boolean listPickBatchPicking = true;

    @Column(name = "printer_name")
    private String printerName;

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

    public String getRfCode() {
        return rfCode;
    }

    public void setRfCode(String rfCode) {
        this.rfCode = rfCode;
    }

    public Boolean getWorkOrderValidatePartialLPNPick() {
        return workOrderValidatePartialLPNPick;
    }

    public void setWorkOrderValidatePartialLPNPick(Boolean workOrderValidatePartialLPNPick) {
        this.workOrderValidatePartialLPNPick = workOrderValidatePartialLPNPick;
    }

    public Boolean getPickToProductionLineInStage() {
        return pickToProductionLineInStage;
    }

    public void setPickToProductionLineInStage(Boolean pickToProductionLineInStage) {
        this.pickToProductionLineInStage = pickToProductionLineInStage;
    }

    public Boolean getReceiveToStage() {
        return receiveToStage;
    }

    public void setReceiveToStage(Boolean receiveToStage) {
        this.receiveToStage = receiveToStage;
    }

    public Boolean getListPickBatchPicking() {
        return listPickBatchPicking;
    }

    public void setListPickBatchPicking(Boolean listPickBatchPicking) {
        this.listPickBatchPicking = listPickBatchPicking;
    }

    public String getPrinterName() {
        return printerName;
    }

    public void setPrinterName(String printerName) {
        this.printerName = printerName;
    }
}

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

package com.garyzhangscm.cwms.layout.model;


import org.codehaus.jackson.annotate.JsonProperty;

import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "warehouse_configuration")
public class WarehouseConfiguration extends AuditibleEntity<String> implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "warehouse_configuration_id")
    @JsonProperty(value="id")
    private Long id;

    @OneToOne
    @JoinColumn(name = "warehouse_id")
    private Warehouse warehouse;


    // whether this warehouse is serving as
    // 3pl warehouse
    @Column(name = "three_party_logistics_flag")
    private Boolean threePartyLogisticsFlag;

    // whether list pick is enabled for this warehouse
    @Column(name = "list_pick_enabled_flag")
    private Boolean listPickEnabledFlag = false;


    @Column(name = "printing_strategy")
    @Enumerated(EnumType.STRING)
    private PrintingStrategy printingStrategy;

    @Column(name = "new_lpn_print_label_at_receiving_flag")
    private Boolean newLPNPrintLabelAtReceivingFlag;
    @Column(name = "new_lpn_print_label_at_producing_flag")
    private Boolean newLPNPrintLabelAtProducingFlag;
    @Column(name = "new_lpn_print_label_at_adjustment_flag")
    private Boolean newLPNPrintLabelAtAdjustmentFlag;


    // whether we will reuse lpn after removed by inventory adjust
    // or reverse receipt / product
    @Column(name = "reuse_lpn_after_removed")
    private Boolean reuseLPNAfterRemovedFlag;

    // whether we will reuse lpn after shipped
    @Column(name = "reuse_lpn_after_shipped")
    private Boolean reuseLPNAfterShippedFlag;

    // whether we will reuse lpn after shipped
    @Column(name = "billing_request_enabled")
    private Boolean billingRequestEnabledFlag;

    @Column(name = "location_utilization_snapshot_enabled")
    private Boolean locationUtilizationSnapshotEnabled;
    @Column(name = "inventory_aging_snapshot_enabled")
    private Boolean inventoryAgingSnapshotEnabled;


    // define week days
    @Column(name = "working_on_sunday")
    private Boolean workingOnSundayFlag;
    @Column(name = "working_on_monday")
    private Boolean workingOnMondayFlag;
    @Column(name = "working_on_tuesday")
    private Boolean workingOnTuesdayFlag;
    @Column(name = "working_on_wednesday")
    private Boolean workingOnWednesdayFlag;
    @Column(name = "working_on_thursday")
    private Boolean workingOnThursdayFlag;
    @Column(name = "working_on_friday")
    private Boolean workingOnFridayFlag;
    @Column(name = "working_on_saturday")
    private Boolean workingOnSaturdayFlag;


    @Column(name = "time_zone")
    private String timeZone = "UTC";

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Warehouse getWarehouse() {
        return warehouse;
    }

    public void setWarehouse(Warehouse warehouse) {
        this.warehouse = warehouse;
    }

    public Boolean getThreePartyLogisticsFlag() {
        return threePartyLogisticsFlag;
    }

    public void setThreePartyLogisticsFlag(Boolean threePartyLogisticsFlag) {
        this.threePartyLogisticsFlag = threePartyLogisticsFlag;
    }

    public Boolean getListPickEnabledFlag() {
        return listPickEnabledFlag;
    }

    public void setListPickEnabledFlag(Boolean listPickEnabledFlag) {
        this.listPickEnabledFlag = listPickEnabledFlag;
    }

    public PrintingStrategy getPrintingStrategy() {
        return printingStrategy;
    }

    public void setPrintingStrategy(PrintingStrategy printingStrategy) {
        this.printingStrategy = printingStrategy;
    }

    public Boolean getNewLPNPrintLabelAtReceivingFlag() {
        return newLPNPrintLabelAtReceivingFlag;
    }

    public void setNewLPNPrintLabelAtReceivingFlag(Boolean newLPNPrintLabelAtReceivingFlag) {
        this.newLPNPrintLabelAtReceivingFlag = newLPNPrintLabelAtReceivingFlag;
    }

    public Boolean getNewLPNPrintLabelAtProducingFlag() {
        return newLPNPrintLabelAtProducingFlag;
    }

    public void setNewLPNPrintLabelAtProducingFlag(Boolean newLPNPrintLabelAtProducingFlag) {
        this.newLPNPrintLabelAtProducingFlag = newLPNPrintLabelAtProducingFlag;
    }

    public Boolean getNewLPNPrintLabelAtAdjustmentFlag() {
        return newLPNPrintLabelAtAdjustmentFlag;
    }

    public void setNewLPNPrintLabelAtAdjustmentFlag(Boolean newLPNPrintLabelAtAdjustmentFlag) {
        this.newLPNPrintLabelAtAdjustmentFlag = newLPNPrintLabelAtAdjustmentFlag;
    }

    public Boolean getReuseLPNAfterRemovedFlag() {
        return reuseLPNAfterRemovedFlag;
    }

    public void setReuseLPNAfterRemovedFlag(Boolean reuseLPNAfterRemovedFlag) {
        this.reuseLPNAfterRemovedFlag = reuseLPNAfterRemovedFlag;
    }

    public Boolean getReuseLPNAfterShippedFlag() {
        return reuseLPNAfterShippedFlag;
    }

    public void setReuseLPNAfterShippedFlag(Boolean reuseLPNAfterShippedFlag) {
        this.reuseLPNAfterShippedFlag = reuseLPNAfterShippedFlag;
    }

    public Boolean getBillingRequestEnabledFlag() {
        return billingRequestEnabledFlag;
    }

    public void setBillingRequestEnabledFlag(Boolean billingRequestEnabledFlag) {
        this.billingRequestEnabledFlag = billingRequestEnabledFlag;
    }

    public Boolean getWorkingOnSundayFlag() {
        return workingOnSundayFlag;
    }

    public void setWorkingOnSundayFlag(Boolean workingOnSundayFlag) {
        this.workingOnSundayFlag = workingOnSundayFlag;
    }

    public Boolean getWorkingOnMondayFlag() {
        return workingOnMondayFlag;
    }

    public void setWorkingOnMondayFlag(Boolean workingOnMondayFlag) {
        this.workingOnMondayFlag = workingOnMondayFlag;
    }

    public Boolean getWorkingOnTuesdayFlag() {
        return workingOnTuesdayFlag;
    }

    public void setWorkingOnTuesdayFlag(Boolean workingOnTuesdayFlag) {
        this.workingOnTuesdayFlag = workingOnTuesdayFlag;
    }

    public Boolean getWorkingOnWednesdayFlag() {
        return workingOnWednesdayFlag;
    }

    public void setWorkingOnWednesdayFlag(Boolean workingOnWednesdayFlag) {
        this.workingOnWednesdayFlag = workingOnWednesdayFlag;
    }

    public Boolean getWorkingOnThursdayFlag() {
        return workingOnThursdayFlag;
    }

    public void setWorkingOnThursdayFlag(Boolean workingOnThursdayFlag) {
        this.workingOnThursdayFlag = workingOnThursdayFlag;
    }

    public Boolean getWorkingOnFridayFlag() {
        return workingOnFridayFlag;
    }

    public void setWorkingOnFridayFlag(Boolean workingOnFridayFlag) {
        this.workingOnFridayFlag = workingOnFridayFlag;
    }

    public Boolean getWorkingOnSaturdayFlag() {
        return workingOnSaturdayFlag;
    }

    public void setWorkingOnSaturdayFlag(Boolean workingOnSaturdayFlag) {
        this.workingOnSaturdayFlag = workingOnSaturdayFlag;
    }

    public String getTimeZone() {
        return timeZone;
    }

    public void setTimeZone(String timeZone) {
        this.timeZone = timeZone;
    }

    public Boolean getLocationUtilizationSnapshotEnabled() {
        return locationUtilizationSnapshotEnabled;
    }

    public void setLocationUtilizationSnapshotEnabled(Boolean locationUtilizationSnapshotEnabled) {
        this.locationUtilizationSnapshotEnabled = locationUtilizationSnapshotEnabled;
    }

    public Boolean getInventoryAgingSnapshotEnabled() {
        return inventoryAgingSnapshotEnabled;
    }

    public void setInventoryAgingSnapshotEnabled(Boolean inventoryAgingSnapshotEnabled) {
        this.inventoryAgingSnapshotEnabled = inventoryAgingSnapshotEnabled;
    }
}

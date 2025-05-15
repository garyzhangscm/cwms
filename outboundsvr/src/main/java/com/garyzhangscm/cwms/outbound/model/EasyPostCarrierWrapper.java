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

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class EasyPostCarrierWrapper extends AuditibleEntity<String> implements Serializable {

    private Long id;

    private Long warehouseId;


    private Long carrierId;


    private String accountNumber;


    private ReportType reportType;


    private String printerName;




    private Boolean printParcelLabelAfterManifestFlag = true;

    private Integer labelCopyCount;

    private Boolean schedulePickupAfterManifestFlag = true;

    // The 2 pickup time will be in the string with the format
    // of HHmm.
    private String minPickupTime;

    private String maxPickupTime;


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

    public Long getCarrierId() {
        return carrierId;
    }

    public void setCarrierId(Long carrierId) {
        this.carrierId = carrierId;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }


    public ReportType getReportType() {
        return reportType;
    }

    public void setReportType(ReportType reportType) {
        this.reportType = reportType;
    }

    public String getPrinterName() {
        return printerName;
    }

    public void setPrinterName(String printerName) {
        this.printerName = printerName;
    }

    public Boolean getPrintParcelLabelAfterManifestFlag() {
        return printParcelLabelAfterManifestFlag;
    }

    public void setPrintParcelLabelAfterManifestFlag(Boolean printParcelLabelAfterManifestFlag) {
        this.printParcelLabelAfterManifestFlag = printParcelLabelAfterManifestFlag;
    }

    public Boolean getSchedulePickupAfterManifestFlag() {
        return schedulePickupAfterManifestFlag;
    }

    public void setSchedulePickupAfterManifestFlag(Boolean schedulePickupAfterManifestFlag) {
        this.schedulePickupAfterManifestFlag = schedulePickupAfterManifestFlag;
    }

    public Integer getLabelCopyCount() {
        return labelCopyCount;
    }

    public void setLabelCopyCount(Integer labelCopyCount) {
        this.labelCopyCount = labelCopyCount;
    }

    public String getMinPickupTime() {
        return minPickupTime;
    }

    public void setMinPickupTime(String minPickupTime) {
        this.minPickupTime = minPickupTime;
    }

    public String getMaxPickupTime() {
        return maxPickupTime;
    }

    public void setMaxPickupTime(String maxPickupTime) {
        this.maxPickupTime = maxPickupTime;
    }
}

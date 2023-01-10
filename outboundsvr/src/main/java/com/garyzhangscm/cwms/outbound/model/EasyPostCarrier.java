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
import com.fasterxml.jackson.datatype.jsr310.deser.LocalTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer;
import org.codehaus.jackson.annotate.JsonProperty;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Entity
@Table(name = "easy_post_carrier")
public class EasyPostCarrier extends AuditibleEntity<String> implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "easy_post_carrier_id")
    @JsonProperty(value="id")
    private Long id;

    @Column(name = "warehouse_id")
    private Long warehouseId;


    @Column(name = "carrier_id")
    private Long carrierId;

    @Transient
    private Carrier carrier;

    @Column(name = "account_number")
    private String accountNumber;


    @Column(name = "report_type")
    @Enumerated(EnumType.STRING)
    private ReportType reportType;


    // default printer for printing parcel label
    @Column(name = "printer_name")
    private String printerName;


    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "easy_post_configuration_id")
    private EasyPostConfiguration easyPostConfiguration;


    @Column(name = "print_parcel_label_after_manifest")
    private Boolean printParcelLabelAfterManifestFlag = true;

    @Column(name = "label_copy_count")
    private Integer labelCopyCount;

    @Column(name = "schedule_pickup_after_manifest")
    private Boolean schedulePickupAfterManifestFlag = true;

    @Column(name = "min_pickup_time")
    @JsonDeserialize(using = LocalTimeDeserializer.class)
    @JsonSerialize(using = LocalTimeSerializer.class)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm")
    private LocalTime minPickupTime;

    @Column(name = "max_pickup_time")
    @JsonDeserialize(using = LocalTimeDeserializer.class)
    @JsonSerialize(using = LocalTimeSerializer.class)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm")
    private LocalTime maxPickupTime;

    public EasyPostCarrier() {

    }

    public EasyPostCarrier(EasyPostCarrierWrapper easyPostCarrierWrapper) {
        setId(easyPostCarrierWrapper.getId());
        setWarehouseId(easyPostCarrierWrapper.getWarehouseId());
        setCarrierId(easyPostCarrierWrapper.getCarrierId());
        setAccountNumber(easyPostCarrierWrapper.getAccountNumber());
        setReportType(easyPostCarrierWrapper.getReportType());
        setPrinterName(easyPostCarrierWrapper.getPrinterName());
        setPrintParcelLabelAfterManifestFlag(easyPostCarrierWrapper.getPrintParcelLabelAfterManifestFlag());
        setLabelCopyCount(easyPostCarrierWrapper.getLabelCopyCount());
        setSchedulePickupAfterManifestFlag(easyPostCarrierWrapper.getSchedulePickupAfterManifestFlag());
        // convert the localdate time to local time
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HHmm", Locale.ENGLISH);
        if (Objects.nonNull(easyPostCarrierWrapper.getMinPickupTime())) {
            setMinPickupTime(LocalTime.parse(easyPostCarrierWrapper.getMinPickupTime(), formatter));
        }
        if (Objects.nonNull(easyPostCarrierWrapper.getMaxPickupTime())) {
            setMaxPickupTime(LocalTime.parse(easyPostCarrierWrapper.getMaxPickupTime(), formatter));
        }
    }

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

    public Carrier getCarrier() {
        return carrier;
    }

    public void setCarrier(Carrier carrier) {
        this.carrier = carrier;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public EasyPostConfiguration getEasyPostConfiguration() {
        return easyPostConfiguration;
    }

    public void setEasyPostConfiguration(EasyPostConfiguration easyPostConfiguration) {
        this.easyPostConfiguration = easyPostConfiguration;
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

    public LocalTime getMinPickupTime() {
        return minPickupTime;
    }

    public void setMinPickupTime(LocalTime minPickupTime) {
        this.minPickupTime = minPickupTime;
    }

    public LocalTime getMaxPickupTime() {
        return maxPickupTime;
    }

    public void setMaxPickupTime(LocalTime maxPickupTime) {
        this.maxPickupTime = maxPickupTime;
    }
}

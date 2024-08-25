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

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import javax.persistence.*;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "custom_report_execution_history")
public class CustomReportExecutionHistory extends AuditibleEntity<String>  {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "custom_report_execution_history_id")
    private Long id;

    @Column(name = "company_id")
    private Long companyId;

    @Column(name = "warehouse_id")
    private Long warehouseId;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "custom_report_id")
    private CustomReport customReport;

    @Column(name = "query")
    private String query;

    @Enumerated(EnumType.STRING)
    @Column(name = "custom_report_execute_status")
    private CustomReportExecutionStatus status;

    @Column(name = "custom_report_execute_percent")
    private Integer customReportExecutionPercent = 0;

    @Column(name = "result")
    private Boolean result = true;


    @Column(name = "result_row_count")
    private Integer resultRowCount = 0;

    @Column(name = "error_message")
    private String errorMessage;

    @JsonIgnore
    @Column(name = "result_file")
    private String resultFile;

    @Column(name = "result_file_expired")
    private Boolean resultFileExpired = false;

    @Column(name = "result_file_expired_time")
    //@CreatedDate
    @JsonDeserialize(using = CustomZonedDateTimeDeserializer.class)
    @JsonSerialize(using = CustomZonedDateTimeSerializer.class)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private ZonedDateTime resultFileExpiredTime;

    public CustomReportExecutionHistory(CustomReport customReport,
                                 Long companyId,
                                 Long warehouseId,
                                 String query){

        setCompanyId(customReport.getCompanyId());
        setWarehouseId(companyId);
        setWarehouseId(warehouseId);

        setCustomReport(customReport);

        setQuery(query);

        setResult(true);

        setResultRowCount(0);

        setErrorMessage("");

        setStatus(CustomReportExecutionStatus.INIT);
        setCustomReportExecutionPercent(0);


    }
    public CustomReportExecutionHistory(){

    }
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public void setCompanyId(Long companyId) {
        this.companyId = companyId;
    }

    public Long getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(Long warehouseId) {
        this.warehouseId = warehouseId;
    }

    public CustomReport getCustomReport() {
        return customReport;
    }

    public void setCustomReport(CustomReport customReport) {
        this.customReport = customReport;
    }

    public Integer getResultRowCount() {
        return resultRowCount;
    }

    public void setResultRowCount(Integer resultRowCount) {
        this.resultRowCount = resultRowCount;
    }

    public String getQuery() {
        return query;
    }

    public CustomReportExecutionStatus getStatus() {
        return status;
    }

    public void setStatus(CustomReportExecutionStatus status) {
        this.status = status;
    }

    public Integer getCustomReportExecutionPercent() {
        return customReportExecutionPercent;
    }

    public void setCustomReportExecutionPercent(Integer customReportExecutionPercent) {
        this.customReportExecutionPercent = customReportExecutionPercent;
    }

    public void setQuery(String query) {
        this.query = query;
    }
    public Boolean getResult() {
        return result;
    }

    public void setResult(Boolean result) {
        this.result = result;
    }


    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getResultFile() {
        return resultFile;
    }

    public void setResultFile(String resultFile) {
        this.resultFile = resultFile;
    }

    public Boolean getResultFileExpired() {
        return resultFileExpired;
    }

    public void setResultFileExpired(Boolean resultFileExpired) {
        this.resultFileExpired = resultFileExpired;
    }

    public ZonedDateTime getResultFileExpiredTime() {
        return resultFileExpiredTime;
    }

    public void setResultFileExpiredTime(ZonedDateTime resultFileExpiredTime) {
        this.resultFileExpiredTime = resultFileExpiredTime;
    }
}

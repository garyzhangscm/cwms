package com.garyzhangscm.cwms.resources.model;


import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.springframework.data.annotation.CreatedDate;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * Printing request: we will save the printing request
 * when we host the server on cloud but print from local printing service
 * the local printing service will get the pending request and print
 * from local printer
 */
@Entity
@Table(name = "printing_request")
public class PrintingRequest extends AuditibleEntity<String> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "printing_request_id")
    private Long id;


    @Column(name = "warehouse_id")
    private Long warehouseId;

    @ManyToOne
    @JoinColumn(name="report_history_id")
    private ReportHistory reportHistory;

    @Column(name = "printer_name")
    private String printerName;

    @Column(name = "copies")
    private Integer copies;

    @Column(name = "printing_time")
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private LocalDateTime printingTime;

    public PrintingRequest() {}

    public PrintingRequest(Long warehouseId, ReportHistory reportHistory, String printerName, Integer copies) {
        this.warehouseId = warehouseId;
        this.reportHistory = reportHistory;
        this.printerName = printerName;
        this.copies = copies;

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

    public ReportHistory getReportHistory() {
        return reportHistory;
    }

    public void setReportHistory(ReportHistory reportHistory) {
        this.reportHistory = reportHistory;
    }

    public String getPrinterName() {
        return printerName;
    }

    public void setPrinterName(String printerName) {
        this.printerName = printerName;
    }

    public Integer getCopies() {
        return copies;
    }

    public void setCopies(Integer copies) {
        this.copies = copies;
    }

    public LocalDateTime getPrintingTime() {
        return printingTime;
    }

    public void setPrintingTime(LocalDateTime printingTime) {
        this.printingTime = printingTime;
    }
}

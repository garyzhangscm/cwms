package com.garyzhangscm.cwms.resources.model;


import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.springframework.data.annotation.CreatedDate;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Entity
@Table(name = "report_history")
public class ReportHistory extends AuditibleEntity<String> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "report_history_id")
    private Long id;


    @Column(name = "warehouse_id")
    private Long warehouseId;
    @Transient
    private Warehouse warehouse;

    @Column(name = "printed_date")
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    //@DateTimeFormat(pattern =  "YYYY-MM-DDTHH:mm:ss.SSSZ")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private LocalDateTime printedDate;
    @Column(name = "printed_username")
    private String printedUsername;


    @Column(name = "description")
    private String description;


    @Enumerated(EnumType.STRING)
    @Column(name = "type")
    private ReportType type;

    // absolute file path and name
    @Column(name = "file_name")
    private String fileName;

    @Column(name = "orientation")
    @Enumerated(EnumType.STRING)
    private ReportOrientation reportOrientation;

    public ReportHistory() {}

    // create the history from report
    public ReportHistory(Report report,
                         String fileName,
                         String printedUsername,
                         Long warehouseId) {
        this.warehouseId = warehouseId;

        this.printedDate = LocalDateTime.now();
        this.printedUsername = printedUsername;
        this.type = report.getType();
        this.reportOrientation  = report.getReportOrientation();

        this.description = report.getDescription();
        this.fileName  = fileName;

    }


    @Override
    public String toString() {
        try {
            return new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(this);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return null;
    }


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ReportOrientation getReportOrientation() {
        return reportOrientation;
    }

    public void setReportOrientation(ReportOrientation reportOrientation) {
        this.reportOrientation = reportOrientation;
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

    public LocalDateTime getPrintedDate() {
        return printedDate;
    }

    public void setPrintedDate(LocalDateTime printedDate) {
        this.printedDate = printedDate;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getPrintedUsername() {
        return printedUsername;
    }

    public void setPrintedUsername(String printedUsername) {
        this.printedUsername = printedUsername;
    }



    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ReportType getType() {
        return type;
    }

    public void setType(ReportType type) {
        this.type = type;
    }
}

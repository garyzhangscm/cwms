package com.garyzhangscm.cwms.inbound.model;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;

public class ReportHistory extends AuditibleEntity<String> {

    private Long id;


    private Long warehouseId;
    private Warehouse warehouse;

    private LocalDateTime printedDate;
    private String printedUsername;

    private String description;

    private ReportType type;
    private String fileName;



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

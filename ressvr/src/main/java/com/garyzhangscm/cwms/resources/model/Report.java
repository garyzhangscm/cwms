package com.garyzhangscm.cwms.resources.model;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.persistence.*;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Entity
@Table(name = "report")
public class Report extends AuditibleEntity<String> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "report_id")
    private Long id;

    @Column(name = "company_id")
    private Long companyId;
    @Transient
    private Company company;

    @Column(name = "warehouse_id")
    private Long warehouseId;
    @Transient
    private Warehouse warehouse;

    @Column(name = "description")
    private String description;


    @Enumerated(EnumType.STRING)
    @Column(name = "type")
    private ReportType type;

    @Column(name = "file_name")
    private String fileName;


    // if the report format is only for
    // a specific printer type
    @ManyToOne
    @JoinColumn(name="printer_type_id")
    private PrinterType printerType;

    @Column(name = "orientation")
    @Enumerated(EnumType.STRING)
    private ReportOrientation reportOrientation ;

    // a collection of label data.
    // one data per label
    @Transient
    Collection<?> data;


    @Transient
    Map<String, Object> parameters = new HashMap<>();


    @Override
    public String toString() {
        try {
            return new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(this);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Report that = (Report) o;
        if (id != null && that.id != null) {
            return Objects.equals(id, that.id);
        }
        return  Objects.equals(warehouseId, that.warehouseId) &&
                Objects.equals(companyId, that.companyId) &&
                Objects.equals(type, that.type) &&
                Objects.equals(printerType, that.getPrinterType());
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

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public Long getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(Long warehouseId) {
        this.warehouseId = warehouseId;
    }

    public Collection<?> getData() {
        return data;
    }

    public void setData(Collection<?> data) {
        this.data = data;
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }

    public ReportOrientation getReportOrientation() {
        return reportOrientation;
    }

    public void setReportOrientation(ReportOrientation reportOrientation) {
        this.reportOrientation = reportOrientation;
    }

    public void addParameter(String key, Object value) {
        if (Objects.isNull(parameters)) {
            parameters = new HashMap<>();
        }
        parameters.put(key, value);
    }

    public void setParameters(Map<String, Object> parameters) {
        this.parameters = parameters;
    }

    public Company getCompany() {
        return company;
    }

    public void setCompany(Company company) {
        this.company = company;
    }

    public Warehouse getWarehouse() {
        return warehouse;
    }

    public void setWarehouse(Warehouse warehouse) {
        this.warehouse = warehouse;
    }

    public PrinterType getPrinterType() {
        return printerType;
    }

    public void setPrinterType(PrinterType printerType) {
        this.printerType = printerType;
    }
}

package com.garyzhangscm.cwms.adminserver.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.garyzhangscm.cwms.adminserver.model.wms.Client;
import com.garyzhangscm.cwms.adminserver.model.wms.Company;
import com.garyzhangscm.cwms.adminserver.model.wms.Warehouse;
import org.codehaus.jackson.annotate.JsonProperty;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Billable web request call
 *
 */
@Entity
@Table(name = "invoice")
public class Invoice extends AuditibleEntity<String>{


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "invoice_id")
    @JsonProperty(value="id")
    private Long id;


    @Column(name = "company_id")
    private Long companyId;

    @Transient
    private Company company;

    @Column(name = "warehouse_id")
    private Long warehouseId;

    @Transient
    private Warehouse warehouse;

    @Column(name = "client_id")
    private Long clientId;

    @Transient
    private Client client;

    @Column(name = "number")
    private String number;
    @Column(name = "reference_number")
    private String referenceNumber;

    @Column(name = "comment")
    private String comment;

    @Column(name = "start_time")
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private LocalDateTime startTime;

    @Column(name = "end_time")
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private LocalDateTime endTime;

    @OneToMany(
            mappedBy = "invoice",
            cascade = CascadeType.ALL,
            fetch = FetchType.LAZY)
    private List<InvoiceLine> lines = new ArrayList<>();

    @Column(name = "total_charge")
    private Double totalCharge;

    public Invoice(){}

    public Invoice(Long companyId, Long warehouseId,
                   Long clientId, String number, String referenceNumber,
                   String comment, LocalDateTime startTime, LocalDateTime endTime,
                   Double totalCharge) {
        this.companyId = companyId;
        this.warehouseId = warehouseId;
        this.clientId = clientId;
        this.number = number;
        this.referenceNumber = referenceNumber;
        this.comment = comment;
        this.startTime = startTime;
        this.endTime = endTime;
        this.totalCharge = totalCharge;
        this.lines = new ArrayList<>();
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

    public Company getCompany() {
        return company;
    }

    public void setCompany(Company company) {
        this.company = company;
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

    public Long getClientId() {
        return clientId;
    }

    public void setClientId(Long clientId) {
        this.clientId = clientId;
    }

    public Client getClient() {
        return client;
    }

    public void setClient(Client client) {
        this.client = client;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public List<InvoiceLine> getLines() {
        return lines;
    }

    public void setLines(List<InvoiceLine> lines) {
        this.lines = lines;
    }

    public void addLine(InvoiceLine line) {
        this.lines.add(line);
    }

    public Double getTotalCharge() {
        return totalCharge;
    }

    public void setTotalCharge(Double totalCharge) {
        this.totalCharge = totalCharge;
    }
}

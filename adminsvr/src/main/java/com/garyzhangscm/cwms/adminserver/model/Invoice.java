package com.garyzhangscm.cwms.adminserver.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.garyzhangscm.cwms.adminserver.model.wms.Client;
import com.garyzhangscm.cwms.adminserver.model.wms.Company;
import com.garyzhangscm.cwms.adminserver.model.wms.Warehouse;
import org.codehaus.jackson.annotate.JsonProperty;

import javax.persistence.*;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

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
    @JsonDeserialize(using = CustomZonedDateTimeDeserializer.class)
    @JsonSerialize(using = CustomZonedDateTimeSerializer.class)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private ZonedDateTime startTime;

    @Column(name = "end_time")
    @JsonDeserialize(using = CustomZonedDateTimeDeserializer.class)
    @JsonSerialize(using = CustomZonedDateTimeSerializer.class)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private ZonedDateTime endTime;


    @Column(name = "invoice_date")
    @JsonDeserialize(using = LocalDateDeserializer.class)
    @JsonSerialize(using = LocalDateSerializer.class)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate invoiceDate;

    @Column(name = "due_date")
    @JsonDeserialize(using = LocalDateDeserializer.class)
    @JsonSerialize(using = LocalDateSerializer.class)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate dueDate;

    @OneToMany(
            mappedBy = "invoice",
            cascade = CascadeType.ALL,
            fetch = FetchType.LAZY)
    private List<InvoiceLine> lines = new ArrayList<>();

    @OneToMany(
            mappedBy = "invoice",
            cascade = CascadeType.ALL,
            fetch = FetchType.LAZY)
    private List<InvoiceDocument> documents = new ArrayList<>();


    @Column(name = "total_charge")
    private Double totalCharge;

    public Invoice(){}

    public Invoice(Long companyId, Long warehouseId,
                   Long clientId, String number, String referenceNumber,
                   String comment, ZonedDateTime startTime, ZonedDateTime endTime,
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
        this.documents = new ArrayList<>();
    }
    public Invoice(Long companyId, Long warehouseId,
                   Long clientId, String number, String referenceNumber,
                   String comment, LocalDate invoiceDate, LocalDate dueDate,
                   Double totalCharge) {
        this.companyId = companyId;
        this.warehouseId = warehouseId;
        this.clientId = clientId;
        this.number = number;
        this.referenceNumber = referenceNumber;
        this.comment = comment;
        this.invoiceDate = invoiceDate;
        this.dueDate = dueDate;
        this.totalCharge = totalCharge;
        this.lines = new ArrayList<>();
        this.documents = new ArrayList<>();
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

    public LocalDate getInvoiceDate() {
        return invoiceDate;
    }

    public void setInvoiceDate(LocalDate invoiceDate) {
        this.invoiceDate = invoiceDate;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
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

    public ZonedDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(ZonedDateTime startTime) {
        this.startTime = startTime;
    }

    public ZonedDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(ZonedDateTime endTime) {
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

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public String getReferenceNumber() {
        return referenceNumber;
    }

    public void setReferenceNumber(String referenceNumber) {
        this.referenceNumber = referenceNumber;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }


    public List<InvoiceDocument> getDocuments() {
        return documents;
    }

    public void setDocuments(List<InvoiceDocument> documents) {
        this.documents = documents;
    }

    public void addDocument(InvoiceDocument invoiceDocument) {
        documents.add(invoiceDocument);
    }
}

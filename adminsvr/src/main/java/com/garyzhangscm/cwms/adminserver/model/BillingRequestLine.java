package com.garyzhangscm.cwms.adminserver.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import org.codehaus.jackson.annotate.JsonProperty;

import javax.persistence.*;
import java.time.LocalDate;
import java.time.ZonedDateTime;

/**
 * Billable web request call
 *
 */
@Entity
@Table(name = "billing_request_line")
public class BillingRequestLine extends AuditibleEntity<String>{


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "billing_request_line_id")
    @JsonProperty(value="id")
    private Long id;



    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "billing_request_id")
    private BillingRequest billingRequest;

    @Column(name = "start_time")
    @JsonDeserialize(using = CustomZonedDateTimeDeserializer.class)
    @JsonSerialize(using = CustomZonedDateTimeSerializer.class)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private ZonedDateTime startTime;

    // @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    // @JsonSerialize(using = LocalDateTimeSerializer.class)
    // @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    // private LocalDateTime startTime;

    @Column(name = "end_time")
    @JsonDeserialize(using = CustomZonedDateTimeDeserializer.class)
    @JsonSerialize(using = CustomZonedDateTimeSerializer.class)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private ZonedDateTime endTime;


    @Transient
    @JsonDeserialize(using = LocalDateDeserializer.class)
    @JsonSerialize(using = LocalDateSerializer.class)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate date;

    // @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    // @JsonSerialize(using = LocalDateTimeSerializer.class)
    // @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    // private LocalDateTime endTime;

    @Column(name = "total_amount")
    private Double totalAmount;
    @Column(name = "total_charge")
    private Double totalCharge;

    @Column(name = "rate")
    private Double rate;

    @Column(name = "document_number")
    private String documentNumber;


    @Column(name = "item_name")
    private String itemName;

    public BillingRequestLine(){

    }

    public BillingRequestLine(BillingRequest billingRequest,
                              ZonedDateTime startTime,
                              ZonedDateTime endTime,
                              String documentNumber,
                              String itemName,
                              Double totalAmount,
                              Double totalCharge,
                              Double rate) {
        this.billingRequest = billingRequest;
        this.startTime = startTime;
        this.endTime = endTime;
        this.documentNumber = documentNumber;
        this.itemName = itemName;
        this.totalAmount = totalAmount;
        this.totalCharge = totalCharge;
        this.rate = rate;
    }
    public BillingRequestLine(BillingRequest billingRequest,
                              ZonedDateTime startTime,
                              ZonedDateTime endTime,
                              LocalDate date,
                              String documentNumber,
                              String itemName,
                              Double totalAmount,
                              Double totalCharge,
                              Double rate) {
        this.billingRequest = billingRequest;
        this.startTime = startTime;
        this.endTime = endTime;
        this.date = date;
        this.documentNumber = documentNumber;
        this.itemName = itemName;
        this.totalAmount = totalAmount;
        this.totalCharge = totalCharge;
        this.rate = rate;
    }

    public BillingRequestLine(BillingRequest billingRequest,
                              ZonedDateTime startTime,
                              ZonedDateTime endTime,
                              LocalDate date,
                              Double totalAmount,
                              Double totalCharge,
                              Double rate) {
        this.billingRequest = billingRequest;
        this.startTime = startTime;
        this.endTime = endTime;
        this.date = date;
        this.totalAmount = totalAmount;
        this.totalCharge = totalCharge;
        this.rate = rate;
    }


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public BillingRequest getBillingRequest() {
        return billingRequest;
    }

    public void setBillingRequest(BillingRequest billingRequest) {
        this.billingRequest = billingRequest;
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

    public Double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(Double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public Double getTotalCharge() {
        return totalCharge;
    }

    public void setTotalCharge(Double totalCharge) {
        this.totalCharge = totalCharge;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public Double getRate() {
        return rate;
    }

    public void setRate(Double rate) {
        this.rate = rate;
    }

    public String getDocumentNumber() {
        return documentNumber;
    }

    public void setDocumentNumber(String documentNumber) {
        this.documentNumber = documentNumber;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public void addAmount(Double amount) {
        setTotalAmount(getTotalAmount() + amount);
        setTotalCharge(getTotalAmount() * getRate());
    }
}

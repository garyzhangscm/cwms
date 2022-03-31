package com.garyzhangscm.cwms.adminserver.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;

import javax.persistence.*;

/**
 * Billable web request call
 *
 */
@Entity
@Table(name = "invoice_line")
public class InvoiceLine extends AuditibleEntity<String>{


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "invoice_line_id")
    @JsonProperty(value="id")
    private Long id;

    @ManyToOne
    @JoinColumn(name="invoice_id")
    @JsonIgnore
    private Invoice invoice;

    @ManyToOne
    @JoinColumn(name="billing_request_id")
    @JsonIgnore
    private BillingRequest billingRequest;



    @Column(name = "billable_category")
    @Enumerated(EnumType.STRING)
    private BillableCategory billableCategory;

    @Column(name = "amount")
    private Double amount;

    @Column(name = "rate")
    private Double rate;


    @Column(name = "total_charge")
    private Double totalCharge;

    public InvoiceLine(){}


    public InvoiceLine(Invoice invoice, BillingRequest billingRequest, BillableCategory billableCategory, Double amount, Double rate, Double totalCharge) {
        this.invoice = invoice;
        this.billingRequest = billingRequest;
        this.billableCategory = billableCategory;
        this.amount = amount;
        this.rate = rate;
        this.totalCharge = totalCharge;
    }

    public InvoiceLine(Invoice invoice, BillingRequest billingRequest) {
        this(invoice, billingRequest, billingRequest.getBillableCategory(),
                billingRequest.getTotalAmount(),
                billingRequest.getRate(), billingRequest.getTotalCharge());
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Invoice getInvoice() {
        return invoice;
    }

    public void setInvoice(Invoice invoice) {
        this.invoice = invoice;
    }

    public BillableCategory getBillableCateory() {
        return billableCategory;
    }

    public void setBillableCateory(BillableCategory billableCategory) {
        this.billableCategory = billableCategory;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public Double getRate() {
        return rate;
    }

    public void setRate(Double rate) {
        this.rate = rate;
    }

    public Double getTotalCharge() {
        return totalCharge;
    }

    public void setTotalCharge(Double totalCharge) {
        this.totalCharge = totalCharge;
    }

    public BillingRequest getBillingRequest() {
        return billingRequest;
    }

    public void setBillingRequest(BillingRequest billingRequest) {
        this.billingRequest = billingRequest;
    }

    public BillableCategory getBillableCategory() {
        return billableCategory;
    }

    public void setBillableCategory(BillableCategory billableCategory) {
        this.billableCategory = billableCategory;
    }
}

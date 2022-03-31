package com.garyzhangscm.cwms.adminserver.model;

import com.garyzhangscm.cwms.adminserver.model.wms.Client;
import com.garyzhangscm.cwms.adminserver.model.wms.Company;
import com.garyzhangscm.cwms.adminserver.model.wms.Warehouse;
import org.codehaus.jackson.annotate.JsonProperty;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Billable web request call
 *
 */
@Entity
@Table(name = "billing_request")
public class BillingRequest extends AuditibleEntity<String>{


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "billing_request_id")
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

    @Column(name = "billable_category")
    @Enumerated(EnumType.STRING)
    private BillableCategory billableCategory;

    @Column(name = "rate")
    private Double rate;

    @Column(name = "billing_cycle")
    @Enumerated(EnumType.STRING)
    private BillingCycle billingCycle;

    @OneToMany(
            mappedBy = "billingRequest",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    private List<BillingRequestLine> billingRequestLines = new ArrayList<>();

    @Column(name = "total_amount")
    private Double totalAmount;
    @Column(name = "total_charge")
    private Double totalCharge;

    public BillingRequest(){}

    public BillingRequest(Long companyId, Long warehouseId, Long clientId,
                          String number,
                          BillableCategory billableCategory,
                          Double rate, BillingCycle billingCycle,
                          Double totalAmount, Double totalCharge){
        this.companyId = companyId;
        this.warehouseId = warehouseId;
        this.clientId = clientId;

        this.number = number;

        this.billableCategory = billableCategory;

        this.rate = rate;

        this.billingCycle = billingCycle;

        billingRequestLines = new ArrayList<>();

        this.totalAmount = totalAmount;
        this.totalCharge = totalCharge;
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

    public BillableCategory getBillableCateory() {
        return billableCategory;
    }

    public void setBillableCateory(BillableCategory billableCategory) {
        this.billableCategory = billableCategory;
    }

    public Double getRate() {
        return rate;
    }

    public void setRate(Double rate) {
        this.rate = rate;
    }

    public BillableCategory getBillableCategory() {
        return billableCategory;
    }

    public void setBillableCategory(BillableCategory billableCategory) {
        this.billableCategory = billableCategory;
    }

    public BillingCycle getBillingCycle() {
        return billingCycle;
    }

    public void setBillingCycle(BillingCycle billingCycle) {
        this.billingCycle = billingCycle;
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

    public List<BillingRequestLine> getBillingRequestLines() {
        return billingRequestLines;
    }

    public void setBillingRequestLines(List<BillingRequestLine> billingRequestLines) {
        this.billingRequestLines = billingRequestLines;
    }
    public void addBillingRequestLine(BillingRequestLine billingRequestLine) {
        this.billingRequestLines.add(billingRequestLine);
    }
}

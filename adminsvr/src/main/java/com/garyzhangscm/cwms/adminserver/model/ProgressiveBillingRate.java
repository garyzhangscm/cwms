package com.garyzhangscm.cwms.adminserver.model;

import com.garyzhangscm.cwms.adminserver.model.wms.Client;
import com.garyzhangscm.cwms.adminserver.model.wms.Company;
import com.garyzhangscm.cwms.adminserver.model.wms.Warehouse;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.persistence.*;

/**
 * Billable web request call
 *
 */
@Entity
@Table(name = "progressive_billing_rate")
public class ProgressiveBillingRate extends AuditibleEntity<String>{


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "progressive_billing_rate_id")
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


    @Column(name = "billable_category")
    @Enumerated(EnumType.STRING)
    private BillableCategory billableCategory;

    @Column(name = "rate")
    private Double rate;

    @Column(name = "billing_cycle")
    @Enumerated(EnumType.STRING)
    private BillingCycle billingCycle;

    @Column(name = "cycle_start")
    private Integer cycleStart;

    @Column(name = "cycle_end")
    private Integer cycleEnd;

    @Column(name = "enabled")
    private Boolean enabled;


    public ProgressiveBillingRate(){}
    public ProgressiveBillingRate(Long companyId,
                                  Long warehouseId,
                                  Long clientId,
                                  BillableCategory billableCategory,
                                  Double rate,
                                  BillingCycle billingCycle,
                                  Integer cycleStart,
                                  Integer cycleEnd,
                                  Boolean enabled) {
        this.companyId = companyId;
        this.warehouseId = warehouseId;
        this.clientId = clientId;
        this.billableCategory = billableCategory;
        this.rate = rate;
        this.billingCycle = billingCycle;
        this.cycleStart = cycleStart;
        this.cycleEnd = cycleEnd;
        this.enabled = enabled;
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

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Integer getCycleStart() {
        return cycleStart;
    }

    public void setCycleStart(Integer cycleStart) {
        this.cycleStart = cycleStart;
    }

    public Integer getCycleEnd() {
        return cycleEnd;
    }

    public void setCycleEnd(Integer cycleEnd) {
        this.cycleEnd = cycleEnd;
    }
}

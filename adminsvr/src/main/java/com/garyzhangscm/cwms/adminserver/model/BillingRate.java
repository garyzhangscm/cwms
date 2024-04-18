package com.garyzhangscm.cwms.adminserver.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.garyzhangscm.cwms.adminserver.model.wms.Client;
import com.garyzhangscm.cwms.adminserver.model.wms.Company;
import com.garyzhangscm.cwms.adminserver.model.wms.Warehouse;
import org.codehaus.jackson.annotate.JsonProperty;

import javax.persistence.*;

/**
 * Billable web request call
 *
 */
@Entity
@Table(name = "billing_rate")
public class BillingRate extends AuditibleEntity<String>{


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "billing_rate_id")
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

    @Column(name = "rate_unit")
    private String rateUnitName;

    @Column(name = "rate_by_quantity")
    private Boolean rateByQuantity;

    @Column(name = "billing_cycle")
    @Enumerated(EnumType.STRING)
    private BillingCycle billingCycle;

    @Column(name = "enabled")
    private Boolean enabled;


    @JsonIgnore
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "billing_rate_by_inventory_age_id")
    private BillingRateByInventoryAge billingRateByInventoryAge;



    public BillingRate(){}
    public BillingRate(Long companyId,
                       Long warehouseId,
                       Long clientId,
                       BillableCategory billableCategory,
                       Double rate,
                       BillingCycle billingCycle,
                       Boolean enabled) {
        this.companyId = companyId;
        this.warehouseId = warehouseId;
        this.clientId = clientId;
        this.billableCategory = billableCategory;
        this.rate = rate;
        this.billingCycle = billingCycle;
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

    public BillingRateByInventoryAge getBillingRateByInventoryAge() {
        return billingRateByInventoryAge;
    }

    public void setBillingRateByInventoryAge(BillingRateByInventoryAge billingRateByInventoryAge) {
        this.billingRateByInventoryAge = billingRateByInventoryAge;
    }

    public String getRateUnitName() {
        return rateUnitName;
    }

    public void setRateUnitName(String rateUnitName) {
        this.rateUnitName = rateUnitName;
    }

    public Boolean getRateByQuantity() {
        return rateByQuantity;
    }

    public void setRateByQuantity(Boolean rateByQuantity) {
        this.rateByQuantity = rateByQuantity;
    }
}

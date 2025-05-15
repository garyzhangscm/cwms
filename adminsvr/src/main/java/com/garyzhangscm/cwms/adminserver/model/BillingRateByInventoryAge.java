package com.garyzhangscm.cwms.adminserver.model;

import com.garyzhangscm.cwms.adminserver.model.wms.Client;
import com.garyzhangscm.cwms.adminserver.model.wms.Company;
import com.garyzhangscm.cwms.adminserver.model.wms.Warehouse;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Billable web request call
 *
 */
@Entity
@Table(name = "billing_rate_by_inventory_age")
public class BillingRateByInventoryAge extends AuditibleEntity<String>{


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "billing_rate_by_inventory_age_id")
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


    // inventory age range in days
    @Column(name = "start_inventory_age")
    private Integer startInventoryAge;

    @Column(name = "end_inventory_age")
    private Integer endInventoryAge;


    @OneToMany(
            mappedBy = "billingRateByInventoryAge",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.EAGER
    )
    private List<BillingRate> billingRates = new ArrayList<>();

    @Column(name = "enabled")
    private Boolean enabled;


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

    public Integer getStartInventoryAge() {
        return startInventoryAge;
    }

    public void setStartInventoryAge(Integer startInventoryAge) {
        this.startInventoryAge = startInventoryAge;
    }

    public Integer getEndInventoryAge() {
        return endInventoryAge;
    }

    public void setEndInventoryAge(Integer endInventoryAge) {
        this.endInventoryAge = endInventoryAge;
    }

    public List<BillingRate> getBillingRates() {
        return billingRates;
    }

    public void setBillingRates(List<BillingRate> billingRates) {
        this.billingRates = billingRates;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }
}

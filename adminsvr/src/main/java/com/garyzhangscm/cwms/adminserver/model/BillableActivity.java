package com.garyzhangscm.cwms.adminserver.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.garyzhangscm.cwms.adminserver.model.wms.Company;
import com.garyzhangscm.cwms.adminserver.model.wms.Warehouse;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.persistence.*;
import java.time.ZonedDateTime;

/**
 * Billable warehouse activity
 *
 */
@Entity
@Table(name = "billable_activity")
public class BillableActivity extends AuditibleEntity<String>{


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "billable_activity_id")
    @JsonProperty(value="id")
    private Long id;


    @Column(name = "company_id")
    private Long companyId;

    @Transient
    private Company company;

    @Column(name = "warehouse_id")
    private Long warehouseId;

    @Column(name = "client_id")
    private Long clientId;

    @Column(name = "billable_activity_type_id")
    private Long billableActivityTypeId;

    @Column(name = "activity_time")
    @JsonDeserialize(using = CustomZonedDateTimeDeserializer.class)
    @JsonSerialize(using = CustomZonedDateTimeSerializer.class)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private ZonedDateTime activityTime;

    @Transient
    private Warehouse warehouse;

    @Column(name = "billable_category")
    @Enumerated(EnumType.STRING)
    private BillableCategory billableCategory;

    @Column(name = "amount")
    private Double amount;

    @Column(name = "rate")
    private Double rate;


    @Column(name = "total_charge")
    private Double totalCharge;

    @Column(name = "document_number")
    private String documentNumber;


    @Column(name = "item_name")
    private String itemName;


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

    public Long getClientId() {
        return clientId;
    }

    public void setClientId(Long clientId) {
        this.clientId = clientId;
    }

    public Warehouse getWarehouse() {
        return warehouse;
    }

    public void setWarehouse(Warehouse warehouse) {
        this.warehouse = warehouse;
    }

    public BillableCategory getBillableCategory() {
        return billableCategory;
    }

    public void setBillableCategory(BillableCategory billableCategory) {
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

    public Long getBillableActivityTypeId() {
        return billableActivityTypeId;
    }

    public void setBillableActivityTypeId(Long billableActivityTypeId) {
        this.billableActivityTypeId = billableActivityTypeId;
    }

    public ZonedDateTime getActivityTime() {
        return activityTime;
    }

    public void setActivityTime(ZonedDateTime activityTime) {
        this.activityTime = activityTime;
    }
}

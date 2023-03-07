package com.garyzhangscm.cwms.inbound.model;

import org.codehaus.jackson.annotate.JsonProperty;

import javax.persistence.*;

/**
 * Billable warehouse activity
 *
 */
public class BillableActivity extends AuditibleEntity<String>{


    private Long id;


    private Long companyId;

    private Long warehouseId;


    private Long clientId;

    private BillableCategory billableCategory;

    private Double amount;

    private Double rate;

    private Double totalCharge;

    private String documentNumber;

    private String itemNumber;


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

    public String getItemNumber() {
        return itemNumber;
    }

    public void setItemNumber(String itemNumber) {
        this.itemNumber = itemNumber;
    }
}

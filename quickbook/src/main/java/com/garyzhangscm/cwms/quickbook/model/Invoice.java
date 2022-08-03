package com.garyzhangscm.cwms.quickbook.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.Serializable;
import java.util.List;

public class Invoice implements Serializable {

    private Long id;

    // invoice number
    private String docNumber;

    private String txnDate;

    private List<InvoiceLine> line;

    // for quickbook, if we have the customer ref
    // then it will become the ship to customer
    // and the bill to customer of the invoice but
    // the customer may have different bill to and ship to
    // address
    private CustomerRef customerRef;

    private Address shipAddr;

    private Address billAddr;

    private String dueDate;

    private Long warehouseId;
    private Long companyId;

    @Override
    public String toString() {
        try {
            return new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(this);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return null;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDocNumber() {
        return docNumber;
    }

    public void setDocNumber(String docNumber) {
        this.docNumber = docNumber;
    }

    public String getTxnDate() {
        return txnDate;
    }

    public void setTxnDate(String txnDate) {
        this.txnDate = txnDate;
    }

    public List<InvoiceLine> getLine() {
        return line;
    }

    public void setLine(List<InvoiceLine> line) {
        this.line = line;
    }

    public CustomerRef getCustomerRef() {
        return customerRef;
    }

    public void setCustomerRef(CustomerRef customerRef) {
        this.customerRef = customerRef;
    }

    public Address getShipAddr() {
        return shipAddr;
    }

    public void setShipAddr(Address shipAddr) {
        this.shipAddr = shipAddr;
    }

    public Address getBillAddr() {
        return billAddr;
    }

    public void setBillAddr(Address billAddr) {
        this.billAddr = billAddr;
    }

    public String getDueDate() {
        return dueDate;
    }

    public void setDueDate(String dueDate) {
        this.dueDate = dueDate;
    }

    public Long getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(Long warehouseId) {
        this.warehouseId = warehouseId;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public void setCompanyId(Long companyId) {
        this.companyId = companyId;
    }
}

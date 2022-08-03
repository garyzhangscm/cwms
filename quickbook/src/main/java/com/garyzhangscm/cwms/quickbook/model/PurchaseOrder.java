package com.garyzhangscm.cwms.quickbook.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.Serializable;
import java.util.List;

public class PurchaseOrder implements Serializable {

    private Long id;

    // purchase order number
    private String docNumber;

    private String txnDate;

    private List<PurchaseOrderLine> line;

    private VendorRef vendorRef;

    private Address shipAddr;

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

    public List<PurchaseOrderLine> getLine() {
        return line;
    }

    public void setLine(List<PurchaseOrderLine> line) {
        this.line = line;
    }

    public VendorRef getVendorRef() {
        return vendorRef;
    }

    public void setVendorRef(VendorRef vendorRef) {
        this.vendorRef = vendorRef;
    }

    public Address getShipAddr() {
        return shipAddr;
    }

    public void setShipAddr(Address shipAddr) {
        this.shipAddr = shipAddr;
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

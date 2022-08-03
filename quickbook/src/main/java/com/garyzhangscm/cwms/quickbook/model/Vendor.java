package com.garyzhangscm.cwms.quickbook.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.Serializable;

/**
 * Vendor
 */
public class Vendor implements Serializable {

    private Long id;

    private String givenName;
    private String middleName;
    private String familyName;
    private String suffix;
    private String companyName;
    private String displayName;
    private String printOnCheckName;
    private Phone primaryPhone;
    private Phone mobile;
    private Email primaryEmailAddr;

    private Address billAddr;
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

    public String getGivenName() {
        return givenName;
    }

    public void setGivenName(String givenName) {
        this.givenName = givenName;
    }

    public String getMiddleName() {
        return middleName;
    }

    public void setMiddleName(String middleName) {
        this.middleName = middleName;
    }

    public String getFamilyName() {
        return familyName;
    }

    public void setFamilyName(String familyName) {
        this.familyName = familyName;
    }

    public String getSuffix() {
        return suffix;
    }

    public void setSuffix(String suffix) {
        this.suffix = suffix;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getPrintOnCheckName() {
        return printOnCheckName;
    }

    public void setPrintOnCheckName(String printOnCheckName) {
        this.printOnCheckName = printOnCheckName;
    }

    public Phone getPrimaryPhone() {
        return primaryPhone;
    }

    public void setPrimaryPhone(Phone primaryPhone) {
        this.primaryPhone = primaryPhone;
    }

    public Phone getMobile() {
        return mobile;
    }

    public void setMobile(Phone mobile) {
        this.mobile = mobile;
    }

    public Email getPrimaryEmailAddr() {
        return primaryEmailAddr;
    }

    public void setPrimaryEmailAddr(Email primaryEmailAddr) {
        this.primaryEmailAddr = primaryEmailAddr;
    }

    public Address getBillAddr() {
        return billAddr;
    }

    public void setBillAddr(Address billAddr) {
        this.billAddr = billAddr;
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

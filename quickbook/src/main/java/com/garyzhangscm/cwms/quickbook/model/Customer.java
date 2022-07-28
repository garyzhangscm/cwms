package com.garyzhangscm.cwms.quickbook.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.Serializable;

/**
 * Customer
 */
public class Customer implements Serializable {

    private Long id;

    private String givenName;
    private String middleName;
    private String familyName;
    private String fullyQualifiedName;
    private String companyName;
    private String displayName;

    private Phone primaryPhone;
    private Phone alternatePhone;
    private Phone mobile;

    private Address billAddr;
    private Address shipAddr;

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

    public String getFullyQualifiedName() {
        return fullyQualifiedName;
    }

    public void setFullyQualifiedName(String fullyQualifiedName) {
        this.fullyQualifiedName = fullyQualifiedName;
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

    public Phone getPrimaryPhone() {
        return primaryPhone;
    }

    public void setPrimaryPhone(Phone primaryPhone) {
        this.primaryPhone = primaryPhone;
    }

    public Phone getAlternatePhone() {
        return alternatePhone;
    }

    public void setAlternatePhone(Phone alternatePhone) {
        this.alternatePhone = alternatePhone;
    }

    public Phone getMobile() {
        return mobile;
    }

    public void setMobile(Phone mobile) {
        this.mobile = mobile;
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
}

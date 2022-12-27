package com.garyzhangscm.cwms.outbound.model.shipengine;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

public class Shipment {
    @JsonProperty("validate_address")
    private String validateAddress;
    @JsonProperty("ship_to")
    private Address shipToAddress;
    @JsonProperty("ship_from")
    private Address shipFromAddress;
    @JsonProperty("packages")
    private List<Package> packages = new ArrayList<>();

    public Shipment(){}
    public Shipment(String validateAddress, Address shipToAddress, Address shipFromAddress, List<Package> packages) {
        this.validateAddress = validateAddress;
        this.shipToAddress = shipToAddress;
        this.shipFromAddress = shipFromAddress;
        this.packages = packages;
    }

    @Override
    public String toString() {
        try {
            return new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(this);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return null;
    }

    public String getValidateAddress() {
        return validateAddress;
    }

    public void setValidateAddress(String validateAddress) {
        this.validateAddress = validateAddress;
    }

    public Address getShipToAddress() {
        return shipToAddress;
    }

    public void setShipToAddress(Address shipToAddress) {
        this.shipToAddress = shipToAddress;
    }

    public Address getShipFromAddress() {
        return shipFromAddress;
    }

    public void setShipFromAddress(Address shipFromAddress) {
        this.shipFromAddress = shipFromAddress;
    }

    public List<Package> getPackages() {
        return packages;
    }

    public void setPackages(List<Package> packages) {
        this.packages = packages;
    }
}

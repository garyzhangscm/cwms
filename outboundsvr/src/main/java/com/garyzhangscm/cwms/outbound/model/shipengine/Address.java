package com.garyzhangscm.cwms.outbound.model.shipengine;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Address {
    @JsonProperty("name")
    private String name;
    @JsonProperty("company_name")
    private String companyName;
    @JsonProperty("phone")
    private String phone;
    @JsonProperty("address_line1")
    private String addressLine1;
    @JsonProperty("city_locality")
    private String city;
    @JsonProperty("state_province")
    private String state;
    @JsonProperty("postal_code")
    private String postalCode;
    @JsonProperty("country_code")
    private String country;
    @JsonProperty("address_residential_indicator")
    private String residentialIndicator;

    public Address(){}

    public Address(String name, String companyName,
                   String phone, String addressLine1,
                   String city, String state, String postalCode, String country, String residentialIndicator) {
        this.name = name;
        this.companyName = companyName;
        this.phone = phone;
        this.addressLine1 = addressLine1;
        this.city = city;
        this.state = state;
        this.postalCode = postalCode;
        this.country = country;
        this.residentialIndicator = residentialIndicator;
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAddressLine1() {
        return addressLine1;
    }

    public void setAddressLine1(String addressLine1) {
        this.addressLine1 = addressLine1;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getResidentialIndicator() {
        return residentialIndicator;
    }

    public void setResidentialIndicator(String residentialIndicator) {
        this.residentialIndicator = residentialIndicator;
    }
}

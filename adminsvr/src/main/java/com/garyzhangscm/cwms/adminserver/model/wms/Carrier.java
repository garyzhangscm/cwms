package com.garyzhangscm.cwms.adminserver.model.wms;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

public class Carrier {

    private Long id;

    private String name;

    private String description;


    private String contactorFirstname;
    private String contactorLastname;

    private String addressCountry;
    private String addressState;
    private String addressCounty;
    private String addressCity;
    private String addressDistrict;
    private String addressLine1;
    private String addressLine2;
    private String addressPostcode;


    private List<CarrierServiceLevel> carrierServiceLevels = new ArrayList<>();


    public Carrier(){}

    public Carrier(String name, String description,String contactorFirstname,
                  String contactorLastname,String addressCountry,String addressState,
                  String addressCounty,String addressCity,String addressDistrict,
                  String addressLine1,String addressLine2,String addressPostcode) {
        this.name = name;
        this.description = description;
        this.contactorFirstname = contactorFirstname;
        this.contactorLastname = contactorLastname;
        this.addressCountry = addressCountry;
        this.addressState = addressState;
        this.addressCounty = addressCounty;
        this.addressCity = addressCity;
        this.addressDistrict = addressDistrict;
        this.addressLine1 = addressLine1;
        this.addressLine2 = addressLine2;
        this.addressPostcode = addressPostcode;

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

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getContactorFirstname() {
        return contactorFirstname;
    }

    public void setContactorFirstname(String contactorFirstname) {
        this.contactorFirstname = contactorFirstname;
    }

    public String getContactorLastname() {
        return contactorLastname;
    }

    public void setContactorLastname(String contactorLastname) {
        this.contactorLastname = contactorLastname;
    }

    public String getAddressCountry() {
        return addressCountry;
    }

    public void setAddressCountry(String addressCountry) {
        this.addressCountry = addressCountry;
    }

    public String getAddressState() {
        return addressState;
    }

    public void setAddressState(String addressState) {
        this.addressState = addressState;
    }

    public String getAddressCounty() {
        return addressCounty;
    }

    public void setAddressCounty(String addressCounty) {
        this.addressCounty = addressCounty;
    }

    public String getAddressCity() {
        return addressCity;
    }

    public void setAddressCity(String addressCity) {
        this.addressCity = addressCity;
    }

    public String getAddressDistrict() {
        return addressDistrict;
    }

    public void setAddressDistrict(String addressDistrict) {
        this.addressDistrict = addressDistrict;
    }

    public String getAddressLine1() {
        return addressLine1;
    }

    public void setAddressLine1(String addressLine1) {
        this.addressLine1 = addressLine1;
    }

    public String getAddressLine2() {
        return addressLine2;
    }

    public void setAddressLine2(String addressLine2) {
        this.addressLine2 = addressLine2;
    }

    public String getAddressPostcode() {
        return addressPostcode;
    }

    public void setAddressPostcode(String addressPostcode) {
        this.addressPostcode = addressPostcode;
    }

    public List<CarrierServiceLevel> getCarrierServiceLevels() {
        return carrierServiceLevels;
    }

    public void setCarrierServiceLevels(List<CarrierServiceLevel> carrierServiceLevels) {
        this.carrierServiceLevels = carrierServiceLevels;
    }
}

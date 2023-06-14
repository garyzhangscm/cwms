package com.garyzhangscm.cwms.integration.model.usps;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Address {
    @JsonProperty("Address1")
    private String address1;

    @JsonProperty("Address2")
    private String address2;
    @JsonProperty("City")
    private String city;

    @JsonProperty("CityAbbreviation")
    private String cityAbbreviation;

    @JsonProperty("State")
    private String state;

    @JsonProperty("Zip5")
    private String zip5;

    @JsonProperty("Zip4")
    private String zip4;

    @JsonProperty("DeliveryPoint")
    private String deliveryPoint;

    @JsonProperty("CarrierRoute")
    private String carrierRoute;

    @JsonProperty("Footnotes")
    private String footnotes;

    @JsonProperty("DPVConfirmation")
    private String DPVConfirmation;

    @JsonProperty("DPVCMRA")
    private String DPVCMRA;

    @JsonProperty("DPVFootnotes")
    private String DPVFootnotes;

    @JsonProperty("Business")
    private String business;

    @JsonProperty("CentralDeliveryPoint")
    private String centralDeliveryPoint;

    @JsonProperty("Vacant")
    private String vacant;
    @JsonProperty("Error")
    private Error error;

    @Override
    public String toString() {
        try {
            return new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(this);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return null;
    }

    public String getAddress1() {
        return address1;
    }

    public void setAddress1(String address1) {
        this.address1 = address1;
    }

    public String getAddress2() {
        return address2;
    }

    public void setAddress2(String address2) {
        this.address2 = address2;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getCityAbbreviation() {
        return cityAbbreviation;
    }

    public void setCityAbbreviation(String cityAbbreviation) {
        this.cityAbbreviation = cityAbbreviation;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getZip5() {
        return zip5;
    }

    public void setZip5(String zip5) {
        this.zip5 = zip5;
    }

    public String getZip4() {
        return zip4;
    }

    public void setZip4(String zip4) {
        this.zip4 = zip4;
    }

    public String getDeliveryPoint() {
        return deliveryPoint;
    }

    public void setDeliveryPoint(String deliveryPoint) {
        this.deliveryPoint = deliveryPoint;
    }

    public String getCarrierRoute() {
        return carrierRoute;
    }

    public void setCarrierRoute(String carrierRoute) {
        this.carrierRoute = carrierRoute;
    }

    public String getFootnotes() {
        return footnotes;
    }

    public void setFootnotes(String footnotes) {
        this.footnotes = footnotes;
    }

    public String getDPVConfirmation() {
        return DPVConfirmation;
    }

    public void setDPVConfirmation(String DPVConfirmation) {
        this.DPVConfirmation = DPVConfirmation;
    }

    public String getDPVCMRA() {
        return DPVCMRA;
    }

    public void setDPVCMRA(String DPVCMRA) {
        this.DPVCMRA = DPVCMRA;
    }

    public String getDPVFootnotes() {
        return DPVFootnotes;
    }

    public void setDPVFootnotes(String DPVFootnotes) {
        this.DPVFootnotes = DPVFootnotes;
    }

    public String getBusiness() {
        return business;
    }

    public void setBusiness(String business) {
        this.business = business;
    }

    public String getCentralDeliveryPoint() {
        return centralDeliveryPoint;
    }

    public void setCentralDeliveryPoint(String centralDeliveryPoint) {
        this.centralDeliveryPoint = centralDeliveryPoint;
    }

    public String getVacant() {
        return vacant;
    }

    public void setVacant(String vacant) {
        this.vacant = vacant;
    }

    public Error getError() {
        return error;
    }

    public void setError(Error error) {
        this.error = error;
    }
}

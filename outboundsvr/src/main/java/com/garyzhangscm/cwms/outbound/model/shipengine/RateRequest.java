package com.garyzhangscm.cwms.outbound.model.shipengine;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class RateRequest {

    @JsonProperty("rate_options")
    private RateOption rateOption;

    @JsonProperty("shipment")
    private Shipment shipment;

    public RateRequest(){}
    public RateRequest(RateOption rateOption, Shipment shipment) {
        this.rateOption = rateOption;
        this.shipment = shipment;
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

    public RateOption getRateOption() {
        return rateOption;
    }

    public void setRateOption(RateOption rateOption) {
        this.rateOption = rateOption;
    }

    public Shipment getShipment() {
        return shipment;
    }

    public void setShipment(Shipment shipment) {
        this.shipment = shipment;
    }
}

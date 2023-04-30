package com.garyzhangscm.cwms.outbound.model.hualei;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.garyzhangscm.cwms.outbound.model.shipengine.Address;
import com.garyzhangscm.cwms.outbound.model.shipengine.Package;

import java.util.ArrayList;
import java.util.List;

public class ShipmentRequest {
    @JsonProperty("getTrackingNumber")
    private String getTrackingNumber;
    @JsonProperty("param")
    private ShipmentRequestParameters shipmentRequestParameters;

    @Override
    public String toString() {
        try {
            return new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(this);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return null;
    }

    public String getGetTrackingNumber() {
        return getTrackingNumber;
    }

    public void setGetTrackingNumber(String getTrackingNumber) {
        this.getTrackingNumber = getTrackingNumber;
    }

    public ShipmentRequestParameters getShipmentRequestParameters() {
        return shipmentRequestParameters;
    }

    public void setShipmentRequestParameters(ShipmentRequestParameters shipmentRequestParameters) {
        this.shipmentRequestParameters = shipmentRequestParameters;
    }
}

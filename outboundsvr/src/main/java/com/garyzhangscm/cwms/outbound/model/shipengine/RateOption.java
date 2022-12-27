package com.garyzhangscm.cwms.outbound.model.shipengine;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

public class RateOption {
    @JsonProperty("carrier_ids")
    private List<String> carrierIds = new ArrayList<>();

    public RateOption(){}

    public RateOption(List<String> carrierIds) {
        this.carrierIds = carrierIds;
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

    public List<String> getCarrierIds() {
        return carrierIds;
    }

    public void setCarrierIds(List<String> carrierIds) {
        this.carrierIds = carrierIds;
    }

    public void addCarrierId(String carrierId) {
        this.carrierIds.add(carrierId);
    }
}

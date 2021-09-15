package com.garyzhangscm.cwms.adminserver.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Billable web request call
 *
 */
public class BillableRequestSummaryByWarehouse {


    private Long companyId;

    private Long warehouseId;

    private String serviceName;

    private Long totalWebAPIEndpointCall = 0l;

    private Long totalTransaction = 0l;

    private Long overallCost; // total transactions * rate


    @Override
    public String toString() {
        try {
            return new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(this);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return null;
    }


    public Long getCompanyId() {
        return companyId;
    }

    public void setCompanyId(Long companyId) {
        this.companyId = companyId;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public Long getTotalWebAPIEndpointCall() {
        return totalWebAPIEndpointCall;
    }

    public void setTotalWebAPIEndpointCall(Long totalWebAPIEndpointCall) {
        this.totalWebAPIEndpointCall = totalWebAPIEndpointCall;
    }

    public Long getTotalTransaction() {
        return totalTransaction;
    }

    public void setTotalTransaction(Long totalTransaction) {
        this.totalTransaction = totalTransaction;
    }

    public Long getOverallCost() {
        return overallCost;
    }

    public void setOverallCost(Long overallCost) {
        this.overallCost = overallCost;
    }
}

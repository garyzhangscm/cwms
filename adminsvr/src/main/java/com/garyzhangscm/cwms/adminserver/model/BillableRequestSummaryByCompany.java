package com.garyzhangscm.cwms.adminserver.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.persistence.*;

/**
 * Billable web request call
 *
 */
public class BillableRequestSummaryByCompany {


    private Long companyId;

    private String serviceName;

    private Long totalWebAPIEndpointCall = 0l;

    private Long totalTransaction = 0l;

    private Double overallCost; // total transactions * rate


    public BillableRequestSummaryByCompany() {

    }

    public BillableRequestSummaryByCompany(Long companyId,
                                           String serviceName,
                                           Long totalWebAPIEndpointCall,
                                           Long totalTransaction,
                                           Double overallCost) {
        this.companyId = companyId;
        this.serviceName = serviceName;
        this.totalWebAPIEndpointCall = totalWebAPIEndpointCall;
        this.totalTransaction = totalTransaction;
        this.overallCost = overallCost;

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

    public Double getOverallCost() {
        return overallCost;
    }

    public void setOverallCost(Double overallCost) {
        this.overallCost = overallCost;
    }
}

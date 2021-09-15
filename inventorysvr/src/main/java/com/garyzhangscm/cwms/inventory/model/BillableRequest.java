package com.garyzhangscm.cwms.inventory.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Billable web request call
 *
 */
public class BillableRequest {


    private Long companyId;

    private Long warehouseId;

    private String serviceName;

    private String webAPIEndpoint;

    private String requestMethod;

    private String parameters;

    private String requestBody;

    private String username;
    private String token;

    // when a request will be fulfilled by
    // multiple web api call, then we may
    // have multiple record in this table
    // with the same transaction id
    private String transactionId;

    private Double rate = 1.0;

    public BillableRequest(){}

    public BillableRequest(Long companyId,
                           Long warehouseId,
                           String serviceName,
                           String webAPIEndpoint,
                           String requestMethod,
                           String parameters,
                           String requestBody,
                           String username,
                           String transactionId,
                           Double rate,
                           String token) {
        this.companyId = companyId;
        this.warehouseId = warehouseId;
        this.serviceName = serviceName;
        this.webAPIEndpoint = webAPIEndpoint;
        this.requestMethod = requestMethod;
        this.parameters = parameters;
        this.requestBody = requestBody;
        this.username = username;
        this.transactionId = transactionId;
        this.rate = rate;
        this.token = token;
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

    public String getRequestMethod() {
        return requestMethod;
    }

    public void setRequestMethod(String requestMethod) {
        this.requestMethod = requestMethod;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public void setCompanyId(Long companyId) {
        this.companyId = companyId;
    }

    public Long getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(Long warehouseId) {
        this.warehouseId = warehouseId;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getWebAPIEndpoint() {
        return webAPIEndpoint;
    }

    public void setWebAPIEndpoint(String webAPIEndpoint) {
        this.webAPIEndpoint = webAPIEndpoint;
    }

    public String getParameters() {
        return parameters;
    }

    public void setParameters(String parameters) {
        this.parameters = parameters;
    }

    public String getRequestBody() {
        return requestBody;
    }

    public void setRequestBody(String requestBody) {
        this.requestBody = requestBody;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public Double getRate() {
        return rate;
    }

    public void setRate(Double rate) {
        this.rate = rate;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}

package com.garyzhangscm.cwms.adminserver.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.codehaus.jackson.annotate.JsonProperty;

import javax.persistence.*;

/**
 * Billable web request call
 *
 */
@Entity
@Table(name = "billable_request")
public class BillableRequest extends AuditibleEntity<String>{


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "billable_request_id")
    @JsonProperty(value="id")
    private Long id;


    @Column(name = "company_id")
    private Long companyId;

    @Column(name = "warehouse_id")
    private Long warehouseId;

    @Column(name = "service_name")
    private String serviceName;

    @Column(name = "web_api_endpoint")
    private String webAPIEndpoint;

    @Column(name = "request_method")
    private String requestMethod;


    @Column(name = "request_parameters")
    private String parameters;

    @Column(name = "request_body")
    private String requestBody;

    @Column(name = "username")
    private String username;

    // when a request will be fulfilled by
    // multiple web api call, then we may
    // have multiple record in this table
    // with the same transaction id
    @Column(name = "transaction_id")
    private String transactionId;

    @Column(name = "rate")
    private Double rate = 1.0;


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

    public String getRequestMethod() {
        return requestMethod;
    }

    public void setRequestMethod(String requestMethod) {
        this.requestMethod = requestMethod;
    }
}

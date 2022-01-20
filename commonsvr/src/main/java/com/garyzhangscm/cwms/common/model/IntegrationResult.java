package com.garyzhangscm.cwms.common.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class IntegrationResult {

    private Long integrationId;

    private Long companyId;
    private Long warehouseId;


    private IntegrationType integrationType;

    private boolean success;

    private String errorMessage;

    @Override
    public String toString() {
        try {
            return new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(this);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return null;
    }

    public IntegrationResult(){}
    

    public IntegrationResult(Long companyId, Long warehouseId,
                             Long integrationId, IntegrationType integrationType, boolean success, String errorMessage) {
        this.companyId = companyId;
        this.warehouseId = warehouseId;
        this.integrationId = integrationId;
        this.integrationType = integrationType;
        this.success = success;
        this.errorMessage = errorMessage;
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

    public Long getIntegrationId() {
        return integrationId;
    }

    public void setIntegrationId(Long integrationId) {
        this.integrationId = integrationId;
    }

    public IntegrationType getIntegrationType() {
        return integrationType;
    }

    public void setIntegrationType(IntegrationType integrationType) {
        this.integrationType = integrationType;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}

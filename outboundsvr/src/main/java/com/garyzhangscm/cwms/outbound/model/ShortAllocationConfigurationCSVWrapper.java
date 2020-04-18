package com.garyzhangscm.cwms.outbound.model;


public class ShortAllocationConfigurationCSVWrapper {

    private String warehouse;

    private Boolean enabled;

    private Long retryInterval;


    public String getWarehouse() {
        return warehouse;
    }

    public void setWarehouse(String warehouse) {
        this.warehouse = warehouse;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Long getRetryInterval() {
        return retryInterval;
    }

    public void setRetryInterval(Long retryInterval) {
        this.retryInterval = retryInterval;
    }
}

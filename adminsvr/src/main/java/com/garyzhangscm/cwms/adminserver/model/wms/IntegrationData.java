package com.garyzhangscm.cwms.adminserver.model.wms;

import java.io.Serializable;
import java.time.LocalDateTime;

public class IntegrationData implements Serializable {

    private Long id;
    private IntegrationStatus integrationStatus;

    private LocalDateTime insertTime;

    private LocalDateTime lastUpdateTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public IntegrationStatus getIntegrationStatus() {
        return integrationStatus;
    }

    public void setIntegrationStatus(IntegrationStatus integrationStatus) {
        this.integrationStatus = integrationStatus;
    }

    public LocalDateTime getInsertTime() {
        return insertTime;
    }

    public void setInsertTime(LocalDateTime insertTime) {
        this.insertTime = insertTime;
    }

    public LocalDateTime getLastUpdateTime() {
        return lastUpdateTime;
    }

    public void setLastUpdateTime(LocalDateTime lastUpdateTime) {
        this.lastUpdateTime = lastUpdateTime;
    }
}

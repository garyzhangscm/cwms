package com.garyzhangscm.cwms.outbound.model;


import org.codehaus.jackson.annotate.JsonProperty;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "short_allocation_configuration")
public class ShortAllocationConfiguration {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "short_allocation_configuration_id")
    @JsonProperty(value="id")
    private Long id;


    @Column(name = "warehouse_id")
    private Long warehouseId;

    @Transient
    private Warehouse warehouse;


    @Column(name = "last_modify_datetime")
    private LocalDateTime lastModifyDatetime;

    /**
     * User who carry out this activity
     */
    @Column(name = "last_modify_username")
    private String lastModifyUsername;


    @Column(name = "enabled")
    private Boolean enabled;

    /**
     * Retry interval in seconds
     */
    @Column(name = "retry_interval")
    private Long retryInterval;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(Long warehouseId) {
        this.warehouseId = warehouseId;
    }

    public Warehouse getWarehouse() {
        return warehouse;
    }

    public void setWarehouse(Warehouse warehouse) {
        this.warehouse = warehouse;
    }

    public LocalDateTime getLastModifyDatetime() {
        return lastModifyDatetime;
    }

    public void setLastModifyDatetime(LocalDateTime lastModifyDatetime) {
        this.lastModifyDatetime = lastModifyDatetime;
    }

    public String getLastModifyUsername() {
        return lastModifyUsername;
    }

    public void setLastModifyUsername(String lastModifyUsername) {
        this.lastModifyUsername = lastModifyUsername;
    }

    public Long getRetryInterval() {
        return retryInterval;
    }

    public void setRetryInterval(Long retryInterval) {
        this.retryInterval = retryInterval;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }
}

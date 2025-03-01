package com.garyzhangscm.cwms.outbound.model;


import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.persistence.*;
import java.time.ZonedDateTime;

@Entity
@Table(name = "short_allocation_configuration")
public class ShortAllocationConfiguration  extends AuditibleEntity<String>{


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
    @JsonDeserialize(using = CustomZonedDateTimeDeserializer.class)
    @JsonSerialize(using = CustomZonedDateTimeSerializer.class)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private ZonedDateTime lastModifyDatetime;

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

    public ZonedDateTime getLastModifyDatetime() {
        return lastModifyDatetime;
    }

    public void setLastModifyDatetime(ZonedDateTime lastModifyDatetime) {
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

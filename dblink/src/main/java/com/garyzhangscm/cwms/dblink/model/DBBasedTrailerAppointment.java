package com.garyzhangscm.cwms.dblink.model;


import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.logging.log4j.util.Strings;

import javax.persistence.*;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "integration_trailer_appointment")
public class DBBasedTrailerAppointment  implements Serializable{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "integration_trailer_appointment_id")
    @JsonProperty(value="id")
    private Long id;

    @Column(name = "company_id")
    private Long companyId;

    @Column(name = "company_code")
    private String companyCode;

    @Column(name = "warehouse_name")
    private String warehouseName;

    @Column(name = "warehouse_id")
    private Long warehouseId;

    @Column(name = "number")
    private String number;
    @Column(name = "description")
    private String description;

    @OneToMany(
            mappedBy = "trailerAppointment",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.EAGER
    )
    private List<DBBasedStop> stops = new ArrayList<>();

    @Column(name = "type")
    @Enumerated(EnumType.STRING)
    private TrailerAppointmentType type;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private IntegrationStatus status;
    @Column(name = "insert_time")
    private LocalDateTime insertTime;
    @Column(name = "last_update_time")
    private LocalDateTime lastUpdateTime;
    @Column(name = "error_message")
    private String errorMessage;

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

    public String getCompanyCode() {
        return companyCode;
    }

    public void setCompanyCode(String companyCode) {
        this.companyCode = companyCode;
    }

    public String getWarehouseName() {
        return warehouseName;
    }

    public void setWarehouseName(String warehouseName) {
        this.warehouseName = warehouseName;
    }

    public Long getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(Long warehouseId) {
        this.warehouseId = warehouseId;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<DBBasedStop> getStops() {
        return stops;
    }

    public void setStops(List<DBBasedStop> stops) {
        this.stops = stops;
    }

    public TrailerAppointmentType getType() {
        return type;
    }

    public void setType(TrailerAppointmentType type) {
        this.type = type;
    }

    public IntegrationStatus getStatus() {
        return status;
    }

    public void setStatus(IntegrationStatus status) {
        this.status = status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
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

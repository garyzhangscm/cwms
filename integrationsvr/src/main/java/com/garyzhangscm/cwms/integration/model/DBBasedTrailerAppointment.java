package com.garyzhangscm.cwms.integration.model;


import com.garyzhangscm.cwms.integration.clients.CommonServiceRestemplateClient;
import com.garyzhangscm.cwms.integration.clients.OutbuondServiceRestemplateClient;
import com.garyzhangscm.cwms.integration.clients.WarehouseLayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.integration.exception.MissingInformationException;
import com.garyzhangscm.cwms.integration.service.ObjectCopyUtil;
import org.apache.logging.log4j.util.Strings;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.*;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "integration_trailer_appointment")
public class DBBasedTrailerAppointment extends AuditibleEntity<String> implements Serializable, IntegrationTrailerAppointmentData{

    private static final Logger logger = LoggerFactory.getLogger(DBBasedTrailerAppointment.class);

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

    @Column(name = "error_message")
    private String errorMessage;


    public TrailerAppointment convertToTrailerAppointment(CommonServiceRestemplateClient commonServiceRestemplateClient,
                                                          WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient,
                                                          OutbuondServiceRestemplateClient outbuondServiceRestemplateClient) throws UnsupportedEncodingException {

        // company ID or company code is required
        if (Objects.isNull(companyId) && Strings.isBlank(companyCode)) {

            throw MissingInformationException.raiseException("company information is required for trailer appointment integration");
        }
        else if (Objects.isNull(companyId)) {
            // if company Id is empty, but we have company code,
            // then get the company id from the code
            setCompanyId(
                    warehouseLayoutServiceRestemplateClient
                            .getCompanyByCode(companyCode).getId()
            );
        }

        Long warehouseId = getWarehouseId();
        if (Objects.isNull(warehouseId)) {
            warehouseId = warehouseLayoutServiceRestemplateClient.getWarehouseId(
                    getCompanyId(), getCompanyCode(), getWarehouseId(), getWarehouseName()
            );
            setWarehouseId(warehouseId);
        }

        TrailerAppointment trailerAppointment = new TrailerAppointment();


        String[] fieldNames = {
                "warehouseId",
                "companyId",
                "number","description",
                "type"
        };

        ObjectCopyUtil.copyValue(this, trailerAppointment,  fieldNames);


        logger.debug("We have {} stops for this appointment {}",
                getStops().size(), getNumber());
        for(DBBasedStop dbBasedStop : getStops()) {
            trailerAppointment.addStop(dbBasedStop.convertToStop(
                    commonServiceRestemplateClient,
                    warehouseLayoutServiceRestemplateClient,
                    outbuondServiceRestemplateClient));
        }

        return trailerAppointment;

    }

    public void completeIntegration(IntegrationStatus integrationStatus) {
        completeIntegration(integrationStatus, "");
    }
    public void completeIntegration(IntegrationStatus integrationStatus, String errorMessage) {
        setStatus(integrationStatus);
        setErrorMessage(errorMessage);

        setLastModifiedTime(ZonedDateTime.now(ZoneId.of("UTC")));
        // Complete related integration
        getStops().forEach(dbBasedStop -> dbBasedStop.completeIntegration(integrationStatus, errorMessage));

    }

    public Long getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(Long warehouseId) {
        this.warehouseId = warehouseId;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public void setCompanyId(Long companyId) {
        this.companyId = companyId;
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

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public List<DBBasedStop> getStops() {
        return stops;
    }

    public void setStops(List<DBBasedStop> stops) {
        this.stops = stops;
    }



}

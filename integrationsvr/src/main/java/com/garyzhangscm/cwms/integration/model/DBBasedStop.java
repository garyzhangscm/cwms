package com.garyzhangscm.cwms.integration.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.garyzhangscm.cwms.integration.clients.CommonServiceRestemplateClient;
import com.garyzhangscm.cwms.integration.clients.InventoryServiceRestemplateClient;
import com.garyzhangscm.cwms.integration.clients.OutbuondServiceRestemplateClient;
import com.garyzhangscm.cwms.integration.clients.WarehouseLayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.integration.exception.MissingInformationException;
import com.garyzhangscm.cwms.integration.service.ObjectCopyUtil;
import org.apache.logging.log4j.util.Strings;
import org.codehaus.jackson.annotate.JsonProperty;

import javax.persistence.*;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "integration_stop")
public class DBBasedStop extends AuditibleEntity<String> implements Serializable, IntegrationStopData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "integration_stop_id")
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


    @Column(name = "sequence")
    private Long sequence;


    @Column(name = "ship_to_customer_id")
    private Long shipToCustomerId;
    @Column(name = "ship_to_customer_name")
    private String shipToCustomerName;


    @Column(name = "contactor_firstname")
    private String contactorFirstname;
    @Column(name = "contactor_lastname")
    private String contactorLastname;

    @Column(name = "address_country")
    private String addressCountry;
    @Column(name = "address_state")
    private String addressState;
    @Column(name = "address_county")
    private String addressCounty;
    @Column(name = "address_city")
    private String addressCity;
    @Column(name = "address_district")
    private String addressDistrict;
    @Column(name = "address_line1")
    private String addressLine1;
    @Column(name = "address_line2")
    private String addressLine2;
    @Column(name = "address_postcode")
    private String addressPostcode;




    @JsonIgnore
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "integration_trailer_appointment_id")
    private DBBasedTrailerAppointment trailerAppointment;

    @OneToMany(
            mappedBy = "stop",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.EAGER
    )
    private List<DBBasedShipment> shipments = new ArrayList<>();


    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private IntegrationStatus status;

    @Column(name = "error_message")
    private String errorMessage;

    public Stop convertToStop(CommonServiceRestemplateClient commonServiceRestemplateClient,
                              WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient,
                              OutbuondServiceRestemplateClient outbuondServiceRestemplateClient) throws UnsupportedEncodingException {
        // company ID or company code is required
        if (Objects.isNull(companyId) && Strings.isBlank(companyCode)) {

            throw MissingInformationException.raiseException("company information is required for item integration");
        }
        else if (Objects.isNull(companyId)) {
            // if company Id is empty, but we have company code,
            // then get the company id from the code
            setCompanyId(
                    warehouseLayoutServiceRestemplateClient
                            .getCompanyByCode(companyCode).getId()
            );
        }

        Stop stop = new Stop();

        Long warehouseId = getWarehouseId();
        if (Objects.isNull(warehouseId)) {
            warehouseId = warehouseLayoutServiceRestemplateClient.getWarehouseId(
                    getCompanyId(), getCompanyCode(), getWarehouseId(), getWarehouseName()
            );
            setWarehouseId(warehouseId);
        }

        if (Objects.isNull(getShipToCustomerId()) &&
                Strings.isNotBlank(getShipToCustomerName())) {
            Customer customer
                    = commonServiceRestemplateClient.getCustomerByName(
                    getCompanyId(),
                    getWarehouseId(),
                    getShipToCustomerName()
            );
            if (Objects.nonNull(customer)) {
                setShipToCustomerId(customer.getId());
            }
        }

        String[] fieldNames = {
                "warehouseId",
                "number","sequence","shipToCustomerId",
                "contactorFirstname","contactorLastname",
                "addressCountry","addressState",
                "addressCounty","addressCity","addressDistrict",
                "addressLine1","addressLine2","addressPostcode"
        };

        ObjectCopyUtil.copyValue(this, stop,  fieldNames);

        for(DBBasedShipment dbBasedShipment : getShipments()) {
            stop.addShipment(dbBasedShipment.convertToShipment(
                    commonServiceRestemplateClient,
                    warehouseLayoutServiceRestemplateClient,
                    outbuondServiceRestemplateClient
            ));
        }

        return stop;
    }
    public void completeIntegration(IntegrationStatus integrationStatus) {
        completeIntegration(integrationStatus, "");
    }
    public void completeIntegration(IntegrationStatus integrationStatus, String errorMessage) {
        setStatus(integrationStatus);
        setErrorMessage(errorMessage);
        setLastModifiedTime(ZonedDateTime.now(ZoneId.of("UTC")));
        // Complete related integration
        getShipments().forEach(dbBasedShipment
                -> dbBasedShipment.completeIntegration(integrationStatus, errorMessage));

    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public List<DBBasedShipment> getShipments() {
        return shipments;
    }

    public void setShipments(List<DBBasedShipment> shipments) {
        this.shipments = shipments;
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

    public DBBasedTrailerAppointment getTrailerAppointment() {
        return trailerAppointment;
    }

    public void setTrailerAppointment(DBBasedTrailerAppointment trailerAppointment) {
        this.trailerAppointment = trailerAppointment;
    }

    public Long getSequence() {
        return sequence;
    }

    public void setSequence(Long sequence) {
        this.sequence = sequence;
    }

    public String getContactorFirstname() {
        return contactorFirstname;
    }

    public void setContactorFirstname(String contactorFirstname) {
        this.contactorFirstname = contactorFirstname;
    }

    public String getContactorLastname() {
        return contactorLastname;
    }

    public void setContactorLastname(String contactorLastname) {
        this.contactorLastname = contactorLastname;
    }

    public String getAddressCountry() {
        return addressCountry;
    }

    public void setAddressCountry(String addressCountry) {
        this.addressCountry = addressCountry;
    }

    public String getAddressState() {
        return addressState;
    }

    public void setAddressState(String addressState) {
        this.addressState = addressState;
    }

    public String getAddressCounty() {
        return addressCounty;
    }

    public void setAddressCounty(String addressCounty) {
        this.addressCounty = addressCounty;
    }

    public String getAddressCity() {
        return addressCity;
    }

    public void setAddressCity(String addressCity) {
        this.addressCity = addressCity;
    }

    public String getAddressDistrict() {
        return addressDistrict;
    }

    public void setAddressDistrict(String addressDistrict) {
        this.addressDistrict = addressDistrict;
    }

    public String getAddressLine1() {
        return addressLine1;
    }

    public void setAddressLine1(String addressLine1) {
        this.addressLine1 = addressLine1;
    }

    public String getAddressLine2() {
        return addressLine2;
    }

    public void setAddressLine2(String addressLine2) {
        this.addressLine2 = addressLine2;
    }

    public String getAddressPostcode() {
        return addressPostcode;
    }

    public void setAddressPostcode(String addressPostcode) {
        this.addressPostcode = addressPostcode;
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

    public Long getShipToCustomerId() {
        return shipToCustomerId;
    }

    public void setShipToCustomerId(Long shipToCustomerId) {
        this.shipToCustomerId = shipToCustomerId;
    }

    public String getShipToCustomerName() {
        return shipToCustomerName;
    }

    public void setShipToCustomerName(String shipToCustomerName) {
        this.shipToCustomerName = shipToCustomerName;
    }
}

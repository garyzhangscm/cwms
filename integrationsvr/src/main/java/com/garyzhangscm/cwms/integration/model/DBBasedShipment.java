/**
 * Copyright 2019
 *
 * @author gzhang
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.garyzhangscm.cwms.integration.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.garyzhangscm.cwms.integration.clients.CommonServiceRestemplateClient;
import com.garyzhangscm.cwms.integration.clients.InventoryServiceRestemplateClient;
import com.garyzhangscm.cwms.integration.clients.OutbuondServiceRestemplateClient;
import com.garyzhangscm.cwms.integration.clients.WarehouseLayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.integration.exception.MissingInformationException;
import com.garyzhangscm.cwms.integration.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.integration.service.DBBasedTrailerAppointmentIntegration;
import com.garyzhangscm.cwms.integration.service.ObjectCopyUtil;
import org.apache.logging.log4j.util.Strings;
import org.codehaus.jackson.annotate.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "integration_shipment")
public class DBBasedShipment extends AuditibleEntity<String> implements Serializable, IntegrationShipmentData {

    private static final Logger logger = LoggerFactory.getLogger(DBBasedShipment.class);
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "integration_shipment_id")
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



    @Column(name = "carrier_id")
    private Long carrierId;
    @Column(name = "carrier_name")
    private String carrierName;


    @Column(name = "carrier_service_level_id")
    private Long carrierServiceLevelId;
    @Column(name = "carrier_service_level_name")
    private String carrierServiceLevelName;


    @Column(name = "ship_to_customer_id")
    private Long shipToCustomerId;
    @Column(name = "ship_to_customer_name")
    private String shipToCustomerName;

    @Column(name = "client_id")
    private Long clientId;
    @Column(name = "client_name")
    private String clientName;

    @Column(name = "order_id")
    private Long orderId;
    @Column(name = "order_number")
    private String orderNumber;



    @Column(name = "ship_to_contactor_firstname")
    private String shipToContactorFirstname;
    @Column(name = "ship_to_contactor_lastname")
    private String shipToContactorLastname;

    @Column(name = "ship_to_address_country")
    private String shipToAddressCountry;
    @Column(name = "ship_to_address_state")
    private String shipToAddressState;
    @Column(name = "ship_to_address_county")
    private String shipToAddressCounty;
    @Column(name = "ship_to_address_city")
    private String shipToAddressCity;
    @Column(name = "ship_to_address_district")
    private String shipToAddressDistrict;
    @Column(name = "ship_to_address_line1")
    private String shipToAddressLine1;
    @Column(name = "ship_to_address_line2")
    private String shipToAddressLine2;
    @Column(name = "ship_to_address_postcode")
    private String shipToAddressPostcode;


    @OneToMany(
            mappedBy = "shipment",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.EAGER
    )
    private List<DBBasedShipmentLine> shipmentLines = new ArrayList<>();

    @JsonIgnore
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "integration_stop_id")
    private DBBasedStop stop;


    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private IntegrationStatus status;

    @Column(name = "error_message")
    private String errorMessage;


    public Shipment convertToShipment(CommonServiceRestemplateClient commonServiceRestemplateClient,
                                      WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient,
                                      OutbuondServiceRestemplateClient outbuondServiceRestemplateClient) {

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

        Shipment shipment = new Shipment();

        Long warehouseId = getWarehouseId();
        if (Objects.isNull(warehouseId)) {
            warehouseId = warehouseLayoutServiceRestemplateClient.getWarehouseId(
                    getCompanyId(), getCompanyCode(), getWarehouseId(), getWarehouseName()
            );
            setWarehouseId(warehouseId);
        }

        // setup the ids
        if (Objects.isNull(getCarrierId()) &&
                Strings.isNotBlank(getCarrierName())) {
            Carrier carrier = commonServiceRestemplateClient.getCarrierByName(
                    getWarehouseId(),
                    getCarrierName()
            );
            if (Objects.nonNull(carrier)) {
                setCarrierId(carrier.getId());
            }
        }

        if (Objects.isNull(getCarrierServiceLevelId()) &&
                Strings.isNotBlank(getCarrierServiceLevelName())) {
            CarrierServiceLevel carrierServiceLevel
                    = commonServiceRestemplateClient.getCarrierServiceLevelByName(
                    getWarehouseId(),
                    getCarrierServiceLevelName()
            );
            if (Objects.nonNull(carrierServiceLevel)) {
                setCarrierServiceLevelId(carrierServiceLevel.getId());
            }
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
        if (Objects.isNull(getCarrierId()) &&
                Strings.isNotBlank(getClientName())) {
            Client client
                    = commonServiceRestemplateClient.getClientByName(
                    getWarehouseId(),
                    getShipToCustomerName()
            );
            if (Objects.nonNull(client)) {
                setClientId(client.getId());
            }
        }

        Order order = null;
        if (Objects.nonNull(getOrderId())) {
            order = outbuondServiceRestemplateClient.getOrderById(
                    getOrderId()
            );
        }
        else if (Strings.isNotBlank(getOrderNumber())) {
            logger.debug("start to get order by number {} , {}",
                    getWarehouseId(), getOrderNumber());
            order
                    = outbuondServiceRestemplateClient.getOrderByNumber(
                    getWarehouseId(),
                    getOrderNumber()
            );

            logger.debug("order exists? {}",
                    Objects.nonNull(order));
            if (Objects.nonNull(order)) {
                setOrderId(order.getId());
            }
        }

        if (Objects.isNull(order)) {
            throw ResourceNotFoundException.raiseException("order by id " +
                            (Objects.nonNull(getOrderId()) ? String.valueOf(getOrderId()) : "N/A") +
                            ", number " + (Strings.isNotBlank(getOrderNumber()) ? getOrderNumber() : "N/A") +
                            " not exists");
        }
        String[] fieldNames = {
                "warehouseId",
                "number","carrierId","carrierServiceLevelId",
                "shipToCustomerId","shipToContactorFirstname","shipToContactorLastname",
                "shipToAddressCountry","shipToAddressState",
                "shipToAddressCounty","shipToAddressCity","shipToAddressDistrict",
                "shipToAddressLine1","shipToAddressLine2","shipToAddressPostcode",
                "clientId",
                "orderId"
        };

        ObjectCopyUtil.copyValue(this, shipment,  fieldNames);

        for (DBBasedShipmentLine dbBasedShipmentLine : getShipmentLines()) {

            shipment.addShipmentLine(dbBasedShipmentLine.convertToShipmentLine(
                    warehouseLayoutServiceRestemplateClient, order
            ));
        }

        return shipment;
    }

    public void completeIntegration(IntegrationStatus integrationStatus) {
        completeIntegration(integrationStatus, "");
    }
    public void completeIntegration(IntegrationStatus integrationStatus, String errorMessage) {
        setStatus(integrationStatus);
        setErrorMessage(errorMessage);
        setLastModifiedTime(LocalDateTime.now());
        // Complete related integration
        getShipmentLines().forEach(dbBasedShipmentLine
                -> dbBasedShipmentLine.completeIntegration(integrationStatus, errorMessage));

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

    public Long getCarrierId() {
        return carrierId;
    }

    public void setCarrierId(Long carrierId) {
        this.carrierId = carrierId;
    }

    public Long getCarrierServiceLevelId() {
        return carrierServiceLevelId;
    }

    public void setCarrierServiceLevelId(Long carrierServiceLevelId) {
        this.carrierServiceLevelId = carrierServiceLevelId;
    }

    public Long getShipToCustomerId() {
        return shipToCustomerId;
    }

    public void setShipToCustomerId(Long shipToCustomerId) {
        this.shipToCustomerId = shipToCustomerId;
    }

    public String getShipToContactorFirstname() {
        return shipToContactorFirstname;
    }

    public void setShipToContactorFirstname(String shipToContactorFirstname) {
        this.shipToContactorFirstname = shipToContactorFirstname;
    }

    public String getShipToContactorLastname() {
        return shipToContactorLastname;
    }

    public void setShipToContactorLastname(String shipToContactorLastname) {
        this.shipToContactorLastname = shipToContactorLastname;
    }

    public String getShipToAddressCountry() {
        return shipToAddressCountry;
    }

    public void setShipToAddressCountry(String shipToAddressCountry) {
        this.shipToAddressCountry = shipToAddressCountry;
    }

    public String getShipToAddressState() {
        return shipToAddressState;
    }

    public void setShipToAddressState(String shipToAddressState) {
        this.shipToAddressState = shipToAddressState;
    }

    public String getShipToAddressCounty() {
        return shipToAddressCounty;
    }

    public void setShipToAddressCounty(String shipToAddressCounty) {
        this.shipToAddressCounty = shipToAddressCounty;
    }

    public String getShipToAddressCity() {
        return shipToAddressCity;
    }

    public void setShipToAddressCity(String shipToAddressCity) {
        this.shipToAddressCity = shipToAddressCity;
    }

    public String getShipToAddressDistrict() {
        return shipToAddressDistrict;
    }

    public void setShipToAddressDistrict(String shipToAddressDistrict) {
        this.shipToAddressDistrict = shipToAddressDistrict;
    }

    public String getShipToAddressLine1() {
        return shipToAddressLine1;
    }

    public void setShipToAddressLine1(String shipToAddressLine1) {
        this.shipToAddressLine1 = shipToAddressLine1;
    }

    public String getShipToAddressLine2() {
        return shipToAddressLine2;
    }

    public void setShipToAddressLine2(String shipToAddressLine2) {
        this.shipToAddressLine2 = shipToAddressLine2;
    }

    public String getShipToAddressPostcode() {
        return shipToAddressPostcode;
    }

    public void setShipToAddressPostcode(String shipToAddressPostcode) {
        this.shipToAddressPostcode = shipToAddressPostcode;
    }

    public Long getClientId() {
        return clientId;
    }

    public void setClientId(Long clientId) {
        this.clientId = clientId;
    }

    public List<DBBasedShipmentLine> getShipmentLines() {
        return shipmentLines;
    }

    public void setShipmentLines(List<DBBasedShipmentLine> shipmentLines) {
        this.shipmentLines = shipmentLines;
    }
    public void addShipmentLine(DBBasedShipmentLine shipmentLine) {
        this.shipmentLines.add(shipmentLine);
    }

    public DBBasedStop getStop() {
        return stop;
    }

    public void setStop(DBBasedStop stop) {
        this.stop = stop;
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

    public String getCarrierName() {
        return carrierName;
    }

    public void setCarrierName(String carrierName) {
        this.carrierName = carrierName;
    }

    public String getCarrierServiceLevelName() {
        return carrierServiceLevelName;
    }

    public void setCarrierServiceLevelName(String carrierServiceLevelName) {
        this.carrierServiceLevelName = carrierServiceLevelName;
    }

    public String getShipToCustomerName() {
        return shipToCustomerName;
    }

    public void setShipToCustomerName(String shipToCustomerName) {
        this.shipToCustomerName = shipToCustomerName;
    }

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }
}

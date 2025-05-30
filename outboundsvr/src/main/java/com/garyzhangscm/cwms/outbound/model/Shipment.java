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

package com.garyzhangscm.cwms.outbound.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Entity
@Table(name = "shipment")
public class Shipment  extends AuditibleEntity<String> implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "shipment_id")
    @JsonProperty(value="id")
    private Long id;

    @Column(name = "number")
    private String number;

    @Column(name = "warehouse_id")
    private Long warehouseId;

    @Transient
    private Warehouse warehouse;

    @OneToMany(
        mappedBy = "shipment",
        cascade = CascadeType.REMOVE,
        orphanRemoval = true,
        fetch = FetchType.LAZY
    )
    private List<ShipmentLine> shipmentLines = new ArrayList<>();

    @Column(name="status")
    @Enumerated(EnumType.STRING)
    private ShipmentStatus status;

    @Column(name = "carrier_id")
    private Long carrierId;

    @Transient
    private Carrier carrier;

    @Column(name = "carrier_service_level_id")
    private Long carrierServiceLevelId;

    @Transient
    private CarrierServiceLevel carrierServiceLevel;

    @Column(name = "ship_to_customer_id")
    private Long shipToCustomerId;
    @Transient
    private Customer shipToCustomer;

    @Column(name = "complete_time")
    @JsonDeserialize(using = CustomZonedDateTimeDeserializer.class)
    @JsonSerialize(using = CustomZonedDateTimeSerializer.class)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private ZonedDateTime completeTime;
    // private LocalDateTime completeTime;

    // Ship to Address
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

    @Column(name = "client_id")
    private Long clientId;

    @Transient
    private Client client;

    @ManyToOne
    @JoinColumn(name="stop_id")
    @JsonIgnore
    private Stop stop;


    // shipping related information.
    // in case the user don't want to go through
    // the stop / trailer / load structure,
    // we provide a way to simplify the process
    // for small business so that the user
    // can specify the load number and
    // bol number at the shipment level
    @Column(name = "load_number")
    private String loadNumber;

    @Column(name = "bill_of_lading_number")
    private String billOfLadingNumber;


    // order id, used by stop integration
    @Transient
    private Long orderId;
    @Transient
    private Order order;

    public Shipment() {}

    public Shipment(String number, Order order) {
        setNumber(number);
        setWarehouseId(order.getWarehouseId());
        setStatus(ShipmentStatus.PENDING);
        setCarrierId(order.getCarrierId());
        setCarrierServiceLevelId(order.getCarrierServiceLevelId());
        setClientId(order.getClientId());

        setShipToCustomer(order.getShipToCustomer());
        setShipToContactorFirstname(order.getShipToContactorFirstname());
        setShipToContactorLastname(order.getShipToContactorLastname());

        setShipToAddressCountry(order.getShipToAddressCountry());
        setShipToAddressState(order.getShipToAddressState());
        setShipToAddressCounty(order.getShipToAddressCounty());
        setShipToAddressCity(order.getShipToAddressCity());
        setShipToAddressDistrict(order.getShipToAddressDistrict());
        setShipToAddressLine1(order.getShipToAddressLine1());
        setShipToAddressLine2(order.getShipToAddressLine2());
        setShipToAddressPostcode(order.getShipToAddressPostcode());
    }
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Shipment shipment = (Shipment) o;

        if (id != null && shipment.id != null) {
            return Objects.equals(id, shipment.id);
        }

        return
                Objects.equals(number, shipment.number) &&
                Objects.equals(warehouseId, shipment.warehouseId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        try {
            return new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(this);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return null;
    }

    public Set<String> getOrderNumbers() {
        if (Objects.nonNull(getOrder())) {
            return Collections.singleton(getOrder().getNumber());
        }
        return getShipmentLines().stream().map(ShipmentLine::getOrderNumber).collect(Collectors.toSet());
    }

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }

    /**
     * Right now, we normally have one order for each shipment
     * @return
     */
    public String getOrderNumber() {
        return getShipmentLines().stream().map(ShipmentLine::getOrderNumber).findFirst().orElse("");
    }
    public String getStopNumber() {
        return Objects.isNull(stop) ? "" : stop.getNumber();
    }
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public ZonedDateTime getCompleteTime() {
        return completeTime;
    }

    public void setCompleteTime(ZonedDateTime completeTime) {
        this.completeTime = completeTime;
    }

    public List<ShipmentLine> getShipmentLines() {
        return shipmentLines;
    }

    public void setShipmentLines(List<ShipmentLine> shipmentLines) {
        this.shipmentLines = shipmentLines;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public ShipmentStatus getStatus() {
        return status;
    }

    public void setStatus(ShipmentStatus status) {
        this.status = status;
    }

    public Stop getStop() {
        return stop;
    }

    public void setStop(Stop stop) {
        this.stop = stop;
    }

    public Long getCarrierId() {
        return carrierId;
    }

    public void setCarrierId(Long carrierId) {
        this.carrierId = carrierId;
    }

    public Carrier getCarrier() {
        return carrier;
    }

    public void setCarrier(Carrier carrier) {
        this.carrier = carrier;
    }

    public Long getCarrierServiceLevelId() {
        return carrierServiceLevelId;
    }

    public void setCarrierServiceLevelId(Long carrierServiceLevelId) {
        this.carrierServiceLevelId = carrierServiceLevelId;
    }

    public CarrierServiceLevel getCarrierServiceLevel() {
        return carrierServiceLevel;
    }

    public void setCarrierServiceLevel(CarrierServiceLevel carrierServiceLevel) {
        this.carrierServiceLevel = carrierServiceLevel;
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

    public Long getShipToCustomerId() {
        return shipToCustomerId;
    }

    public void setShipToCustomerId(Long shipToCustomerId) {
        this.shipToCustomerId = shipToCustomerId;
    }

    public Customer getShipToCustomer() {
        return shipToCustomer;
    }

    public void setShipToCustomer(Customer shipToCustomer) {
        this.shipToCustomer = shipToCustomer;
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

    public Client getClient() {
        return client;
    }

    public void setClient(Client client) {
        this.client = client;
    }

    public String getLoadNumber() {
        return loadNumber;
    }

    public void setLoadNumber(String loadNumber) {
        this.loadNumber = loadNumber;
    }

    public String getBillOfLadingNumber() {
        return billOfLadingNumber;
    }

    public void setBillOfLadingNumber(String billOfLadingNumber) {
        this.billOfLadingNumber = billOfLadingNumber;
    }
}

package com.garyzhangscm.cwms.outbound.model.hualei;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.garyzhangscm.cwms.outbound.model.AuditibleEntity;
import com.garyzhangscm.cwms.outbound.model.Order;

import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "hualei_shipment_request")
public class ShipmentRequest  extends AuditibleEntity<String> implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "hualei_shipment_request_id")
    @JsonProperty(value="id")
    private Long id;

    @Column(name = "warehouse_id")
    private Long warehouseId;

    @JsonProperty("getTrackingNumber")
    @Column(name = "get_tracking_number")
    private String getTrackingNumber;

    @OneToOne(
            mappedBy = "shipmentRequest",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    @JsonProperty("param")
    private ShipmentRequestParameters shipmentRequestParameters;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private ShipmentRequestStatus status;

    @OneToOne(
            mappedBy = "shipmentRequest",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    private ShipmentResponse shipmentResponse;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "outbound_order_id")
    private Order order;

    @Override
    public String toString() {
        try {
            return new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(this);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return null;
    }

    public String getGetTrackingNumber() {
        return getTrackingNumber;
    }

    public void setGetTrackingNumber(String getTrackingNumber) {
        this.getTrackingNumber = getTrackingNumber;
    }

    public ShipmentRequestParameters getShipmentRequestParameters() {
        return shipmentRequestParameters;
    }

    public void setShipmentRequestParameters(ShipmentRequestParameters shipmentRequestParameters) {
        this.shipmentRequestParameters = shipmentRequestParameters;
    }

    public ShipmentResponse getShipmentResponse() {
        return shipmentResponse;
    }

    public void setShipmentResponse(ShipmentResponse shipmentResponse) {
        this.shipmentResponse = shipmentResponse;
    }

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

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }

    public ShipmentRequestStatus getStatus() {
        return status;
    }

    public void setStatus(ShipmentRequestStatus status) {
        this.status = status;
    }
}

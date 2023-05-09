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

import com.easypost.model.Shipment;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.garyzhangscm.cwms.outbound.model.hualei.*;
import org.apache.logging.log4j.util.Strings;
import org.codehaus.jackson.annotate.JsonProperty;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "parcel_package")
public class ParcelPackage extends AuditibleEntity<String> implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "parcel_package_id")
    @JsonProperty(value="id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @NotFound(action = NotFoundAction.IGNORE)
    @JoinColumn(name = "order_id")
    @JsonIgnore
    private Order order;

    @Column(name = "warehouse_id")
    private Long warehouseId;

    @Column(name = "tracking_code")
    private String trackingCode;

    @Column(name = "tracking_url")
    private String trackingUrl;
    @Column(name = "status")
    private String status;
    @Column(name = "shipment_id")
    private String shipmentId;


    @Column(name = "length")
    private Double length;
    @Column(name = "width")
    private Double width;
    @Column(name = "height")
    private Double height;
    @Column(name = "weight")
    private Double weight;

    @Column(name = "carrier")
    private String carrier;
    @Column(name = "service")
    private String service;

    @Column(name = "delivery_days")
    private Integer deliveryDays;
    @Column(name = "rate")
    private Double rate;

    @Column(name = "label_resolution")
    private Integer labelResolution;
    @Column(name = "label_size")
    private String labelSize;
    @Column(name = "label_url")
    private String labelUrl;


    @Column(name = "insurance")
    private String insurance;

    public ParcelPackage() {}

    // get parcel package from easy post system
    public ParcelPackage(Long warehouseId, Order order, Shipment easyPostShipment) {
        this.warehouseId = warehouseId;
        this.order = order;

        trackingCode = easyPostShipment.getTrackingCode();

        trackingUrl = easyPostShipment.getTracker().getPublicUrl();
        status = easyPostShipment.getStatus();
        shipmentId = easyPostShipment.getId();


        length = Double.valueOf(easyPostShipment.getParcel().getLength());
        width = Double.valueOf(easyPostShipment.getParcel().getWidth());
        height = Double.valueOf(easyPostShipment.getParcel().getHeight());
        weight = Double.valueOf(easyPostShipment.getParcel().getWeight());

        carrier = easyPostShipment.getSelectedRate().getCarrier();
        service = easyPostShipment.getSelectedRate().getService();

        deliveryDays = easyPostShipment.getSelectedRate().getDeliveryDays().intValue();
        rate = Double.valueOf(easyPostShipment.getSelectedRate().getRate());

        labelResolution = easyPostShipment.getPostageLabel().getLabelResolution();
        labelSize = easyPostShipment.getPostageLabel().getLabelSize();
        labelUrl = easyPostShipment.getPostageLabel().getLabelUrl();

        insurance = easyPostShipment.getInsurance();

    }

    public ParcelPackage(Long warehouseId, Order order, Carrier carrier,
                         CarrierServiceLevel carrierServiceLevel,
                         String trackingCode, double length, double width,
                         double height, double weight,
                         Integer deliveryDays, Double rate
                         ) {
        this.warehouseId = warehouseId;
        this.order = order;
        // shipment ID is the business key for the package
        this.shipmentId = trackingCode;

        this.trackingCode = trackingCode;

        trackingUrl = Objects.nonNull(carrier) && Strings.isNotBlank(carrier.getTrackingInfoUrl()) ?
            carrier.getTrackingInfoUrl() + trackingCode : "";
        status = "";
        shipmentId = "";


        this.length = length;
        this.width = width;
        this.height = height;
        this.weight = weight;


        this.carrier = Objects.nonNull(carrier) ? carrier.getName() : "";
        this.service = Objects.nonNull(carrierServiceLevel) ? carrier.getName() : "";

        this.deliveryDays = deliveryDays;
        this.rate = rate;

        labelResolution = 0;
        labelSize = "";
        labelUrl = "";

        insurance = "";

    }
    // get parcel package from hualei system
    public ParcelPackage(Long warehouseId, Order order, Carrier carrier,
                         CarrierServiceLevel carrierServiceLevel,
                         HualeiConfiguration hualeiConfiguration,
                         HualeiShippingLabelFormatByProduct hualeiShippingLabelFormatByProduct,
                         String trackingCode,
                         String hualeiOrderId,
                         double length,
                         double width,
                         double height,
                         double weight) {
        this.warehouseId = warehouseId;
        this.order = order;

        this.trackingCode = trackingCode;
        // shipment ID is the business key for the package
        this.shipmentId = trackingCode;

        trackingUrl = Objects.nonNull(hualeiShippingLabelFormatByProduct) ?
                        hualeiShippingLabelFormatByProduct.getTrackingInfoUrl() + trackingCode
                        : "";

        this.length = length;
        this.width = width;
        this.height = height;
        this.weight = weight;

        // we will assume there's only one package per shipment
        /**
        if (Objects.nonNull(shipmentResponse.getShipmentRequest()) &&
            Objects.nonNull(shipmentResponse.getShipmentRequest().getShipmentRequestParameters()) &&
            Objects.nonNull(shipmentResponse.getShipmentRequest().getShipmentRequestParameters().getOrderVolumeParams()) &&
                !shipmentResponse.getShipmentRequest().getShipmentRequestParameters().getOrderVolumeParams().isEmpty()) {

            ShipmentRequestOrderVolumeParameters shipmentRequestOrderVolumeParameters =
                    shipmentResponse.getShipmentRequest().getShipmentRequestParameters().getOrderVolumeParams().get(0);
            length = Double.valueOf(shipmentRequestOrderVolumeParameters.getVolumeLength());
            width = Double.valueOf(shipmentRequestOrderVolumeParameters.getVolumeWidth());
            height = Double.valueOf(shipmentRequestOrderVolumeParameters.getVolumeHeight());
            weight = Double.valueOf(shipmentRequestOrderVolumeParameters.getVolumeWeight());
        }
         **/

        this.carrier = Objects.nonNull(carrier) ? carrier.getName() : "";
        service = Objects.nonNull(carrierServiceLevel) ? carrierServiceLevel.getName() : "";

        deliveryDays = 0;
        rate = 0.0;

        labelResolution = 0;
        labelSize = Objects.nonNull(hualeiShippingLabelFormatByProduct) ?
                       hualeiShippingLabelFormatByProduct.getShippingLabelFormat().name() : "";
        labelUrl = Objects.nonNull(hualeiShippingLabelFormatByProduct) ?
                   (hualeiConfiguration.getPrintLabelProtocol() + "://" + hualeiConfiguration.getPrintLabelHost() +
                        ":" + hualeiConfiguration.getPrintLabelPort() + "/" + hualeiConfiguration.getPrintLabelEndpoint() +
                        "?PrintType=" + hualeiShippingLabelFormatByProduct.getShippingLabelFormat().name() +
                        "&order_id=" + hualeiOrderId)
                        : "";

        insurance = "0";

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

    public String getTrackingCode() {
        return trackingCode;
    }

    public void setTrackingCode(String trackingCode) {
        this.trackingCode = trackingCode;
    }

    public String getTrackingUrl() {
        return trackingUrl;
    }

    public void setTrackingUrl(String trackingUrl) {
        this.trackingUrl = trackingUrl;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getShipmentId() {
        return shipmentId;
    }

    public void setShipmentId(String shipmentId) {
        this.shipmentId = shipmentId;
    }

    public Double getLength() {
        return length;
    }

    public void setLength(Double length) {
        this.length = length;
    }

    public Double getWidth() {
        return width;
    }

    public void setWidth(Double width) {
        this.width = width;
    }

    public Double getHeight() {
        return height;
    }

    public void setHeight(Double height) {
        this.height = height;
    }

    public Double getWeight() {
        return weight;
    }

    public void setWeight(Double weight) {
        this.weight = weight;
    }

    public String getCarrier() {
        return carrier;
    }

    public void setCarrier(String carrier) {
        this.carrier = carrier;
    }

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    public Integer getDeliveryDays() {
        return deliveryDays;
    }

    public void setDeliveryDays(Integer deliveryDays) {
        this.deliveryDays = deliveryDays;
    }

    public Double getRate() {
        return rate;
    }

    public void setRate(Double rate) {
        this.rate = rate;
    }

    public int getLabelResolution() {
        return labelResolution;
    }

    public void setLabelResolution(int labelResolution) {
        this.labelResolution = labelResolution;
    }

    public String getLabelSize() {
        return labelSize;
    }

    public void setLabelSize(String labelSize) {
        this.labelSize = labelSize;
    }

    public String getLabelUrl() {
        return labelUrl;
    }

    public void setLabelUrl(String labelUrl) {
        this.labelUrl = labelUrl;
    }

    public void setLabelResolution(Integer labelResolution) {
        this.labelResolution = labelResolution;
    }

    public String getInsurance() {
        return insurance;
    }

    public void setInsurance(String insurance) {
        this.insurance = insurance;
    }
}

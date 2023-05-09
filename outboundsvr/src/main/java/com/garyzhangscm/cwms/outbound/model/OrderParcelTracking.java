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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.codehaus.jackson.annotate.JsonProperty;

import javax.persistence.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "outbound_order_parcel_tracking")
public class OrderParcelTracking extends AuditibleEntity<String> implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "outbound_order_parcel_tracking_id")
    @JsonProperty(value="id")
    private Long id;

    @Column(name = "warehouse_id")
    private Long warehouseId;

    @Column(name = "tracking_number")
    private String trackingNumber;

    @Transient
    private String orderNumber;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "outbound_order_id")
    private Order order;

    @Column(name = "carrier_id")
    private Long carrierId;

    @Transient
    private Carrier carrier;

    @Column(name = "carrier_service_level_id")
    private Long carrierServiceLevelId;

    @Transient
    private CarrierServiceLevel carrierServiceLevel;

    public OrderParcelTracking() {}
    public OrderParcelTracking(Order order,
                               String trackingNumber,
                               Long carrierId,
                               Long carrierServiceLevelId) {


        setOrder(order);
        setTrackingNumber(trackingNumber);

        setCarrierId(carrierId);
        setCarrierServiceLevelId(carrierServiceLevelId);

        setWarehouseId(order.getWarehouseId());

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

    public String getTrackingNumber() {
        return trackingNumber;
    }

    public void setTrackingNumber(String trackingNumber) {
        this.trackingNumber = trackingNumber;
    }

    public String getOrderNumber() {
        return Objects.isNull(order) ? "" : order.getNumber();
    }

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
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
}

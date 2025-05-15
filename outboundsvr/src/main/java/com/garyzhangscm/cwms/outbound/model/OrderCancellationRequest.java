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
import org.apache.logging.log4j.util.Strings;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.persistence.*;
import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "order_cancellation_request")
public class OrderCancellationRequest extends AuditibleEntity<String> implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_cancellation_request_id")
    @JsonProperty(value="id")
    private Long id;


    @Column(name = "warehouse_id")
    private Long warehouseId;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "outbound_order_id")
    private Order order;

    @Transient
    private String orderNumber;

    @Column(name = "cancel_requested_time")
    @JsonDeserialize(using = CustomZonedDateTimeDeserializer.class)
    @JsonSerialize(using = CustomZonedDateTimeSerializer.class)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private ZonedDateTime cancelRequestedTime;

    @Column(name = "cancel_requested_username")
    private String cancelRequestedUsername;

    @Column(name = "request_result")
    @Enumerated(EnumType.STRING)
    private OrderCancellationRequestResult result;

    @Column(name = "message")
    private String message;

    public OrderCancellationRequest() {}
    public OrderCancellationRequest(Order order,
                                    ZonedDateTime cancelRequestedTime,
                                    String cancelRequestedUsername,
                                    OrderCancellationRequestResult result,
                                    String message) {
        this.order = order;
        this.warehouseId = order.getWarehouseId();
        this.cancelRequestedTime = cancelRequestedTime;
        this.cancelRequestedUsername = cancelRequestedUsername;
        this.result = result;
        this.message = message;
    }
    public OrderCancellationRequest(Order order,
                                    String cancelRequestedUsername,
                                    OrderCancellationRequestResult result,
                                    String message) {
        this(order, ZonedDateTime.now(), cancelRequestedUsername,
                result, message);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }

    public ZonedDateTime getCancelRequestedTime() {
        return cancelRequestedTime;
    }

    public void setCancelRequestedTime(ZonedDateTime cancelRequestedTime) {
        this.cancelRequestedTime = cancelRequestedTime;
    }

    public String getCancelRequestedUsername() {
        return cancelRequestedUsername;
    }

    public void setCancelRequestedUsername(String cancelRequestedUsername) {
        this.cancelRequestedUsername = cancelRequestedUsername;
    }

    public OrderCancellationRequestResult getResult() {
        return result;
    }

    public void setResult(OrderCancellationRequestResult result) {
        this.result = result;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Long getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(Long warehouseId) {
        this.warehouseId = warehouseId;
    }

    public String getOrderNumber() {
        if (Objects.nonNull(order)) {
            return order.getNumber();
        }
        else if(Strings.isBlank(orderNumber)) {
            return orderNumber;
        }
        else {
            return "";
        }
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }
}

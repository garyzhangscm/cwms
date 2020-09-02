/**
 * Copyright 2018
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


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class OrderConfirmation  extends AuditibleEntity<String> implements Serializable {

    private String number;

    private Long warehouseId;

    private String warehouseName;


    private List<OrderLineConfirmation> orderLines = new ArrayList<>();


    public OrderConfirmation(){}

    public OrderConfirmation(Order order){
        this.number = order.getNumber();
        this.warehouseId = order.getWarehouseId();
        if (Objects.nonNull(order.getWarehouse())) {
            this.warehouseName = order.getWarehouse().getName();
        }
        order.getOrderLines().forEach(orderLine -> {
            OrderLineConfirmation orderLineConfirmation = new OrderLineConfirmation(orderLine);
            orderLineConfirmation.setOrder(this);
            addOrderLine(orderLineConfirmation);
        });
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

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public Long getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(Long warehouseId) {
        this.warehouseId = warehouseId;
    }

    public String getWarehouseName() {
        return warehouseName;
    }

    public void setWarehouseName(String warehouseName) {
        this.warehouseName = warehouseName;
    }

    public List<OrderLineConfirmation> getOrderLines() {
        return orderLines;
    }

    public void setOrderLines(List<OrderLineConfirmation> orderLines) {
        this.orderLines = orderLines;
    }
    public void addOrderLine(OrderLineConfirmation orderLine) {
        this.orderLines.add(orderLine);
    }


}

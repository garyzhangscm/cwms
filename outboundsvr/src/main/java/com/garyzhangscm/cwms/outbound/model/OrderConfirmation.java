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
import com.garyzhangscm.cwms.outbound.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.*;

public class OrderConfirmation  extends AuditibleEntity<String> implements Serializable {

    private static final Logger logger = LoggerFactory.getLogger(OrderConfirmation.class);
    private String number;

    private Long warehouseId;

    private String warehouseName;

    // quickbook customer list id
    private String quickbookCustomerListId;

    private String quickbookTxnID;

    private List<OrderLineConfirmation> orderLines = new ArrayList<>();


    public OrderConfirmation(){}

    public OrderConfirmation(Order order){
        this.number = order.getNumber();
        this.warehouseId = order.getWarehouseId();
        setQuickbookTxnID(order.getQuickbookTxnID());
        setQuickbookCustomerListId(order.getQuickbookCustomerListId());


        if (Objects.nonNull(order.getWarehouse())) {
            this.warehouseName = order.getWarehouse().getName();
        }
        order.getOrderLines().forEach(orderLine -> {
            OrderLineConfirmation orderLineConfirmation = new OrderLineConfirmation(orderLine);
            orderLineConfirmation.setOrder(this);
            addOrderLine(orderLineConfirmation);
        });
    }

    public OrderConfirmation(Shipment shipment, boolean includeZeroQuantity){
        this.number = shipment.getOrderNumber();
        this.warehouseId = shipment.getWarehouseId();
        if (Objects.nonNull(shipment.getOrder())) {
            logger.debug("setup the quickbook related information when complete the order");

            setQuickbookTxnID(shipment.getOrder().getQuickbookTxnID());
            setQuickbookCustomerListId(shipment.getOrder().getQuickbookCustomerListId());
        }
        else {

            logger.debug("FAIL to setup the quickbook related information when complete the order" +
                    ", as the order is not setup on the shipment");
        }


        if (Objects.nonNull(shipment.getWarehouse())) {
            this.warehouseName = shipment.getWarehouse().getName();
        }
        Set<Order> orders = new HashSet<>();
        Set<Long> shippedOrderLineId = new HashSet<>();

        shipment.getShipmentLines().forEach(shipmentLine -> {
            OrderLineConfirmation orderLineConfirmation = new OrderLineConfirmation(shipmentLine);
            orderLineConfirmation.setOrder(this);
            addOrderLine(orderLineConfirmation);
            shippedOrderLineId.add(shipmentLine.getOrderLine().getId());

            orders.add(shipmentLine.getOrderLine().getOrder());
        });

        // if we will need to include zero quantity, then get the orders that in this shipment
        // and include all the order lines in the orders but not in the shipment and mark it as
        // shipped 0 quantity
        if (includeZeroQuantity) {
            logger.debug("We will need to include zero quantity order line in the order confirmation");
            orders.forEach(order -> {
                logger.debug(">> start to process order {}", order.getNumber());
                order.getOrderLines().stream().filter(
                        orderLine -> {
                            logger.debug(">>>> need to include order line {} / {} ? {}",
                                    order.getNumber(),
                                    orderLine.getNumber(),
                                    !shippedOrderLineId.contains(orderLine.getId()));
                            return !shippedOrderLineId.contains(orderLine.getId());
                        }
                ).forEach(
                        orderLine -> {
                            logger.debug(">>>> start to add the zeor shipped quantity line {} / {}",
                                    order.getNumber(),
                                    orderLine.getNumber());

                            OrderLineConfirmation orderLineConfirmation = new OrderLineConfirmation(orderLine);
                            orderLineConfirmation.setOrder(this);
                            // set the shipped quantity to 0 as the order line is not in this shipment
                            orderLineConfirmation.setShippedQuantity(0L);
                            addOrderLine(orderLineConfirmation);
                        }
                );
            });
        }
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

    public String getQuickbookTxnID() {
        return quickbookTxnID;
    }

    public void setQuickbookTxnID(String quickbookTxnID) {
        this.quickbookTxnID = quickbookTxnID;
    }

    public String getQuickbookCustomerListId() {
        return quickbookCustomerListId;
    }

    public void setQuickbookCustomerListId(String quickbookCustomerListId) {
        this.quickbookCustomerListId = quickbookCustomerListId;
    }
}

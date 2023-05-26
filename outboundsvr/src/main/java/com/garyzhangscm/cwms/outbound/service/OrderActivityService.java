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

package com.garyzhangscm.cwms.outbound.service;

import com.garyzhangscm.cwms.outbound.clients.CommonServiceRestemplateClient;
import com.garyzhangscm.cwms.outbound.clients.KafkaSender;
import com.garyzhangscm.cwms.outbound.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.outbound.model.*;
import com.garyzhangscm.cwms.outbound.model.Order;
import com.garyzhangscm.cwms.outbound.repository.OrderActivityRepository;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import javax.persistence.criteria.*;
import javax.servlet.http.HttpSession;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


@Service
public class OrderActivityService {
    private static final Logger logger = LoggerFactory.getLogger(OrderActivityService.class);

    @Autowired
    private OrderActivityRepository orderActivityRepository;

    @Autowired
    private HttpSession httpSession;
    @Autowired
    private KafkaSender kafkaSender;
    @Autowired
    private CommonServiceRestemplateClient commonServiceRestemplateClient;

    @Value("${outbound.login.username}")
    private String outboundUsername;

    @Value("${outbound.login.password}")
    private String outboundPassword;

    public OrderActivity findById(Long id) {
        return orderActivityRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("carton not found by id: " + id));
    }

    private OrderActivity save(OrderActivity orderActivity) {
        return orderActivityRepository.save(orderActivity);
    }

    public List<OrderActivity> findAll(Long warehouseId,
                                String beginDateTime,
                                String endDateTime,
                                String date,
                                String orderNumber,
                                Long orderId,
                                String shipmentNumber,
                                Long shipmentId,
                                String shipmentLineNumber,
                                Long shipmentLineId,
                                String pickNumber,
                                Long pickId,
                                Long shortAllocationId,
                                String username,
                                       String rfCode,
                                       Long clientId,
                                       ClientRestriction clientRestriction) {

        return orderActivityRepository.findAll(
                (Root<OrderActivity> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();
                    predicates.add(criteriaBuilder.equal(root.get("warehouseId"), warehouseId));

                    if (!StringUtils.isBlank(beginDateTime)) {
                        LocalDateTime begin = LocalDateTime.parse(beginDateTime);
                        predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("activityDateTime"), begin));
                    }
                    if (!StringUtils.isBlank(endDateTime)) {
                        LocalDateTime end = LocalDateTime.parse(endDateTime);
                        predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("activityDateTime"), end));
                    }

                    if (!StringUtils.isBlank(date)) {
                        LocalDateTime begin = LocalDate.parse(date).atStartOfDay();
                        LocalDateTime end = begin.plusDays(1).minusNanos(1);
                        predicates.add(criteriaBuilder.between(root.get("activityDateTime"), begin, end));
                    }


                    if (Objects.nonNull(clientId)) {

                        predicates.add(criteriaBuilder.equal(
                                root.get("clientId"), clientId));
                    }
                    // query by order
                    if (Objects.nonNull(orderId)) {

                        Join<OrderActivity, Order> joinOrder = root.join("order", JoinType.INNER);
                        predicates.add(criteriaBuilder.equal(joinOrder.get("id"), orderId));
                    }
                    if (Strings.isNotBlank(orderNumber)) {

                        Join<OrderActivity, Order> joinOrder = root.join("order", JoinType.INNER);

                        if (orderNumber.contains("*")) {
                            predicates.add(criteriaBuilder.like(joinOrder.get("number"), orderNumber.replaceAll("\\*", "%")));
                        }
                        else {
                            predicates.add(criteriaBuilder.equal(joinOrder.get("number"), orderNumber));
                        }
                    }

                    // query by shipment
                    if (Objects.nonNull(shipmentId)) {

                        Join<OrderActivity, Shipment> joinShipment = root.join("shipment", JoinType.INNER);
                        predicates.add(criteriaBuilder.equal(joinShipment.get("id"), shipmentId));
                    }
                    if (Strings.isNotBlank(shipmentNumber)) {

                        Join<OrderActivity, Shipment> joinShipment = root.join("shipment", JoinType.INNER);


                        if (shipmentNumber.contains("*")) {
                            predicates.add(criteriaBuilder.like(joinShipment.get("number"), shipmentNumber.replaceAll("\\*", "%")));
                        }
                        else {
                            predicates.add(criteriaBuilder.equal(joinShipment.get("number"), shipmentNumber));
                        }

                    }
                    // query by shipment line
                    if (Objects.nonNull(shipmentLineId)) {

                        Join<OrderActivity, ShipmentLine> joinShipmentLine = root.join("shipmentLine", JoinType.INNER);
                        predicates.add(criteriaBuilder.equal(joinShipmentLine.get("id"), shipmentId));
                    }
                    if (Strings.isNotBlank(shipmentNumber) &&
                            Strings.isNotBlank(shipmentLineNumber)) {

                        Join<OrderActivity, Shipment> joinShipment = root.join("shipment", JoinType.INNER);
                        Join<Shipment, ShipmentLine> joinShipmentLine = joinShipment.join("shipmentLine", JoinType.INNER);

                        if (shipmentNumber.contains("*")) {
                            predicates.add(criteriaBuilder.like(joinShipment.get("number"), shipmentNumber.replaceAll("\\*", "%")));
                        }
                        else {
                            predicates.add(criteriaBuilder.equal(joinShipment.get("number"), shipmentNumber));
                        }

                        if (shipmentLineNumber.contains("*")) {
                            predicates.add(criteriaBuilder.like(joinShipmentLine.get("number"), shipmentLineNumber.replaceAll("\\*", "%")));
                        }
                        else {
                            predicates.add(criteriaBuilder.equal(joinShipmentLine.get("number"), shipmentLineNumber));
                        }

                    }
                    // query by pick
                    if (Objects.nonNull(pickId)) {

                        Join<OrderActivity, Pick> joinPick= root.join("pick", JoinType.INNER);
                        predicates.add(criteriaBuilder.equal(joinPick.get("id"), shipmentId));
                    }
                    if (Strings.isNotBlank(pickNumber)) {

                        Join<OrderActivity, Pick> joinPick = root.join("pick", JoinType.INNER);

                        if (pickNumber.contains("*")) {
                            predicates.add(criteriaBuilder.like(joinPick.get("number"), pickNumber.replaceAll("\\*", "%")));
                        }
                        else {
                            predicates.add(criteriaBuilder.equal(joinPick.get("number"), pickNumber));
                        }
                    }
                    // query by short allocation
                    if (Objects.nonNull(shortAllocationId)) {

                        Join<OrderActivity, ShortAllocation> joinShortAllocation = root.join("shortAllocation", JoinType.INNER);
                        predicates.add(criteriaBuilder.equal(joinShortAllocation.get("id"), shortAllocationId));
                    }

                    if (!StringUtils.isBlank(username)) {
                        if (username.contains("*")) {
                            predicates.add(criteriaBuilder.like(root.get("username"), username.replaceAll("\\*", "%")));
                        }
                        else {
                            predicates.add(criteriaBuilder.equal(root.get("username"), username));
                        }

                    }

                    if (!StringUtils.isBlank(rfCode)) {

                        if (rfCode.contains("*")) {
                            predicates.add(criteriaBuilder.like(root.get("rfCode"), rfCode.replaceAll("\\*", "%")));
                        }
                        else {
                            predicates.add(criteriaBuilder.equal(root.get("rfCode"), rfCode));
                        }


                    }

                    Predicate[] p = new Predicate[predicates.size()];

                    // special handling for 3pl
                    Predicate predicate = criteriaBuilder.and(predicates.toArray(p));

                    return Objects.isNull(clientRestriction) ?
                            predicate :
                            clientRestriction.addClientRestriction(predicate,
                                    root, criteriaBuilder);
                }
                ,
                Sort.by(Sort.Direction.DESC, "activityDateTime")
        );
    }


    public OrderActivity addOrderActivity(OrderActivity orderActivity) {
        return save(orderActivity);
    }

    public String getNextNumber(Long warehouseId) {
        return commonServiceRestemplateClient.getNextNumber(warehouseId, "order-activity-number");
    }

    public String getNextTransactionGroupId(Long warehouseId) {
        return commonServiceRestemplateClient.getNextNumber(warehouseId, "order-activity-transaction-group-id");
    }

    private String getTransactionGroupId(Long warehouseId) {
        String transactionGroupId;
        try {
            if (Objects.isNull(httpSession.getAttribute("order-activity-transaction-group-id"))) {
                logger.debug("Current session doesn't have any transaction id yet, let's get a new one");
                transactionGroupId = getNextTransactionGroupId(warehouseId);
                httpSession.setAttribute("order-activity-transaction-group-id", transactionGroupId);
                logger.debug(">> {}", transactionGroupId);
            }
            else {
                transactionGroupId = httpSession.getAttribute("order-activity-transaction-group-id").toString();
                logger.debug("Get transaction ID {} from current session", transactionGroupId);
            }
        }
        catch (IllegalStateException ex) {
            ex.printStackTrace();

            logger.debug("We are not in a http session");
            transactionGroupId = getNextTransactionGroupId(warehouseId);
            logger.debug(">> {}", transactionGroupId);
        }
        return transactionGroupId;
    }

    public OrderActivity createOrderActivity(Long warehouseId, BulkPick bulkPick, OrderActivityType orderActivityType) {
        String transactionId = getTransactionGroupId(warehouseId);
        return createOrderActivity(warehouseId, transactionId, bulkPick, orderActivityType);
    }

    private OrderActivity createOrderActivity(Long warehouseId, String transactionId,
                                              BulkPick bulkPick, OrderActivityType orderActivityType) {
        OrderActivity orderActivity =  OrderActivity.build(warehouseId, transactionId,
                getNextNumber(warehouseId), getCurrentUsername())
                .withBulkPick(bulkPick)
                .withOrderActivityType(orderActivityType);
        return orderActivity;
    }

    public OrderActivity createOrderActivity(Long warehouseId, Order order, OrderActivityType orderActivityType) {
        String transactionId = getTransactionGroupId(warehouseId);
        return createOrderActivity(warehouseId, transactionId, order, orderActivityType);
    }
    private OrderActivity createOrderActivity(Long warehouseId, String transactionId,
                                             Order order, OrderActivityType orderActivityType) {
        OrderActivity orderActivity =  OrderActivity.build(warehouseId, transactionId,
                getNextNumber(warehouseId), getCurrentUsername())
                .withOrder(order)
                .withOrderActivityType(orderActivityType);
        return orderActivity;
    }
    public OrderActivity createOrderActivity(Long warehouseId, Order order,
                                             Shipment shipment, OrderActivityType orderActivityType) {

        String transactionId = getTransactionGroupId(warehouseId);
        return createOrderActivity(warehouseId, transactionId, order, shipment, orderActivityType);
    }
    private OrderActivity createOrderActivity(Long warehouseId, String transactionId,
                                             Order order, Shipment shipment, OrderActivityType orderActivityType) {
        OrderActivity orderActivity =  OrderActivity.build(warehouseId, transactionId,
                getNextNumber(warehouseId), getCurrentUsername())
                .withOrder(order)
                .withShipment(shipment).withOrderActivityType(orderActivityType);
        return orderActivity;
    }

    public OrderActivity createOrderActivity(Long warehouseId, Order order,
                                             Shipment shipment, ShipmentLine shipmentLine, OrderActivityType orderActivityType) {

        String transactionId = getTransactionGroupId(warehouseId);
        return createOrderActivity(warehouseId, transactionId, order, shipment, shipmentLine, orderActivityType);
    }
    private OrderActivity createOrderActivity(Long warehouseId, String transactionId,
                                             Order order, Shipment shipment,
                                             ShipmentLine shipmentLine, OrderActivityType orderActivityType) {
        OrderActivity orderActivity =  OrderActivity.build(warehouseId, transactionId,
                getNextNumber(warehouseId), getCurrentUsername())
                .withOrder(order)
                .withShipment(shipment)
                .withShipmentLine(shipmentLine).withOrderActivityType(orderActivityType);

        if (Objects.nonNull(shipmentLine.getOrderLine())) {
            orderActivity = orderActivity.withOrderLine(shipmentLine.getOrderLine());

        }
        return orderActivity;
    }


    public OrderActivity createOrderActivity(Long warehouseId, Order order,
                                             ShipmentLine shipmentLine, OrderActivityType orderActivityType) {

        String transactionId = getTransactionGroupId(warehouseId);
        return createOrderActivity(warehouseId, transactionId, order, shipmentLine, orderActivityType);
    }
    private OrderActivity createOrderActivity(Long warehouseId, String transactionId,
                                             Order order, ShipmentLine shipmentLine, OrderActivityType orderActivityType) {
        OrderActivity orderActivity =  OrderActivity.build(warehouseId, transactionId,
                getNextNumber(warehouseId), getCurrentUsername())
                .withOrder(order)
                .withShipmentLine(shipmentLine).withOrderActivityType(orderActivityType);

        if (Objects.nonNull(shipmentLine.getOrderLine())) {
            orderActivity = orderActivity.withOrderLine(shipmentLine.getOrderLine());

        }

        return orderActivity;
    }

    public OrderActivity createOrderActivity(Long warehouseId,
                                             ShipmentLine shipmentLine, OrderActivityType orderActivityType) {

        String transactionId = getTransactionGroupId(warehouseId);
        return createOrderActivity(warehouseId, transactionId, shipmentLine, orderActivityType);
    }
    private OrderActivity createOrderActivity(Long warehouseId, String transactionId,
                                             ShipmentLine shipmentLine, OrderActivityType orderActivityType) {
        OrderActivity orderActivity =
                OrderActivity.build(warehouseId, transactionId,
                        getNextNumber(warehouseId), getCurrentUsername())
                .withShipmentLine(shipmentLine).withOrderActivityType(orderActivityType);
        if (Objects.nonNull(shipmentLine.getOrderLine())) {
            orderActivity = orderActivity.withOrderLine(shipmentLine.getOrderLine());
            if (Objects.nonNull(shipmentLine.getOrderLine().getOrder())) {
                orderActivity = orderActivity.withOrder(
                        shipmentLine.getOrderLine().getOrder()
                );
            }
        }
        if (Objects.nonNull(shipmentLine.getShipment())) {
            orderActivity = orderActivity.withShipment(shipmentLine.getShipment());
        }

        return orderActivity;
    }


    public OrderActivity createOrderActivity(Long warehouseId,
                                             ShipmentLine shipmentLine,
                                             Pick pick,
                                             OrderActivityType orderActivityType) {

        String transactionId = getTransactionGroupId(warehouseId);
        return createOrderActivity(warehouseId, transactionId, shipmentLine, pick,  orderActivityType);
    }
    private OrderActivity createOrderActivity(Long warehouseId, String transactionId,
                                             ShipmentLine shipmentLine,
                                              Pick pick,
                                              OrderActivityType orderActivityType) {
        OrderActivity orderActivity =
                OrderActivity.build(warehouseId, transactionId,
                        getNextNumber(warehouseId), getCurrentUsername())
                        .withPick(pick).withOrderActivityType(orderActivityType);
        if (Objects.nonNull(shipmentLine)) {
            orderActivity = orderActivity.withShipmentLine(shipmentLine);
            if (Objects.nonNull(shipmentLine.getOrderLine())) {
                orderActivity = orderActivity.withOrderLine(shipmentLine.getOrderLine());
                if (Objects.nonNull(shipmentLine.getOrderLine().getOrder())) {
                    orderActivity = orderActivity.withOrder(
                            shipmentLine.getOrderLine().getOrder()
                    );
                }
            }

            if (Objects.nonNull(shipmentLine.getShipment())) {
                orderActivity = orderActivity.withShipment(shipmentLine.getShipment());

            }
        }

        return orderActivity;
    }
    public OrderActivity createOrderActivity(Long warehouseId,
                                             ShipmentLine shipmentLine,
                                             ShortAllocation shortAllocation,
                                             OrderActivityType orderActivityType) {

        String transactionId = getTransactionGroupId(warehouseId);
        return createOrderActivity(warehouseId, transactionId, shipmentLine, shortAllocation,  orderActivityType);
    }

    private OrderActivity createOrderActivity(Long warehouseId, String transactionId,
                                              ShipmentLine shipmentLine,
                                              ShortAllocation shortAllocation,
                                              OrderActivityType orderActivityType) {
        OrderActivity orderActivity =
                OrderActivity.build(warehouseId, transactionId,
                        getNextNumber(warehouseId), getCurrentUsername())
                        .withShortAllocation(shortAllocation).withOrderActivityType(orderActivityType);

        if (Objects.nonNull(shipmentLine)) {
            orderActivity = orderActivity.withShipmentLine(shipmentLine);
            if (Objects.nonNull(shipmentLine.getOrderLine())) {
                orderActivity = orderActivity.withOrderLine(shipmentLine.getOrderLine());
                if (Objects.nonNull(shipmentLine.getOrderLine().getOrder())) {
                    orderActivity = orderActivity.withOrder(
                            shipmentLine.getOrderLine().getOrder()
                    );
                }
            }
            if (Objects.nonNull(shipmentLine.getShipment())) {
                orderActivity = orderActivity.withShipment(shipmentLine.getShipment());

            }
        }

        return orderActivity;
    }

    public void sendOrderActivity(OrderActivity orderActivity) {
        kafkaSender.send(orderActivity);
    }

    // if this is the activity from web user, then return the username
    // if this is from integration or other activity that is initiated by system
    // then get the default user
    private String getCurrentUsername() {
        if (Objects.isNull(SecurityContextHolder.getContext())  ||
                Objects.isNull(SecurityContextHolder.getContext().getAuthentication())  ||
                Objects.isNull(SecurityContextHolder.getContext().getAuthentication().getName())) {
            return outboundUsername;
        }
        else {
            return SecurityContextHolder.getContext().getAuthentication().getName();
        }


    }
}

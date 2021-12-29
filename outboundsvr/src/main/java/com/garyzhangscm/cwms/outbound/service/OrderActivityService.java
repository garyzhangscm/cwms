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

import com.garyzhangscm.cwms.outbound.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.outbound.model.*;
import com.garyzhangscm.cwms.outbound.model.Order;
import com.garyzhangscm.cwms.outbound.repository.OrderActivityRepository;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import javax.persistence.criteria.*;
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


    public OrderActivity findById(Long id) {
        return orderActivityRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("carton not found by id: " + id));
    }

    public OrderActivity save(OrderActivity orderActivity) {
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
                                       String rfCode) {

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

                    // query by order
                    if (Objects.nonNull(orderId)) {

                        Join<OrderActivity, Order> joinOrder = root.join("order", JoinType.INNER);
                        predicates.add(criteriaBuilder.equal(joinOrder.get("id"), orderId));
                    }
                    if (Strings.isNotBlank(orderNumber)) {

                        Join<OrderActivity, Order> joinOrder = root.join("order", JoinType.INNER);
                        predicates.add(criteriaBuilder.equal(joinOrder.get("number"), orderNumber));
                    }

                    // query by shipment
                    if (Objects.nonNull(shipmentId)) {

                        Join<OrderActivity, Shipment> joinShipment = root.join("shipment", JoinType.INNER);
                        predicates.add(criteriaBuilder.equal(joinShipment.get("id"), shipmentId));
                    }
                    if (Strings.isNotBlank(shipmentNumber)) {

                        Join<OrderActivity, Shipment> joinShipment = root.join("shipment", JoinType.INNER);
                        predicates.add(criteriaBuilder.equal(joinShipment.get("number"), shipmentNumber));
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
                        predicates.add(criteriaBuilder.equal(joinShipment.get("number"), shipmentNumber));
                        predicates.add(criteriaBuilder.equal(joinShipmentLine.get("number"), shipmentLineNumber));
                    }
                    // query by pick
                    if (Objects.nonNull(pickId)) {

                        Join<OrderActivity, Pick> joinPick= root.join("pick", JoinType.INNER);
                        predicates.add(criteriaBuilder.equal(joinPick.get("id"), shipmentId));
                    }
                    if (Strings.isNotBlank(pickNumber)) {

                        Join<OrderActivity, Pick> joinPick = root.join("pick", JoinType.INNER);
                        predicates.add(criteriaBuilder.equal(joinPick.get("number"), pickNumber));
                    }
                    // query by short allocation
                    if (Objects.nonNull(shortAllocationId)) {

                        Join<OrderActivity, ShortAllocation> joinShortAllocation = root.join("shortAllocation", JoinType.INNER);
                        predicates.add(criteriaBuilder.equal(joinShortAllocation.get("id"), shortAllocationId));
                    }

                    if (!StringUtils.isBlank(username)) {
                        predicates.add(criteriaBuilder.equal(root.get("username"), username));

                    }

                    if (!StringUtils.isBlank(rfCode)) {
                        predicates.add(criteriaBuilder.equal(root.get("rfCode"), rfCode));

                    }
                    Predicate[] p = new Predicate[predicates.size()];
                    return criteriaBuilder.and(predicates.toArray(p));
                }
                ,
                Sort.by(Sort.Direction.DESC, "activityDateTime")
        );
    }


    public OrderActivity addOrderActivity(OrderActivity orderActivity) {
        return save(orderActivity);
    }
}

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

import com.garyzhangscm.cwms.outbound.exception.OrderOperationException;
import com.garyzhangscm.cwms.outbound.model.*;
import com.garyzhangscm.cwms.outbound.repository.OrderBillableActivityRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class OrderBillableActivityService {
    private static final Logger logger = LoggerFactory.getLogger(OrderBillableActivityService.class);

    @Autowired
    private OrderBillableActivityRepository orderBillableActivityRepository;

    @Autowired
    private ClientRestrictionUtil clientRestrictionUtil;
    @Autowired
    private OrderLineBillableActivityService orderLineBillableActivityService;

    @Autowired
    private OrderService orderService;


    public List<OrderBillableActivity> findAll(Long warehouseId,
                                                 Long clientId,
                                                 ZonedDateTime startTime,
                                                 ZonedDateTime endTime,
                                                 ClientRestriction clientRestriction) {



        return orderBillableActivityRepository.findAll(
                (Root<OrderBillableActivity> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();

                    predicates.add(criteriaBuilder.equal(root.get("warehouseId"), warehouseId));

                    if (Objects.nonNull(startTime)) {

                        predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                                root.get("activityTime"), startTime));
                    }
                    if (Objects.nonNull(endTime)) {

                        predicates.add(criteriaBuilder.lessThanOrEqualTo(
                                root.get("activityTime"), endTime));
                    }
                    if (Objects.nonNull(clientId)) {

                        predicates.add(criteriaBuilder.equal(root.get("clientId"), clientId));
                    }

                    return clientRestrictionUtil.addClientRestriction(root,
                            predicates,
                            clientRestriction,
                            criteriaBuilder);
/**
                    Predicate[] p = new Predicate[predicates.size()];

                    // special handling for 3pl
                    Predicate predicate = criteriaBuilder.and(predicates.toArray(p));

                    if (Objects.isNull(clientRestriction) ||
                            !Boolean.TRUE.equals(clientRestriction.getThreePartyLogisticsFlag()) ||
                            Boolean.TRUE.equals(clientRestriction.getAllClientAccess())) {
                        // not a 3pl warehouse, let's not put any restriction on the client
                        // (unless the client restriction is from the web request, which we already
                        // handled previously
                        return predicate;
                    }


                    // build the accessible client list predicated based on the
                    // client ID that the user has access
                    Predicate accessibleClientListPredicate;
                    if (clientRestriction.getClientAccesses().trim().isEmpty()) {
                        // the user can't access any client, then the user
                        // can only access the non 3pl data
                        accessibleClientListPredicate = criteriaBuilder.isNull(root.get("clientId"));
                    }
                    else {
                        CriteriaBuilder.In<Long> inClientIds = criteriaBuilder.in(root.get("clientId"));
                        for(String id : clientRestriction.getClientAccesses().trim().split(",")) {
                            inClientIds.value(Long.parseLong(id));
                        }
                        accessibleClientListPredicate = criteriaBuilder.and(inClientIds);
                    }

                    if (Boolean.TRUE.equals(clientRestriction.getNonClientDataAccessible())) {
                        // the user can access the non 3pl data
                        return criteriaBuilder.and(predicate,
                                criteriaBuilder.or(
                                        criteriaBuilder.isNull(root.get("clientId")),
                                        accessibleClientListPredicate));
                    }
                    else {

                        // the user can NOT access the non 3pl data
                        return criteriaBuilder.and(predicate,
                                criteriaBuilder.and(
                                        criteriaBuilder.isNotNull(root.get("clientId")),
                                        accessibleClientListPredicate));
                    }
**/
                }
        );
    }

    public OrderBillableActivity save(OrderBillableActivity orderBillableActivity) {
        return orderBillableActivityRepository.save(orderBillableActivity);
    }


    public OrderBillableActivity addOrderBillableActivity(Long orderId, OrderBillableActivity orderBillableActivity) {
        // we don't have any existing billable activity with same type in this receipt, let's create one
        Order order = orderService.findById(orderId);
        orderBillableActivity.setOrder(order);

        return save(orderBillableActivity);

    }

    public OrderBillableActivity changeOrderBillableActivity(OrderBillableActivity orderBillableActivity) {
        if (Objects.isNull(orderBillableActivity.getId())) {
            throw OrderOperationException.raiseException("Can't change an non exists billable activity");
        }
        return save(orderBillableActivity);

    }
    public void removeOrderBillableActivity(OrderBillableActivity orderBillableActivity) {
        if (Objects.isNull(orderBillableActivity.getId())) {
            throw OrderOperationException.raiseException("Can't remove an non exists billable activity");
        }
        removeOrderBillableActivity(orderBillableActivity.getId());

    }
    public void removeOrderBillableActivity(Long id) {
        orderBillableActivityRepository.deleteById(id);

    }

    public List<BillableActivity> findBillableActivities(Long warehouseId, Long clientId,
                                                         ZonedDateTime startTime, ZonedDateTime endTime,
                                                         Boolean includeLineActivity, ClientRestriction clientRestriction) {

        List<OrderBillableActivity> orderBillableActivities = findAll(
                warehouseId, clientId, startTime, endTime, clientRestriction
        );

        // convert from the order billable activity to billable activity
        List<BillableActivity> billableActivities = orderBillableActivities.stream()
                .map(orderBillableActivity -> convert(orderBillableActivity)).collect(Collectors.toList());

        if (Boolean.TRUE.equals(includeLineActivity)) {
            List<BillableActivity> lineBillableActivity =
                    orderLineBillableActivityService.findBillableActivities(
                            warehouseId, clientId, startTime, endTime, clientRestriction);
            billableActivities.addAll(lineBillableActivity);


        }
        return billableActivities;
    }

    private BillableActivity convert(OrderBillableActivity orderBillableActivity) {
        BillableActivity billableActivity = new BillableActivity();
        billableActivity.setWarehouseId(orderBillableActivity.getOrder().getWarehouseId());
        billableActivity.setClientId(orderBillableActivity.getOrder().getClientId());
        billableActivity.setBillableCategory(BillableCategory.RECEIPT_PROCESS_FEE);
        billableActivity.setRate(orderBillableActivity.getRate());
        billableActivity.setAmount(orderBillableActivity.getAmount());
        billableActivity.setTotalCharge(orderBillableActivity.getTotalCharge());
        billableActivity.setDocumentNumber(orderBillableActivity.getOrder().getNumber());
        billableActivity.setActivityTime(orderBillableActivity.getActivityTime());
        billableActivity.setBillableActivityTypeId(orderBillableActivity.getBillableActivityTypeId());
        return billableActivity;
    }


}

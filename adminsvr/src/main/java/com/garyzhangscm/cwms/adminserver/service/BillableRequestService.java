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

package com.garyzhangscm.cwms.adminserver.service;

import com.garyzhangscm.cwms.adminserver.clients.ResourceServiceRestemplateClient;
import com.garyzhangscm.cwms.adminserver.clients.WarehouseLayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.adminserver.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.adminserver.model.*;
import com.garyzhangscm.cwms.adminserver.repository.BillableRequestRepository;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.persistence.criteria.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.*;

@Service
public class BillableRequestService {

    private static final Logger logger = LoggerFactory.getLogger(BillableRequestService.class);

    @Autowired
    private BillableRequestRepository billableRequestRepository;

    @Autowired
    private ResourceServiceRestemplateClient resourceServiceRestemplateClient;
    @Autowired
    private WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient;

    public BillableRequest findById(Long id ) {
        return findById(id, true);
    }

    public BillableRequest findById(Long id, boolean loadDetails) {
        BillableRequest billableRequest = billableRequestRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("billable request not found by id: " + id));
        if (loadDetails) {
            loadAttribute(billableRequest);
        }
        return billableRequest;
    }



    public List<BillableRequest> findAll(Long companyId,
                                         Long warehouseId,
                                         ZonedDateTime startTime,
                                         ZonedDateTime endTime,
                                         LocalDate date
    ) {

        return findAll(
                companyId,
                warehouseId,
                startTime,
                endTime,
                date,
                true
        );
    }

    public List<BillableRequest> findAll(Long companyId,
                                         Long warehouseId,
                                         ZonedDateTime startTime,
                                         ZonedDateTime endTime,
                                         LocalDate date,
                                         boolean loadDetails
                                         ) {

        List<BillableRequest> billableRequests =
                billableRequestRepository.findAll(
                (Root<BillableRequest> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();

                    if (Objects.nonNull(companyId)) {

                        predicates.add(criteriaBuilder.equal(root.get("companyId"), companyId));
                    }
                    if (Objects.nonNull(warehouseId)) {

                        predicates.add(criteriaBuilder.equal(root.get("warehouseId"), warehouseId));
                    }

                    if (Objects.nonNull(startTime)) {
                        predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                                root.get("createdTime"), startTime));

                    }

                    if (Objects.nonNull(endTime)) {
                        predicates.add(criteriaBuilder.lessThanOrEqualTo(
                                root.get("createdTime"), endTime));

                    }

                    if (Objects.nonNull(date)) {
                        LocalDateTime dateStartTime = date.atStartOfDay();
                        LocalDateTime dateEndTime = date.plusDays(1).atStartOfDay().minusSeconds(1);
                        predicates.add(criteriaBuilder.between(
                                root.get("createdTime"), dateStartTime.atZone(ZoneOffset.UTC), dateEndTime.atZone(ZoneOffset.UTC)));

                    }

                    Predicate[] p = new Predicate[predicates.size()];
                    return criteriaBuilder.and(predicates.toArray(p));
                }
        );

        if (billableRequests.size() > 0 && loadDetails) {
            loadAttribute(billableRequests);
        }
        return billableRequests;

    }

    private void loadAttribute(List<BillableRequest> billableRequests) {
        billableRequests.forEach(
                billableRequest -> loadAttribute(billableRequest)
        );
    }

    private void loadAttribute(BillableRequest billableRequest) {
        if (Objects.nonNull(billableRequest.getCompanyId())) {

            billableRequest.setCompany(
                    warehouseLayoutServiceRestemplateClient.getCompanyById(
                            billableRequest.getCompanyId()
                    )
            );
        }

        if (Objects.nonNull(billableRequest.getWarehouseId())) {

            billableRequest.setWarehouse(
                    resourceServiceRestemplateClient.getWarehouseById(
                            billableRequest.getWarehouseId()
                    )
            );
        }
    }
    public BillableRequest save(BillableRequest billableRequest) {
        return billableRequestRepository.save(billableRequest);
    }

    public Collection<BillableRequestSummaryByCompany> getBillableRequestSummaryByCompany(Long companyId,
                                                                                          ZonedDateTime startTime,
                                                                                          ZonedDateTime endTime,
                                                                                          LocalDate date) {
        List<BillableRequest> billableRequests = findAll(companyId, null, startTime, endTime, date);

        Set<String> transactionIdSet = new HashSet<>();

        // key: service number
        // value: totalWebAPIEndpointCall
        Map<String, Long> totalWebAPIEndpointCallCountMap = new HashMap<>();

        // key: service number
        // value: totalTransaction
        Map<String, Long> totalTransactionCountMap = new HashMap<>();

        // key: service number
        // value: overallCost
        Map<String, Double> overallCostMap = new HashMap<>();

        // key: service number
        // value: overallCost
        Map<String, BillableRequestSummaryByCompany> billableRequestSummaryByCompanyMap = new HashMap<>();

        billableRequests.stream().forEach(
                billableRequest -> {
                    String serviceName = billableRequest.getServiceName();
                    Long totalWebAPIEndpointCallCount
                            = totalWebAPIEndpointCallCountMap.getOrDefault(serviceName, 0l) +
                                1;
                    totalWebAPIEndpointCallCountMap.put(serviceName, totalWebAPIEndpointCallCount);

                    // only contains this transaction when it is not count yet
                    // we may have multiple web call in multiple services for the same transaction
                    // which we may not want to over charge our customer
                    if (!transactionIdSet.contains(billableRequest.getTransactionId())) {


                        Long totalTransactionCount
                                = totalTransactionCountMap.getOrDefault(serviceName, 0l) + 1;
                        totalTransactionCountMap.put(serviceName, totalTransactionCount);

                        Double overallCost = overallCostMap.getOrDefault(serviceName, 0.0) +
                                billableRequest.getRate();
                        overallCostMap.put(serviceName, overallCost);

                        transactionIdSet.add(billableRequest.getTransactionId());
                    }
                }
        );

        for(Map.Entry<String, Long> totalWebAPIEndpointCallCountEntry: totalWebAPIEndpointCallCountMap.entrySet()) {
            String serviceName = totalWebAPIEndpointCallCountEntry.getKey();
            Long totalWebAPIEndpointCallCount = totalWebAPIEndpointCallCountEntry.getValue();

            Long totalTransactionCount
                    = totalTransactionCountMap.getOrDefault(serviceName, 0l);
            Double overallCost = overallCostMap.getOrDefault(serviceName, 0.0);

            BillableRequestSummaryByCompany billableRequestSummaryByCompany = new BillableRequestSummaryByCompany(
                    companyId,
                    serviceName, totalWebAPIEndpointCallCount, totalTransactionCount,
                    overallCost
            );
            billableRequestSummaryByCompanyMap.put(serviceName, billableRequestSummaryByCompany);
        }

        return billableRequestSummaryByCompanyMap.values();

    }

    public void createBillableRequest(BillableRequest billableRequest) {

        logger.debug("Start to create billable request for ");
        logger.debug("=========   Billable   Request  =====");
        logger.debug(billableRequest.toString());

        if (Strings.isNotBlank(billableRequest.getUsername())){
            User user = resourceServiceRestemplateClient.getUserByUsernameAndToken(
                    billableRequest.getUsername(),
                    billableRequest.getToken()
            );
            if (Objects.nonNull(user)) {
                billableRequest.setCompanyId(user.getLastLoginCompanyId());
                billableRequest.setWarehouseId(user.getLastLoginWarehouseId());
            }
        }
        save(billableRequest);
    }
}

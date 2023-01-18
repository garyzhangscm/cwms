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

import com.garyzhangscm.cwms.adminserver.clients.CommonServiceRestemplateClient;
import com.garyzhangscm.cwms.adminserver.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.adminserver.model.BillingRequest;
import com.garyzhangscm.cwms.adminserver.repository.BillingRequestRepository;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class BillingRequestService {
    private static final Logger logger = LoggerFactory.getLogger(BillingRequestService.class);
    @Autowired
    private BillingRequestRepository billingRequestRepository;

    @Autowired
    private CommonServiceRestemplateClient commonServiceRestemplateClient;

    @Autowired
    private List<BillingService> billingServices;

    public BillingRequest findById(Long id) {
        return findById(id, true);
    }
    public BillingRequest findById(Long id, boolean includeDetails) {
        BillingRequest billingRequest = billingRequestRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("billing request not found by id: " + id));
        if (Objects.nonNull(billingRequest) && includeDetails) {
            loadDetails(billingRequest);
        }
        return billingRequest;
    }

    public BillingRequest save(BillingRequest billingRequest) {
        return save(billingRequest, true);
    }
    public BillingRequest save(BillingRequest billingRequest, boolean loadDetails) {
        BillingRequest newBillingRequest=  billingRequestRepository.save(billingRequest);
        if (loadDetails) {

            loadDetails(newBillingRequest);
        }
        return newBillingRequest;
    }

    /**
     * return the exactly matched record if the exactmath
     * @param companyId
     * @param warehouseId
     * @param number
     * @return
     */
    public List<BillingRequest> findAll(Long companyId,
                                     Long warehouseId,
                                     Long clientId,
                                     String number ) {
        return findAll(companyId, warehouseId, clientId, number, true);
    }


    public List<BillingRequest> findAll(Long companyId,
                                     Long warehouseId,
                                     Long clientId,
                                        String number ,
                                     boolean includeDetails) {

        List<BillingRequest> billingRequests =  billingRequestRepository.findAll(
                (Root<BillingRequest> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {

                    List<Predicate> predicates = new ArrayList<Predicate>();

                    predicates.add(criteriaBuilder.equal(root.get("companyId"), companyId));
                    predicates.add(criteriaBuilder.equal(root.get("warehouseId"), warehouseId));
                    if (Objects.isNull(clientId)) {
                        // if the client id is not passed in, then we will only return the
                        // rate that defined for the warehouse, not for any client
                        predicates.add(criteriaBuilder.isNull(root.get("clientId")));
                    }
                    else {
                        predicates.add(criteriaBuilder.equal(root.get("clientId"), clientId));
                    }

                    if (StringUtils.isNotBlank(number)) {
                        if (number.contains("%")) {

                            predicates.add(criteriaBuilder.like(root.get("number"), number));
                        }
                        else {

                            predicates.add(criteriaBuilder.equal(root.get("number"), number));
                        }
                    }

                    Predicate[] p = new Predicate[predicates.size()];
                    return criteriaBuilder.and(predicates.toArray(p));
                }
                ,
                Sort.by(Sort.Direction.ASC, "warehouseId", "clientId")
        );

        if (!billingRequests.isEmpty() && includeDetails) {

            loadDetails(billingRequests);
        }

        return billingRequests;
    }

    private void loadDetails(List<BillingRequest> billingRequests) {
        billingRequests.forEach(
                this::loadDetails
        );
    }
    private void loadDetails(BillingRequest billingRequest) {
        if (Objects.nonNull(billingRequest.getClientId()) &&
                Objects.isNull(billingRequest.getClient())) {
            billingRequest.setClient(
                    commonServiceRestemplateClient.getClientById(
                            billingRequest.getClientId()
                    )
            );
        }
    }
    public String getNextNumber(Long warehouseId) {
        return commonServiceRestemplateClient.getNextNumber(warehouseId, "billing-request-number");
    }

    public List<BillingRequest> generateBillingRequest(
            ZonedDateTime startTime, ZonedDateTime endTime,
            Long companyId, Long warehouseId, Long clientId,
            String number, Boolean serialize) {
        return billingServices.stream().map(
                billingService -> billingService.generateBillingRequest(
                        startTime, endTime, companyId, warehouseId,
                        clientId, number, serialize
                )
        ).filter(billingRequest -> Objects.nonNull(billingRequest)).collect(Collectors.toList());
    }

    public BillingRequest addBillingRequest(BillingRequest billingRequest) {
        billingRequest.getBillingRequestLines().forEach(
                billingRequestLine -> billingRequestLine.setBillingRequest(
                        billingRequest
                )
        );
        return save(billingRequest);
    }
}

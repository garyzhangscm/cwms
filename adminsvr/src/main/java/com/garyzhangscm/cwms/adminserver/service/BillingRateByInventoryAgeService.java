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

import com.garyzhangscm.cwms.adminserver.ResponseBodyWrapper;
import com.garyzhangscm.cwms.adminserver.clients.CommonServiceRestemplateClient;
import com.garyzhangscm.cwms.adminserver.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.adminserver.model.*;
import com.garyzhangscm.cwms.adminserver.repository.BillingRateByInventoryAgeRepository;
import com.garyzhangscm.cwms.adminserver.repository.BillingRateRepository;
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
import java.util.*;
import java.util.stream.Collectors;

@Service
public class BillingRateByInventoryAgeService {
    private static final Logger logger = LoggerFactory.getLogger(BillingRateByInventoryAgeService.class);
    @Autowired
    private BillingRateByInventoryAgeRepository billingRateByInventoryAgeRepository;

    @Autowired
    private CommonServiceRestemplateClient commonServiceRestemplateClient;


    public BillingRateByInventoryAge findById(Long id) {
        return findById(id, true);
    }
    public BillingRateByInventoryAge findById(Long id, boolean includeDetails) {
        BillingRateByInventoryAge billingRateByInventoryAge = billingRateByInventoryAgeRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("billing rate by inventory age not found by id: " + id));
        if (Objects.nonNull(billingRateByInventoryAge) && includeDetails) {
            loadDetails(billingRateByInventoryAge);
        }
        return billingRateByInventoryAge;
    }

    public BillingRateByInventoryAge save(BillingRateByInventoryAge billingRateByInventoryAge) {
        return save(billingRateByInventoryAge, true);
    }
    public BillingRateByInventoryAge save(BillingRateByInventoryAge billingRateByInventoryAge, boolean loadDetails) {
        BillingRateByInventoryAge newBillingRateByInventoryAge =  billingRateByInventoryAgeRepository.save(billingRateByInventoryAge);
        if (loadDetails) {

            loadDetails(newBillingRateByInventoryAge);
        }
        return newBillingRateByInventoryAge;
    }

    public BillingRateByInventoryAge saveOrUpdate(BillingRateByInventoryAge billingRate) {
        return saveOrUpdate(billingRate, true);

    }
    public BillingRateByInventoryAge saveOrUpdate(BillingRateByInventoryAge billingRateByInventoryAge, boolean loadDetails) {
        if (billingRateByInventoryAge.getId() == null &&
                findByInventoryAgeRange(billingRateByInventoryAge.getCompanyId(),
                        billingRateByInventoryAge.getWarehouseId(),
                        billingRateByInventoryAge.getClientId(),
                        billingRateByInventoryAge.getStartInventoryAge(), billingRateByInventoryAge.getEndInventoryAge(),
                        true) != null) {
            billingRateByInventoryAge.setId(
                    findByInventoryAgeRange(billingRateByInventoryAge.getCompanyId(),
                            billingRateByInventoryAge.getWarehouseId(),
                            billingRateByInventoryAge.getClientId(),
                            billingRateByInventoryAge.getStartInventoryAge(), billingRateByInventoryAge.getEndInventoryAge(),
                            true).getId());
        }
        return save(billingRateByInventoryAge, loadDetails);
    }

    public BillingRateByInventoryAge findByInventoryAgeRange(Long companyId, Long warehouseId, Long clientId,
                                                             int startInventoryAge, int endInventoryAge,
                                                             boolean exactMatch) {
        List<BillingRateByInventoryAge> inventorySnapshots = findAll(companyId,
                warehouseId, clientId,
                startInventoryAge, endInventoryAge, exactMatch);
        if (inventorySnapshots.size() > 0) {
            return inventorySnapshots.get(0);
        }
        else {
            return null;
        }
    }

    /**
     * return the exactly matched record if the exactmath
     * @param companyId
     * @param warehouseId
     * @return
     */
    public List<BillingRateByInventoryAge> findAll(Long companyId,
                                                     Long warehouseId,
                                                     Long clientId,
                                                   Integer startInventoryAge,
                                                   Integer endInventoryAge, boolean exactMatch) {
        return findAll(companyId, warehouseId, clientId, startInventoryAge, endInventoryAge, exactMatch, true);
    }


    public List<BillingRateByInventoryAge> findAll(Long companyId,
                                                 Long warehouseId,
                                                 Long clientId,
                                                   Integer startInventoryAge,
                                                   Integer endInventoryAge,
                                                   boolean exactMatch,
                                     Boolean includeDetails) {

        List<BillingRateByInventoryAge> billingRateByInventoryAges =  billingRateByInventoryAgeRepository.findAll(
                (Root<BillingRateByInventoryAge> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {

                    List<Predicate> predicates = new ArrayList<Predicate>();

                    predicates.add(criteriaBuilder.equal(root.get("companyId"), companyId));
                    if (Objects.isNull(clientId)) {
                        // if the client id is not passed in, then we will only return the
                        // rate that defined for the warehouse, not for any client
                        predicates.add(criteriaBuilder.isNull(root.get("clientId")));
                    }
                    else {
                        predicates.add(criteriaBuilder.equal(root.get("clientId"), clientId));
                    }
                    if (Objects.nonNull(startInventoryAge)) {
                        predicates.add(criteriaBuilder.equal(root.get("startInventoryAge"), startInventoryAge));
                    }
                    if (Objects.nonNull(endInventoryAge)) {
                        predicates.add(criteriaBuilder.equal(root.get("endInventoryAge"), endInventoryAge));
                    }
                    Predicate[] p = new Predicate[predicates.size()];

                    // special handling for warehouse id
                    // if warehouse id is passed in, then return both the warehouse level item
                    // and the company level item information.
                    // otherwise, return the company level item information
                    Predicate predicate = criteriaBuilder.and(predicates.toArray(p));
                    if (Objects.nonNull(warehouseId)) {
                        if (Boolean.TRUE.equals(exactMatch)) {

                            // use requires a exact match. so if warehouse id is passed in,
                            // we will only return the warehouse level configuration. If the
                            // rate is not configured at the warehouse level but configured at the company level
                            // we will not return the configuration for an 'exactMatch'
                            return criteriaBuilder.and(predicate, criteriaBuilder.equal(root.get("warehouseId"), warehouseId));
                        }
                        else {
                            return criteriaBuilder.and(predicate,
                                    criteriaBuilder.or(
                                            criteriaBuilder.equal(root.get("warehouseId"), warehouseId),
                                            criteriaBuilder.isNull(root.get("warehouseId"))));

                        }
                    }
                    else  {
                        return criteriaBuilder.and(predicate,criteriaBuilder.isNull(root.get("warehouseId")));
                    }
                }
                ,
                Sort.by(Sort.Direction.ASC, "warehouseId", "startInventoryAge")
        );

        if (!billingRateByInventoryAges.isEmpty() && Boolean.TRUE.equals(includeDetails)) {

            loadDetails(billingRateByInventoryAges);
        }

        return billingRateByInventoryAges;
    }


    private void loadDetails(List<BillingRateByInventoryAge> billingRateByInventoryAges) {
        billingRateByInventoryAges.forEach(
                this::loadDetails
        );
    }
    private void loadDetails(BillingRateByInventoryAge billingRateByInventoryAges) {
        if (Objects.nonNull(billingRateByInventoryAges.getClientId()) &&
                Objects.isNull(billingRateByInventoryAges.getClient())) {
            billingRateByInventoryAges.setClient(
                    commonServiceRestemplateClient.getClientById(
                            billingRateByInventoryAges.getClientId()
                    )
            );
        }
    }

    public BillingRateByInventoryAge saveBillingRateByInventoryAge(BillingRateByInventoryAge billingRateByInventoryAge) {
        billingRateByInventoryAge.getBillingRates().forEach(
                billingRate -> billingRate.setBillingRateByInventoryAge(
                        billingRateByInventoryAge
                )
        );
        return saveOrUpdate(billingRateByInventoryAge);
    }

    public List<BillingRateByInventoryAge> saveBillingRateByInventoryAges(List<BillingRateByInventoryAge> billingRateByInventoryAges) {
        return billingRateByInventoryAges.stream().map(this::saveBillingRateByInventoryAge).collect(Collectors.toList());
    }

    public void removeBillingRateByInventoryAge(Long id) {
        billingRateByInventoryAgeRepository.deleteById(id);
    }
}

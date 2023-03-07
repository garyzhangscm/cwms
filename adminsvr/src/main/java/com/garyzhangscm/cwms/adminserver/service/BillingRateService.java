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
import com.garyzhangscm.cwms.adminserver.clients.WarehouseLayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.adminserver.exception.DataTransferException;
import com.garyzhangscm.cwms.adminserver.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.adminserver.model.*;
import com.garyzhangscm.cwms.adminserver.model.wms.Company;
import com.garyzhangscm.cwms.adminserver.repository.BillingRateRepository;
import com.garyzhangscm.cwms.adminserver.repository.DataTransferRequestRepository;
import com.garyzhangscm.cwms.adminserver.service.datatransfer.DataTransferExportService;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class BillingRateService {
    private static final Logger logger = LoggerFactory.getLogger(BillingRateService.class);
    @Autowired
    private BillingRateRepository billingRateRepository;

    @Autowired
    private CommonServiceRestemplateClient commonServiceRestemplateClient;


    public BillingRate findById(Long id) {
        return findById(id, true);
    }
    public BillingRate findById(Long id, boolean includeDetails) {
        BillingRate billingRate = billingRateRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("billing rate not found by id: " + id));
        if (Objects.nonNull(billingRate) && includeDetails) {
            loadDetails(billingRate);
        }
        return billingRate;
    }

    public BillingRate save(BillingRate billingRate) {
        return save(billingRate, true);
    }
    public BillingRate save(BillingRate billingRate, boolean loadDetails) {
        BillingRate newBillingRate =  billingRateRepository.save(billingRate);
        if (loadDetails) {

            loadDetails(newBillingRate);
        }
        return newBillingRate;
    }

    public BillingRate saveOrUpdate(BillingRate billingRate) {
        return saveOrUpdate(billingRate, true);

    }
    public BillingRate saveOrUpdate(BillingRate billingRate, boolean loadDetails) {
        if (billingRate.getId() == null &&
                findByCategory(billingRate.getCompanyId(),
                        billingRate.getWarehouseId(),
                        billingRate.getClientId(),
                        billingRate.getBillableCategory(), true) != null) {
            billingRate.setId(
                    findByCategory(billingRate.getCompanyId(),
                            billingRate.getWarehouseId(),
                            billingRate.getClientId(),
                            billingRate.getBillableCategory(), true).getId());
        }
        return save(billingRate, loadDetails);
    }

    public BillingRate findByCategory(Long companyId, Long warehouseId, Long clientId, BillableCategory category, boolean exactMatch) {
        List<BillingRate> inventorySnapshots = findAll(companyId,
                warehouseId, clientId,
                category.name(), exactMatch);
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
     * @param billableCategory
     * @param exactMatch
     * @return
     */
    public List<BillingRate> findAll(Long companyId,
                                     Long warehouseId,
                                     Long clientId,
                                     String billableCategory,
                                     Boolean exactMatch) {
        return findAll(companyId, warehouseId, clientId, billableCategory, exactMatch, true);
    }


    public List<BillingRate> findAll(Long companyId,
                                     Long warehouseId,
                                     Long clientId,
                                     String billableCategory,
                                     Boolean exactMatch,
                                     Boolean includeDetails) {

        List<BillingRate> billingRates =  billingRateRepository.findAll(
                (Root<BillingRate> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {

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
                    if (StringUtils.isNotBlank(billableCategory)) {
                        predicates.add(criteriaBuilder.equal(root.get("billableCategory"), BillableCategory.valueOf(billableCategory)));
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
                Sort.by(Sort.Direction.ASC, "warehouseId", "billableCategory")
        );

        // we may get duplicated record from the above query when we pass in the warehouse id
        // if so, we may need to remove the company level item if we have the warehouse level item
        // we will do so only when
        if (Objects.nonNull(warehouseId)) {
            removeDuplicatedRecords(billingRates);
        }
        if (!billingRates.isEmpty() && Boolean.TRUE.equals(includeDetails)) {

            loadDetails(billingRates);
        }

        return billingRates;
    }
    /**
     * Remove teh duplicated clients record. If we have 2 record with the same clients name
     * but different warehouse, then we will remove the one without any warehouse information
     * from the result
     * @param billingRates
     */
    private void removeDuplicatedRecords(List<BillingRate> billingRates) {
        Iterator<BillingRate> billingRateIterator = billingRates.listIterator();
        Set<BillableCategory> billingRateProcessed = new HashSet<>();
        while(billingRateIterator.hasNext()) {
            BillingRate billingRate = billingRateIterator.next();

            if (billingRateProcessed.contains(billingRate.getBillableCategory()) &&
                    Objects.isNull(billingRate.getWarehouseId())) {
                // ok, we already processed the item and the current
                // record is a company level item, then we will remove
                // this record from the result
                billingRateIterator.remove();
            }
            billingRateProcessed.add(billingRate.getBillableCategory());
        }
    }

    private void loadDetails(List<BillingRate> billingRates) {
        billingRates.forEach(
                this::loadDetails
        );
    }
    private void loadDetails(BillingRate billingRate) {
        if (Objects.nonNull(billingRate.getClientId()) &&
                Objects.isNull(billingRate.getClient())) {
            billingRate.setClient(
                    commonServiceRestemplateClient.getClientById(
                            billingRate.getClientId()
                    )
            );
        }
    }

    public BillingRate saveBillingRate(BillingRate billingRate) {
        return saveOrUpdate(billingRate);
    }

    public List<BillingRate> saveBillingRates(List<BillingRate> billingRates) {
        return billingRates.stream().map(this::saveBillingRate).collect(Collectors.toList());
    }
}

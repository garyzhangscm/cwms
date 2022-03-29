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
import org.springframework.stereotype.Service;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.io.File;
import java.io.IOException;
import java.util.*;

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
                findByBatchNumber(inventorySnapshot.getWarehouseId(), inventorySnapshot.getBatchNumber()) != null) {
            inventorySnapshot.setId(
                    findByBatchNumber(inventorySnapshot.getWarehouseId(), inventorySnapshot.getBatchNumber()).getId());
        }
        return save(inventorySnapshot, loadDetails);
    }

    private BillingRate findByCategory(Long companyId, Long warehouseId, BillableCategory category) {
        List<BillingRate> inventorySnapshots = findAll(warehouseId, null, batchNumber);
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
                                     String billableCategory,
                                     Boolean exactMatch) {
        return findAll(companyId, warehouseId, billableCategory, true);
    }


    public List<BillingRate> findAll(Long companyId,
                                     Long warehouseId,
                                     String billableCategory,
                                     boolean includeDetails) {

        List<BillingRate> billingRates =  billingRateRepository.findAll(
                (Root<InventorySnapshot> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();

                    predicates.add(criteriaBuilder.equal(root.get("warehouseId"), warehouseId));

                    if (StringUtils.isNotBlank(status)) {

                        predicates.add(criteriaBuilder.equal(
                                root.get("status"), InventorySnapshotStatus.valueOf(status)));
                    }

                    if (StringUtils.isNotBlank(batchNumber)) {

                        predicates.add(criteriaBuilder.equal(root.get("batchNumber"), batchNumber));
                    }


                    Predicate[] p = new Predicate[predicates.size()];
                    return criteriaBuilder.and(predicates.toArray(p));
                }
        );

        if (inventorySnapshots.size() > 0 && includeDetails) {
            loadDetails(inventorySnapshots);
        }

        return inventorySnapshots;
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
}

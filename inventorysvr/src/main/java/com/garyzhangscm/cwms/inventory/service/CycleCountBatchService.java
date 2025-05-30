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

package com.garyzhangscm.cwms.inventory.service;

import com.garyzhangscm.cwms.inventory.model.*;
import com.garyzhangscm.cwms.inventory.repository.CycleCountBatchRepository;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class CycleCountBatchService {
    private static final Logger logger = LoggerFactory.getLogger(CycleCountBatchService.class);

    @Autowired
    private CycleCountBatchRepository cycleCountBatchRepository;

    @Autowired
    private CycleCountRequestService cycleCountRequestService;
    @Autowired
    private CycleCountResultService cycleCountResultService;
    @Autowired
    private AuditCountRequestService auditCountRequestService;
    @Autowired
    private AuditCountResultService auditCountResultService;

    public CycleCountBatch findByBatchId(String batchId) {
        return cycleCountBatchRepository.findByBatchId(batchId);
    }

    @Transactional
    public CycleCountBatch save(CycleCountBatch cycleCountBatch) {
        if (findByBatchId(cycleCountBatch.getBatchId()) == null) {
            return cycleCountBatchRepository.save(cycleCountBatch);
        }
        else {
            return findByBatchId(cycleCountBatch.getBatchId());
        }
    }

    @Transactional
    public CycleCountBatch createCycleCountBatch(Long warehouseId, String batchId) {
        if (findByBatchId(batchId) != null) {
            return findByBatchId(batchId);
        }
        else {
            CycleCountBatch cycleCountBatch = new CycleCountBatch();
            cycleCountBatch.setBatchId(batchId);
            cycleCountBatch.setWarehouseId(warehouseId);
            return save(cycleCountBatch);
        }
    }

    public List<CycleCountBatch> findAll(Long warehouseId,
                                         String batchId) {
        List<CycleCountBatch> cycleCountBatches = cycleCountBatchRepository.findAll(
                (Root<CycleCountBatch> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();

                    predicates.add(criteriaBuilder.equal(root.get("warehouseId"), warehouseId));

                    if (StringUtils.isNotBlank(batchId)) {

                        predicates.add(criteriaBuilder.equal(root.get("batchId"), batchId));

                    }


                    Predicate[] p = new Predicate[predicates.size()];
                    return criteriaBuilder.and(predicates.toArray(p));
                }
        );


        loadCycleCountBatchStatistics(cycleCountBatches);
        return cycleCountBatches;
    }

    private void loadCycleCountBatchStatistics(List<CycleCountBatch> cycleCountBatches) {
        cycleCountBatches.forEach(cycleCountBatch -> loadCycleCountBatchStatistics(cycleCountBatch));
    }

    private void loadCycleCountBatchStatistics(CycleCountBatch cycleCountBatch) {


        String batchId = cycleCountBatch.getBatchId();
        Long warehouseId = cycleCountBatch.getWarehouseId();
        int openLocationCount =
                cycleCountRequestService.
                        getOpenCycleCountRequests(warehouseId, batchId).size();
        int cancelledLocationCount =
                cycleCountRequestService.
                        getCancelledCycleCountRequests(warehouseId, batchId).size();
        int finishedLocationCount =
                cycleCountResultService.findByBatchId(warehouseId, batchId).size();
        int openAuditLocationCount =
                auditCountRequestService.findByBatchId(warehouseId, batchId).size();
        int finishedAuditLocationCount =
                auditCountResultService.findByBatchId(warehouseId, batchId).size();

        int requestLocationCount = openLocationCount + cancelledLocationCount + finishedLocationCount;

        cycleCountBatch.setRequestLocationCount(requestLocationCount);
        cycleCountBatch.setOpenLocationCount(openLocationCount);
        cycleCountBatch.setCancelledLocationCount(cancelledLocationCount);
        cycleCountBatch.setFinishedLocationCount(finishedLocationCount);
        cycleCountBatch.setOpenAuditLocationCount(openAuditLocationCount);
        cycleCountBatch.setFinishedAuditLocationCount(finishedAuditLocationCount);
    }


    public List<CycleCountBatch> getCycleCountBatchesWithOpenCycleCount() {
        List<CycleCountBatch> cycleCountBatches =
                cycleCountBatchRepository.getCycleCountBatchesWithOpenCycleCount();


        loadCycleCountBatchStatistics(cycleCountBatches);
        return cycleCountBatches;
    }

    public List<CycleCountBatch> getCycleCountBatchesWithOpenAuditCount() {
        List<CycleCountBatch> cycleCountBatches =
                cycleCountBatchRepository.getCycleCountBatchesWithOpenAuditCount();
        loadCycleCountBatchStatistics(cycleCountBatches);
        return cycleCountBatches;
    }

    public List<CycleCountBatch> getOpenCycleCountBatches() {
        List<CycleCountBatch> cycleCountBatches =
                cycleCountBatchRepository.getOpenCycleCountBatches();
        loadCycleCountBatchStatistics(cycleCountBatches);
        return cycleCountBatches;
    }
}


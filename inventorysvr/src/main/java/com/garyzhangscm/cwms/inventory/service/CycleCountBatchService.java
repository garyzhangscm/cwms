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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

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

    public List<CycleCountBatch> findAll() {
        List<CycleCountBatch> cycleCountBatches = cycleCountBatchRepository.findAll();
        return cycleCountBatches.stream().map(cycleCountBatch -> loadCycleCountBatchStatistics(cycleCountBatch))
                .collect(Collectors.toList());
    }

    private CycleCountBatch loadCycleCountBatchStatistics(CycleCountBatch cycleCountBatch) {


        String batchId = cycleCountBatch.getBatchId();
        int openLocationCount = cycleCountRequestService.getOpenCycleCountRequests(batchId).size();
        int cancelledLocationCount = cycleCountRequestService.getCancelledCycleCountRequests(batchId).size();
        int finishedLocationCount = cycleCountResultService.findByBatchId(batchId).size();
        int openAuditLocationCount = auditCountRequestService.findByBatchId(batchId).size();
        int finishedAuditLocationCount = auditCountResultService.findByBatchId(batchId).size();

        int requestLocationCount = openLocationCount + cancelledLocationCount + finishedLocationCount;

        cycleCountBatch.setRequestLocationCount(requestLocationCount);
        cycleCountBatch.setOpenLocationCount(openLocationCount);
        cycleCountBatch.setCancelledLocationCount(cancelledLocationCount);
        cycleCountBatch.setFinishedLocationCount(finishedLocationCount);
        cycleCountBatch.setOpenAuditLocationCount(openAuditLocationCount);
        cycleCountBatch.setFinishedAuditLocationCount(finishedAuditLocationCount);
        return cycleCountBatch;
    }


}


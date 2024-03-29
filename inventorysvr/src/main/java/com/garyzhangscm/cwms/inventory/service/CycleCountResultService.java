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


import com.garyzhangscm.cwms.inventory.clients.WarehouseLayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.inventory.exception.GenericException;
import com.garyzhangscm.cwms.inventory.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.inventory.model.*;
import com.garyzhangscm.cwms.inventory.repository.CycleCountRequestRepository;
import com.garyzhangscm.cwms.inventory.repository.CycleCountResultRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class CycleCountResultService {
    private static final Logger logger = LoggerFactory.getLogger(CycleCountResultService.class);

    @Autowired
    private CycleCountResultRepository cycleCountResultRepository;

    @Autowired
    private ItemService itemService;


    @Autowired
    private WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient;

    public CycleCountResult findById(Long id) {
        return cycleCountResultRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("cycle count result not found by id: " + id));
    }
    public List<CycleCountResult> findByBatchId(Long warehouseId, String batchId) {
        return warehouseLayoutServiceRestemplateClient.
                setupCycleCountResultLocations(
                        cycleCountResultRepository.findByWarehouseIdAndBatchId(
                                warehouseId, batchId));
    }

    @Transactional
    public CycleCountResult save(CycleCountResult cycleCountResult) {
        return cycleCountResultRepository.save(cycleCountResult);
    }

    public List<CycleCountResult> findByAuditCountRequestId(AuditCountRequest auditCountRequest) {
        return cycleCountResultRepository.findByAuditCountRequestId(auditCountRequest.getId());
    }

    // After we finish the cycle count, we may move the request to the
    // result table, here we will clean the related cycle count result's
    // auditCountRequest field
    @Transactional
    public void postActionOfAuditCountRequestComplete(AuditCountRequest auditCountRequest) {

        logger.debug("Start to disconnect cycle count result from audit count: {} ", auditCountRequest.getId());
        List<CycleCountResult> cycleCountResults
                = findByAuditCountRequestId(auditCountRequest);
        cycleCountResults.forEach(cycleCountResult -> {
            cycleCountResult.setAuditCountRequest(null);
            save(cycleCountResult);
        });
    }

    public void handleItemOverride(Long warehouseId, Long oldItemId, Long newItemId) {
        cycleCountResultRepository.processItemOverride(warehouseId,
                oldItemId, newItemId);
    }
}


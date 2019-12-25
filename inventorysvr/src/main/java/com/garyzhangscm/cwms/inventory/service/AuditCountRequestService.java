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
import com.garyzhangscm.cwms.inventory.model.AuditCountRequest;
import com.garyzhangscm.cwms.inventory.model.CycleCountResult;
import com.garyzhangscm.cwms.inventory.repository.AuditCountRequestRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuditCountRequestService {
    private static final Logger logger = LoggerFactory.getLogger(AuditCountRequestService.class);

    @Autowired
    private AuditCountRequestRepository auditCountRequestRepository;
    @Autowired
    private CycleCountResultService cycleCountResultService;

    @Autowired
    private WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient;

    public List<AuditCountRequest> findByBatchId(String batchId) {
        return warehouseLayoutServiceRestemplateClient.setupAuditCountRequestLocations(auditCountRequestRepository.findByBatchId(batchId));
    }

    public AuditCountRequest findByLocationId(Long locationId) {
        return auditCountRequestRepository.findByLocationId(locationId);
    }

    public AuditCountRequest save(AuditCountRequest auditCountRequest) {
        return auditCountRequestRepository.save(auditCountRequest);
    }

    public AuditCountRequest generateAuditCountRequest(CycleCountResult cycleCountResult) {
        AuditCountRequest auditCountRequest = new AuditCountRequest();
        auditCountRequest.setBatchId(cycleCountResult.getBatchId());
        auditCountRequest.setLocationId(cycleCountResult.getLocationId());
        return save(auditCountRequest);
    }

    public AuditCountRequest findByBatchIdAndLocationId(String batchId, Long locationId) {
        return auditCountRequestRepository.findByBatchIdAndLocationId(batchId, locationId);
    }

    public void delete(AuditCountRequest auditCountRequest) {
        auditCountRequestRepository.delete(auditCountRequest);
    }
    public void removeAuditCountRequestByBatchIdAndLocationId(String batchId, Long locationId) {
        AuditCountRequest auditCountRequest = findByBatchIdAndLocationId(batchId, locationId);
        delete(findByBatchIdAndLocationId(batchId, locationId));
        cycleCountResultService.postActionOfAuditCountRequestComplete(auditCountRequest);

        // disconnect the cycle count result from the audit count request
        // after we remove the audit count request

    }

}


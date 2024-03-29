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

package com.garyzhangscm.cwms.inventory.controller;

import com.garyzhangscm.cwms.inventory.model.AuditCountResult;
import com.garyzhangscm.cwms.inventory.model.BillableEndpoint;
import com.garyzhangscm.cwms.inventory.model.CycleCountResult;
import com.garyzhangscm.cwms.inventory.service.AuditCountResultService;
import com.garyzhangscm.cwms.inventory.service.CycleCountResultService;
import com.garyzhangscm.cwms.inventory.service.InventoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class AuditCountResultController {
    private static final Logger logger = LoggerFactory.getLogger(AuditCountResultController.class);

    @Autowired
    AuditCountResultService auditCountResultService;

    @RequestMapping(value = "/audit-count-result/{warehouseId}/{batchId}", method = RequestMethod.GET)
    public List<AuditCountResult> getAuditCountResultByBatch(
            @PathVariable Long warehouseId,
            @PathVariable String batchId){
        return auditCountResultService.findByBatchId(warehouseId, batchId);
    }


    @RequestMapping(value = "/audit-count-result/{batchId}/{locationId}/inventories", method = RequestMethod.GET)
    public List<AuditCountResult> getEmptyAuditCountResults(@PathVariable String batchId,
                                                           @PathVariable Long locationId){
        return auditCountResultService.getEmptyAuditCountResults(batchId, locationId);
    }

    @BillableEndpoint
    @RequestMapping(value = "/audit-count-result/{batchId}/{locationId}/confirm", method = RequestMethod.POST)
    public List<AuditCountResult> confirmAuditCountResults(@PathVariable String batchId,
                                                           @PathVariable Long locationId,
                                                           @RequestBody List<AuditCountResult> auditCountResults){

        logger.debug("Start to confirm \n{}", auditCountResults);
        return auditCountResultService.confirmAuditCountResults(batchId, locationId, auditCountResults);
    }

}

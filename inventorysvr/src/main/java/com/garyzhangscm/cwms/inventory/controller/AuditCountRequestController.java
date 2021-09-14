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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.garyzhangscm.cwms.inventory.model.*;
import com.garyzhangscm.cwms.inventory.service.AuditCountRequestService;
import com.garyzhangscm.cwms.inventory.service.AuditCountResultService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class AuditCountRequestController {
    @Autowired
    AuditCountRequestService auditCountRequestService;

    @RequestMapping(value = "/audit-count-request/batch/{warehouseId}/{batchId}", method = RequestMethod.GET)
    public List<AuditCountRequest> getAuditCountRequestByBatch(
            @PathVariable Long warehouseId,
            @PathVariable String batchId){
        return auditCountRequestService.findByBatchId(warehouseId, batchId);
    }
    @RequestMapping(value = "/audit-count-request/{id}", method = RequestMethod.GET)
    public AuditCountRequest getAuditCountRequestByBatch(@PathVariable Long id){
        return auditCountRequestService.findById(id);
    }



    @BillableEndpoint
    @RequestMapping(
            value="/audit-count-request/{warehouseId}/{batchId}/audit-count-sheet",
            method = RequestMethod.POST)
    public ReportHistory generateAuditCountSheet(
            @PathVariable Long warehouseId,
            @PathVariable String batchId,
            @RequestParam(name = "audit_count_request_ids", defaultValue = "", required = false) String auditCountRequestIds,
            @RequestParam(name = "locale", defaultValue = "", required = false) String locale) throws JsonProcessingException {

        return auditCountRequestService.generateAuditCountSheet(
                warehouseId, batchId, auditCountRequestIds, locale);
    }

}

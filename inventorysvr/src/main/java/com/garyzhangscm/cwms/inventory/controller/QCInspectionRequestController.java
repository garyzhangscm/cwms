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

import com.garyzhangscm.cwms.inventory.ResponseBodyWrapper;
import com.garyzhangscm.cwms.inventory.model.BillableEndpoint;
import com.garyzhangscm.cwms.inventory.model.QCInspectionRequest;
import com.garyzhangscm.cwms.inventory.model.QCInspectionResult;
import com.garyzhangscm.cwms.inventory.model.QCRuleConfiguration;
import com.garyzhangscm.cwms.inventory.service.QCInspectionRequestService;
import com.garyzhangscm.cwms.inventory.service.QCRuleConfigurationService;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class QCInspectionRequestController {
    @Autowired
    QCInspectionRequestService qcInspectionRequestService;

    @RequestMapping(value="/qc-inspection-requests", method = RequestMethod.GET)
    public List<QCInspectionRequest> findAllQCInspectionRequests(
            @RequestParam Long warehouseId,
            @RequestParam(name="number", required = false, defaultValue = "") String number,
            @RequestParam(name="inventoryId", required = false, defaultValue = "") Long inventoryId,
            @RequestParam(name="inventoryIds", required = false, defaultValue = "") String inventoryIds,
            @RequestParam(name="lpn", required = false, defaultValue = "") String lpn ,
            @RequestParam(name="workOrderQCSampleNumber", required = false, defaultValue = "") String workOrderQCSampleNumber ,
            @RequestParam(name="type", required = false, defaultValue = "") String type ,
            @RequestParam(name="qcInspectionResult", required = false, defaultValue = "") String qcInspectionResult) {
        if (Strings.isNotBlank(qcInspectionResult)) {

            return qcInspectionRequestService.findAll(warehouseId, inventoryId, inventoryIds, lpn, workOrderQCSampleNumber,
                    QCInspectionResult.valueOf(qcInspectionResult), type, number);
        }
        else {

            return qcInspectionRequestService.findAll(warehouseId, inventoryId, inventoryIds, lpn, workOrderQCSampleNumber, null, type, number);
        }
    }

    @RequestMapping(value="/qc-inspection-requests/result", method = RequestMethod.GET)
    public List<QCInspectionRequest> findAllQCInspectionRequestResults(
            @RequestParam Long warehouseId,
            @RequestParam(name="lpn", required = false, defaultValue = "") String lpn,
            @RequestParam(name="workOrderQCSampleNumber", required = false, defaultValue = "") String workOrderQCSampleNumber ) {
        return qcInspectionRequestService.findAllQCInspectionRequestResults(warehouseId, lpn, workOrderQCSampleNumber);
    }


    @RequestMapping(value="/qc-inspection-requests/pending", method = RequestMethod.GET)
    public List<QCInspectionRequest> findPendingQCInspectionRequests(
            @RequestParam Long warehouseId,
            @RequestParam(name="inventoryId", required = false, defaultValue = "") Long inventoryId,
            @RequestParam(name="inventoryIds", required = false, defaultValue = "") String inventoryIds ) {
        return qcInspectionRequestService.findPendingQCInspectionRequests(warehouseId, inventoryId, inventoryIds);
    }

    @RequestMapping(value="/qc-inspection-requests", method = RequestMethod.POST)
    public List<QCInspectionRequest> savePendingQCInspectionRequest(
            @RequestParam Long warehouseId,
            @RequestBody List<QCInspectionRequest> qcInspectionRequests,
            @RequestParam(name="rfCode", required = false, defaultValue = "") String rfCode) {
        return qcInspectionRequestService.savePendingQCInspectionRequest(warehouseId, qcInspectionRequests, rfCode);
    }


    @RequestMapping(value="/qc-inspection-requests/work-order", method = RequestMethod.PUT)
    public QCInspectionRequest requestWorkOrderQCInspectionRequest(
            @RequestParam Long warehouseId,
            @RequestParam Long workOrderQCSampleId,
            @RequestParam(name="qcQuantity", required = false, defaultValue = "0") Long qcQuantity,
            @RequestParam String ruleIds) {
        return qcInspectionRequestService.generateWorkOrderQCInspectionRequest(warehouseId, workOrderQCSampleId, ruleIds, qcQuantity);
    }

    @RequestMapping(value="/qc-inspection-requests", method = RequestMethod.PUT)
    public QCInspectionRequest addQCInspectionRequest(
            @RequestParam Long warehouseId,
            @RequestBody QCInspectionRequest qcInspectionRequest) {
        return qcInspectionRequestService.addQCInspectionRequest(warehouseId, qcInspectionRequest);
    }
}

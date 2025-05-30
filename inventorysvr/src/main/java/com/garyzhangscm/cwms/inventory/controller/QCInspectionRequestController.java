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
import com.garyzhangscm.cwms.inventory.ResponseBodyWrapper;
import com.garyzhangscm.cwms.inventory.model.*;
import com.garyzhangscm.cwms.inventory.service.QCInspectionRequestService;
import com.garyzhangscm.cwms.inventory.service.QCRuleConfigurationService;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

@RestController
public class QCInspectionRequestController {

    private static final Logger logger = LoggerFactory.getLogger(QCInspectionRequestController.class);

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
            @RequestParam(name="number", required = false, defaultValue = "") String number,
            @RequestParam(name="workOrderQCSampleNumber", required = false, defaultValue = "") String workOrderQCSampleNumber ) {
        return qcInspectionRequestService.findAllQCInspectionRequestResults(warehouseId, lpn, workOrderQCSampleNumber, number);
    }


    @RequestMapping(value="/qc-inspection-requests/pending", method = RequestMethod.GET)
    public List<QCInspectionRequest> findPendingQCInspectionRequests(
            @RequestParam Long warehouseId,
            @RequestParam(name="inventoryId", required = false, defaultValue = "") Long inventoryId,
            @RequestParam(name="inventoryIds", required = false, defaultValue = "") String inventoryIds ) {
        return qcInspectionRequestService.findPendingQCInspectionRequests(warehouseId, inventoryId, inventoryIds);
    }

    @RequestMapping(value="/qc-inspection-request/{id}", method = RequestMethod.GET)
    public QCInspectionRequest getQCInspectionRequestResult(
            @RequestParam Long warehouseId, @PathVariable Long id ) {
        return qcInspectionRequestService.findById(id);
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

    /**
     * make sure we can use the LPN for the qc and return the inventories of this LPN
     * @param warehouseId
     * @param lpn
     * @return
     */
    @RequestMapping(value="/qc-inspection-requests/{id}/inspect-by-request/validate-lpn", method = RequestMethod.POST)
    public List<Inventory> validateLPNForInspectionByQCRequest(
            @PathVariable Long id,
            @RequestParam Long warehouseId,
            @RequestParam String lpn,
            @RequestParam(name = "reQC", defaultValue = "", required = false) Boolean reQC) {
        return qcInspectionRequestService.validateLPNForInspectionByQCRequest(id, warehouseId, lpn, reQC);
    }



    @BillableEndpoint
    @RequestMapping(value="/qc-inspection-requests/{id}/report", method = RequestMethod.POST)
    public ReportHistory generateQCInspectionRequestReport(
            @PathVariable Long id,
            @RequestParam(name = "locale", defaultValue = "", required = false) String locale) throws JsonProcessingException {

        logger.debug("start print report for qc inspection request with id: {}", id);
        return qcInspectionRequestService.generateQCInspectionRequestReport(id, locale);
    }


    @BillableEndpoint
    @RequestMapping(method=RequestMethod.POST, value="/qc-inspection-requests/{id}/documents")
    public ResponseBodyWrapper uploadQCInspectionDocument(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) throws IOException {


        String filePath = qcInspectionRequestService.uploadQCInspectionDocument(id, file);
        return  ResponseBodyWrapper.success(filePath);
    }

    @BillableEndpoint
    @RequestMapping(method=RequestMethod.POST, value="/qc-inspection-requests/{id}/change-document-urls")
    public QCInspectionRequest changeQCInspectionDocumentUrls(
            @PathVariable Long id,
            @RequestParam Long warehouseId,
            @RequestParam String documentUrls)  {


        return qcInspectionRequestService.changeQCInspectionDocumentUrls(id, documentUrls);
    }

    @RequestMapping(value="/qc-inspection-requests/documents/{warehouseId}/{id}/{fileName}", method = RequestMethod.GET)
    public ResponseEntity<Resource> getQCInspectionDocument(@PathVariable Long warehouseId,
                                                              @PathVariable Long id,
                                                              @PathVariable String fileName) throws FileNotFoundException {

        File imageFile = qcInspectionRequestService.getQCInspectionDocument(warehouseId, id, fileName);

        InputStreamResource resource
                = new InputStreamResource(new FileInputStream(imageFile));
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment;fileName=" + fileName)
                .contentLength(imageFile.length())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);

    }



}

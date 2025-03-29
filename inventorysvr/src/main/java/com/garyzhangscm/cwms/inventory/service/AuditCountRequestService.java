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


import com.fasterxml.jackson.core.JsonProcessingException;
import com.garyzhangscm.cwms.inventory.clients.ResourceServiceRestemplateClient;
import com.garyzhangscm.cwms.inventory.clients.WarehouseLayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.inventory.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.inventory.model.*;
import com.garyzhangscm.cwms.inventory.repository.AuditCountRequestRepository;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class AuditCountRequestService {
    private static final Logger logger = LoggerFactory.getLogger(AuditCountRequestService.class);

    @Autowired
    private AuditCountRequestRepository auditCountRequestRepository;
    @Autowired
    private CycleCountResultService cycleCountResultService;

    @Autowired
    private WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient;
    @Autowired
    private ResourceServiceRestemplateClient resourceServiceRestemplateClient;

    public AuditCountRequest findById(Long id) {
        return findById(id, true);
    }

    public AuditCountRequest findById(Long id, boolean loadDetails) {

        AuditCountRequest auditCountRequest =
                auditCountRequestRepository.findById(id)
                    .orElseThrow(() -> ResourceNotFoundException.raiseException("audit count request not found by id: " + id));

        if (Objects.nonNull(auditCountRequest) && loadDetails) {
            loadAttribute(auditCountRequest);

        }
        return auditCountRequest;
    }

    private void loadAttribute(List<AuditCountRequest> auditCountRequests) {
        auditCountRequests.forEach(auditCountRequest ->
                loadAttribute(auditCountRequest));
    }

    private void loadAttribute(AuditCountRequest auditCountRequest) {
        logger.debug("Start to load attribute for audit count reuqest: {}",
                auditCountRequest.getBatchId());
        if (Objects.nonNull(auditCountRequest.getWarehouseId())) {
            auditCountRequest.setWarehouse(
                    warehouseLayoutServiceRestemplateClient.getWarehouseById(
                            auditCountRequest.getWarehouseId()
                    )
            );
        }

        if (Objects.nonNull(auditCountRequest.getLocationId())) {

            try {
                logger.debug(">> location id is not null: {}",
                        auditCountRequest.getLocationId());
                auditCountRequest.setLocation(
                        warehouseLayoutServiceRestemplateClient.getLocationById(
                                auditCountRequest.getLocationId()
                        )
                );
                logger.debug("Get location {} by id {}",
                        auditCountRequest.getLocation().getName(),
                        auditCountRequest.getLocationId());
            }
            catch (Exception ex){}
        }
    }

    public List<AuditCountRequest> findByBatchId(
            Long warehouseId, String batchId) {
        return findByBatchId(warehouseId, batchId, true);
    }
    public List<AuditCountRequest> findByBatchId(
            Long warehouseId, String batchId, boolean loadDetails) {
        List<AuditCountRequest> auditCountRequests =
                warehouseLayoutServiceRestemplateClient.setupAuditCountRequestLocations(
                    auditCountRequestRepository.findByWarehouseIdAndBatchId(
                        warehouseId, batchId));

        if (auditCountRequests.size() > 0 && loadDetails) {
            loadAttribute(auditCountRequests);
        }

        return auditCountRequests;
    }

    public AuditCountRequest findByLocationId(Long locationId) {
        return auditCountRequestRepository.findByLocationId(locationId);
    }

    @Transactional
    public AuditCountRequest save(AuditCountRequest auditCountRequest) {
        return auditCountRequestRepository.save(auditCountRequest);
    }

    /**
     * Save the audit count, we will make sure there's no count against
     * this location first.
     * @param auditCountRequest
     * @return
     */
    @Transactional
    public AuditCountRequest saveAuditCount(
            AuditCountRequest auditCountRequest) {
        if (auditCountExistsInLocation(auditCountRequest.getLocationId())) {
            return findByLocationId(
                    auditCountRequest.getLocationId()
            );
        }
        else {
            return save(auditCountRequest);
        }
    }

    private boolean auditCountExistsInLocation(Long locationId) {
        return Objects.nonNull(
                findByLocationId(locationId)
        );
    }


    @Transactional
    public AuditCountRequest generateAuditCountRequest(
            CycleCountResult cycleCountResult) {
        AuditCountRequest auditCountRequest = new AuditCountRequest();
        auditCountRequest.setBatchId(cycleCountResult.getBatchId());
        auditCountRequest.setLocationId(cycleCountResult.getLocationId());
        auditCountRequest.setWarehouseId(cycleCountResult.getWarehouseId());


        return saveAuditCount(auditCountRequest);
    }

    public AuditCountRequest findByBatchIdAndLocationId(
            String batchId, Long locationId) {
        return auditCountRequestRepository.findByBatchIdAndLocationId(batchId, locationId);
    }

    @Transactional
    public void delete(AuditCountRequest auditCountRequest) {

        auditCountRequestRepository.delete(auditCountRequest);
    }
    @Transactional
    public void removeAuditCountRequestByBatchIdAndLocationId(
            String batchId, Long locationId) {
        AuditCountRequest auditCountRequest
                = findByBatchIdAndLocationId(batchId, locationId);
        delete(findByBatchIdAndLocationId(batchId, locationId));
        cycleCountResultService.postActionOfAuditCountRequestComplete(
                auditCountRequest);

        // disconnect the cycle count result from the audit count request
        // after we remove the audit count request

    }



    public ReportHistory generateAuditCountSheet(Long warehouseId,
                                                 String batchId,
                                                 String auditCountRequestIds,
                                                 String locale)
            throws JsonProcessingException {


        List<AuditCountRequest> auditCountRequests;
        if (Strings.isNotBlank(auditCountRequestIds)) {
            auditCountRequests =
                    Arrays.stream(auditCountRequestIds.split(","))
                            .map(Long::parseLong)
                            .map(id -> findById(id))
                            .collect(Collectors.toList());
        }
        else {

            // get all cycle count requests based on the batch id=
            auditCountRequests = findByBatchId(warehouseId, batchId);
        }

        return generateAuditCountSheet(warehouseId, batchId,
                auditCountRequests, locale);
    }
    public ReportHistory generateAuditCountSheet(
            Long warehouseId,
            String batchId,
            List<AuditCountRequest> auditCountRequests,
            String locale)
            throws JsonProcessingException {


        Report reportData = new Report();
        setupAuditCountSheetData(
                reportData, auditCountRequests
        );
        setupAuditCountSheetParameters(
                reportData, batchId, auditCountRequests
        );

        logger.debug("will call resource service to print the audit count sheet with locale: {}",
                locale);
        // logger.debug("####   Report   Data  ######");
        // logger.debug(reportData.toString());
        ReportHistory reportHistory =
                resourceServiceRestemplateClient.generateReport(
                        warehouseId, ReportType.AUDIT_COUNT_SHEET,
                        reportData, locale
                );


        logger.debug("####   Report   printed: {}", reportHistory.getFileName());
        return reportHistory;

    }

    private void setupAuditCountSheetParameters(
            Report report,
            String batchId,
            List<AuditCountRequest> auditCountRequests) {

        // set the parameters to be the meta data of
        // the order

        logger.debug("Start to setup audit count's parameters");
        report.addParameter("audit_count.batch_number",
                batchId);

        logger.debug("=======    Audit Count Request  =======");
        logger.debug(auditCountRequests.toString());
        Set<String> locationName = auditCountRequests.stream()
                .filter(auditCountRequest -> Objects.nonNull(auditCountRequest.getLocation()))
                .map(AuditCountRequest::getLocation)
                .map(Location::getName).collect(Collectors.toSet());

        report.addParameter("totalLocationCount", locationName.size());



    }

    private void setupAuditCountSheetData(
            Report report, List<AuditCountRequest> auditCountRequests) {
        report.setData(auditCountRequests);

    }
}


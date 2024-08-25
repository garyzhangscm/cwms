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

package com.garyzhangscm.cwms.resources.service;

import com.garyzhangscm.cwms.resources.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.resources.model.*;
import com.garyzhangscm.cwms.resources.repository.CustomReportExecutionHistoryRepository;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.persistence.criteria.*;
import java.io.File;
import java.time.ZonedDateTime;
import java.util.*;

@Service
public class CustomReportExecutionHistoryService {
    private static final Logger logger = LoggerFactory.getLogger(CustomReportExecutionHistoryService.class);
    @Autowired
    private CustomReportExecutionHistoryRepository customReportExecutionHistoryRepository;


    public CustomReportExecutionHistory findById(Long id) {
        return customReportExecutionHistoryRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("Customer Report Execution History not found by id: " + id));
    }


    public List<CustomReportExecutionHistory> findAll(Long companyId,
                                                      Long warehouseId,
                                                      Long customReportId,
                                                      String customReportExecutionHistoryIDs,
                                                      Boolean includeExpiredExecutionHistory) {

        return customReportExecutionHistoryRepository.findAll(
                (Root<CustomReportExecutionHistory> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();

                    predicates.add(criteriaBuilder.equal(root.get("companyId"), companyId));
                    if (Objects.nonNull(warehouseId)) {
                        predicates.add(criteriaBuilder.equal(root.get("warehouseId"), warehouseId));

                    }
                    if (Objects.nonNull(customReportId)) {

                        Join<CustomReportExecutionHistory, CustomReport> joinCustomReport = root.join("customReport",JoinType.INNER);
                        predicates.add(criteriaBuilder.equal(joinCustomReport.get("id"), customReportId));

                    }
                    if (Strings.isNotBlank(customReportExecutionHistoryIDs)){
                        CriteriaBuilder.In<Long> inCustomReportExecutionHistoryIDs = criteriaBuilder.in(root.get("id"));
                        for(String id : customReportExecutionHistoryIDs.split(",")) {
                            inCustomReportExecutionHistoryIDs.value(Long.parseLong(id));
                        }
                        predicates.add(criteriaBuilder.and(inCustomReportExecutionHistoryIDs));
                    }
                    // by default, only return the not expired history
                    if (Objects.isNull(includeExpiredExecutionHistory)) {
                        predicates.add(criteriaBuilder.equal(root.get("resultFileExpired"), false));
                    }

                    Predicate[] p = new Predicate[predicates.size()];
                    return criteriaBuilder.and(predicates.toArray(p));
                },
                Sort.by(Sort.Direction.DESC, "createdTime")

        );
    }

    public List<CustomReportExecutionHistory> findByCustomReport(Long companyId,
                                                      Long warehouseId,
                                                      Long customReportId) {
        return findAll(companyId, warehouseId, customReportId, null, null);
    }


    public CustomReportExecutionHistory save(CustomReportExecutionHistory customReportExecution) {
        return customReportExecutionHistoryRepository.save(customReportExecution);
    }

    public CustomReportExecutionHistory saveAndFlush(CustomReportExecutionHistory customReportExecution) {
        return customReportExecutionHistoryRepository.saveAndFlush(customReportExecution);
    }



    public CustomReportExecutionHistory addCustomReportExecutionHistory(CustomReportExecutionHistory CustomReportExecutionHistory) {
        return saveAndFlush(CustomReportExecutionHistory);

    }


    /**
     * Refresh every minutes to remove expired files
     */
    @Scheduled(fixedDelay = 60000)
    public void removeExpiredFiles(){

        logger.debug("start to remove expired export file every minutes, " +
                "while the file should be expired in 1 hour after it is crated");

        List<CustomReportExecutionHistory> expiredCustomReportExecutionHistories =
            customReportExecutionHistoryRepository.findByResultFileExpiredTimeLessThanEqual(ZonedDateTime.now());


        expiredCustomReportExecutionHistories.forEach(
                expiredCustomReportExecutionHistory -> {
                    logger.debug("file {} from execution id {} of name {} is expired, we will remove it",
                            expiredCustomReportExecutionHistory.getResultFile(),
                            expiredCustomReportExecutionHistory.getId(),
                            expiredCustomReportExecutionHistory.getCustomReport().getName());
                    removeExpiredCustomReportExecutionHistoryFile(expiredCustomReportExecutionHistory.getResultFile());
                    expiredCustomReportExecutionHistory.setStatus(CustomReportExecutionStatus.EXPIRED);
                    save(expiredCustomReportExecutionHistory);

                }
        );

    }

    private void removeExpiredCustomReportExecutionHistoryFile(String resultFilePath) {

        File resultFile = new File(resultFilePath);
        // remove the file if it already exists
        resultFile.deleteOnExit();
    }

    public File getCustomReportResultFile(Long id) {

        CustomReportExecutionHistory customReportExecutionHistory
                = findById(id);

        return new File(customReportExecutionHistory.getResultFile());
    }
}

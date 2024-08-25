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
import com.garyzhangscm.cwms.resources.model.CustomReport;
import com.garyzhangscm.cwms.resources.model.CustomReportExecutionHistory;
import com.garyzhangscm.cwms.resources.model.Role;
import com.garyzhangscm.cwms.resources.model.User;
import com.garyzhangscm.cwms.resources.repository.CustomReportExecutionHistoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import javax.persistence.criteria.*;
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
        return findAll(companyId, warehouseId, customReportId, null);
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
}

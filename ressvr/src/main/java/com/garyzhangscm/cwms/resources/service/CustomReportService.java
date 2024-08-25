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
import com.garyzhangscm.cwms.resources.repository.CustomReportRepository;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.transaction.Transactional;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class CustomReportService {
    private static final Logger logger = LoggerFactory.getLogger(CustomReportService.class);
    @Autowired
    private CustomReportRepository customReportRepository;

    @Autowired
    private CustomReportExecutionHistoryService customReportExecutionHistoryService;

    private Map<Long, CustomReportExecutionHistory> inProcessCustomReport = new ConcurrentHashMap<>();

    @Autowired
    private EntityManager entityManager;


    public CustomReport findById(Long id) {
        return customReportRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("Customer Report not found by id: " + id));
    }


    public List<CustomReport> findAll(Long companyId,
                                      Long warehouseId,
                                      String name) {

        return customReportRepository.findAll(
                (Root<CustomReport> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();

                    predicates.add(criteriaBuilder.equal(root.get("companyId"), companyId));
                    if (Objects.nonNull(warehouseId)) {
                        predicates.add(criteriaBuilder.equal(root.get("warehouseId"), warehouseId));

                    }
                    if (Strings.isNotBlank(name)) {
                        predicates.add(criteriaBuilder.equal(root.get("name"), name));

                    }


                    Predicate[] p = new Predicate[predicates.size()];
                    return criteriaBuilder.and(predicates.toArray(p));
                },
                Sort.by(Sort.Direction.DESC, "createdTime")

        );
    }

    public CustomReport findByName(Long companyId,
                                   Long warehouseId,
                                   String name) {

        return customReportRepository.findByCompanyIdAndWarehouseIdAndName(
                companyId, warehouseId, name
        );
    }

    public CustomReport save(CustomReport customReport) {
        return customReportRepository.save(customReport);
    }

    public CustomReport saveOrUpdate(CustomReport customReport) {

        if (Objects.isNull(customReport.getId()) &&
            Objects.nonNull(findByName(
                    customReport.getCompanyId(), customReport.getWarehouseId(),
                    customReport.getName()
            ))) {
            customReport.setId(
                    findByName(
                            customReport.getCompanyId(),
                            customReport.getWarehouseId(),
                            customReport.getName()
                    ).getId()
            );
        }
        return save(customReport);
    }


    public CustomReport addCustomReport(CustomReport customReport) {
        for (CustomReportParameter customReportParameter : customReport.getCustomReportParameters()) {
            customReportParameter.setCustomReport(customReport);
        }
        customReport.setQuery(
                customReport.getQuery().replace("\n", "  ")
        );
        return saveOrUpdate(customReport);

    }

    public CustomReport changeCustomReport(Long id, CustomReport customReport) {
        customReport.setId(id);
        for (CustomReportParameter customReportParameter : customReport.getCustomReportParameters()) {
            customReportParameter.setCustomReport(customReport);
        }
        customReport.setQuery(
                customReport.getQuery().replace("\n", "  ")
        );
        return saveOrUpdate(customReport);

    }

    public void delete(Long id) {
        customReportRepository.deleteById(id);

    }

    @Transactional
    public CustomReportExecutionHistory runCustomReport(Long id,
                                                        Long companyId,
                                                        Long warehouseId,
                                                        CustomReport customReport) {

        StringBuilder queryString = new StringBuilder()
                .append(customReport.getQuery()).append(" where 1 = 1");

        StringBuilder actualQueryString = new StringBuilder()
                .append(customReport.getQuery()).append(" where 1 = 1");

        Map<String, String> validParameters = new HashMap<>();

        customReport.getCustomReportParameters().stream().filter(
                customReportParameter -> Strings.isNotBlank(customReportParameter.getName()) &&
                        Strings.isNotBlank(customReportParameter.getValue())
        )
                .forEach(
                        customReportParameter -> {
                            queryString.append(" and ").append(customReportParameter.getName())
                                    .append(" = ")
                                    .append(" :").append(customReportParameter.getName());

                            actualQueryString.append(" and ").append(customReportParameter.getName())
                                    .append(" = '").append(customReportParameter.getValue()).append("'");

                            validParameters.put(customReportParameter.getName(),
                                    customReportParameter.getValue());
                        }
                );


        CustomReportExecutionHistory customReportExecutionHistory =
                new CustomReportExecutionHistory(customReport, companyId, warehouseId,
                        actualQueryString.toString());

        customReportExecutionHistory =
                customReportExecutionHistoryService.addCustomReportExecutionHistory(customReportExecutionHistory);

        Long customReportExecutionHistoryId = customReportExecutionHistory.getId();
        inProcessCustomReport.put(customReportExecutionHistoryId, customReportExecutionHistory);

        new Thread(() ->{

            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            CustomReportExecutionHistory existingCustomReportExecutionHistory =
                    inProcessCustomReport.get(customReportExecutionHistoryId);
            try {

                existingCustomReportExecutionHistory.setStatus(CustomReportExecutionStatus.RUNNING);

                logger.debug("will save {} and change status to {}",
                        existingCustomReportExecutionHistory.getId(),
                        CustomReportExecutionStatus.RUNNING);

                existingCustomReportExecutionHistory =
                        customReportExecutionHistoryService.save(existingCustomReportExecutionHistory);

                logger.debug("execution history  {} saved",
                        existingCustomReportExecutionHistory.getId());

                Query query = entityManager.createNativeQuery(queryString.toString());
                for (Map.Entry<String, String> parameter : validParameters.entrySet()) {
                    query.setParameter(parameter.getKey(), parameter.getValue());
                }

                List<Object[]> results = query.getResultList();


                logger.debug("Get {} result from the query: \n{}",
                        results.size(), queryString.toString());
                for (Object[] row : results) {
                    for (Object cell : row) {
                        logger.debug(cell.toString() + ",");
                    }
                    logger.debug("\n");
                }

                existingCustomReportExecutionHistory.setCustomReportExecutionPercent(20);
                existingCustomReportExecutionHistory.setResultRowCount(results.size());
                existingCustomReportExecutionHistory.setStatus(CustomReportExecutionStatus.EXPORT_RESULT);
                existingCustomReportExecutionHistory =
                        customReportExecutionHistoryService.save(existingCustomReportExecutionHistory);

                // save to the file
                exportReportData(results);

                existingCustomReportExecutionHistory.setCustomReportExecutionPercent(100);
                existingCustomReportExecutionHistory.setStatus(CustomReportExecutionStatus.COMPLETE);
                customReportExecutionHistoryService.save(existingCustomReportExecutionHistory);

            }
            catch (Exception ex) {

                ex.printStackTrace();

                existingCustomReportExecutionHistory.setCustomReportExecutionPercent(100);
                existingCustomReportExecutionHistory.setStatus(CustomReportExecutionStatus.FAIL);
                existingCustomReportExecutionHistory.setErrorMessage(ex.getMessage());
                customReportExecutionHistoryService.save(existingCustomReportExecutionHistory);

            }





        }).start();

        return customReportExecutionHistory;

    }

    public void exportReportData(List<Object[]> results) {}
}

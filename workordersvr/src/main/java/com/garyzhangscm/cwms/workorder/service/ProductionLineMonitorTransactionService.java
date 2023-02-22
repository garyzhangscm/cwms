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

package com.garyzhangscm.cwms.workorder.service;

import com.garyzhangscm.cwms.workorder.exception.ProductionLineException;
import com.garyzhangscm.cwms.workorder.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.workorder.model.ProductionLine;
import com.garyzhangscm.cwms.workorder.model.ProductionLineMonitor;
import com.garyzhangscm.cwms.workorder.model.ProductionLineMonitorTransaction;
import com.garyzhangscm.cwms.workorder.repository.ProductionLineMonitorRepository;
import com.garyzhangscm.cwms.workorder.repository.ProductionLineMonitorTransactionRepository;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import javax.persistence.criteria.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


@Service
public class ProductionLineMonitorTransactionService {
    private static final Logger logger = LoggerFactory.getLogger(ProductionLineMonitorTransactionService.class);

    @Autowired
    private ProductionLineMonitorTransactionRepository productionLineMonitorTransactionRepository;

    @Autowired
    private ProductionLineMonitorService productionLineMonitorService;


    public ProductionLineMonitorTransaction findById(Long id) {
        return productionLineMonitorTransactionRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException(
                        "production line monitor transaction not found by id: " + id));

    }


    public List<ProductionLineMonitorTransaction> findAll(Long warehouseId,
                                                          String productionLineMonitorName,
                                                          String productionLineName,
                                                          Long productionLineId,
                                                          ZonedDateTime startTime, ZonedDateTime endTime, LocalDate date) {
        return productionLineMonitorTransactionRepository.findAll(
                (Root<ProductionLineMonitorTransaction> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();

                    predicates.add(criteriaBuilder.equal(root.get("warehouseId"), warehouseId));

                    if (StringUtils.isNotBlank(productionLineMonitorName)) {

                        Join<ProductionLineMonitorTransaction, ProductionLineMonitor>
                                joinProductionLineMonitor = root.join("productionLineMonitor", JoinType.INNER);
                        if (productionLineMonitorName.contains("*")) {
                            predicates.add(criteriaBuilder.like(joinProductionLineMonitor.get("name"),
                                    productionLineMonitorName.replaceAll("\\*", "%")));
                        }
                        else {
                            predicates.add(criteriaBuilder.equal(joinProductionLineMonitor.get("name"), productionLineMonitorName));
                        }

                    }
                    if (StringUtils.isNotBlank(productionLineName) || Objects.nonNull(productionLineId)) {

                        Join<ProductionLineMonitorTransaction, ProductionLine> joinProductionLine = root.join("productionLine", JoinType.INNER);
                        if (StringUtils.isNotBlank(productionLineName)) {

                            if (productionLineName.contains("*")) {
                                predicates.add(criteriaBuilder.like(joinProductionLine.get("name"), productionLineName.replaceAll("\\*", "%")));
                            }
                            else {
                                predicates.add(criteriaBuilder.equal(joinProductionLine.get("name"), productionLineName));
                            }
                        }
                        if (Objects.nonNull(productionLineId)) {

                            predicates.add(criteriaBuilder.equal(joinProductionLine.get("id"), productionLineId));
                        }


                    }


                    if (Objects.nonNull(startTime)) {
                        predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                                root.get("createdTime"), startTime));

                    }

                    if (Objects.nonNull(endTime)) {
                        predicates.add(criteriaBuilder.lessThanOrEqualTo(
                                root.get("createdTime"), endTime));

                    }
                    logger.debug(">> Date is passed in {}", date);
                    if (Objects.nonNull(date)) {
                        LocalDateTime dateStartTime = date.atStartOfDay();
                        LocalDateTime dateEndTime = date.plusDays(1).atStartOfDay().minusSeconds(1);
                        predicates.add(criteriaBuilder.between(
                                root.get("createdTime"), dateStartTime.atZone(ZoneId.of("UTC")),
                                dateEndTime.atZone(ZoneId.of("UTC"))));

                    }
                    Predicate[] p = new Predicate[predicates.size()];
                    return criteriaBuilder.and(predicates.toArray(p));
                }
                ,
                Sort.by(Sort.Direction.DESC, "createdTime")
        );
    }




    public ProductionLineMonitorTransaction save(
            ProductionLineMonitorTransaction productionLineMonitorTransaction) {
        return productionLineMonitorTransactionRepository.save(productionLineMonitorTransaction);
    }



    public ProductionLineMonitorTransaction addProductionLineMonitorTransaction(
            ProductionLineMonitorTransaction productionLineMonitorTransaction) {
        // setup the production line according to the production line monitor

        return save(productionLineMonitorTransaction);
    }

    public ProductionLineMonitorTransaction addProductionLineMonitorTransaction(
            Long warehouseId, String productionLineMonitorName, Double cycleTime) {
        // setup the production line according to the production line monitor

        ProductionLineMonitor productionLineMonitor =
                productionLineMonitorService.findByName(
                        warehouseId, productionLineMonitorName
                );
        if (Objects.isNull(productionLineMonitor)) {
            throw ProductionLineException.raiseException("Can't find monitor by name " +
                    productionLineMonitorName);
        }

        return addProductionLineMonitorTransaction(new ProductionLineMonitorTransaction(
                warehouseId, productionLineMonitor,
                productionLineMonitor.getProductionLine(),
                cycleTime
        ));
    }




}

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

import com.garyzhangscm.cwms.workorder.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.workorder.model.*;
import com.garyzhangscm.cwms.workorder.repository.ProductionLineMonitorRepository;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import javax.persistence.criteria.*;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


@Service
public class ProductionLineMonitorService  {
    private static final Logger logger = LoggerFactory.getLogger(ProductionLineMonitorService.class);

    @Autowired
    private ProductionLineMonitorRepository productionLineMonitorRepository;


    public ProductionLineMonitor findById(Long id) {
        return productionLineMonitorRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("production line monitor not found by id: " + id));

    }


    public List<ProductionLineMonitor> findAll(Long warehouseId,
                                               String name,
                                               String description,
                                               String productionLineName) {
        return productionLineMonitorRepository.findAll(
                (Root<ProductionLineMonitor> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();

                    predicates.add(criteriaBuilder.equal(root.get("warehouseId"), warehouseId));

                    if (StringUtils.isNotBlank(name)) {
                        if (name.contains("%")) {
                            predicates.add(criteriaBuilder.like(root.get("name"), name));
                        }
                        else {
                            predicates.add(criteriaBuilder.equal(root.get("name"), name));
                        }

                    }
                    if (StringUtils.isNotBlank(description)) {
                        if (description.contains("%")) {
                            predicates.add(criteriaBuilder.like(root.get("description"), description));
                        }
                        else {
                            predicates.add(criteriaBuilder.equal(root.get("description"), description));
                        }

                    }
                    if (StringUtils.isNotBlank(productionLineName)) {

                        Join<ProductionLineMonitor, ProductionLine> joinProductionLine = root.join("productionLine", JoinType.INNER);
                        if (productionLineName.contains("%")) {
                            predicates.add(criteriaBuilder.like(joinProductionLine.get("name"), productionLineName));
                        }
                        else {
                            predicates.add(criteriaBuilder.equal(joinProductionLine.get("name"), productionLineName));
                        }

                    }
                    Predicate[] p = new Predicate[predicates.size()];
                    return criteriaBuilder.and(predicates.toArray(p));
                }
                ,
                Sort.by(Sort.Direction.ASC, "name")
        );
    }


    public ProductionLineMonitor findByName(Long warehouseId, String name) {

        return productionLineMonitorRepository.findByWarehouseIdAndName(warehouseId, name);
    }

    public ProductionLineMonitor save(ProductionLineMonitor productionLineMonitor) {
        return productionLineMonitorRepository.save(productionLineMonitor);
    }

    public ProductionLineMonitor saveOrUpdate(ProductionLineMonitor productionLineMonitor) {
        if (Objects.isNull(productionLineMonitor.getId()) &&
            Objects.nonNull(findByName(productionLineMonitor.getWarehouseId(), productionLineMonitor.getName()))) {
            productionLineMonitor.setId(
                    findByName(
                            productionLineMonitor.getWarehouseId(), productionLineMonitor.getName()
                    ).getId()
            );
        }
        return productionLineMonitorRepository.save(productionLineMonitor);
    }

    public ProductionLineMonitor addProductionLineMonitor(ProductionLineMonitor productionLineMonitor) {
        return saveOrUpdate(productionLineMonitor);
    }
    public ProductionLineMonitor changeProductionLineMonitor(ProductionLineMonitor productionLineMonitor) {
        return saveOrUpdate(productionLineMonitor);
    }
    public void removeProductionLineMonitor(ProductionLineMonitor productionLineMonitor) {
        productionLineMonitorRepository.delete(productionLineMonitor);
    }

    public void removeProductionLineMonitor(Long id) {
        productionLineMonitorRepository.deleteById(id);
    }


    public String processProductionLineMonitorHeartBeat(Long warehouseId, String productionLineMonitorName) {
        ProductionLineMonitor productionLineMonitor = findByName(warehouseId, productionLineMonitorName);
        if (Objects.nonNull(productionLineMonitor)) {
            productionLineMonitor.setLastHeartBeatTime(LocalDateTime.now().atZone(ZoneOffset.UTC));

            productionLineMonitor = saveOrUpdate(productionLineMonitor);
            logger.debug("heart beat for monitor {} at {}",
                    productionLineMonitor.getName(),
                    productionLineMonitor.getLastHeartBeatTime());
            return productionLineMonitorName + " heart beat!";
        }
        return "not able to find the monitor " + productionLineMonitorName;
    }
}

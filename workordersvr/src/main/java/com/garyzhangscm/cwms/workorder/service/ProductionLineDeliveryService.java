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
import com.garyzhangscm.cwms.workorder.exception.WorkOrderException;
import com.garyzhangscm.cwms.workorder.model.*;
import com.garyzhangscm.cwms.workorder.repository.ProductionLineDeliveryRepository;
import jakarta.persistence.criteria.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


@Service
public class ProductionLineDeliveryService {
    private static final Logger logger = LoggerFactory.getLogger(ProductionLineDeliveryService.class);

    @Autowired
    private ProductionLineDeliveryRepository productionLineDeliveryRepository;
    @Autowired
    WorkOrderLineService workOrderLineService;
    @Autowired
    ProductionLineService productionLineService;

    public ProductionLineDelivery findById(Long id ) {
        ProductionLineDelivery productionLineAssignment = productionLineDeliveryRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("production line delivery not found by id: " + id));
        return productionLineAssignment;
    }


    public List<ProductionLineDelivery> findAll(Long productionLineId,
                                                  Long workOrderLineId ) {
        return productionLineDeliveryRepository.findAll(
                        (Root<ProductionLineDelivery> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                            List<Predicate> predicates = new ArrayList<Predicate>();


                            if (Objects.nonNull(productionLineId)) {
                                Join<ProductionLineDelivery, ProductionLine> joinProductionLine = root.join("productionLine", JoinType.INNER);
                                predicates.add(criteriaBuilder.equal(joinProductionLine.get("id"), productionLineId));

                            }

                            if (Objects.nonNull(workOrderLineId)) {
                                Join<ProductionLineDelivery, WorkOrderLine> joinWorkOrderLine = root.join("workOrderLine", JoinType.INNER);
                                predicates.add(criteriaBuilder.equal(joinWorkOrderLine.get("id"), workOrderLineId));

                            }
                            Predicate[] p = new Predicate[predicates.size()];
                            return criteriaBuilder.and(predicates.toArray(p));
                        }
                );

    }

    public ProductionLineDelivery save(ProductionLineDelivery productionLineDelivery) {
        return productionLineDeliveryRepository.save(productionLineDelivery);
    }




    public ProductionLineDelivery getProductionLineDelivery(
            ProductionLine productionLine, WorkOrderLine workOrderLine
    ) {
        return productionLineDeliveryRepository.findByProductionLineAndWorkOrderLine(
                productionLine, workOrderLine);
    }



    public ProductionLineDelivery addConsumedQuantity(WorkOrderLine workOrderLine, ProductionLine productionLine,
                                                      Long consumedQuantity) {

        logger.debug("Will setup the consume quantity for {} / {}, production line {}, quantity: {}",
                workOrderLine.getWorkOrder().getNumber(),
                workOrderLine.getItem().getName(),
                productionLine.getName(),
                consumedQuantity);
        List<ProductionLineDelivery> productionLineDeliveries = findAll(
                productionLine.getId(), workOrderLine.getId()
        );

        if (productionLineDeliveries.size() == 0) {
            // WE DIDN'T find any deliver transaction yet,
            // let's raise error as we can't consume if there's no
            // delivery
            throw  WorkOrderException.raiseException(
                    "Can't consume the quantity for work order line " + workOrderLine.getWorkOrder().getNumber() +
                    " / " + workOrderLine.getItem().getName() +
                            " from Production line " + productionLine.getName() +
                            ". There's nothing delivered yet");
        }
        else {

            // Normally we should only get one record from the work order line
            // and production line
            ProductionLineDelivery productionLineDelivery = productionLineDeliveries.get(0);
            productionLineDelivery.setConsumedQuantity(
                    productionLineDelivery.getConsumedQuantity() + consumedQuantity
            );
            return save(productionLineDelivery);

        }
    }
    public ProductionLineDelivery addDeliveryQuantity(WorkOrderLine workOrderLine, ProductionLine productionLine,
                                    Long deliveredQuantity) {
        List<ProductionLineDelivery> productionLineDeliveries = findAll(
                productionLine.getId(), workOrderLine.getId()
        );
        if (productionLineDeliveries.size() == 0) {
            ProductionLineDelivery productionLineDelivery = new ProductionLineDelivery();
            productionLineDelivery.setProductionLine(productionLine);
            productionLineDelivery.setWorkOrderLine(workOrderLine);
            productionLineDelivery.setDeliveredQuantity(deliveredQuantity);
            productionLineDelivery.setConsumedQuantity(0L);
            return save(productionLineDelivery);
        }
        else {
            // Normally we should only get one record from the work order line
            // and production line
            ProductionLineDelivery productionLineDelivery = productionLineDeliveries.get(0);
            productionLineDelivery.setDeliveredQuantity(
                    productionLineDelivery.getDeliveredQuantity() + deliveredQuantity
            );
            return save(productionLineDelivery);

        }
    }
}

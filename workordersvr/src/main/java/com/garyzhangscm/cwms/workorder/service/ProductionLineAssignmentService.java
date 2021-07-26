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

import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.garyzhangscm.cwms.workorder.clients.InventoryServiceRestemplateClient;
import com.garyzhangscm.cwms.workorder.clients.WarehouseLayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.workorder.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.workorder.exception.WorkOrderException;
import com.garyzhangscm.cwms.workorder.model.*;
import com.garyzhangscm.cwms.workorder.repository.ProductionLineAssignmentRepository;
import com.garyzhangscm.cwms.workorder.repository.ProductionLineRepository;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.hibernate.jdbc.Work;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import javax.persistence.criteria.*;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;


@Service
public class ProductionLineAssignmentService   {
    private static final Logger logger = LoggerFactory.getLogger(ProductionLineAssignmentService.class);

    @Autowired
    private ProductionLineAssignmentRepository productionLineAssignmentRepository;

    @Autowired
    private ProductionLineDeliveryService productionLineDeliveryService;
    @Autowired
    WorkOrderService workOrderService;
    @Autowired
    ProductionLineService productionLineService;

    public ProductionLineAssignment findById(Long id ) {
        ProductionLineAssignment productionLineAssignment = productionLineAssignmentRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("production line assignment not found by id: " + id));
        return productionLineAssignment;
    }



    public List<ProductionLineAssignment> findAll(Long productionLineId,
                                                  String productionLineIds,
                                                  Long workOrderId ) {
        return productionLineAssignmentRepository.findAll(
                        (Root<ProductionLineAssignment> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                            List<Predicate> predicates = new ArrayList<Predicate>();


                            if (Objects.nonNull(productionLineId)) {
                                Join<ProductionLineAssignment, ProductionLine> joinProductionLine = root.join("productionLine", JoinType.INNER);
                                predicates.add(criteriaBuilder.equal(joinProductionLine.get("id"), productionLineId));
                            }
                            else if (Strings.isNotBlank(productionLineIds)) {
                                Join<ProductionLineAssignment, ProductionLine> joinProductionLine = root.join("productionLine", JoinType.INNER);
                                CriteriaBuilder.In<Long> inProductionLineIds = criteriaBuilder.in(joinProductionLine.get("id"));
                                for(String id : productionLineIds.split(",")) {
                                    inProductionLineIds.value(Long.parseLong(id));
                                }
                                predicates.add(criteriaBuilder.and(inProductionLineIds));

                            }

                            if (Objects.nonNull(workOrderId)) {
                                Join<ProductionLineAssignment, WorkOrder> joinWorkOrder = root.join("workOrder", JoinType.INNER);
                                predicates.add(criteriaBuilder.equal(joinWorkOrder.get("id"), workOrderId));

                            }
                            Predicate[] p = new Predicate[predicates.size()];
                            return criteriaBuilder.and(predicates.toArray(p));
                        }
                );

    }

    public ProductionLineAssignment findByWorkOrderAndProductionLine(Long productionLineId,
                                                                     Long workOrderId) {
        return productionLineAssignmentRepository.findByWorkOrderAndProductionLine(
                workOrderId, productionLineId
        );
    }




    public ProductionLineAssignment save(ProductionLineAssignment productionLineAssignment) {
        return productionLineAssignmentRepository.save(productionLineAssignment);
    }

    public ProductionLineAssignment saveOrUpdate(ProductionLineAssignment productionLineAssignment) {
        if (productionLineAssignment.getId() == null &&
                findByWorkOrderAndProductionLine(
                        productionLineAssignment.getWorkOrder().getId(),
                        productionLineAssignment.getProductionLine().getId()
                ) != null) {
            productionLineAssignment.setId(
                    findByWorkOrderAndProductionLine(
                            productionLineAssignment.getWorkOrder().getId(),
                            productionLineAssignment.getProductionLine().getId()
                    ).getId());
        }
        return save(productionLineAssignment);
    }


    public void delete(ProductionLineAssignment productionLineAssignment) {
        productionLineAssignmentRepository.delete(productionLineAssignment);
    }

    public void delete(Long id) {
        productionLineAssignmentRepository.deleteById(id);
    }

    public void removeProductionLineAssignmentForWorkOrder(Long workOrderId) {
        List<ProductionLineAssignment> productionLineAssignments = findAll(
                null, null, workOrderId
        );
        productionLineAssignments.forEach(productionLineAssignment -> {
            delete(productionLineAssignment);
        });
    }

    public List<ProductionLineAssignment> assignWorkOrderToProductionLines(
            Long workOrderId, List<ProductionLineAssignment> productionLineAssignments) {

        WorkOrder workOrder = workOrderService.findById(workOrderId);

        for (ProductionLineAssignment productionLineAssignment : productionLineAssignments) {

            assignWorkOrderToProductionLines(workOrder,productionLineAssignment);

        }

        return findAll(null, null, workOrderId);

    }
    public List<ProductionLineAssignment> assignWorkOrderToProductionLines(Long workOrderId, String productionLineIds, String quantities) {
        // remove the assignment for the work order first

        removeProductionLineAssignmentForWorkOrder(workOrderId);

        String[] productionLineIdArray = productionLineIds.split(",");
        String[] quantityArray = quantities.split(",");

        if (productionLineIdArray.length == 0 ||
                productionLineIdArray.length != quantityArray.length) {
            throw WorkOrderException.raiseException("Can't assign production lines to the work order");
        }

        WorkOrder workOrder = workOrderService.findById(workOrderId);
        // let's make sure the total quantity matches with the work order quantity
        Long totalQuantity = Arrays.stream(quantityArray).mapToLong(Long::parseLong).sum();

        if (workOrder.getExpectedQuantity() != totalQuantity) {

            throw WorkOrderException.raiseException("Can't assign production lines to the work order, total quantity doesn't match with work order's quantity");
        }

        for (int i = 0; i < productionLineIdArray.length; i++) {
            Long productionLineId = Long.parseLong(productionLineIdArray[i]);
            Long quantity = Long.parseLong(quantityArray[i]);

            assignWorkOrderToProductionLines(workOrder,
                    productionLineService.findById(productionLineId)
                    , quantity);

        }

        return findAll(null, null, workOrderId);
    }




    public void assignWorkOrderToProductionLines(WorkOrder workOrder, ProductionLine productionLine, Long quantity) {
        ProductionLineAssignment productionLineAssignment = new ProductionLineAssignment(
                workOrder,
                productionLine,
                quantity
        );
        saveOrUpdate(productionLineAssignment);
    }


    public void assignWorkOrderToProductionLines(WorkOrder workOrder, ProductionLineAssignment productionLineAssignment) {
        productionLineAssignment.setWorkOrder(workOrder);
        // set the open quantity to the total quantity of the assignment
        // we will use this quantity to keep track of how much quantity we can still
        // allocate from the work order on this production line. When this number
        // become 0, it means we have fully allocated this work order
        productionLineAssignment.setOpenQuantity(productionLineAssignment.getQuantity());
        logger.debug("Save production line assignment\n{}",
                productionLineAssignment);
        saveOrUpdate(productionLineAssignment);
    }

    public List<WorkOrder> getAssignedWorkOrderByProductionLine(Long productionLineId) {
        List<ProductionLineAssignment> productionLineAssignments =
                findAll(productionLineId, null, null);

        return productionLineAssignments.stream().map(
                productionLineAssignment -> productionLineAssignment.getWorkOrder()
        ).map(workOrder -> {
            workOrderService.loadAttribute(workOrder);
            return workOrder;
        }).collect(Collectors.toList());
    }

}

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

import com.garyzhangscm.cwms.workorder.clients.InventoryServiceRestemplateClient;
import com.garyzhangscm.cwms.workorder.clients.ResourceServiceRestemplateClient;
import com.garyzhangscm.cwms.workorder.clients.WarehouseLayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.workorder.exception.ProductionLineException;
import com.garyzhangscm.cwms.workorder.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.workorder.model.*;
import com.garyzhangscm.cwms.workorder.repository.ProductionLineActivityRepository;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.parameters.P;
import org.springframework.stereotype.Service;

import javax.persistence.criteria.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;


@Service
public class ProductionLineActivityService {
    private static final Logger logger = LoggerFactory.getLogger(ProductionLineActivityService.class);

    @Autowired
    private ProductionLineActivityRepository productionLineActivityRepository;
    @Autowired
    private WorkOrderService workOrderService;
    @Autowired
    private ProductionLineService productionLineService;

    @Autowired
    private WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient;
    @Autowired
    private ResourceServiceRestemplateClient resourceServiceRestemplateClient;

    public ProductionLineActivity findById(Long id, boolean loadDetails) {
        ProductionLineActivity productionLineActivity = productionLineActivityRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("production line activity can't be found by id: " + id));
        if (loadDetails) {
            loadAttribute(productionLineActivity);
        }
        return productionLineActivity;
    }

    public ProductionLineActivity findById(Long id) {
        return findById(id, true);
    }


    public List<ProductionLineActivity> findAll(Long warehouseId,
                                                String workOrderNumber, Long workOrderId,
                                                String productionLineName, Long productionLineId,
                                                String username,
                                                String type,
                                                String transactionTimeStart, String transactionTimeEnd,
                                                boolean loadDetails) {
        List<ProductionLineActivity> productionLineActivities =  productionLineActivityRepository.findAll(
                (Root<ProductionLineActivity> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();

                    predicates.add(criteriaBuilder.equal(root.get("warehouseId"), warehouseId));

                    if (StringUtils.isNotBlank(workOrderNumber) || Objects.nonNull(workOrderId)) {
                        Join<ProductionLineActivity, WorkOrder> joinWorkOrder = root.join("workOrder", JoinType.INNER);
                        if (StringUtils.isNotBlank(workOrderNumber)) {
                            predicates.add(criteriaBuilder.equal(joinWorkOrder.get("number"), workOrderNumber));
                        }
                        if (Objects.nonNull(workOrderId)) {
                            predicates.add(criteriaBuilder.equal(joinWorkOrder.get("id"), workOrderId));
                        }
                    }

                    if (StringUtils.isNotBlank(productionLineName) || Objects.nonNull(productionLineId)) {
                        Join<ProductionLineActivity, ProductionLine> joinProductionLine = root.join("productionLine", JoinType.INNER);
                        if (StringUtils.isNotBlank(productionLineName)) {
                            predicates.add(criteriaBuilder.equal(joinProductionLine.get("name"), productionLineName));
                        }
                        if (Objects.nonNull(productionLineId)) {
                            predicates.add(criteriaBuilder.equal(joinProductionLine.get("id"), productionLineId));
                        }
                    }

                    if (StringUtils.isNotBlank(username)) {

                        predicates.add(criteriaBuilder.equal(root.get("username"), username));
                    }
                    if (StringUtils.isNotBlank(type)) {

                        predicates.add(criteriaBuilder.equal(root.get("type"), ProductionLineActivityType.valueOf(type)));
                    }
                    if (StringUtils.isNotBlank(transactionTimeStart)) {
                        LocalDateTime dateTime = LocalDateTime.parse(transactionTimeStart);
                        predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("transactionTime"), dateTime));
                    }
                    if (StringUtils.isNotBlank(transactionTimeEnd)) {
                        LocalDateTime dateTime = LocalDateTime.parse(transactionTimeEnd);
                        predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("transactionTime"), dateTime));
                    }
                    Predicate[] p = new Predicate[predicates.size()];
                    return criteriaBuilder.and(predicates.toArray(p));
                }
        );


        if (productionLineActivities.size() > 0 && loadDetails) {
            loadAttribute(productionLineActivities);
        }
        return productionLineActivities;
    }

    public List<ProductionLineActivity> findAll(Long warehouseId,
                                                String workOrderNumber, Long workOrderId,
                                                String productionLineName, Long productionLineId,
                                                String username,
                                                String type,
                                                String transactionTimeStart, String transactionTimeEnd) {
        return findAll(warehouseId, workOrderNumber, workOrderId, productionLineName, productionLineId,
                username, type, transactionTimeStart, transactionTimeEnd);
    }



    public void loadAttribute(List<ProductionLineActivity> productionLineActivities) {
        for (ProductionLineActivity productionLineActivity : productionLineActivities) {
            loadAttribute(productionLineActivity);
        }
    }

    public void loadAttribute(ProductionLineActivity productionLineActivity) {
        // Load the details for client and supplier informaiton
        Warehouse warehouse = warehouseLayoutServiceRestemplateClient.getWarehouseById(
                productionLineActivity.getWarehouseId()
        );

        User user = resourceServiceRestemplateClient.getUserByUsername(
                warehouse.getCompanyId(),
                productionLineActivity.getUsername()
        );
        productionLineActivity.setUser(user);

    }


    public ProductionLineActivity save(ProductionLineActivity productionLineActivity) {
        ProductionLineActivity newProductionLineActivity = productionLineActivityRepository.save(productionLineActivity);
        loadAttribute(newProductionLineActivity);
        return newProductionLineActivity;
    }


    public void delete(ProductionLineActivity productionLineActivity) {
        productionLineActivityRepository.delete(productionLineActivity);
    }

    public void delete(Long id) {
        productionLineActivityRepository.deleteById(id);
    }

    public ProductionLineActivity addProductionLineActivity(ProductionLineActivity productionLineActivity) {
        return save(productionLineActivity);
    }

    public ProductionLineActivity productionLineCheckIn(
            Long warehouseId, Long workOrderId,
            Long productionLineId, String username, Integer workingTeamMemberCount) {

        // make sure the user has not checked in this production line yet

        ProductionLine productionLine =
                productionLineService.findById(productionLineId);

        logger.debug("Start to check in user {} into production line {}",
                username, productionLine.getName());

        ProductionLineActivity lastCheckInActivity =
                getLastCheckInActivity(productionLine, username);
        if (Objects.nonNull(lastCheckInActivity)) {
            // OK, the user  checked in before, let's make
            // sure the user has already checked out
            ProductionLineActivity lastCheckOutActivity =
                    getLastCheckOutActivity(productionLine, username);

            logger.debug("last check in activity date: {}",
                    lastCheckInActivity.getTransactionTime());
            logger.debug("last check out activity date: {}",
                    Objects.isNull(lastCheckOutActivity) ? "" : lastCheckOutActivity.getTransactionTime());

            if (Objects.isNull(lastCheckOutActivity) ||
                    lastCheckOutActivity.getTransactionTime().isBefore(
                    lastCheckInActivity.getTransactionTime()
            )) {
                // the user's last checkout transaction is before the last check in
                // transaction, so that we know the user is still working on the production.
                // so there's no need to check in again

                throw ProductionLineException.raiseException("can't check in the user, as the user is already checked in");
            }

        }

        return save(generateproductionLineActivity(
                warehouseId, workOrderId,
                productionLineId, username, workingTeamMemberCount,
                ProductionLineActivityType.USER_CHECK_IN
        ));
    }
    public ProductionLineActivity productionLineCheckOut(
            Long warehouseId, Long workOrderId,
            Long productionLineId, String username, Integer workingTeamMemberCount) {

        // make sure the user has already checked in the production line and
        // has not checked out yet

        ProductionLine productionLine =
                productionLineService.findById(productionLineId);

        logger.debug("Start to check out user {} into production line {}",
                username, productionLine.getName());

        ProductionLineActivity lastCheckInActivity =
                getLastCheckInActivity(productionLine, username);
        if (Objects.isNull(lastCheckInActivity)) {
            // the user hasn't check in yet

            logger.debug("The user hasn't check in yet");
            throw ProductionLineException.raiseException("can't check out the user, as the user is not checked in yet");
        }
        else {
            // OK, the user  checked in before, let's make
            // sure the user has already checked out
            ProductionLineActivity lastCheckOutActivity =
                    getLastCheckOutActivity(productionLine, username);


            logger.debug("last check in activity date: {}",
                    lastCheckInActivity.getTransactionTime());
            logger.debug("last check out activity date: {}",
                    Objects.isNull(lastCheckOutActivity) ? "" : lastCheckOutActivity.getTransactionTime());

            if (Objects.nonNull(lastCheckOutActivity) &&
                    lastCheckOutActivity.getTransactionTime().isAfter(
                    lastCheckInActivity.getTransactionTime()
            )) {
                // the user already checked in, but there's checkout transaction
                // after the last check in transaction. So we know that the
                // user is already checked out

                throw ProductionLineException.raiseException("can't check out the user, as the user is already check out");
            }

        }

        return save(generateproductionLineActivity(
                warehouseId, workOrderId,
                productionLineId, username, workingTeamMemberCount,
                ProductionLineActivityType.USER_CHECK_OUT
        ));
    }

    public ProductionLineActivity generateproductionLineActivity(
            Long warehouseId, Long workOrderId,
            Long productionLineId, String username, Integer workingTeamMemberCount,
            ProductionLineActivityType type) {
        WorkOrder workOrder = workOrderService.findById(workOrderId);
        ProductionLine productionLine = productionLineService.findById(productionLineId);
        ProductionLineActivity productionLineActivity = new ProductionLineActivity(
                warehouseId, workOrder, productionLine,
                username, workingTeamMemberCount, type

        );
        return productionLineActivity;
    }


    public ProductionLineActivity getLastCheckInActivity(ProductionLine productionLine) {


        return getLastActivity(productionLine, ProductionLineActivityType.USER_CHECK_IN);
    }
    public ProductionLineActivity getLastCheckOutActivity(ProductionLine productionLine) {


        return getLastActivity(productionLine, ProductionLineActivityType.USER_CHECK_OUT);
    }


    public ProductionLineActivity getLastActivity(ProductionLine productionLine,
                                                  ProductionLineActivityType type) {

        return productionLineActivityRepository.findTop1ByProductionLineAndTypeOrderByTransactionTimeDesc(
                productionLine, type
        );

    }
    public ProductionLineActivity getLastCheckInActivity(ProductionLine productionLine,
                                                         String username) {


        return getLastActivity(productionLine, ProductionLineActivityType.USER_CHECK_IN, username);
    }
    public ProductionLineActivity getLastCheckOutActivity(ProductionLine productionLine,
                                                          String username) {


        return getLastActivity(productionLine, ProductionLineActivityType.USER_CHECK_OUT, username);
    }


    public ProductionLineActivity getLastActivity(ProductionLine productionLine,
                                                  ProductionLineActivityType type,
                                                  String username) {

        return productionLineActivityRepository.findTop1ByProductionLineAndUsernameAndTypeOrderByTransactionTimeDesc(
                productionLine, username, type
        );

    }


}

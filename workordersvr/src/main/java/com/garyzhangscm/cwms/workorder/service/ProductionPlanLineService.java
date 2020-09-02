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
import com.garyzhangscm.cwms.workorder.clients.CommonServiceRestemplateClient;
import com.garyzhangscm.cwms.workorder.clients.InventoryServiceRestemplateClient;
import com.garyzhangscm.cwms.workorder.clients.OutboundServiceRestemplateClient;
import com.garyzhangscm.cwms.workorder.clients.WarehouseLayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.workorder.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.workorder.model.*;

import com.garyzhangscm.cwms.workorder.repository.ProductionPlanLineRepository;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import javax.persistence.criteria.*;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


@Service
public class ProductionPlanLineService implements TestDataInitiableService {
    private static final Logger logger = LoggerFactory.getLogger(ProductionPlanLineService.class);

    @Autowired
    private ProductionPlanLineRepository productionPlanLineRepository;
    @Autowired
    private ProductionPlanService productionPlanService;
    @Autowired
    private WorkOrderService workOrderService;
    @Autowired
    private BillOfMaterialService billOfMaterialService;

    @Autowired
    private CommonServiceRestemplateClient commonServiceRestemplateClient;
    @Autowired
    private WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient;
    @Autowired
    private InventoryServiceRestemplateClient inventoryServiceRestemplateClient;
    @Autowired
    private OutboundServiceRestemplateClient outboundServiceRestemplateClient;
    @Autowired
    private FileService fileService;
    @Autowired
    private IntegrationService integrationService;

    @Value("${fileupload.test-data.production-plan-line:production-plan-line}")
    String testDataFile;

    public ProductionPlanLine findById(Long id, boolean loadDetails) {
        ProductionPlanLine productionPlanLine = productionPlanLineRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("production plan line not found by id: " + id));
        if (loadDetails) {
            loadAttribute(productionPlanLine);
        }
        return productionPlanLine;
    }

    public ProductionPlanLine findById(Long id) {
        return findById(id, true);
    }


    public List<ProductionPlanLine> findAll(Long warehouseId,
                                            String productionPlannumber,
                                            String itemName, boolean loadDetails) {

        List<ProductionPlanLine> productionPlanLines =  productionPlanLineRepository.findAll(
                (Root<ProductionPlanLine> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();

                    if (Objects.nonNull(warehouseId) ||
                            StringUtils.isNotBlank(productionPlannumber)) {
                        Join<ProductionPlanLine, ProductionPlan> joinProductionPlan = root.join("productionPlan", JoinType.INNER);

                        if (Objects.nonNull(warehouseId)) {

                            predicates.add(criteriaBuilder.equal(joinProductionPlan.get("warehouseId"), warehouseId));
                        }
                        if (StringUtils.isNotBlank(productionPlannumber)) {

                            predicates.add(criteriaBuilder.equal(joinProductionPlan.get("number"), productionPlannumber));
                        }
                    }


                    if (!StringUtils.isBlank(itemName)) {
                        Item item = inventoryServiceRestemplateClient.getItemByName(warehouseId, itemName);
                        if (item != null) {
                            predicates.add(criteriaBuilder.equal(root.get("itemId"), item.getId()));
                        }
                        else {
                            // The client passed in an invalid item name, let's return nothing
                            predicates.add(criteriaBuilder.equal(root.get("itemId"), -1L));
                        }
                    }
                    Predicate[] p = new Predicate[predicates.size()];
                    return criteriaBuilder.and(predicates.toArray(p));
                }
        );

        if (productionPlanLines.size() > 0 && loadDetails) {
            loadAttribute(productionPlanLines);
        }
        return productionPlanLines;
    }

    public List<ProductionPlanLine> findAll(Long warehouseId, String productionPlannumber, String itemName) {
        return findAll(warehouseId, productionPlannumber, itemName, true);
    }




    public void loadAttribute(List<ProductionPlanLine> productionPlanLines) {
        for (ProductionPlanLine productionPlanLine : productionPlanLines) {
            loadAttribute(productionPlanLine);
        }
    }

    public void loadAttribute(ProductionPlanLine productionPlanLine) {

        if (productionPlanLine.getItemId() != null && productionPlanLine.getItem() == null) {
            productionPlanLine.setItem(inventoryServiceRestemplateClient.getItemById(productionPlanLine.getItemId()));
        }
        if (productionPlanLine.getWarehouseId() != null && productionPlanLine.getWarehouse() == null) {
            productionPlanLine.setWarehouse(warehouseLayoutServiceRestemplateClient.getWarehouseById(productionPlanLine.getWarehouseId()));
        }
        if (productionPlanLine.getOrderLineId() != null && productionPlanLine.getOrderLine() == null) {
            productionPlanLine.setOrderLine(
                    outboundServiceRestemplateClient.getOrderLineById(productionPlanLine.getOrderLineId()));
        }

        if (productionPlanLine.getInventoryStatusId() != null && productionPlanLine.getInventoryStatus() == null) {
            productionPlanLine.setInventoryStatus(
                    inventoryServiceRestemplateClient.getInventoryStatusById(productionPlanLine.getInventoryStatusId()));
        }


    }

    public ProductionPlanLine findByNatrualKey(Long warehouseId, String productionPlanNumber, String itemName) {
        List<ProductionPlanLine> productionPlanLines = findAll(
                warehouseId, productionPlanNumber, itemName);
        if (productionPlanLines.size() > 0) {
            // if we are able to find something, there should be only one record
            return productionPlanLines.get(0);
        }
        return null;
    }


    public ProductionPlanLine save(ProductionPlanLine productionPlanLine) {
        ProductionPlanLine newProductionPlanLine = productionPlanLineRepository.save(productionPlanLine);
        loadAttribute(newProductionPlanLine);
        return newProductionPlanLine;
    }

    public ProductionPlanLine saveOrUpdate(ProductionPlanLine productionPlanLine) {

        if (productionPlanLine.getId() == null &&
                findByNatrualKey(productionPlanLine.getWarehouseId(),
                        productionPlanLine.getProductionPlan().getNumber(),
                        productionPlanLine.getItem().getName()) != null) {
            productionPlanLine.setId(
                    findByNatrualKey(productionPlanLine.getWarehouseId(),
                            productionPlanLine.getProductionPlan().getNumber(),
                            productionPlanLine.getItem().getName()).getId());
        }
        return save(productionPlanLine);
    }


    public void delete(ProductionPlanLine productionPlanLine) {
        productionPlanLineRepository.delete(productionPlanLine);
    }

    public void delete(Long id) {
        productionPlanLineRepository.deleteById(id);
    }


    public List<ProductionPlanLineCSVWrapper> loadData(InputStream inputStream) throws IOException {

        CsvSchema schema = CsvSchema.builder().
                addColumn("warehouse").
                addColumn("productionPlan").
                addColumn("billOfMaterial").
                addColumn("item").
                addColumn("expectedQuantity").
                addColumn("inventoryStatus").
                build().withHeader();

        return fileService.loadData(inputStream, schema, ProductionPlanLineCSVWrapper.class);
    }

    public void initTestData(String warehouseName) {
        try {
            String testDataFileName = StringUtils.isBlank(warehouseName) ?
                    testDataFile + ".csv" :
                    testDataFile + "-" + warehouseName + ".csv";
            InputStream inputStream = new ClassPathResource(testDataFileName).getInputStream();
            List<ProductionPlanLineCSVWrapper> productionPlanLineCSVWrappers = loadData(inputStream);
            productionPlanLineCSVWrappers.stream().forEach(productionPlanLineCSVWrapper -> saveOrUpdate(convertFromWrapper(productionPlanLineCSVWrapper)));
        } catch (IOException ex) {
            logger.debug("Exception while load test data: {}", ex.getMessage());
        }
    }

    private ProductionPlanLine convertFromWrapper(ProductionPlanLineCSVWrapper productionPlanLineCSVWrapper) {

        ProductionPlanLine productionPlanLine = new ProductionPlanLine();

        productionPlanLine.setExpectedQuantity(productionPlanLineCSVWrapper.getExpectedQuantity());
        productionPlanLine.setInprocessQuantity(0L);
        productionPlanLine.setProducedQuantity(0L);


        Warehouse warehouse = warehouseLayoutServiceRestemplateClient.getWarehouseByName(
                productionPlanLineCSVWrapper.getWarehouse()
        );
        productionPlanLine.setWarehouseId(warehouse.getId());
        productionPlanLine.setWarehouse(warehouse);

        Item item = inventoryServiceRestemplateClient.getItemByName(
                warehouse.getId(), productionPlanLineCSVWrapper.getItem());
        productionPlanLine.setItemId(item.getId());
        productionPlanLine.setItem(item);



        productionPlanLine.setBillOfMaterial(
                billOfMaterialService.findByNumber(
                        warehouse.getId(),
                        productionPlanLineCSVWrapper.getBillOfMaterial()
                )
        );

        productionPlanLine.setProductionPlan(
                productionPlanService.findByNumber(
                        warehouse.getId(),
                        productionPlanLineCSVWrapper.getProductionPlan()
                )
        );
        productionPlanLine.setInventoryStatusId(
                inventoryServiceRestemplateClient.getInventoryStatusByName(
                        warehouse.getId(), productionPlanLineCSVWrapper.getInventoryStatus()).getId()
        );

        return productionPlanLine;
    }

    public WorkOrder createWorkOrder(Long productionPlanLineId, String workOrderNumber,
                                     Long quantity, Long productionLineId) {

        return createWorkOrder(findById(productionPlanLineId),
                workOrderNumber, quantity, productionLineId);
    }
    public WorkOrder createWorkOrder(ProductionPlanLine productionPlanLine, String workOrderNumber,
                                     Long quantity, Long productionLineId) {

        if (StringUtils.isBlank(workOrderNumber)) {
            workOrderNumber =  commonServiceRestemplateClient.getNextWorkOrderNumber();
        }
        WorkOrder workOrder = workOrderService.createWorkOrderFromProductionPlanLine(
                productionPlanLine,
                workOrderNumber, quantity, productionLineId
        );
        // update the production line plan
        productionPlanLine.setInprocessQuantity(productionPlanLine.getInprocessQuantity() + quantity);
        save(productionPlanLine);
        return workOrder;
    }

    public ProductionPlanLine addProductionPlanLine(ProductionPlanLine productionPlanLine) {
        ProductionPlanLine newProductionPlanLine = save(productionPlanLine);
        registerNewProductionLine(newProductionPlanLine);
        return newProductionPlanLine;
    }

    /**
     * When we complete a work order with a production plan, let's
     * calculate the quantity
     * @param workOrder
     * @return
     */
    public ProductionPlanLine registerWorkOrderComplete(WorkOrder workOrder) {

        if (Objects.nonNull(workOrder.getProductionPlanLine())) {
            ProductionPlanLine productionPlanLine = findById(workOrder.getProductionPlanLine().getId());
            // the work order may produce more than planned
            if (productionPlanLine.getInprocessQuantity() <  workOrder.getProducedQuantity()) {
                productionPlanLine.setInprocessQuantity(0L);
            }
            else {

                productionPlanLine.setInprocessQuantity(
                        productionPlanLine.getInprocessQuantity() - workOrder.getProducedQuantity()
                );
            }
            productionPlanLine.setProducedQuantity(
                    productionPlanLine.getProducedQuantity() + workOrder.getProducedQuantity()
            );
            productionPlanLine = saveOrUpdate(productionPlanLine);

            if (Objects.nonNull(productionPlanLine.getOrderLineId())) {

                outboundServiceRestemplateClient.registerProductionPlanLineProduced(
                        productionPlanLine.getOrderLineId(),
                        workOrder.getProducedQuantity()
                );
            }
            return productionPlanLine;
        }
        return  null;
    }


    /**
     * When we create a new production line, we will update the order line's quantity
     * if there's a order line attached to this production line
     * @param productionPlanLine
     */
    public void registerNewProductionLine(ProductionPlanLine productionPlanLine) {
        if (Objects.nonNull(productionPlanLine.getOrderLineId())) {

            outboundServiceRestemplateClient.registerProductionPlanLine(
                    productionPlanLine.getOrderLineId(),
                    productionPlanLine
            );
        }
    }
}

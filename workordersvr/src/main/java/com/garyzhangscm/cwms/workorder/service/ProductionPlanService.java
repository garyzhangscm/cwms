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
import com.garyzhangscm.cwms.workorder.exception.WorkOrderException;
import com.garyzhangscm.cwms.workorder.model.*;
import com.garyzhangscm.cwms.workorder.repository.ProductionPlanRepository;
import com.garyzhangscm.cwms.workorder.repository.WorkOrderRepository;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;

import javax.persistence.criteria.*;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;


@Service
public class ProductionPlanService implements TestDataInitiableService {
    private static final Logger logger = LoggerFactory.getLogger(ProductionPlanService.class);

    @Autowired
    private ProductionPlanRepository productionPlanRepository;
    @Autowired
    private ProductionPlanLineService productionPlanLineService;
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

    @Value("${fileupload.test-data.production-plan:production-plan}")
    String testDataFile;

    public ProductionPlan findById(Long id, boolean loadDetails) {
        ProductionPlan productionPlan = productionPlanRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("production plan not found by id: " + id));
        if (loadDetails) {
            loadAttribute(productionPlan);
        }
        return productionPlan;
    }

    public ProductionPlan findById(Long id) {
        return findById(id, true);
    }


    public List<ProductionPlan> findAll(Long warehouseId, String number, String itemName, boolean loadDetails) {

        List<ProductionPlan> productionPlans =  productionPlanRepository.findAll(
                (Root<ProductionPlan> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();

                    predicates.add(criteriaBuilder.equal(root.get("warehouseId"), warehouseId));

                    if (!StringUtils.isBlank(number)) {
                        predicates.add(criteriaBuilder.equal(root.get("number"), number));

                    }
                    if (!StringUtils.isBlank(itemName)) {
                        Item item = inventoryServiceRestemplateClient.getItemByName(warehouseId, itemName);
                        if (item != null) {
                            Join<ProductionPlan, ProductionPlanLine> joinProductionPlanLine =
                                    root.join("productionPlanLines", JoinType.INNER);
                            predicates.add(criteriaBuilder.equal(joinProductionPlanLine.get("itemId"), item.getId()));
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

        if (productionPlans.size() > 0 && loadDetails) {
            loadAttribute(productionPlans);
        }
        return productionPlans;
    }

    public List<ProductionPlan> findAll(Long warehouseId, String number, String itemName) {
        return findAll(warehouseId, number, itemName, true);
    }


    public ProductionPlan findByNumber(Long warehouseId, String number, boolean loadDetails) {
        ProductionPlan productionPlan = productionPlanRepository.findByWarehouseIdAndNumber(warehouseId, number);
        if (productionPlan != null && loadDetails) {
            loadAttribute(productionPlan);
        }
        return productionPlan;
    }

    public ProductionPlan findByNumber(Long warehouseId,String number) {
        return findByNumber(warehouseId, number, true);
    }


    public void loadAttribute(List<ProductionPlan> productionPlans) {
        for (ProductionPlan productionPlan : productionPlans) {
            loadAttribute(productionPlan);
        }
    }

    public void loadAttribute(ProductionPlan productionPlan) {

        if (productionPlan.getWarehouseId() != null && productionPlan.getWarehouse() == null) {
            productionPlan.setWarehouse(warehouseLayoutServiceRestemplateClient.getWarehouseById(productionPlan.getWarehouseId()));
        }
        productionPlan.getProductionPlanLines().forEach(productionPlanLine -> {
            productionPlanLine.setProductionPlan(productionPlan);
            productionPlanLineService.loadAttribute(productionPlanLine);
        });


    }



    public ProductionPlan save(ProductionPlan productionPlan) {
        ProductionPlan newProductionPlan = productionPlanRepository.save(productionPlan);
        loadAttribute(newProductionPlan);
        return newProductionPlan;
    }

    public ProductionPlan saveOrUpdate(ProductionPlan productionPlan) {
        if (productionPlan.getId() == null && findByNumber(productionPlan.getWarehouseId(), productionPlan.getNumber()) != null) {
            productionPlan.setId(
                    findByNumber(productionPlan.getWarehouseId(), productionPlan.getNumber()).getId());
        }
        return save(productionPlan);
    }


    public void delete(ProductionPlan productionPlan) {
        productionPlanRepository.delete(productionPlan);
    }

    public void delete(Long id) {
        productionPlanRepository.deleteById(id);
    }


    public List<ProductionPlanCSVWrapper> loadData(InputStream inputStream) throws IOException {

        CsvSchema schema = CsvSchema.builder().
                addColumn("company").
                addColumn("warehouse").
                addColumn("number").
                addColumn("description").
                build().withHeader();

        return fileService.loadData(inputStream, schema, ProductionPlanCSVWrapper.class);
    }

    public void initTestData(String warehouseName) {
        try {
            String testDataFileName = StringUtils.isBlank(warehouseName) ?
                    testDataFile + ".csv" :
                    testDataFile + "-" + warehouseName + ".csv";
            InputStream inputStream = new ClassPathResource(testDataFileName).getInputStream();
            List<ProductionPlanCSVWrapper> productionPlanCSVWrappers = loadData(inputStream);
            productionPlanCSVWrappers.stream().forEach(productionPlanCSVWrapper -> saveOrUpdate(convertFromWrapper(productionPlanCSVWrapper)));
        } catch (IOException ex) {
            logger.debug("Exception while load test data: {}", ex.getMessage());
        }
    }

    private ProductionPlan convertFromWrapper(ProductionPlanCSVWrapper productionPlanCSVWrappers) {

        ProductionPlan productionPlan = new ProductionPlan();

        productionPlan.setNumber(productionPlanCSVWrappers.getNumber());
        productionPlan.setDescription(productionPlanCSVWrappers.getDescription());



        Warehouse warehouse = warehouseLayoutServiceRestemplateClient.getWarehouseByName(
                productionPlanCSVWrappers.getCompany(),
                productionPlanCSVWrappers.getWarehouse()
        );
        productionPlan.setWarehouseId(warehouse.getId());


        return productionPlan;
    }


    public ProductionPlan addProductionPlan(ProductionPlan productionPlan) {
        productionPlan.getProductionPlanLines().forEach(productionPlanLine -> productionPlanLine.setProductionPlan(productionPlan));
        ProductionPlan newProductionPlan =  save(productionPlan);

        // update the order line quantity if the production line is
        // attached to a order line
        newProductionPlan.getProductionPlanLines().forEach(productionPlanLine -> productionPlanLineService.registerNewProductionLine(productionPlanLine));

        return newProductionPlan;
    }

    public String validateNewNumber(Long warehouseId, String number) {
        ProductionPlan productionPlan =
                findByNumber(warehouseId, number, false);

        return Objects.isNull(productionPlan) ? "" : ValidatorResult.VALUE_ALREADY_EXISTS.name();
    }
}

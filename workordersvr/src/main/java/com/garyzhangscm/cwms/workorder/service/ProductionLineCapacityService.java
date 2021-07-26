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
import com.garyzhangscm.cwms.workorder.clients.WarehouseLayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.workorder.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.workorder.model.*;
import com.garyzhangscm.cwms.workorder.repository.ProductionLineCapacityRepository;
import com.garyzhangscm.cwms.workorder.repository.ProductionLineRepository;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.util.Strings;
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
public class ProductionLineCapacityService implements TestDataInitiableService {
    private static final Logger logger = LoggerFactory.getLogger(ProductionLineCapacityService.class);

    @Autowired
    private ProductionLineCapacityRepository productionLineCapacityRepository;
    @Autowired
    private ProductionLineService productionLineService;
    @Autowired
    private MouldService mouldService;

    @Autowired
    private WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient;
    @Autowired
    private InventoryServiceRestemplateClient inventoryServiceRestemplateClient;
    @Autowired
    private CommonServiceRestemplateClient commonServiceRestemplateClient;
    @Autowired
    private FileService fileService;

    @Value("${fileupload.test-data.production-line-capacity:production-line-capacity}")
    String testDataFile;

    public ProductionLineCapacity findById(Long id, boolean loadDetails) {
        ProductionLineCapacity productionLineCapacity = productionLineCapacityRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("production line capacity not found by id: " + id));
        if (loadDetails) {
            loadAttribute(productionLineCapacity);
        }
        return productionLineCapacity;
    }

    public ProductionLineCapacity findById(Long id) {
        return findById(id, true);
    }


    public List<ProductionLineCapacity> findAll(Long warehouseId,
                                                Long productionLineId,
                                                String productionLineIds,
                                                Long itemId,
                                                boolean loadDetails) {
        List<ProductionLineCapacity> productionLineCapacities
                =  productionLineCapacityRepository.findAll(
                (Root<ProductionLineCapacity> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();

                    predicates.add(criteriaBuilder.equal(root.get("warehouseId"), warehouseId));

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
                    if (Objects.nonNull(itemId)) {

                        predicates.add(criteriaBuilder.equal(root.get("itemId"), itemId));
                    }
                    Predicate[] p = new Predicate[predicates.size()];
                    return criteriaBuilder.and(predicates.toArray(p));
                }
        );


        if (productionLineCapacities.size() > 0 && loadDetails) {
            loadAttribute(productionLineCapacities);
        }
        return productionLineCapacities;
    }

    public List<ProductionLineCapacity> findAll(Long warehouseId,
                                        Long productionLineId,
                                        String productionLineIds,
                                        Long itemId) {
        return findAll(warehouseId, productionLineId, productionLineIds, itemId, true);
    }

    public ProductionLineCapacity findByProductionLineAndItem(Long warehouseId,
                                                              Long productionLineId,
                                                              Long itemId) {
        return findByProductionLineAndItem(warehouseId, productionLineId, itemId, true);

    }
    public ProductionLineCapacity findByProductionLineAndItem(Long warehouseId,
                                                              Long productionLineId,
                                                              Long itemId,
                                                              boolean loadDetails) {
        // we should only have at maximum one record for production + item combination
        List<ProductionLineCapacity> productionLineCapacities =
                findAll(warehouseId, productionLineId, null, itemId, loadDetails);
        if (productionLineCapacities.size() > 0) {
            return productionLineCapacities.get(0);
        }
        else {
            return null;
        }

    }

    public void loadAttribute(List<ProductionLineCapacity> productionLineCapacities) {
        for (ProductionLineCapacity productionLineCapacity : productionLineCapacities) {
            loadAttribute(productionLineCapacity);
        }
    }

    public void loadAttribute(ProductionLineCapacity productionLineCapacity) {

        if (productionLineCapacity.getWarehouseId() != null && productionLineCapacity.getWarehouse() == null) {
            productionLineCapacity.setWarehouse(
                    warehouseLayoutServiceRestemplateClient.getWarehouseById(productionLineCapacity.getWarehouseId()));
        }
        if (productionLineCapacity.getItemId() != null && productionLineCapacity.getItem() == null) {

            productionLineCapacity.setItem(
                    inventoryServiceRestemplateClient.getItemById(
                            productionLineCapacity.getItemId()
                    )
            );
        }
        if (productionLineCapacity.getUnitOfMeasureId() != null && productionLineCapacity.getUnitOfMeasure() == null) {

            productionLineCapacity.setUnitOfMeasure(
                    commonServiceRestemplateClient.getUnitOfMeasureById(
                            productionLineCapacity.getUnitOfMeasureId()
                    )

            );
        }

    }



    public ProductionLineCapacity save(ProductionLineCapacity productionLineCapacity) {
        ProductionLineCapacity newProductionLineCapacity
                = productionLineCapacityRepository.save(productionLineCapacity);
        loadAttribute(newProductionLineCapacity);
        return newProductionLineCapacity;
    }

    public ProductionLineCapacity saveOrUpdate(ProductionLineCapacity productionLineCapacity) {
        if(Objects.isNull(productionLineCapacity.getId()) &&
                Objects.nonNull(findByProductionLineAndItem(
                        productionLineCapacity.getWarehouseId(),
                        productionLineCapacity.getProductionLine().getId(),
                        productionLineCapacity.getItemId(), false
                ))) {
            productionLineCapacity.setId(
                    findByProductionLineAndItem(
                            productionLineCapacity.getWarehouseId(),
                            productionLineCapacity.getProductionLine().getId(),
                            productionLineCapacity.getItemId(), false
                    ).getId()
            );
        }
        return save(productionLineCapacity);
    }


    public void delete(ProductionLineCapacity productionLineCapacity) {
        productionLineCapacityRepository.delete(productionLineCapacity);
    }

    public void delete(Long id) {
        productionLineCapacityRepository.deleteById(id);
    }


    public List<ProductionLineCapacityCSVWrapper> loadData(InputStream inputStream) throws IOException {

        CsvSchema schema = CsvSchema.builder().
                addColumn("company").
                addColumn("warehouse").
                addColumn("production").
                addColumn("mould").
                addColumn("item").
                addColumn("capacity").
                addColumn("unitOfMeasure").
                addColumn("capacityUnit").
                addColumn("staffCount").
                build().withHeader();

        return fileService.loadData(inputStream, schema, ProductionLineCapacityCSVWrapper.class);
    }

    public void initTestData(Long companyId, String warehouseName) {
        try {
            logger.debug("####### Start loading production line capacity data  ======");
            String companyCode = warehouseLayoutServiceRestemplateClient.getCompanyById(companyId).getCode();

            String testDataFileName = StringUtils.isBlank(warehouseName) ?
                    testDataFile + ".csv" :
                    testDataFile + "-" + companyCode + "-" + warehouseName + ".csv";
            logger.debug("testDataFileName: {}", testDataFileName);

            InputStream inputStream = new ClassPathResource(testDataFileName).getInputStream();
            List<ProductionLineCapacityCSVWrapper> productionLineCapacityCSVWrappers = loadData(inputStream);
            productionLineCapacityCSVWrappers.stream().forEach(
                    productionLineCapacityCSVWrapper -> saveOrUpdate(convertFromWrapper(
                            productionLineCapacityCSVWrapper)));
        } catch (IOException ex) {
            logger.debug("Exception while load test data: {}", ex.getMessage());
        }
    }

    private ProductionLineCapacity convertFromWrapper(ProductionLineCapacityCSVWrapper productionLineCapacityCSVWrapper) {

        ProductionLineCapacity productionLineCapacity = new ProductionLineCapacity();
        productionLineCapacity.setCapacity(productionLineCapacityCSVWrapper.getCapacity());
        productionLineCapacity.setStaffCount(productionLineCapacityCSVWrapper.getStaffCount());

        Warehouse warehouse = warehouseLayoutServiceRestemplateClient.getWarehouseByName(
                productionLineCapacityCSVWrapper.getCompany(),
                productionLineCapacityCSVWrapper.getWarehouse()
        );
        productionLineCapacity.setWarehouseId(warehouse.getId());

        productionLineCapacity.setProductionLine(
                productionLineService.findByName(warehouse.getId(), productionLineCapacityCSVWrapper.getProduction())
        );
        if (Strings.isNotBlank(productionLineCapacityCSVWrapper.getMould())) {

            productionLineCapacity.setMould(
                    mouldService.findByName(warehouse.getId(), productionLineCapacityCSVWrapper.getMould())
            );
        }
        if (Strings.isNotBlank(productionLineCapacityCSVWrapper.getItem())) {
            productionLineCapacity.setItemId(
                    inventoryServiceRestemplateClient.getItemByName(
                            warehouse.getId(), productionLineCapacityCSVWrapper.getItem()
                    ).getId()
            );
        }
        if (Strings.isNotBlank(productionLineCapacityCSVWrapper.getUnitOfMeasure())) {
            productionLineCapacity.setUnitOfMeasureId(
                    commonServiceRestemplateClient.getUnitOfMeasureByName(
                            warehouse.getId(), productionLineCapacityCSVWrapper.getUnitOfMeasure()
                    ).getId()
            );
        }
        if (Strings.isNotBlank(productionLineCapacityCSVWrapper.getCapacityUnit())) {
            productionLineCapacity.setCapacityUnit(
                    TimeUnit.valueOf(productionLineCapacityCSVWrapper.getCapacityUnit())
            );
        }


        return productionLineCapacity;
    }


    public ProductionLineCapacity addProductionCapacity(ProductionLineCapacity productionLineCapacity) {
        return  saveOrUpdate(productionLineCapacity);
    }

    public ProductionLineCapacity changeProductionCapacity(Long id, ProductionLineCapacity productionLineCapacity) {
        return  saveOrUpdate(productionLineCapacity);
    }
}

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
import com.garyzhangscm.cwms.workorder.model.*;
import com.garyzhangscm.cwms.workorder.repository.ProductionLineRepository;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;


@Service
public class ProductionLineService implements TestDataInitiableService {
    private static final Logger logger = LoggerFactory.getLogger(ProductionLineService.class);

    @Autowired
    private ProductionLineRepository productionLineRepository;
    @Autowired
    private WorkOrderService workOrderService;

    @Autowired
    private WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient;
    @Autowired
    private InventoryServiceRestemplateClient inventoryServiceRestemplateClient;
    @Autowired
    private FileService fileService;

    @Value("${fileupload.test-data.production-lines:production-lines}")
    String testDataFile;

    public ProductionLine findById(Long id, boolean loadDetails) {
        ProductionLine productionLine = productionLineRepository.findById(id).orElse(null);
        if (productionLine != null && loadDetails) {
            loadAttribute(productionLine);
        }
        return productionLine;
    }

    public ProductionLine findById(Long id) {
        return findById(id, true);
    }


    public List<ProductionLine> findAll(Long warehouseId, String name, String productionLineIds, boolean loadDetails) {
        List<ProductionLine> productionLines;

        if (!StringUtils.isBlank(name)) {
            // find by production name
            ProductionLine productionLine = findByName(warehouseId, name);
            if (productionLine != null) {
                productionLines = Arrays.asList(new ProductionLine[]{productionLine});
            } else {
                productionLines = new ArrayList<>();
            }
        }
        else if (!StringUtils.isBlank(productionLineIds)) {
            // find production lines by a list of Ids
            productionLines = findByIds(warehouseId, productionLineIds);
        }
        else {
            // none of the parameters are passed in, let's return everything
            productionLines = productionLineRepository.findByWarehouseId(warehouseId);
        }
        if (productionLines.size() > 0 && loadDetails) {
            loadAttribute(productionLines);
        }
        return productionLines;
    }

    public List<ProductionLine> findAll(Long warehouseId,String name, String productionLineIds) {
        return findAll(warehouseId, name, productionLineIds, true);
    }

    public List<ProductionLine> findAllAvailableProductionLines(Long warehouseId) {
        return findAllAvailableProductionLines(warehouseId, true);
    }

    public List<ProductionLine> findAllAvailableProductionLines(Long warehouseId, boolean loadDetails) {
        List<ProductionLine> productionLines = productionLineRepository.findByWarehouseId(warehouseId);
        productionLines = productionLines.stream().filter(this::isAvailableForNewWorkOrder)
                            .collect(Collectors.toList());
        if (productionLines.size() > 0 && loadDetails) {
            loadAttribute(productionLines);
        }
        return productionLines;

    }

    private boolean isAvailableForNewWorkOrder(ProductionLine productionLine) {
        // The production line is available for new work order only if
        // both of the following conditions are met
        // 1. production line is enabled
        // 2. production line is not exclusive
        //    or is exclusive but no work order on it yet
        if (!productionLine.getEnabled()) {
            return false;
        }
        if (productionLine.getWorkOrderExclusiveFlag() &&
            productionLine.getWorkOrders().size() > 0) {
            return false;
        }
        return true;
    }


    public ProductionLine findByName(Long warehouseId, String name, boolean loadDetails) {
        ProductionLine productionLine = productionLineRepository.findByWarehouseIdAndName(warehouseId, name);
        if (productionLine != null && loadDetails) {
            loadAttribute(productionLine);
        }
        return productionLine;
    }

    public ProductionLine findByName(Long warehouseId, String name) {
        return findByName(warehouseId, name, true);
    }



    public List<ProductionLine> findByIds(Long warehouseId, String productionLineIds, boolean loadDetails) {

        List<Long> productionLineIdList = Arrays.stream(productionLineIds.split(","))
                .mapToLong(Long::parseLong).boxed().collect(Collectors.toList());

        List<ProductionLine> productionLines = productionLineRepository.findByIds(warehouseId, productionLineIdList);
        if (productionLines.size() > 0 && loadDetails) {
            loadAttribute(productionLines);
        }
        return productionLines;
    }

    public List<ProductionLine> findByIds(Long warehouseId, String productionLineIds) {
        return findByIds(warehouseId, productionLineIds, true);
    }

    public void loadAttribute(List<ProductionLine> productionLines) {
        for (ProductionLine productionLine : productionLines) {
            loadAttribute(productionLine);
        }
    }

    public void loadAttribute(ProductionLine productionLine) {

        if (productionLine.getWarehouseId() != null && productionLine.getWarehouse() == null) {
            productionLine.setWarehouse(
                    warehouseLayoutServiceRestemplateClient.getWarehouseById(productionLine.getWarehouseId()));
        }

        if (productionLine.getInboundStageLocationId() != null && productionLine.getInboundStageLocation() == null) {
            productionLine.setInboundStageLocation(
                    warehouseLayoutServiceRestemplateClient.getLocationById(productionLine.getInboundStageLocationId()));
        }
        if (productionLine.getOutboundStageLocationId() != null && productionLine.getOutboundStageLocation() == null) {
            productionLine.setOutboundStageLocation(
                    warehouseLayoutServiceRestemplateClient.getLocationById(productionLine.getOutboundStageLocationId()));
        }
        if (productionLine.getProductionLineLocationId() != null && productionLine.getProductionLineLocation() == null) {
            productionLine.setProductionLineLocation(
                    warehouseLayoutServiceRestemplateClient.getLocationById(productionLine.getProductionLineLocationId()));
        }

        productionLine.getWorkOrders().forEach(workOrder -> workOrderService.loadAttribute(workOrder));

    }



    public ProductionLine save(ProductionLine productionLine) {
        ProductionLine newProductionLine = productionLineRepository.save(productionLine);
        loadAttribute(newProductionLine);
        return newProductionLine;
    }

    public ProductionLine saveOrUpdate(ProductionLine productionLine) {
        Long warehouseId = productionLine.getWarehouseId();
        String name = productionLine.getName();

        if (productionLine.getId() == null &&
                findByName(warehouseId, name) != null) {
            productionLine.setId(findByName(warehouseId, name).getId());
        }
        return save(productionLine);
    }


    public void delete(ProductionLine productionLine) {
        productionLineRepository.delete(productionLine);
    }

    public void delete(Long id) {
        productionLineRepository.deleteById(id);
    }

    public void delete(String productionLineIds) {
        if (!productionLineIds.isEmpty()) {
            long[] productionLineIdArray = Arrays.asList(productionLineIds.split(",")).stream().mapToLong(Long::parseLong).toArray();
            for (long id : productionLineIdArray) {
                delete(id);
            }
        }
    }

    public List<ProductionLineCSVWrapper> loadData(InputStream inputStream) throws IOException {

        CsvSchema schema = CsvSchema.builder().
                addColumn("warehouse").
                addColumn("name").
                addColumn("inboundStageLocation").
                addColumn("outboundStageLocation").
                addColumn("productionLineLocation").
                addColumn("workOrderExclusiveFlag").
                addColumn("enabled").
                build().withHeader();

        return fileService.loadData(inputStream, schema, ProductionLineCSVWrapper.class);
    }

    public void initTestData(String warehouseName) {
        try {
            String testDataFileName  = StringUtils.isBlank(warehouseName) ?
                    testDataFile + ".csv" :
                    testDataFile + "-" + warehouseName + ".csv";
            InputStream inputStream = new ClassPathResource(testDataFileName).getInputStream();
            List<ProductionLineCSVWrapper> productionLineCSVWrappers = loadData(inputStream);
            productionLineCSVWrappers.stream().forEach(productionLineCSVWrapper -> saveOrUpdate(convertFromWrapper(productionLineCSVWrapper)));
        } catch (IOException ex) {
            logger.debug("Exception while load test data: {}", ex.getMessage());
        }
    }

    private ProductionLine convertFromWrapper(ProductionLineCSVWrapper productionLineCSVWrapper) {

        ProductionLine productionLine = new ProductionLine();
        productionLine.setName(productionLineCSVWrapper.getName());
        productionLine.setWorkOrderExclusiveFlag(productionLineCSVWrapper.getWorkOrderExclusiveFlag());
        productionLine.setEnabled(productionLineCSVWrapper.getEnabled());

        Warehouse warehouse = warehouseLayoutServiceRestemplateClient.getWarehouseByName(
                productionLineCSVWrapper.getWarehouse()
        );
        productionLine.setWarehouseId(warehouse.getId());

        productionLine.setInboundStageLocationId(
                warehouseLayoutServiceRestemplateClient.getLocationByName(
                        warehouse.getId(), productionLineCSVWrapper.getInboundStageLocation()
                ).getId()
        );
        productionLine.setOutboundStageLocationId(
                warehouseLayoutServiceRestemplateClient.getLocationByName(
                        warehouse.getId(), productionLineCSVWrapper.getOutboundStageLocation()
                ).getId()
        );
        productionLine.setProductionLineLocationId(
                warehouseLayoutServiceRestemplateClient.getLocationByName(
                        warehouse.getId(), productionLineCSVWrapper.getProductionLineLocation()
                ).getId()
        );
        return productionLine;
    }


    public ProductionLine disableProductionLine(@PathVariable Long id,
                                                @RequestParam boolean disabled) {
        ProductionLine productionLine = findById(id);
        productionLine.setEnabled(!disabled);
        return saveOrUpdate(productionLine);
    }
}

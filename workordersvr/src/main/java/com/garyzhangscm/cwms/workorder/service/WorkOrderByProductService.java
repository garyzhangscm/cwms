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
import com.garyzhangscm.cwms.workorder.clients.OutboundServiceRestemplateClient;
import com.garyzhangscm.cwms.workorder.clients.WarehouseLayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.workorder.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.workorder.exception.WorkOrderException;
import com.garyzhangscm.cwms.workorder.model.*;
import com.garyzhangscm.cwms.workorder.repository.WorkOrderByProductRepository;
import com.garyzhangscm.cwms.workorder.repository.WorkOrderLineRepository;
import org.apache.commons.lang.StringUtils;
import org.hibernate.jdbc.Work;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;


@Service
public class WorkOrderByProductService implements TestDataInitiableService {
    private static final Logger logger = LoggerFactory.getLogger(WorkOrderByProductService.class);

    @Autowired
    private WorkOrderByProductRepository workOrderByProductReRepository;
    @Autowired
    private WorkOrderService workOrderService;

    @Autowired
    private OutboundServiceRestemplateClient outboundServiceRestemplateClient;
    @Autowired
    private WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient;
    @Autowired
    private InventoryServiceRestemplateClient inventoryServiceRestemplateClient;
    @Autowired
    private FileService fileService;

    @Value("${fileupload.test-data.work-order-by-product:work-order-by-product}")
    String testDataFile;

    public WorkOrderByProduct findById(Long id, boolean loadDetails) {
        WorkOrderByProduct workOrderByProduct = workOrderByProductReRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("work order by product not found by id: " + id));
        if (loadDetails) {
            loadAttribute(workOrderByProduct);
        }
        return workOrderByProduct;
    }

    public WorkOrderByProduct findById(Long id) {
        return findById(id, true);
    }


    public List<WorkOrderByProduct> findAll(boolean loadDetails) {
        List<WorkOrderByProduct> workOrderByProducts = workOrderByProductReRepository.findAll();

        if (workOrderByProducts.size() > 0 && loadDetails) {
            loadAttribute(workOrderByProducts);
        }
        return workOrderByProducts;
    }

    public List<WorkOrderByProduct> findAll() {
        return findAll(true);
    }


    public void loadAttribute(List<WorkOrderByProduct> workOrderByProducts) {
        for (WorkOrderByProduct workOrderByProduct : workOrderByProducts) {
            loadAttribute(workOrderByProduct);
        }
    }

    public void loadAttribute(WorkOrderByProduct workOrderByProduct) {

        if (Objects.nonNull(workOrderByProduct.getWorkOrder())) {

            if (workOrderByProduct.getWorkOrder().getWarehouseId() != null && workOrderByProduct.getWorkOrder().getWarehouse() == null) {
                workOrderByProduct.getWorkOrder().setWarehouse(
                        warehouseLayoutServiceRestemplateClient.getWarehouseById(workOrderByProduct.getWorkOrder().getWarehouseId()));
            }
        }
        if (workOrderByProduct.getItemId() != null && workOrderByProduct.getItem() == null) {
            workOrderByProduct.setItem(inventoryServiceRestemplateClient.getItemById(workOrderByProduct.getItemId()));
        }
        if (workOrderByProduct.getInventoryStatusId() != null && workOrderByProduct.getInventoryStatus() == null) {
            workOrderByProduct.setInventoryStatus(
                    inventoryServiceRestemplateClient.getInventoryStatusById(workOrderByProduct.getInventoryStatusId()));
        }




    }






    public WorkOrderByProduct save(WorkOrderByProduct workOrderByProduct) {
        WorkOrderByProduct newWorkOrderByProduct = workOrderByProductReRepository.save(workOrderByProduct);
        loadAttribute(newWorkOrderByProduct);
        return newWorkOrderByProduct;
    }




    public void delete(WorkOrderByProduct workOrderByProduct) {
        workOrderByProductReRepository.delete(workOrderByProduct);
    }

    public void delete(Long id) {
        workOrderByProductReRepository.deleteById(id);
    }





    public List<WorkOrderByProductCSVWrapper> loadData(InputStream inputStream) throws IOException {

        CsvSchema schema = CsvSchema.builder().
                addColumn("company").
                addColumn("warehouse").
                addColumn("workOrder").
                addColumn("item").
                addColumn("expectedQuantity").
                addColumn("inventoryStatus").
                build().withHeader();

        return fileService.loadData(inputStream, schema, WorkOrderByProductCSVWrapper.class);
    }

    public void initTestData(Long companyId, String warehouseName) {
        try {
            String companyCode = warehouseLayoutServiceRestemplateClient.getCompanyById(companyId).getCode();

            String testDataFileName = StringUtils.isBlank(warehouseName) ?
                    testDataFile + ".csv" :
                    testDataFile + "-" + companyCode + "-" + warehouseName + ".csv";
            InputStream inputStream = new ClassPathResource(testDataFileName).getInputStream();
            List<WorkOrderByProductCSVWrapper> workOrderByProductCSVWrappers = loadData(inputStream);
            workOrderByProductCSVWrappers.stream().forEach(workOrderByProductCSVWrapper -> save(convertFromWrapper(workOrderByProductCSVWrapper)));
        } catch (IOException ex) {
            logger.debug("Exception while load test data: {}", ex.getMessage());
        }
    }

    private WorkOrderByProduct convertFromWrapper(WorkOrderByProductCSVWrapper workOrderByProductCSVWrapper) {

        WorkOrderByProduct workOrderByProduct = new WorkOrderByProduct();

        Warehouse warehouse =
                warehouseLayoutServiceRestemplateClient.getWarehouseByName(
                        workOrderByProductCSVWrapper.getCompany(),
                        workOrderByProductCSVWrapper.getWarehouse()
                );

        workOrderByProduct.setWorkOrder(
                workOrderService.findByNumber(
                        warehouse.getId(), workOrderByProductCSVWrapper.getWorkOrder()
                )
        );



        workOrderByProduct.setItemId(
                inventoryServiceRestemplateClient.getItemByName(
                        warehouse.getId(), workOrderByProductCSVWrapper.getItem()).getId()
        );

        workOrderByProduct.setExpectedQuantity(workOrderByProductCSVWrapper.getExpectedQuantity());
        workOrderByProduct.setProducedQuantity(0L);

        workOrderByProduct.setInventoryStatusId(
                inventoryServiceRestemplateClient.getInventoryStatusByName(
                        warehouse.getId(), workOrderByProductCSVWrapper.getInventoryStatus()).getId()
        );
        return workOrderByProduct;
    }


    public WorkOrderByProduct createWorkOrderByProductFromBOMByProduct(WorkOrder workOrder, Long workOrderCount,  BillOfMaterialByProduct billOfMaterialByProduct) {
        WorkOrderByProduct workOrderByProduct = new WorkOrderByProduct();
        workOrderByProduct.setWorkOrder(workOrder);

        workOrderByProduct.setItemId(billOfMaterialByProduct.getItemId());
        workOrderByProduct.setInventoryStatusId(billOfMaterialByProduct.getInventoryStatusId());

        workOrderByProduct.setExpectedQuantity(billOfMaterialByProduct.getExpectedQuantity() * workOrderCount);

        workOrderByProduct.setProducedQuantity(0L);

        return save(workOrderByProduct);


    }

    public void processWorkOrderByProductProduceTransaction(WorkOrderByProductProduceTransaction workOrderByProductProduceTransaction,
                                                            Location location) {

        logger.debug("Start to receive inventory from work order by product: \n{}", workOrderByProductProduceTransaction.getWorkOrderByProduct());
        Inventory inventory = workOrderByProductProduceTransaction.createInventory(location);

        logger.debug("Inventory: \n{}", inventory);
        inventoryServiceRestemplateClient.receiveInventoryFromWorkOrder(inventory);

        // Let's change the quantities of the by product information
        WorkOrderByProduct workOrderByProduct = workOrderByProductProduceTransaction.getWorkOrderByProduct();
        workOrderByProduct.setProducedQuantity(
                workOrderByProduct.getProducedQuantity() + workOrderByProductProduceTransaction.getQuantity()
        );
        save(workOrderByProduct);
    }

    public void processWorkOrderByProductProduceTransaction(WorkOrder workOrder,
                                                            WorkOrderByProductProduceTransaction workOrderByProductProduceTransaction,
                                                            Location location) {

        // skip the record with incorrect value
        if (StringUtils.isBlank(workOrderByProductProduceTransaction.getLpn()) ||
                Objects.isNull(workOrderByProductProduceTransaction.getInventoryStatus()) ||
                Objects.isNull(workOrderByProductProduceTransaction.getItemPackageType()) ||
                Objects.isNull(workOrderByProductProduceTransaction.getQuantity())  ) {
            return;
        }
        logger.debug("Start to process by product transaction \n{} \n for work order\n {}",
                workOrderByProductProduceTransaction, workOrder);
        // set up the work order by product information
        if (Objects.isNull(workOrderByProductProduceTransaction.getWorkOrderByProduct())) {
            throw WorkOrderException.raiseException("Can't produce by product as the master data is missing");
        }
        if (Objects.isNull(workOrderByProductProduceTransaction.getWorkOrderByProduct().getWorkOrder())) {
            workOrderByProductProduceTransaction.getWorkOrderByProduct().setWorkOrder(
                    workOrder
            );
        }
        processWorkOrderByProductProduceTransaction(workOrderByProductProduceTransaction, location);
    }
    public void processWorkOrderByProductProduceTransaction(WorkOrderCompleteTransaction workOrderCompleteTransaction,
                                                            WorkOrderByProductProduceTransaction workOrderByProductProduceTransaction,
                                                            Location location) {

        processWorkOrderByProductProduceTransaction(workOrderCompleteTransaction.getWorkOrder(),
                workOrderByProductProduceTransaction, location);
    }

    public void processWorkOrderByProductProduceTransaction(WorkOrderProduceTransaction workOrderProduceTransaction,
                                                            WorkOrderByProductProduceTransaction workOrderByProductProduceTransaction,
                                                            Location location) {

        processWorkOrderByProductProduceTransaction(workOrderProduceTransaction.getWorkOrder(), workOrderByProductProduceTransaction,
                location);
    }


}

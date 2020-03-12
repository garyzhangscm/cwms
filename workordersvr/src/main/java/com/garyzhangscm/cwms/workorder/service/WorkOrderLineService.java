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
import com.garyzhangscm.cwms.workorder.model.*;
import com.garyzhangscm.cwms.workorder.repository.WorkOrderLineRepository;
import org.apache.commons.lang.StringUtils;
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


@Service
public class WorkOrderLineService implements TestDataInitiableService {
    private static final Logger logger = LoggerFactory.getLogger(WorkOrderLineService.class);

    @Autowired
    private WorkOrderLineRepository workOrderLineRepository;
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

    @Value("${fileupload.test-data.work-order-line:work-order-line}")
    String testDataFile;

    public WorkOrderLine findById(Long id, boolean loadDetails) {
        WorkOrderLine workOrderLine = workOrderLineRepository.findById(id).orElse(null);
        if (workOrderLine != null && loadDetails) {
            loadAttribute(workOrderLine);
        }
        return workOrderLine;
    }

    public WorkOrderLine findById(Long id) {
        return findById(id, true);
    }


    public List<WorkOrderLine> findAll(boolean loadDetails) {
        List<WorkOrderLine> workOrderLines = workOrderLineRepository.findAll();

        if (workOrderLines.size() > 0 && loadDetails) {
            loadAttribute(workOrderLines);
        }
        return workOrderLines;
    }

    public List<WorkOrderLine> findAll() {
        return findAll(true);
    }


    public void loadAttribute(List<WorkOrderLine> workOrderLines) {
        for (WorkOrderLine workOrderLine : workOrderLines) {
            loadAttribute(workOrderLine);
        }
    }

    public void loadAttribute(WorkOrderLine workOrderLine) {

        if (workOrderLine.getWorkOrder().getWarehouseId() != null && workOrderLine.getWorkOrder().getWarehouse() == null) {
            workOrderLine.getWorkOrder().setWarehouse(
                    warehouseLayoutServiceRestemplateClient.getWarehouseById(workOrderLine.getWorkOrder().getWarehouseId()));
        }
        if (workOrderLine.getItemId() != null && workOrderLine.getItem() == null) {
            workOrderLine.setItem(inventoryServiceRestemplateClient.getItemById(workOrderLine.getItemId()));
        }
        if (workOrderLine.getInventoryStatusId() != null && workOrderLine.getInventoryStatus() == null) {
            workOrderLine.setInventoryStatus(
                    inventoryServiceRestemplateClient.getInventoryStatusById(workOrderLine.getInventoryStatusId()));
        }

        workOrderLine.setPicks(outboundServiceRestemplateClient.getWorkOrderLinePicks(workOrderLine));

        workOrderLine.setShortAllocations(outboundServiceRestemplateClient.getWorkOrderLineShortAllocations(workOrderLine));


    }

    public WorkOrderLine findByNumber(Long warehouseId, String workOrderNumber, String number) {
        return findByNumber(warehouseId, workOrderNumber, number, true);

    }

    public WorkOrderLine findByNumber(Long warehouseId, String workOrderNumber, String number, boolean loadDetails) {

        WorkOrderLine workOrderLine
                = workOrderLineRepository.findByNaturalKey(
                         warehouseId, workOrderNumber,number);
        if (workOrderLine != null && loadDetails) {
            loadAttribute(workOrderLine);
        }
        return workOrderLine;
    }




    public WorkOrderLine save(WorkOrderLine workOrderLine) {
        WorkOrderLine newWorkOrderLine = workOrderLineRepository.save(workOrderLine);
        loadAttribute(newWorkOrderLine);
        return newWorkOrderLine;
    }

    public WorkOrderLine saveOrUpdate(WorkOrderLine workOrderLine) {
        Long warehouseId = workOrderLine.getWorkOrder().getWarehouseId();
        String workOrderNumber = workOrderLine.getWorkOrder().getNumber();
        String number = workOrderLine.getNumber();

        if (workOrderLine.getId() == null
                && findByNumber(warehouseId, workOrderNumber, number) != null) {
            workOrderLine.setId(
                    findByNumber(warehouseId, workOrderNumber, number).getId());
        }
        return save(workOrderLine);
    }


    public void delete(WorkOrderLine workOrderLine) {
        workOrderLineRepository.delete(workOrderLine);
    }

    public void delete(Long id) {
        workOrderLineRepository.deleteById(id);
    }

    public void delete(String workOrderLineIds) {
        if (!workOrderLineIds.isEmpty()) {
            long[] workOrderLineIdArray = Arrays.asList(workOrderLineIds.split(",")).stream().mapToLong(Long::parseLong).toArray();
            for (long id : workOrderLineIdArray) {
                delete(id);
            }
        }
    }

    public List<WorkOrderLineCSVWrapper> loadData(InputStream inputStream) throws IOException {

        CsvSchema schema = CsvSchema.builder().
                addColumn("warehouse").
                addColumn("workOrder").
                addColumn("number").
                addColumn("item").
                addColumn("expectedQuantity").
                addColumn("inventoryStatus").
                build().withHeader();

        return fileService.loadData(inputStream, schema, WorkOrderLineCSVWrapper.class);
    }

    public void initTestData(String warehouseName) {
        try {
            String testDataFileName = StringUtils.isBlank(warehouseName) ?
                    testDataFile + ".csv" :
                    testDataFile + "-" + warehouseName + ".csv";
            InputStream inputStream = new ClassPathResource(testDataFileName).getInputStream();
            List<WorkOrderLineCSVWrapper> workOrderLineCSVWrappers = loadData(inputStream);
            workOrderLineCSVWrappers.stream().forEach(workOrderLineCSVWrapper -> saveOrUpdate(convertFromWrapper(workOrderLineCSVWrapper)));
        } catch (IOException ex) {
            logger.debug("Exception while load test data: {}", ex.getMessage());
        }
    }

    private WorkOrderLine convertFromWrapper(WorkOrderLineCSVWrapper workOrderLineCSVWrapper) {

        WorkOrderLine workOrderLine = new WorkOrderLine();

        Warehouse warehouse =
                warehouseLayoutServiceRestemplateClient.getWarehouseByName(
                      workOrderLineCSVWrapper.getWarehouse()
                );

        workOrderLine.setWorkOrder(
                workOrderService.findByNumber(
                        warehouse.getId(), workOrderLineCSVWrapper.getWorkOrder()
                )
        );

        workOrderLine.setNumber(workOrderLineCSVWrapper.getNumber());

        workOrderLine.setItemId(
                inventoryServiceRestemplateClient.getItemByName(
                        warehouse.getId(), workOrderLineCSVWrapper.getItem()).getId()
        );

        workOrderLine.setExpectedQuantity(workOrderLineCSVWrapper.getExpectedQuantity());
        workOrderLine.setOpenQuantity(workOrderLineCSVWrapper.getExpectedQuantity());
        workOrderLine.setInprocessQuantity(0L);
        workOrderLine.setConsumedQuantity(0L);

        workOrderLine.setInventoryStatusId(
                inventoryServiceRestemplateClient.getInventoryStatusByName(
                        warehouse.getId(), workOrderLineCSVWrapper.getInventoryStatus()).getId()
        );
        return workOrderLine;
    }


    public WorkOrderLine createWorkOrderLineFromBOMLine(WorkOrder workOrder, Long workOrderCount,  BillOfMaterialLine billOfMaterialLine) {
        WorkOrderLine workOrderLine = new WorkOrderLine();
        workOrderLine.setWorkOrder(workOrder);
        workOrderLine.setNumber(billOfMaterialLine.getNumber());
        workOrderLine.setItemId(billOfMaterialLine.getItemId());
        workOrderLine.setInventoryStatusId(billOfMaterialLine.getInventoryStatusId());

        workOrderLine.setExpectedQuantity(billOfMaterialLine.getExpectedQuantity() * workOrderCount);
        workOrderLine.setOpenQuantity(billOfMaterialLine.getExpectedQuantity() * workOrderCount);
        workOrderLine.setInprocessQuantity(0L);
        workOrderLine.setConsumedQuantity(0L);

        return save(workOrderLine);


    }


    @Transactional
    public void registerPickCancelled(Long workOrderLineId, Long cancelledQuantity) {
        registerPickCancelled(findById(workOrderLineId), cancelledQuantity);
    }
    @Transactional
    public void registerPickCancelled(WorkOrderLine workOrderLine, Long cancelledQuantity) {
        logger.debug("registerPickCancelled: work order line: {}, cancelledQuantity: {}",
                workOrderLine.getNumber(), cancelledQuantity);
        workOrderLine.setOpenQuantity(workOrderLine.getOpenQuantity() + cancelledQuantity);
        workOrderLine.setInprocessQuantity(workOrderLine.getInprocessQuantity() - cancelledQuantity);
        workOrderLine = save(workOrderLine);
        logger.debug("after pick cancelled, work order line {} has open quantity {}, in process quantity: {}",
                workOrderLine.getNumber(), workOrderLine.getOpenQuantity(), workOrderLine.getInprocessQuantity());

    }
    @Transactional
    public void registerShortAllocationCancelled(Long workOrderLineId, Long cancelledQuantity) {

        registerShortAllocationCancelled(findById(workOrderLineId), cancelledQuantity);
    }

    @Transactional
    public void registerShortAllocationCancelled(WorkOrderLine workOrderLine, Long cancelledQuantity) {
        logger.debug("registerShortAllocationCancelled: work order line: {}, cancelledQuantity: {}",
                workOrderLine.getNumber(), cancelledQuantity);
        workOrderLine.setOpenQuantity(workOrderLine.getOpenQuantity() + cancelledQuantity);
        workOrderLine.setInprocessQuantity(workOrderLine.getInprocessQuantity() - cancelledQuantity);
        workOrderLine = save(workOrderLine);
        logger.debug("after pick cancelled, work order line {} has open quantity {}, in process quantity: {}",
                workOrderLine.getNumber(), workOrderLine.getOpenQuantity(), workOrderLine.getInprocessQuantity());

    }


}

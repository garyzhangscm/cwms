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
import com.garyzhangscm.cwms.workorder.exception.GenericException;
import com.garyzhangscm.cwms.workorder.model.*;
import com.garyzhangscm.cwms.workorder.repository.BillOfMaterialRepository;
import com.garyzhangscm.cwms.workorder.repository.WorkOrderRepository;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestParam;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;


@Service
public class WorkOrderService implements TestDataInitiableService {
    private static final Logger logger = LoggerFactory.getLogger(WorkOrderService.class);

    @Autowired
    private WorkOrderRepository workOrderRepository;
    @Autowired
    private WorkOrderLineService workOrderLineService;
    @Autowired
    private BillOfMaterialService billOfMaterialService;
    @Autowired
    private ProductionLineService productionLineService;
    @Autowired
    private WorkOrderInstructionService workOrderInstructionService;

    @Autowired
    private OutboundServiceRestemplateClient outboundServiceRestemplateClient;
    @Autowired
    private WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient;
    @Autowired
    private InventoryServiceRestemplateClient inventoryServiceRestemplateClient;
    @Autowired
    private FileService fileService;

    @Value("${fileupload.test-data.work-order:work-order}")
    String testDataFile;

    public WorkOrder findById(Long id, boolean loadDetails) {
        WorkOrder workOrder = workOrderRepository.findById(id).orElse(null);
        if (workOrder != null && loadDetails) {
            loadAttribute(workOrder);
        }
        return workOrder;
    }

    public WorkOrder findById(Long id) {
        return findById(id, true);
    }


    public List<WorkOrder> findAll(Long warehouseId, String number, String itemName, boolean loadDetails) {

        List<WorkOrder> workOrders =  workOrderRepository.findAll(
                (Root<WorkOrder> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();

                    predicates.add(criteriaBuilder.equal(root.get("warehouseId"), warehouseId));

                    if (!StringUtils.isBlank(number)) {
                        predicates.add(criteriaBuilder.equal(root.get("number"), number));

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

        if (workOrders.size() > 0 && loadDetails) {
            loadAttribute(workOrders);
        }
        return workOrders;
    }

    public List<WorkOrder> findAll(Long warehouseId, String number, String itemName) {
        return findAll(warehouseId, number, itemName, true);
    }


    public WorkOrder findByNumber(Long warehouseId, String number, boolean loadDetails) {
        WorkOrder workOrder = workOrderRepository.findByWarehouseIdAndNumber(warehouseId, number);
        if (workOrder != null && loadDetails) {
            loadAttribute(workOrder);
        }
        return workOrder;
    }

    public WorkOrder findByNumber(Long warehouseId,String number) {
        return findByNumber(warehouseId, number, true);
    }


    public void loadAttribute(List<WorkOrder> workOrders) {
        for (WorkOrder workOrder : workOrders) {
            loadAttribute(workOrder);
        }
    }

    public void loadAttribute(WorkOrder workOrder) {

        if (workOrder.getItemId() != null && workOrder.getItem() == null) {
            workOrder.setItem(inventoryServiceRestemplateClient.getItemById(workOrder.getItemId()));
        }
        if (workOrder.getWarehouseId() != null && workOrder.getWarehouse() == null) {
            workOrder.setWarehouse(warehouseLayoutServiceRestemplateClient.getWarehouseById(workOrder.getWarehouseId()));
        }

        // Load the item and inventory status information for each lines
        workOrder.getWorkOrderLines()
                .forEach(workOrderLine -> workOrderLineService.loadAttribute(workOrderLine));

    }



    public WorkOrder save(WorkOrder workOrder) {
        WorkOrder newWorkOrder = workOrderRepository.save(workOrder);
        loadAttribute(newWorkOrder);
        return newWorkOrder;
    }

    public WorkOrder saveOrUpdate(WorkOrder workOrder) {
        if (workOrder.getId() == null && findByNumber(workOrder.getWarehouseId(), workOrder.getNumber()) != null) {
            workOrder.setId(
                    findByNumber(workOrder.getWarehouseId(), workOrder.getNumber()).getId());
        }
        return save(workOrder);
    }


    public void delete(WorkOrder workOrder) {
        workOrderRepository.delete(workOrder);
    }

    public void delete(Long id) {
        workOrderRepository.deleteById(id);
    }

    public void delete(String workOrderIds) {
        if (!workOrderIds.isEmpty()) {
            long[] workOrderIdArray = Arrays.asList(workOrderIds.split(",")).stream().mapToLong(Long::parseLong).toArray();
            for (long id : workOrderIdArray) {
                delete(id);
            }
        }
    }

    public List<WorkOrderCSVWrapper> loadData(InputStream inputStream) throws IOException {

        CsvSchema schema = CsvSchema.builder().
                addColumn("warehouse").
                addColumn("number").
                addColumn("item").
                addColumn("expectedQuantity").
                build().withHeader();

        return fileService.loadData(inputStream, schema, WorkOrderCSVWrapper.class);
    }

    public void initTestData(String warehouseName) {
        try {
            String testDataFileName = StringUtils.isBlank(warehouseName) ?
                    testDataFile + ".csv" :
                    testDataFile + "-" + warehouseName + ".csv";
            InputStream inputStream = new ClassPathResource(testDataFileName).getInputStream();
            List<WorkOrderCSVWrapper> workOrderCSVWrappers = loadData(inputStream);
            workOrderCSVWrappers.stream().forEach(workOrderCSVWrapper -> saveOrUpdate(convertFromWrapper(workOrderCSVWrapper)));
        } catch (IOException ex) {
            logger.debug("Exception while load test data: {}", ex.getMessage());
        }
    }

    private WorkOrder convertFromWrapper(WorkOrderCSVWrapper workOrderCSVWrapper) {

        WorkOrder workOrder = new WorkOrder();
        workOrder.setNumber(workOrderCSVWrapper.getNumber());
        workOrder.setExpectedQuantity(workOrderCSVWrapper.getExpectedQuantity());
        workOrder.setProducedQuantity(0L);

        Warehouse warehouse = warehouseLayoutServiceRestemplateClient.getWarehouseByName(
                workOrderCSVWrapper.getWarehouse()
        );

        workOrder.setItemId(
                inventoryServiceRestemplateClient.getItemByName(
                        warehouse.getId(), workOrderCSVWrapper.getItem()).getId()
        );
        workOrder.setWarehouseId(warehouse.getId());

        return workOrder;
    }
    public WorkOrder createWorkOrderFromBOM(Long billOfMaterialId,
                            String workOrderNumber, Long expectedQuantity,
                            Long productionLineId) {

        BillOfMaterial billOfMaterial = billOfMaterialService.findById(billOfMaterialId, false);

        WorkOrder workOrder = new WorkOrder();
        workOrder.setNumber(workOrderNumber);
        if (productionLineId != null) {
            workOrder.setProductionLine(productionLineService.findById(productionLineId));
        }
        workOrder.setItemId(billOfMaterial.getItemId());
        workOrder.setWarehouseId(billOfMaterial.getWarehouseId());
        workOrder.setExpectedQuantity(expectedQuantity);
        workOrder.setProducedQuantity(0L);

        WorkOrder savedWorkOrder = save(workOrder);

        Long workOrderCount = expectedQuantity / billOfMaterial.getExpectedQuantity();
        // Start to create work order line
        billOfMaterial.getBillOfMaterialLines()
                .forEach(billOfMaterialLine ->
                        workOrderLineService.createWorkOrderLineFromBOMLine(savedWorkOrder, workOrderCount, billOfMaterialLine));

        // Start to create work order instruction
        billOfMaterial.getWorkOrderInstructionTemplates()
                .forEach(workOrderInstructionTemplate ->
                        workOrderInstructionService.createWorkOrderInstructionFromBOMLine(savedWorkOrder,workOrderInstructionTemplate));

        return findById(savedWorkOrder.getId());
    }

    public WorkOrder allocateWorkOrder(Long workOrderId) {
        WorkOrder workOrder = findById(workOrderId);
        try {
            AllocationResult allocationResult
                    = outboundServiceRestemplateClient.allocateWorkOrder(workOrder);

            // After we get the allocation result,
            // let's update the quantity in each work order line

            // A map to store the quantities
            // Key: work order line id
            // value: pick quantity + short allocation quantity
            Map<Long, Long> inprocessQuantities = new HashMap<>();

            allocationResult.getPicks().forEach(pick -> {
                Long workOrderLineId = pick.getWorkOrderLineId();
                Long inprocessQuantity = inprocessQuantities.getOrDefault(workOrderLineId, 0L);
                inprocessQuantities.put(workOrderLineId, (inprocessQuantity + pick.getQuantity()));
            });

            allocationResult.getShortAllocations().forEach(shortAllocation -> {
                Long workOrderLineId = shortAllocation.getWorkOrderLineId();
                Long inprocessQuantity = inprocessQuantities.getOrDefault(workOrderLineId, 0L);
                inprocessQuantities.put(workOrderLineId, (inprocessQuantity + shortAllocation.getQuantity()));
            });

            inprocessQuantities.entrySet().stream().forEach(entry ->{
                WorkOrderLine workOrderLine = workOrderLineService.findById(entry.getKey());
                // Move the 'inprocess quantity' we just calculated from 'Open quantity'
                // to 'inprocess quantity'
                Long inprocessQuantity = entry.getValue();
                workOrderLine.setOpenQuantity(workOrderLine.getOpenQuantity() - inprocessQuantity);
                workOrderLine.setInprocessQuantity(workOrderLine.getInprocessQuantity() + inprocessQuantity);
                workOrderLineService.save(workOrderLine);
            });

        }
        catch (IOException ex) {
            throw new GenericException(10000, ex.getMessage());
        }

        // return the latest work order information
        return findById(workOrderId);
    }

}

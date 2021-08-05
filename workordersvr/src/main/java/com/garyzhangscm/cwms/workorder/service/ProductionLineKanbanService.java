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
import com.garyzhangscm.cwms.workorder.clients.WarehouseLayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.workorder.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;


@Service
public class ProductionLineKanbanService {
    private static final Logger logger = LoggerFactory.getLogger(ProductionLineKanbanService.class);

    private final int DAY_START_HOUR = 16;  // days start from 16:00 until 15:59 the next day

    @Autowired
    private ProductionLineAssignmentService productionLineAssignmentService;



    @Autowired
    private WorkOrderProduceTransactionService workOrderProduceTransactionService;
    @Autowired
    private ProductionLineCapacityService productionLineCapacityService;
    @Autowired
    private ProductionLineService productionLineService;

    @Autowired
    private InventoryServiceRestemplateClient inventoryServiceRestemplateClient;

    public List<ProductionLineKanbanData> getProductionLineKanbanData(Long productionLineId,
                                                                      String productionLineIds) {

        // Get all the production line assignment and we will calculate
        // all the number
        List<ProductionLineAssignment> productionLineAssignments
                = productionLineAssignmentService.findAll(productionLineId, productionLineIds, null);
        return getProductionLineKanbanData(productionLineAssignments);
    }
    public List<ProductionLineKanbanData> getProductionLineKanbanData(List<ProductionLineAssignment> productionLineAssignments) {
        List<ProductionLineKanbanData> productionLineKanbanDataList = new ArrayList<>();
        productionLineAssignments.forEach(
                productionLineAssignment -> {
                    ProductionLineKanbanData productionLineKanbanData = new ProductionLineKanbanData();
                    productionLineKanbanData.setProductionLineName(
                            productionLineAssignment.getProductionLine().getName()
                    );
                    productionLineKanbanData.setWorkOrderNumber(
                            productionLineAssignment.getWorkOrder().getNumber()
                    );
                    productionLineKanbanData.setItemName(
                            inventoryServiceRestemplateClient.getItemById(
                                    productionLineAssignment.getWorkOrder().getItemId()
                            ).getName()
                    );
                    productionLineKanbanData.setProductionLineModel(
                            productionLineAssignment.getProductionLine().getModel()
                    );
                    productionLineKanbanData.setProductionLineTargetOutput(
                            getProductionLineDailyTargetOutput(productionLineAssignment)
                    );
                    productionLineKanbanData.setProductionLineTotalTargetOutput(
                            productionLineAssignment.getQuantity()
                    );

                    // we will get the actual output and total actual output
                    // from the work order transactions
                    List<WorkOrderProduceTransaction>  workOrderProduceTransactions
                            = workOrderProduceTransactionService.findAll(
                            productionLineAssignment.getWorkOrder().getWarehouseId(),
                            productionLineAssignment.getWorkOrder().getNumber(),
                            productionLineAssignment.getProductionLine().getId(),
                            false, false
                    );

                    logger.debug("We found {} work order produce transaction for work order {} on line {}",
                            workOrderProduceTransactions.size(),
                            productionLineAssignment.getWorkOrder().getNumber(),
                            productionLineAssignment.getProductionLine().getName());

                    List<Inventory> unPutawayInventory
                            = inventoryServiceRestemplateClient.findInventoryByLocation(
                                    productionLineAssignment.getWorkOrder().getWarehouseId(),
                                    productionLineAssignment.getProductionLine().getOutboundStageLocationId()
                            );
                    List<Inventory> dailyUnputawayInventory =
                            getProductionLineDailyUnputawayInventory(unPutawayInventory, workOrderProduceTransactions);
                    // get the quantity of the inventory that is not putaway yet, both total and daily
                    Long dailyUnPutawayInventoryQuantity = dailyUnputawayInventory.stream().mapToLong(
                            Inventory::getQuantity
                    ).sum();
                    Long totalUnPutawayInventoryQuantity = unPutawayInventory.stream().mapToLong(
                            Inventory::getQuantity
                    ).sum();

                    // get the actual output, including inventory that is putaway and not
                    // putaway
                    Long productionLineDailyActualOutput =
                            getProductionLineDailyActualOutput(workOrderProduceTransactions);

                    productionLineKanbanData.setProductionLineActualOutput(
                            productionLineDailyActualOutput
                    );
                    productionLineKanbanData.setProductionLineActualPutawayOutput(
                            productionLineDailyActualOutput - dailyUnPutawayInventoryQuantity
                    );


                    Long totalProducedQuantity =
                            getTotalProducedQuantity(workOrderProduceTransactions);
                    productionLineKanbanData.setProductionLineTotalActualOutput(
                            totalProducedQuantity
                    );

                    productionLineKanbanData.setProductionLineTotalActualPutawayOutput(
                            totalProducedQuantity - totalUnPutawayInventoryQuantity
                    );

                    ProductionLineActivity checkedInUser
                            = productionLineService.getCheckedInUser(
                                productionLineAssignment.getProductionLine());
                    if (Objects.nonNull(checkedInUser)) {

                        productionLineKanbanData.setShift(checkedInUser.getUsername());
                    }

                    productionLineKanbanDataList.add(productionLineKanbanData);
                }
        );
        return productionLineKanbanDataList;

    }

    /**
     * Get the unputaway inventory that is created on the previous day, based on the cutoff time
     * If NOW is before cutoff time, the it is from previous day's cutoff time to
     * today's cutoff time. Otherwise, it is from today's cutoff time to next day's
     * cutoff time
     * @param unPutawayInventory
     * @return
     */
    private List<Inventory> getProductionLineDailyUnputawayInventory(
            List<Inventory> unPutawayInventory,
            List<WorkOrderProduceTransaction> workOrderProduceTransactions) {
        Set<String> dailyProducedLPNs =
                getDailyWorkOrderProducedInventory(workOrderProduceTransactions)
                        .stream()
                    .map(workOrderProducedInventory -> workOrderProducedInventory.getLpn())
                .collect(Collectors.toSet());
        return unPutawayInventory.stream()
                .filter(inventory -> dailyProducedLPNs.contains(inventory.getLpn()))
                .collect(Collectors.toList());
    }



    private Long getProductionLineDailyActualOutput( List<WorkOrderProduceTransaction>  workOrderProduceTransactions) {
        return getDailyWorkOrderProducedInventory(workOrderProduceTransactions)
                .stream()
                .mapToLong(WorkOrderProducedInventory::getQuantity).sum();


    }

    /**
     * Get the actual daily produced inventory of the previous day , based on the cutoff time
     * If NOW is before cutoff time, the it is from previous day's cutoff time to
     * today's cutoff time. Otherwise, it is from today's cutoff time to next day's
     * cutoff time
     * @param workOrderProduceTransactions all transaction relate to this work order and production line
     * @return
     */
    private List<WorkOrderProducedInventory> getDailyWorkOrderProducedInventory(
            List<WorkOrderProduceTransaction>  workOrderProduceTransactions) {
        // the daily actual output is based on the time from 4:00 PM to 3:59:59 PM the next day
        LocalDateTime dayStartDateTime = getProductionLineKanbanDataStartTime();
        LocalDateTime dayEndDateTime = getProductionLineKanbanDataEndTime();

        logger.debug("Will get daily output from {} to {}",
                dayStartDateTime, dayEndDateTime);

        // only return the transaction that between the start and end time
        // and get the total quantity of the produced inventory
        return workOrderProduceTransactions.stream().filter(
                workOrderProduceTransaction -> {
                    logger.debug("workOrderProduceTransaction.getCreatedTime(): {}",
                            workOrderProduceTransaction.getCreatedTime());
                    logger.debug("!workOrderProduceTransaction.getCreatedTime().isBefore(dayStartDateTime): {}",
                            !workOrderProduceTransaction.getCreatedTime().isBefore(dayStartDateTime));
                    logger.debug("workOrderProduceTransaction.getCreatedTime().isBefore(dayEndDateTime): {}",
                            workOrderProduceTransaction.getCreatedTime().isBefore(dayEndDateTime));
                    return !workOrderProduceTransaction.getCreatedTime().isBefore(dayStartDateTime)
                            & workOrderProduceTransaction.getCreatedTime().isBefore(dayEndDateTime);
                }
        ).map(workOrderProduceTransaction -> workOrderProduceTransaction.getWorkOrderProducedInventories())
                .flatMap(Collection::stream).collect(Collectors.toList());
    }
    /**
     * Get the kanban's end time. If it is already after the cut off time, then the kanban
     * will show the data from today's cut off time until next day's cutoff time, otherwise,
     * the kanban will show the data from previous day's cutoff time until today's cutoff time
     * @return kanban data's end time
     */
    private LocalDateTime getProductionLineKanbanDataEndTime() {
        return LocalDateTime.now().getHour() >= DAY_START_HOUR ?
             LocalDateTime.of(LocalDate.now().plusDays(1), LocalTime.of(DAY_START_HOUR - 1, 59, 59))
             : LocalDateTime.of(LocalDate.now(), LocalTime.of(DAY_START_HOUR - 1, 59, 59));

    }

    /**
     * Get the kanban's end time. If it is already after the cut off time, then the kanban
     * will show the data from today's cut off time until next day's cutoff time, otherwise,
     * the kanban will show the data from previous day's cutoff time until today's cutoff time
     * @return kanban data's start time
     */
    private LocalDateTime getProductionLineKanbanDataStartTime() {
         return LocalDateTime.now().getHour() >= DAY_START_HOUR ?
            LocalDateTime.of(LocalDate.now(), LocalTime.of(DAY_START_HOUR, 0, 0))
            : LocalDateTime.of(LocalDate.now().minusDays(1), LocalTime.of(DAY_START_HOUR, 0, 0));
    }

    /**
     * Get daily production line target output
     */
    private Long getProductionLineDailyTargetOutput(ProductionLineAssignment productionLineAssignment) {
        // Get the total quantity from the transactions
        ProductionLineCapacity productionLineCapacity =
                productionLineCapacityService.findByProductionLineAndItem(
                        productionLineAssignment.getWorkOrder().getWarehouseId(),
                        productionLineAssignment.getProductionLine().getId(),
                        productionLineAssignment.getWorkOrder().getItemId(),
                        false
                );
        if (Objects.nonNull(productionLineCapacity)) {
            return productionLineCapacity.getCapacity();
        }
        else {
            return 0L;
        }
    }

    private Long getTotalProducedQuantity(List<WorkOrderProduceTransaction>  workOrderProduceTransactions) {
        Long totalProducedQuantity =
                workOrderProduceTransactions.stream()
                        .map(workOrderProduceTransaction -> workOrderProduceTransaction.getWorkOrderProducedInventories())
                        .flatMap(Collection::stream).mapToLong(WorkOrderProducedInventory::getQuantity).sum();
        return totalProducedQuantity;
    }

}

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
import com.garyzhangscm.cwms.workorder.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;


@Service
public class ProductionLineKanbanService {
    private static final Logger logger = LoggerFactory.getLogger(ProductionLineKanbanService.class);

    private final int DAY_START_HOUR = 16;  // days start from 16:00 until 15:59 the next day

    @Autowired
    private ProductionLineAssignmentService productionLineAssignmentService;
    @Autowired
    private WorkOrderReverseProductionInventoryService workOrderReverseProductionInventoryService;



    @Autowired
    private WorkOrderProduceTransactionService workOrderProduceTransactionService;
    @Autowired
    private ProductionLineCapacityService productionLineCapacityService;
    @Autowired
    private ProductionLineService productionLineService;

    @Autowired
    private InventoryServiceRestemplateClient inventoryServiceRestemplateClient;

    public List<ProductionLineKanbanData> getProductionLineKanbanData(Long warehouseId,
                                                                      Long productionLineId,
                                                                      String productionLineIds,
                                                                      String productionLineNames) {

        // Get all the production line assignment and we will calculate
        // all the number
        LocalDateTime currentLocalDateTime = LocalDateTime.now();
        logger.debug("========> @ {} start to find all production line assignment",
                currentLocalDateTime );

        List<ProductionLineAssignment> productionLineAssignments
                = productionLineAssignmentService.findAll(warehouseId, productionLineId, productionLineIds, null, productionLineNames);


        logger.debug("====> after : {} millisecond(1/1000 second) @ {},we found all production line assignment",
                ChronoUnit.MILLIS.between(
                        currentLocalDateTime, LocalDateTime.now()),
                LocalDateTime.now());

        return getProductionLineKanbanData(productionLineAssignments);
    }
    public List<ProductionLineKanbanData> getProductionLineKanbanData(List<ProductionLineAssignment> productionLineAssignments) {
        List<ProductionLineKanbanData> productionLineKanbanDataList = new ArrayList<>();

        AtomicReference<LocalDateTime> currentLocalDateTime = new AtomicReference<>(LocalDateTime.now());
        logger.debug("========> @ {} start to loop through {} production line assignment",
                currentLocalDateTime, productionLineAssignments.size());

        AtomicInteger i = new AtomicInteger();

        productionLineAssignments.forEach(
                productionLineAssignment -> {

                    i.getAndIncrement();
                    currentLocalDateTime.set(LocalDateTime.now());
                    LocalDateTime loopStartDateTime = LocalDateTime.now();
                    logger.debug("========> @ {}, loop {} start",
                            currentLocalDateTime, i);

                    ProductionLineKanbanData productionLineKanbanData = new ProductionLineKanbanData();
                    productionLineKanbanData.setProductionLineName(
                            productionLineAssignment.getProductionLine().getName()
                    );
                    productionLineKanbanData.setWorkOrderNumber(
                            productionLineAssignment.getWorkOrder().getNumber()
                    );

                    logger.debug("====> after : {} millisecond(1/1000 second) @ {},we setup the production line name and work order number",
                            ChronoUnit.MILLIS.between(
                                    currentLocalDateTime.get(), LocalDateTime.now()),
                            LocalDateTime.now());
                    currentLocalDateTime.set(LocalDateTime.now());

                    productionLineKanbanData.setItemName(
                            inventoryServiceRestemplateClient.getItemById(
                                    productionLineAssignment.getWorkOrder().getItemId()
                            ).getName()
                    );

                    logger.debug("====> after : {} millisecond(1/1000 second) @ {},we setup the item name",
                            ChronoUnit.MILLIS.between(
                                    currentLocalDateTime.get(), LocalDateTime.now()),
                            LocalDateTime.now());
                    currentLocalDateTime.set(LocalDateTime.now());

                    productionLineKanbanData.setProductionLineModel(
                            productionLineAssignment.getProductionLine().getModel()
                    );
                    productionLineKanbanData.setProductionLineTargetOutput(
                            getProductionLineDailyTargetOutput(productionLineAssignment)
                    );
                    productionLineKanbanData.setProductionLineTotalTargetOutput(
                            productionLineAssignment.getQuantity()
                    );

                    logger.debug("====> after : {} millisecond(1/1000 second) @ {},we setup model / target output / daily target output",
                            ChronoUnit.MILLIS.between(
                                    currentLocalDateTime.get(), LocalDateTime.now()),
                            LocalDateTime.now());
                    currentLocalDateTime.set(LocalDateTime.now());

                    // we will get the actual output and total actual output
                    // from the work order transactions
                    List<WorkOrderProduceTransaction>  workOrderProduceTransactions
                            = workOrderProduceTransactionService.findAll(
                            productionLineAssignment.getWorkOrder().getWarehouseId(),
                            productionLineAssignment.getWorkOrder().getNumber(),
                            productionLineAssignment.getProductionLine().getId(),
                            false, null, null, null, false
                    );


                    logger.debug("====> after : {} millisecond(1/1000 second) @ {},We found {} work order produce transaction for work order {} on line {}",
                            ChronoUnit.MILLIS.between(
                                    currentLocalDateTime.get(), LocalDateTime.now()),
                            LocalDateTime.now(),
                            workOrderProduceTransactions.size(),
                            productionLineAssignment.getWorkOrder().getNumber(),
                            productionLineAssignment.getProductionLine().getName());
                    currentLocalDateTime.set(LocalDateTime.now());

                    List<Inventory> unPutawayInventory
                            = inventoryServiceRestemplateClient.findInventoryByLocation(
                                    productionLineAssignment.getWorkOrder().getWarehouseId(),
                                    productionLineAssignment.getProductionLine().getOutboundStageLocationId()
                            );

                    logger.debug("====> after : {} millisecond(1/1000 second) @ {},We found {} unputaway inventory",
                            ChronoUnit.MILLIS.between(
                                    currentLocalDateTime.get(), LocalDateTime.now()),
                            LocalDateTime.now(), unPutawayInventory.size());
                    currentLocalDateTime.set(LocalDateTime.now());

                    List<Inventory> dailyUnputawayInventory =
                            getProductionLineDailyUnputawayInventory(unPutawayInventory, workOrderProduceTransactions);

                    logger.debug("====> after : {} millisecond(1/1000 second) @ {},We found {} daily unputaway inventory",
                            ChronoUnit.MILLIS.between(
                                    currentLocalDateTime.get(), LocalDateTime.now()),
                            LocalDateTime.now(), dailyUnputawayInventory.size());
                    currentLocalDateTime.set(LocalDateTime.now());

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

                    logger.debug("====> after : {} millisecond(1/1000 second) @ {},We setup the all the output quantities",
                            ChronoUnit.MILLIS.between(
                                    currentLocalDateTime.get(), LocalDateTime.now()),
                            LocalDateTime.now());
                    currentLocalDateTime.set(LocalDateTime.now());


                    ProductionLineActivity checkedInUser
                            = productionLineService.getCheckedInUser(
                                productionLineAssignment.getProductionLine());
                    if (Objects.nonNull(checkedInUser)) {

                        productionLineKanbanData.setShift(checkedInUser.getUsername());
                    }

                    logger.debug("====> after : {} millisecond(1/1000 second) @ {},We setup the check in user",
                            ChronoUnit.MILLIS.between(
                                    currentLocalDateTime.get(), LocalDateTime.now()),
                            LocalDateTime.now());
                    currentLocalDateTime.set(LocalDateTime.now());

                    productionLineKanbanData.setProductionLineEnabled(
                            productionLineAssignment.getProductionLine().getEnabled()
                    );

                    productionLineKanbanDataList.add(productionLineKanbanData);

                    logger.debug("======== {} : End of Loop {}, total cost {} ==========",

                            LocalDateTime.now(),
                            i.get(),
                            ChronoUnit.MILLIS.between(
                                    loopStartDateTime, LocalDateTime.now())
                            );
                }
        );
        Collections.sort(productionLineKanbanDataList, (o1, o2) -> o1.getProductionLineName().compareToIgnoreCase(o2.getProductionLineName()));
        return productionLineKanbanDataList;

    }

    private List<WorkOrderReverseProductionInventory> getReversedInventory(WorkOrderProduceTransaction workOrderProduceTransaction) {
        return workOrderReverseProductionInventoryService.findAll(
                workOrderProduceTransaction.getId(),
                null, null
        );
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
        List<WorkOrderProduceTransaction> dailyWorkOrderProduceTransactions =
                  workOrderProduceTransactions.stream().filter(
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
                ).collect(Collectors.toList());

        // loop through each transaction and get the produced inventory and ignore
        // the reversed inventory
        List<WorkOrderProducedInventory> workOrderProducedInventories = new ArrayList<>();
        dailyWorkOrderProduceTransactions.forEach(
                workOrderProduceTransaction -> {
                    // get the reversed LPN for this transaction
                    List<WorkOrderReverseProductionInventory> workOrderReverseProductionInventories =
                            workOrderReverseProductionInventoryService.findAll(
                                    workOrderProduceTransaction.getId(),
                                    null, null
                            );
                    Set<String> workOrderReverseProductionInventorySet =
                            workOrderReverseProductionInventories.stream().map(
                                    WorkOrderReverseProductionInventory::getLpn
                            ).collect(Collectors.toSet());
                    // loop through the produced inventory and skip the reversed inventory
                    workOrderProducedInventories.addAll(
                            workOrderProduceTransaction.getWorkOrderProducedInventories()
                                    .stream().filter(
                                            workOrderProducedInventory -> !workOrderReverseProductionInventorySet.contains(workOrderProducedInventory.getLpn())
                            ).collect(Collectors.toList())
                    );
                }
        );
        return workOrderProducedInventories;

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

        // loop through each transaction and get the produced inventory and ignore
        // the reversed inventory
        List<WorkOrderProducedInventory> workOrderProducedInventories = new ArrayList<>();
        workOrderProduceTransactions.forEach(
                workOrderProduceTransaction -> {
                    // get the reversed LPN for this transaction
                    List<WorkOrderReverseProductionInventory> workOrderReverseProductionInventories =
                            workOrderReverseProductionInventoryService.findAll(
                                    workOrderProduceTransaction.getId(),
                                    null, null
                            );
                    Set<String> workOrderReverseProductionInventorySet =
                            workOrderReverseProductionInventories.stream().map(
                                    WorkOrderReverseProductionInventory::getLpn
                            ).collect(Collectors.toSet());
                    // loop through the produced inventory and skip the reversed inventory
                    workOrderProducedInventories.addAll(
                            workOrderProduceTransaction.getWorkOrderProducedInventories()
                                    .stream().filter(
                                    workOrderProducedInventory -> !workOrderReverseProductionInventorySet.contains(workOrderProducedInventory.getLpn())
                            ).collect(Collectors.toList())
                    );
                }
        );

        return workOrderProducedInventories.stream().mapToLong(WorkOrderProducedInventory::getQuantity).sum();


    }

}

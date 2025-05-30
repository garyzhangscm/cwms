/**
 * Copyright 2019
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

package com.garyzhangscm.cwms.workorder.controller;

import com.garyzhangscm.cwms.workorder.clients.InventoryServiceRestemplateClient;
import org.hibernate.jdbc.Work;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.garyzhangscm.cwms.workorder.ResponseBodyWrapper;
import com.garyzhangscm.cwms.workorder.model.*;

import com.garyzhangscm.cwms.workorder.service.WorkOrderLineService;
import com.garyzhangscm.cwms.workorder.service.WorkOrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Sort;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
public class WorkOrderController {

    private static final Logger logger = LoggerFactory.getLogger(WorkOrderController.class);


    @Autowired
    WorkOrderService workOrderService;

    @Autowired
    WorkOrderLineService workOrderLineService;

    @Autowired
    private InventoryServiceRestemplateClient inventoryServiceRestemplateClient;



    @RequestMapping(value="/work-orders", method = RequestMethod.GET)
    public List<WorkOrder> findAllWorkOrders(@RequestParam Long warehouseId,
                                             @RequestParam(name="number", required = false, defaultValue = "") String number,
                                             @RequestParam(name="itemName", required = false, defaultValue = "") String itemName,
                                             @RequestParam(name="statusList", required = false, defaultValue = "") String statusList,
                                             @RequestParam(name="productionPlanId", required = false, defaultValue = "") Long productionPlanId) {

        return workOrderService.findAll(warehouseId, number, itemName, statusList, productionPlanId);

    }

    @RequestMapping(value="/work-orders/pagination", method = RequestMethod.GET)
    public Page<WorkOrder> findAllWorkOrders(@RequestParam Long warehouseId,
                                             @RequestParam(name="number", required = false, defaultValue = "") String number,
                                             @RequestParam(name="itemName", required = false, defaultValue = "") String itemName,
                                             @RequestParam(name="statusList", required = false, defaultValue = "") String statusList,
                                             @RequestParam(name="productionPlanId", required = false, defaultValue = "") Long productionPlanId,
                                             @RequestParam(name = "pageIndex", required = false, defaultValue = "0") int pageIndex,
                                             @RequestParam(name = "pageSize", required = false, defaultValue = "20") int pageSize) {
        Pageable paging = PageRequest.of(pageIndex, pageSize, Sort.by(Sort.Direction.DESC, "number"));
        return workOrderService.findAllByPagination(warehouseId, number, itemName, statusList, productionPlanId,paging);


    }

    @BillableEndpoint
    @RequestMapping(value="/work-orders", method = RequestMethod.POST)
    public WorkOrder addWorkOrder(@RequestBody WorkOrder workOrder) {
        return workOrderService.addWorkOrder(workOrder);
    }


    @RequestMapping(value="/work-orders/{id}", method = RequestMethod.GET)
    public WorkOrder findWorkOrder(@PathVariable Long id) {
        return workOrderService.findById(id);
    }

    @BillableEndpoint
    @RequestMapping(value="/work-orders/{id}", method = RequestMethod.PUT)
    @Caching(
            evict = {
                    @CacheEvict(cacheNames = "IntegrationService_WorkOrder", allEntries = true),
                    @CacheEvict(cacheNames = "InventoryService_WorkOrder", allEntries = true),
                    @CacheEvict(cacheNames = "OutboundService_WorkOrder", allEntries = true),
                    @CacheEvict(cacheNames = "OutboundService_WorkOrderLine", allEntries = true),
            }
    )
    public WorkOrder changeWorkOrder(@RequestBody WorkOrder workOrder){
        return workOrderService.changeWorkOrder(workOrder);
    }

    @BillableEndpoint
    @RequestMapping(value="/work-orders", method = RequestMethod.DELETE)
    @Caching(
            evict = {
                    @CacheEvict(cacheNames = "IntegrationService_WorkOrder", allEntries = true),
                    @CacheEvict(cacheNames = "InventoryService_WorkOrder", allEntries = true),
                    @CacheEvict(cacheNames = "OutboundService_WorkOrder", allEntries = true),
                    @CacheEvict(cacheNames = "OutboundService_WorkOrderLine", allEntries = true),
            }
    )
    public void removeWorkOrders(@RequestParam(name = "workOrderIds", required = false, defaultValue = "") String workOrderIds) {
        workOrderService.delete(workOrderIds);
    }

    @BillableEndpoint
    @RequestMapping(value="/work-orders/create-from-bom", method = RequestMethod.POST)
    public WorkOrder createWorkOrderFromBOM(@RequestParam Long billOfMaterialId,
                                            @RequestParam String workOrderNumber,
                                            @RequestParam Long expectedQuantity,
                                            @RequestParam(name="productionLineId", required = false) Long productionLineId) {
        return workOrderService.createWorkOrderFromBOM(billOfMaterialId,
                workOrderNumber, expectedQuantity, productionLineId);
    }


    @BillableEndpoint
    @RequestMapping(value="/work-orders/create-for-short-allocation", method = RequestMethod.POST)
    public WorkOrder createWorkOrderForShortAllocation(
            @RequestParam Long shortAllocationId,
            @RequestParam Long billOfMaterialId,
            @RequestParam String workOrderNumber,
            @RequestParam Long expectedQuantity,
            @RequestParam(name="productionLineId", required = false) Long productionLineId) {
        return workOrderService.createWorkOrderForShortAllocation(shortAllocationId, billOfMaterialId,
                workOrderNumber, expectedQuantity, productionLineId);
    }


    @BillableEndpoint
    @RequestMapping(value="/work-orders/{id}/allocate", method = RequestMethod.POST)
    @Caching(
            evict = {
                    @CacheEvict(cacheNames = "IntegrationService_WorkOrder", allEntries = true),
                    @CacheEvict(cacheNames = "InventoryService_WorkOrder", allEntries = true),
                    @CacheEvict(cacheNames = "OutboundService_WorkOrder", allEntries = true),
                    @CacheEvict(cacheNames = "OutboundService_WorkOrderLine", allEntries = true),
            }
    )
    public WorkOrder allocateWorkOrder(@PathVariable Long id,
                                       @RequestBody List<ProductionLineAllocationRequest> productionLineAllocationRequests) {
        logger.debug("Get request for allocate work order by id {}, productionLineAllocationRequest: {} ",
                id, productionLineAllocationRequests);
        return workOrderService.allocateWorkOrder(id, productionLineAllocationRequests);
    }

    @BillableEndpoint
    @RequestMapping(value="/work-orders/lines/{id}/short-allocation-cancelled", method = RequestMethod.POST)
    @Caching(
            evict = {
                    @CacheEvict(cacheNames = "IntegrationService_WorkOrder", allEntries = true),
                    @CacheEvict(cacheNames = "InventoryService_WorkOrder", allEntries = true),
                    @CacheEvict(cacheNames = "OutboundService_WorkOrder", allEntries = true),
                    @CacheEvict(cacheNames = "OutboundService_WorkOrderLine", allEntries = true),
            }
    )
    public void registerShortAllocationCancelled(@PathVariable Long id,
                                           @RequestParam Long cancelledQuantity) {
        workOrderLineService.registerShortAllocationCancelled(id, cancelledQuantity);
    }

    @RequestMapping(value="/work-orders/lines/{id}", method = RequestMethod.GET)
    public WorkOrderLine findWorkOrderLine(@PathVariable Long id) {
        return workOrderLineService.findById(id);
    }

    @BillableEndpoint
    @RequestMapping(value="/work-orders/lines/{id}/inventory-being-delivered", method = RequestMethod.POST)
    @Caching(
            evict = {
                    @CacheEvict(cacheNames = "IntegrationService_WorkOrder", allEntries = true),
                    @CacheEvict(cacheNames = "InventoryService_WorkOrder", allEntries = true),
                    @CacheEvict(cacheNames = "OutboundService_WorkOrder", allEntries = true),
                    @CacheEvict(cacheNames = "OutboundService_WorkOrderLine", allEntries = true),
            }
    )
    public WorkOrderMaterialConsumeTiming changeDeliveredQuantity(@PathVariable Long id,
                                                 @RequestParam Long quantityBeingDelivered,
                                                 @RequestParam Long deliveredLocationId,
                                                                  @RequestParam(name="inventoryId", required = false) Long inventoryId) {
        return workOrderLineService.changeDeliveredQuantity(id, quantityBeingDelivered, deliveredLocationId, inventoryId);
    }


    @BillableEndpoint
    @RequestMapping(value="/work-orders/lines/{id}/pick-cancelled", method = RequestMethod.POST)
    @Caching(
            evict = {
                    @CacheEvict(cacheNames = "IntegrationService_WorkOrder", allEntries = true),
                    @CacheEvict(cacheNames = "InventoryService_WorkOrder", allEntries = true),
                    @CacheEvict(cacheNames = "OutboundService_WorkOrder", allEntries = true),
                    @CacheEvict(cacheNames = "OutboundService_WorkOrderLine", allEntries = true),
            }
    )
    public void registerPickCancelled(@PathVariable Long id,
                                      @RequestParam Long cancelledQuantity,
                                      @RequestParam Long destinationLocationId) {
        workOrderLineService.registerPickCancelled(id, cancelledQuantity, destinationLocationId);
    }

    /***
    @RequestMapping(value="/work-orders/{id}/change-production-line", method = RequestMethod.POST)
    public WorkOrder changeProductionLine(@PathVariable Long id,
                                  @RequestParam Long productionLineId) {
        return workOrderService.changeProductionLine(id, productionLineId);
    }
***/

    @RequestMapping(value="/work-orders/{id}/produced-inventory", method = RequestMethod.GET)
    public List<Inventory> getProducedInventory(@PathVariable Long id) {
        return workOrderService.getProducedInventory(id);
    }

    @RequestMapping(value="/work-orders/{id}/produced-by-product", method = RequestMethod.GET)
    public List<Inventory> getProducedByProduct(@PathVariable Long id) {
        return workOrderService.getProducedByProduct(id);
    }


    @RequestMapping(value="/work-orders/{id}/delivered-inventory", method = RequestMethod.GET)
    public List<Inventory> getDeliveredInventory(@PathVariable Long id,
                                                 @RequestParam(name = "productionLineId", required = false, defaultValue = "") Long productionLineId) {
        return workOrderService.getDeliveredInventory(id, productionLineId);
    }


    @RequestMapping(value="/work-orders/{id}/kpi", method = RequestMethod.GET)
    public List<WorkOrderKPI> getKPIs(@PathVariable Long id) {
        return workOrderService.getKPIs(id);
    }


    @RequestMapping(value="/work-orders/{id}/kpi-transaction", method = RequestMethod.GET)
    public List<WorkOrderKPITransaction> getKPITransactions(@PathVariable Long id) {
        return workOrderService.getKPITransactions(id);
    }


    @RequestMapping(value="/work-orders/{id}/returned-inventory", method = RequestMethod.GET)
    public List<Inventory> getReturnedInventory(@PathVariable Long id) {
        return workOrderService.getReturnedInventory(id);
    }

    @BillableEndpoint
    @RequestMapping(value="/work-orders/{id}/modify-lines", method = RequestMethod.POST)
    @Caching(
            evict = {
                    @CacheEvict(cacheNames = "IntegrationService_WorkOrder", allEntries = true),
                    @CacheEvict(cacheNames = "InventoryService_WorkOrder", allEntries = true),
                    @CacheEvict(cacheNames = "OutboundService_WorkOrder", allEntries = true),
                    @CacheEvict(cacheNames = "OutboundService_WorkOrderLine", allEntries = true),
            }
    )
    public WorkOrder modifyWorkOrderLines(@PathVariable Long id,
                                          @RequestBody WorkOrder workOrder) {
        return workOrderService.modifyWorkOrderLines(id, workOrder);
    }

    @BillableEndpoint
    @RequestMapping(value="/work-orders/{id}/add-qc-quantity", method = RequestMethod.POST)
    @Caching(
            evict = {
                    @CacheEvict(cacheNames = "IntegrationService_WorkOrder", allEntries = true),
                    @CacheEvict(cacheNames = "InventoryService_WorkOrder", allEntries = true),
                    @CacheEvict(cacheNames = "OutboundService_WorkOrder", allEntries = true),
                    @CacheEvict(cacheNames = "OutboundService_WorkOrderLine", allEntries = true),
            }
    )
    public WorkOrder addQCQuantity(@PathVariable Long id,
                                   @RequestParam Long  qcQuantity) {
        return workOrderService.addQCQuantity(id, qcQuantity);
    }

    @BillableEndpoint
    @RequestMapping(value="/work-orders/{id}/reverse-production", method = RequestMethod.POST)
    @Caching(
            evict = {
                    @CacheEvict(cacheNames = "IntegrationService_WorkOrder", allEntries = true),
                    @CacheEvict(cacheNames = "InventoryService_WorkOrder", allEntries = true),
                    @CacheEvict(cacheNames = "OutboundService_WorkOrder", allEntries = true),
                    @CacheEvict(cacheNames = "OutboundService_WorkOrderLine", allEntries = true),
            }
    )
    public WorkOrder reverseProduction(@PathVariable Long id,
                                       @RequestParam String lpn) {
        return workOrderService.reverseProduction(id, lpn);
    }

    @BillableEndpoint
    @RequestMapping(value="/work-orders/{id}/reverse-by-product", method = RequestMethod.POST)
    @Caching(
            evict = {
                    @CacheEvict(cacheNames = "IntegrationService_WorkOrder", allEntries = true),
                    @CacheEvict(cacheNames = "InventoryService_WorkOrder", allEntries = true),
                    @CacheEvict(cacheNames = "OutboundService_WorkOrder", allEntries = true),
                    @CacheEvict(cacheNames = "OutboundService_WorkOrderLine", allEntries = true),
            }
    )
    public WorkOrder reverseByProduct(@PathVariable Long id,
                                       @RequestParam String lpn) {
        return workOrderService.reverseByProduct(id, lpn);
    }

    @BillableEndpoint
    @RequestMapping(value="/work-orders/{id}/unpick-inventory", method = RequestMethod.POST)
    @Caching(
            evict = {
                    @CacheEvict(cacheNames = "IntegrationService_WorkOrder", allEntries = true),
                    @CacheEvict(cacheNames = "InventoryService_WorkOrder", allEntries = true),
                    @CacheEvict(cacheNames = "OutboundService_WorkOrder", allEntries = true),
                    @CacheEvict(cacheNames = "OutboundService_WorkOrderLine", allEntries = true),
            }
    )
    public Inventory unpickInventory(@PathVariable Long id,
                                           @RequestParam Long inventoryId,
                                           @RequestParam(name = "unpickedQuantity", required = false, defaultValue = "") Long unpickedQuantity,
                                           @RequestParam(name = "overrideConsumedQuantity", required = false, defaultValue = "false") Boolean overrideConsumedQuantity,
                                           @RequestParam(name = "consumedQuantity", required = false, defaultValue = "")  Long consumedQuantity,
                                           @RequestParam(name = "destinationLocationId", required = false, defaultValue = "") Long destinationLocationId,
                                           @RequestParam(name = "destinationLocationName", required = false, defaultValue = "") String destinationLocationName,
                                           @RequestParam(name = "immediateMove", required = false, defaultValue = "true") boolean immediateMove) {
        return workOrderService.unpickInventory(id, inventoryId, unpickedQuantity,  overrideConsumedQuantity, consumedQuantity,
                destinationLocationId, destinationLocationName, immediateMove);
    }


    @BillableEndpoint
    @RequestMapping(value="/work-orders/validate-new-number", method = RequestMethod.POST)
    public ResponseBodyWrapper<String> validateNewNumber(@RequestParam Long warehouseId,
                                                         @RequestParam String number) {
        return ResponseBodyWrapper.success(workOrderService.validateNewNumber(warehouseId, number));
    }

    @BillableEndpoint
    @RequestMapping(value="/work-orders/{id}/pick-report", method = RequestMethod.POST)
    public ReportHistory generateOrderPickReport(
            @PathVariable Long id,
            @RequestParam(name = "locale", defaultValue = "", required = false) String locale,
            @RequestParam(name = "printerName", defaultValue = "", required = false) String printerName) throws IOException {

        logger.debug("start print pick sheet for order with id: {}", id);
        return workOrderService.generatePickReportByWorkOrder(id, locale, printerName);
    }


    @RequestMapping(value="/work-orders-with-open-pick", method = RequestMethod.GET)
    public List<WorkOrder> getWorkOrdersWithOpenPick(@RequestParam Long warehouseId) {
        return workOrderService.getWorkOrdersWithOpenPick(warehouseId);
    }


    @BillableEndpoint
    @RequestMapping(value="/work-orders/{id}/consume-method", method = RequestMethod.POST)
    @Caching(
            evict = {
                    @CacheEvict(cacheNames = "IntegrationService_WorkOrder", allEntries = true),
                    @CacheEvict(cacheNames = "InventoryService_WorkOrder", allEntries = true),
                    @CacheEvict(cacheNames = "OutboundService_WorkOrder", allEntries = true),
                    @CacheEvict(cacheNames = "OutboundService_WorkOrderLine", allEntries = true),
            }
    )
    public WorkOrder changeConsumeMethod(
            @PathVariable Long id,
            @RequestParam String materialConsumeTiming,
            @RequestParam(name = "consumeByBomFlag", defaultValue = "", required = false) Boolean consumeByBomFlag,
            @RequestParam(name = "consumeByBOMId", defaultValue = "", required = false) Long consumeByBOMId) {

        return workOrderService.changeConsumeMethod(id, materialConsumeTiming, consumeByBomFlag, consumeByBOMId);
    }


    @BillableEndpoint
    @RequestMapping(value="/work-orders/{id}/pre-print-lpn-label", method = RequestMethod.POST)
    public ReportHistory generatePrePrintLPNLabel(
            @PathVariable Long id,
            @RequestParam String lpn,
            @RequestParam(name = "printerName", defaultValue = "", required = false) String printerName,
            @RequestParam(name = "quantity", defaultValue = "", required = false) Long quantity,
            @RequestParam(name = "productionLineName", defaultValue = "", required = false) String productionLineName,
            @RequestParam(name = "locale", defaultValue = "", required = false) String locale
            ) throws JsonProcessingException {

        logger.debug("start generate pre-printed lpn label with id: {}", id);
        return workOrderService.generatePrePrintLPNLabel(id, lpn, quantity, productionLineName, locale, printerName);
    }

    @BillableEndpoint
    @RequestMapping(value="/work-orders/{id}/pre-print-lpn-label/batch", method = RequestMethod.POST)
    public ReportHistory generatePrePrintLPNLabelInBatch(
            @PathVariable Long id,
            @RequestParam String lpn,
            @RequestParam(name = "quantity", defaultValue = "", required = false) Long lpnQuantity,
            @RequestParam(name = "productionLineName", defaultValue = "", required = false) String productionLineName,
            @RequestParam(name = "count", defaultValue = "1", required = false) Integer count,
            @RequestParam(name = "copies", defaultValue = "1", required = false) Integer copies,
            @RequestParam(name = "locale", defaultValue = "", required = false) String locale,
            @RequestParam(name = "printerName", defaultValue = "", required = false) String printerName
    ) throws JsonProcessingException {

        logger.debug("start generate pre-printed lpn label with id: {}", id);
        return workOrderService.generatePrePrintLPNLabelInBatch(id, lpn, lpnQuantity,
                count, copies, productionLineName, locale, printerName);
    }


    /**
     * Recalculate the qc quantity for the work order. We can specify the qc quantity and percentage, or let
     * the system run the configuration again to refresh the qc quantity required
     * @param workOrderId
     * @param qcQuantity
     * @param qcPercentage
     * @return
     */
    @BillableEndpoint
    @RequestMapping(value="/work-orders/{workOrderId}/recalculate-qc-quantity", method = RequestMethod.POST)
    @Caching(
            evict = {
                    @CacheEvict(cacheNames = "IntegrationService_WorkOrder", allEntries = true),
                    @CacheEvict(cacheNames = "InventoryService_WorkOrder", allEntries = true),
                    @CacheEvict(cacheNames = "OutboundService_WorkOrder", allEntries = true),
                    @CacheEvict(cacheNames = "OutboundService_WorkOrderLine", allEntries = true),
            }
    )
    public WorkOrder recalculateQCQuantity(@PathVariable Long workOrderId,
                                             @RequestParam(name = "qcQuantity", required = false, defaultValue = "") Long qcQuantity,
                                             @RequestParam(name = "qcPercentage", required = false, defaultValue = "") Double qcPercentage) {
        return workOrderService.recalculateQCQuantity(workOrderId, qcQuantity, qcPercentage);
    }


    @RequestMapping(value="/work-orders/available-for-mps", method = RequestMethod.GET)
    public List<WorkOrder> getAvailableWorkOrderForMPS(
            @RequestParam Long warehouseId,
            @RequestParam Long itemId) {

        return workOrderService.getAvailableWorkOrderForMPS(warehouseId,
                itemId);
    }


    @BillableEndpoint
    @RequestMapping(value="/work-orders/{workOrderId}/generate-manual-pick", method = RequestMethod.POST)
    @Caching(
            evict = {
                    @CacheEvict(cacheNames = "IntegrationService_WorkOrder", allEntries = true),
                    @CacheEvict(cacheNames = "InventoryService_WorkOrder", allEntries = true),
                    @CacheEvict(cacheNames = "OutboundService_WorkOrder", allEntries = true),
                    @CacheEvict(cacheNames = "OutboundService_WorkOrderLine", allEntries = true),
            }
    )
    public List<Pick> generateManualPick(@PathVariable  Long workOrderId,
                                         @RequestParam String lpn,
                                         @RequestParam Long productionLineId,
                                         @RequestParam Boolean pickWholeLPN) {
        logger.debug("======        Start to generateManualPick pick   ========");
        logger.debug("=> workOrderId: {}", workOrderId);
        logger.debug("=> lpn: {}", lpn);
        logger.debug("=> productionLineId: {}", productionLineId);
        logger.debug("=> pickWholeLPN: {}", pickWholeLPN);
        return workOrderService.generateManualPick(workOrderId, lpn, productionLineId, pickWholeLPN);
    }


    @BillableEndpoint
    @RequestMapping(value="/work-orders/{workOrderId}/process-manual-pick", method = RequestMethod.POST)
    @Caching(
            evict = {
                    @CacheEvict(cacheNames = "IntegrationService_WorkOrder", allEntries = true),
                    @CacheEvict(cacheNames = "InventoryService_WorkOrder", allEntries = true),
                    @CacheEvict(cacheNames = "OutboundService_WorkOrder", allEntries = true),
                    @CacheEvict(cacheNames = "OutboundService_WorkOrderLine", allEntries = true),
            }
    )
    public List<Pick> processManualPick(@RequestParam Long warehouseId,
                                        @PathVariable  Long workOrderId,
                                        @RequestParam String lpn,
                                        @RequestParam Long productionLineId,
                                        @RequestParam Long nextLocationId,
                                        @RequestParam Boolean pickWholeLPN) {
        logger.debug("======        Start to processManualPick pick   ========");
        logger.debug("=> workOrderId: {}", workOrderId);
        logger.debug("=> lpn: {}", lpn);
        logger.debug("=> productionLineId: {}", productionLineId);
        logger.debug("=> pickWholeLPN: {}", pickWholeLPN);
        logger.debug("=> nextLocationId: {}", nextLocationId);
        return workOrderService.processManualPick(warehouseId, workOrderId, lpn, productionLineId, nextLocationId, pickWholeLPN);
    }

    @BillableEndpoint
    @RequestMapping(value="/work-orders/{workOrderId}/get-manual-pick-quantity", method = RequestMethod.GET)
    public Long getPickableQuantityForManualPick(@PathVariable  Long workOrderId,
                                                 @RequestParam String lpn,
                                                 @RequestParam Long productionLineId,
                                                 @RequestParam(name = "pickWholeLPN", required = false, defaultValue = "") Boolean pickWholeLPN) {
        logger.debug("======        Start to getPickableQuantityForManualPick pick   ========");
        logger.debug("=> workOrderId: {}", workOrderId);
        logger.debug("=> lpn: {}", lpn);
        logger.debug("=> productionLineId: {}", productionLineId);
        logger.debug("=> pickWholeLPN: {}", pickWholeLPN);
        return workOrderService.getPickableQuantityForManualPick(workOrderId, lpn, productionLineId, pickWholeLPN);
    }


    @BillableEndpoint
    @RequestMapping(value="/work-orders/lines/{id}/spare-parts", method = RequestMethod.POST)
    @Caching(
            evict = {
                    @CacheEvict(cacheNames = "IntegrationService_WorkOrder", allEntries = true),
                    @CacheEvict(cacheNames = "InventoryService_WorkOrder", allEntries = true),
                    @CacheEvict(cacheNames = "OutboundService_WorkOrder", allEntries = true),
                    @CacheEvict(cacheNames = "OutboundService_WorkOrderLine", allEntries = true),
            }
    )
    public WorkOrderLine changeSpareParts(@PathVariable Long id,
                                          @RequestBody List<WorkOrderLineSparePart> workOrderLineSpareParts) {
        return workOrderLineService.changeSpareParts(id, workOrderLineSpareParts);
    }


    @RequestMapping(value="/work-orders/item-override", method = RequestMethod.POST)
    @Caching(
            evict = {
                    @CacheEvict(cacheNames = "IntegrationService_WorkOrder", allEntries = true),
                    @CacheEvict(cacheNames = "InventoryService_WorkOrder", allEntries = true),
                    @CacheEvict(cacheNames = "OutboundService_WorkOrder", allEntries = true),
                    @CacheEvict(cacheNames = "OutboundService_WorkOrderLine", allEntries = true),
            }
    )
    public ResponseBodyWrapper<String> handleItemOverride(
            @RequestParam Long warehouseId,
            @RequestParam Long oldItemId,
            @RequestParam Long newItemId
    ) {
        workOrderService.handleItemOverride(warehouseId,
                oldItemId, newItemId);


        return ResponseBodyWrapper.success("success");
    }


    // Graphql
    @QueryMapping
    public WorkOrder workOrderById(@Argument Long id) {
        return workOrderService.findById(id);
    }

    @QueryMapping
    public List<WorkOrder> findWorkOrders(@Argument Long warehouseId,
                                          @Argument  String number,
                                          @Argument  String itemName,
                                          @Argument  String statusList,
                                          @Argument  Long productionPlanId,
                                          @Argument  int pageIndex,
                                          @Argument  int pageSize) {
        return workOrderService.findAll(warehouseId, number,
                itemName, statusList, productionPlanId,
                PageRequest.of(pageIndex, pageSize, Sort.by(Sort.Direction.DESC, "number")));
    }

    @SchemaMapping(typeName="WorkOrder", field="item")
    public Item item(WorkOrder workOrder) {
        return inventoryServiceRestemplateClient.getItemById(workOrder.getItemId());
    }

    @SchemaMapping
    public InventoryStatus inventoryStatus(WorkOrderLine workOrderLine) {
        return inventoryServiceRestemplateClient.getInventoryStatusById(workOrderLine.getInventoryStatusId());
    }

}

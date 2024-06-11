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

package com.garyzhangscm.cwms.outbound.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.garyzhangscm.cwms.outbound.ResponseBodyWrapper;
import com.garyzhangscm.cwms.outbound.model.*;
import com.garyzhangscm.cwms.outbound.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;


import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;

@RestController
public class OrderController {

    private static final Logger logger = LoggerFactory.getLogger(OrderController.class);

    @Autowired
    OrderService orderService;
    @Autowired
    OrderLineService orderLineService;
    @Autowired
    FileService fileService;
    @Autowired
    private UploadFileService uploadFileService;
    @Autowired
    private OrderBillableActivityService orderBillableActivityService;


    @ClientValidationEndpoint
    @RequestMapping(value="/orders", method = RequestMethod.GET)
    public List<Order> findAllOrders(@RequestParam Long warehouseId,
                                     @RequestParam(name="number", required = false, defaultValue = "") String number,
                                     @RequestParam(name="numbers", required = false, defaultValue = "") String numbers,
                                     @RequestParam(name="status", required = false, defaultValue = "") String status,
                                     @RequestParam(name="category", required = false, defaultValue = "") String category,
                                     @RequestParam(name="customerName", required = false, defaultValue = "") String customerName,
                                     @RequestParam(name="customerId", required = false, defaultValue = "") Long customerId,
                                     @RequestParam(name="clientId", required = false, defaultValue = "") Long clientId,
                                     @RequestParam(name="trailerAppointmentId", required = false, defaultValue = "") Long trailerAppointmentId,
                                     @RequestParam(name="loadDetails", required = false, defaultValue = "true") Boolean loadDetails,
                                     @RequestParam(name = "startCompleteTime", required = false, defaultValue = "") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime startCompleteTime,
                                     @RequestParam(name = "endCompleteTime", required = false, defaultValue = "") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)  ZonedDateTime endCompleteTime,
                                     @RequestParam(name = "specificCompleteDate", required = false, defaultValue = "") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate specificCompleteDate,
                                     @RequestParam(name = "startCreatedTime", required = false, defaultValue = "") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime startCreatedTime,
                                     @RequestParam(name = "endCreatedTime", required = false, defaultValue = "") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)  ZonedDateTime endCreatedTime,
                                     @RequestParam(name = "specificCreatedDate", required = false, defaultValue = "") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate specificCreatedDate,
                                     @RequestParam(name = "poNumber", required = false, defaultValue = "") String poNumber,
                                     ClientRestriction clientRestriction) {
        logger.debug("Start to find order by number {}", number);
        return orderService.findAll(warehouseId, number, numbers, status, startCompleteTime, endCompleteTime, specificCompleteDate,
                startCreatedTime, endCreatedTime, specificCreatedDate,
                category,  customerName, customerId, clientId, trailerAppointmentId, poNumber, loadDetails,
                clientRestriction);
    }

    @BillableEndpoint
    @RequestMapping(value="/orders", method = RequestMethod.POST)
    public Order addOrders(@RequestBody Order order) {
        return orderService.addOrders(order);
    }


    @RequestMapping(value="/orders/{id}", method = RequestMethod.GET)
    public Order findOrder(@PathVariable Long id) {
        return orderService.findById(id);
    }


    @BillableEndpoint
    @RequestMapping(value="/orders/{id}", method = RequestMethod.DELETE)
    @Caching(
            evict = {
                    @CacheEvict(cacheNames = "AdminService_Order", allEntries = true),
                    @CacheEvict(cacheNames = "IntegrationService_Order", allEntries = true),
                    @CacheEvict(cacheNames = "WorkOrderService_OrderLine", allEntries = true),
            }
    )
    public ResponseBodyWrapper<String> removeOrder(@PathVariable Long id) {

         orderService.removeOrder(id);
         return ResponseBodyWrapper.success("Success");
    }

    @BillableEndpoint
    @RequestMapping(value="/orders/{id}", method = RequestMethod.PUT)
    @Caching(
            evict = {
                    @CacheEvict(cacheNames = "AdminService_Order", allEntries = true),
                    @CacheEvict(cacheNames = "IntegrationService_Order", allEntries = true),
                    @CacheEvict(cacheNames = "WorkOrderService_OrderLine", allEntries = true),
            }
    )
    public Order changeOrder(@RequestBody Order order){
        return orderService.save(order);
    }

    @BillableEndpoint
    @RequestMapping(value="/orders/{id}/allocate", method = RequestMethod.POST)
    @Caching(
            evict = {
                    @CacheEvict(cacheNames = "AdminService_Order", allEntries = true),
                    @CacheEvict(cacheNames = "IntegrationService_Order", allEntries = true),
                    @CacheEvict(cacheNames = "WorkOrderService_OrderLine", allEntries = true),
            }
    )
    public Order allocateOrder(@PathVariable Long id,
                               @RequestParam(name = "asynchronous", required = false, defaultValue = "") Boolean asynchronous){

        return orderService.allocate(id, asynchronous);
    }

    @BillableEndpoint
    @RequestMapping(value="/orders/{id}/change-stage-loctions", method = RequestMethod.POST)
    @Caching(
            evict = {
                    @CacheEvict(cacheNames = "AdminService_Order", allEntries = true),
                    @CacheEvict(cacheNames = "IntegrationService_Order", allEntries = true),
                    @CacheEvict(cacheNames = "WorkOrderService_OrderLine", allEntries = true),
            }
    )
    public Order changeAssignedStageLocations(@PathVariable Long id,
                                      @RequestParam(name = "locationGroupId", required = false, defaultValue = "") Long locationGroupId,
                                      @RequestParam(name = "locationId", required = false, defaultValue = "") Long locationId){

        return orderService.changeAssignedStageLocations(id, locationGroupId, locationId);
    }

    @BillableEndpoint
    @RequestMapping(value="/orders/{id}/complete", method = RequestMethod.POST)
    @Caching(
            evict = {
                    @CacheEvict(cacheNames = "AdminService_Order", allEntries = true),
                    @CacheEvict(cacheNames = "IntegrationService_Order", allEntries = true),
                    @CacheEvict(cacheNames = "WorkOrderService_OrderLine", allEntries = true),
            }
    )
    public Order completeOrder(@PathVariable Long id,
                               @RequestBody Order order){
        return orderService.completeOrder(id, order);
    }

    @BillableEndpoint
    @RequestMapping(value="/orders/{id}/stage", method = RequestMethod.POST)
    @Caching(
            evict = {
                    @CacheEvict(cacheNames = "AdminService_Order", allEntries = true),
                    @CacheEvict(cacheNames = "IntegrationService_Order", allEntries = true),
                    @CacheEvict(cacheNames = "WorkOrderService_OrderLine", allEntries = true),
            }
    )
    public Order stageOrder(@PathVariable Long id,
                            @RequestParam(name = "ignoreUnfinishedPicks", required = false, defaultValue = "false") boolean ignoreUnfinishedPicks){
        return orderService.stage(id, ignoreUnfinishedPicks);
    }

    @BillableEndpoint
    @RequestMapping(value="/orders/{id}/load", method = RequestMethod.POST)
    @Caching(
            evict = {
                    @CacheEvict(cacheNames = "AdminService_Order", allEntries = true),
                    @CacheEvict(cacheNames = "IntegrationService_Order", allEntries = true),
                    @CacheEvict(cacheNames = "WorkOrderService_OrderLine", allEntries = true),
            }
    )
    public Order loadOrder(@PathVariable Long id,
                           @RequestParam(name = "ignoreUnfinishedPicks", required = false, defaultValue = "false") boolean ignoreUnfinishedPicks){
        return orderService.load(id, ignoreUnfinishedPicks);
    }

    @BillableEndpoint
    @RequestMapping(value="/orders/{id}/dispatch", method = RequestMethod.POST)
    @Caching(
            evict = {
                    @CacheEvict(cacheNames = "AdminService_Order", allEntries = true),
                    @CacheEvict(cacheNames = "IntegrationService_Order", allEntries = true),
                    @CacheEvict(cacheNames = "WorkOrderService_OrderLine", allEntries = true),
            }
    )
    public Order dispatchOrder(@PathVariable Long id,
                               @RequestParam(name = "ignoreUnfinishedPicks", required = false, defaultValue = "false") boolean ignoreUnfinishedPicks){
        return orderService.dispatch(id, ignoreUnfinishedPicks);
    }


    @RequestMapping(value="/orders/{id}/next-line-number", method = RequestMethod.GET)
    public ResponseBodyWrapper getNextOrderLineNumber(@PathVariable Long id) {
        return ResponseBodyWrapper.success(orderService.getNextOrderLineNumber(id));
    }


    @BillableEndpoint
    @RequestMapping(value="/orders/{id}/pick-report", method = RequestMethod.POST)
    public ReportHistory generateOrderPickReport(
            @PathVariable Long id,
            @RequestParam(name = "locale", defaultValue = "", required = false) String locale) throws JsonProcessingException {

        logger.debug("start print pick sheet for order with id: {}", id);
        return orderService.generatePickReportByOrder(id, locale);
    }

    @BillableEndpoint
    @RequestMapping(value="/orders/{id}/packing-list-report", method = RequestMethod.POST)
    public ReportHistory generatePackingListReport(
            @PathVariable Long id,
            @RequestParam(name = "locale", defaultValue = "", required = false) String locale) throws JsonProcessingException {

        logger.debug("start print packing list for order with id: {}", id);
        return orderService.generatePackingListByOrder(id, locale);
    }

    @BillableEndpoint
    @RequestMapping(value="/orders/{id}/bill-of-lading-report", method = RequestMethod.POST)
    public ReportHistory generateBillOfLadingReport(
            @PathVariable Long id,
            @RequestParam(name = "locale", defaultValue = "", required = false) String locale) throws JsonProcessingException {

        logger.debug("start print bill of lading for order with id: {}", id);
        return orderService.generateBillOfLadingByOrder(id, locale);
    }



    @RequestMapping(value="/orders-with-open-pick", method = RequestMethod.GET)
    public List<Order> getOrdersWithOpenPick(@RequestParam Long warehouseId) {
        return orderService.getOrdersWithOpenPick(warehouseId);
    }



    @RequestMapping(method=RequestMethod.POST, value="/orders/validate-new-order-number")
    public ResponseBodyWrapper<String> validateNewOrderNumber(@RequestParam Long warehouseId,
                                                              @RequestParam String orderNumber,
                                                              @RequestParam(name = "clientId", required = false, defaultValue = "") Long clientId)  {

        return ResponseBodyWrapper.success(orderService.validateNewOrderNumber(warehouseId, clientId, orderNumber));
    }

    @RequestMapping(method=RequestMethod.POST, value="/orders/{id}/retrigger-order-confirm-integration")
    public Order retriggerOrderConfirmIntegration(@RequestParam Long warehouseId,
                                                              @PathVariable Long id)  {

        return orderService.retriggerOrderConfirmIntegration(id);
    }

    @RequestMapping(value="/orders/open-for-stop", method = RequestMethod.GET)
    public List<Order> getOpenOrdersForStop(@RequestParam Long warehouseId,
                                            @RequestParam(name="number", required = false, defaultValue = "") String number){

        return orderService.getOpenOrdersForStop(warehouseId, number);
    }


    @BillableEndpoint
    @RequestMapping(value="/orders/{orderId}/generate-manual-pick", method = RequestMethod.POST)
    @Caching(
            evict = {
                    @CacheEvict(cacheNames = "AdminService_Order", allEntries = true),
                    @CacheEvict(cacheNames = "IntegrationService_Order", allEntries = true),
                    @CacheEvict(cacheNames = "WorkOrderService_OrderLine", allEntries = true),
            }
    )
    public List<Pick> generateManualPick(@PathVariable  Long orderId,
                                         @RequestParam String lpn,
                                         @RequestParam Boolean pickWholeLPN) {
        logger.debug("======        Start to generateManualPick pick   ========");
        logger.debug("=> orderId: {}", orderId);
        logger.debug("=> lpn: {}", lpn);
        logger.debug("=> pickWholeLPN: {}", pickWholeLPN);
        return orderService.generateManualPick(orderId, lpn, pickWholeLPN);
    }

    @BillableEndpoint
    @RequestMapping(value="/orders/process-manual-pick", method = RequestMethod.POST)
    @Caching(
            evict = {
                    @CacheEvict(cacheNames = "AdminService_Order", allEntries = true),
                    @CacheEvict(cacheNames = "IntegrationService_Order", allEntries = true),
                    @CacheEvict(cacheNames = "WorkOrderService_OrderLine", allEntries = true),
            }
    )
    public List<Pick> processManualPick(@RequestParam String lpn,
                                        @RequestParam Long warehouseId,
                                        @RequestParam String orderNumber,
                                        @RequestParam(required = false, defaultValue = "", name = "clientId") Long clientId,
                                        @RequestParam(required = false, defaultValue = "true", name = "pickWholeLPN") Boolean pickWholeLPN) {
        logger.debug("======        Start to processManualPick pick   ========");
        logger.debug("=> warehouseId: {}", warehouseId);
        logger.debug("=> clientId: {}", clientId);
        logger.debug("=> orderNumber: {}", orderNumber);
        logger.debug("=> lpn: {}", lpn);
        logger.debug("=> pickWholeLPN: {}", pickWholeLPN);
        return orderService.processManualPick(warehouseId, clientId, orderNumber, lpn, pickWholeLPN);
    }



    @BillableEndpoint
    @RequestMapping(method=RequestMethod.POST, value="/orders/upload")
    @Caching(
            evict = {
                    @CacheEvict(cacheNames = "AdminService_Order", allEntries = true),
                    @CacheEvict(cacheNames = "IntegrationService_Order", allEntries = true),
                    @CacheEvict(cacheNames = "WorkOrderService_OrderLine", allEntries = true),
            }
    )
    public ResponseBodyWrapper uploadOrders(Long companyId, Long warehouseId,
                                            @RequestParam(name = "ignoreUnknownFields", defaultValue = "false", required = false) Boolean ignoreUnknownFields,
                                            @RequestParam("file") MultipartFile file) throws IOException {


        try {

            File localFile = uploadFileService.convertToCSVFile(
                    companyId, warehouseId, "orders", fileService.saveFile(file), ignoreUnknownFields);

            String fileUploadProgressKey = orderService.saveOrderData(warehouseId, localFile);
            return  ResponseBodyWrapper.success(fileUploadProgressKey);
        }
        catch (Exception ex) {
            return new ResponseBodyWrapper(-1, ex.getMessage(), "");
        }

    }
    @RequestMapping(method=RequestMethod.GET, value="/orders/upload/progress")
    public ResponseBodyWrapper getOrderFileUploadProgress(Long warehouseId,
                                                            String key) throws IOException {



        return  ResponseBodyWrapper.success(
                String.format("%.2f",orderService.getOrderFileUploadProgress(key)));
    }
    @RequestMapping(method=RequestMethod.GET, value="/orders/upload/result")
    public List<FileUploadResult> getOrderFileUploadResult(Long warehouseId,
                                                             String key) throws IOException {


        return orderService.getOrderFileUploadResult(warehouseId, key);
    }

    @RequestMapping(method=RequestMethod.PUT, value="/orders/{orderId}/billable-activities")
    public OrderBillableActivity addOrderBillableActivity(Long warehouseId,
                                                              @PathVariable Long orderId,
                                                              @RequestBody OrderBillableActivity orderBillableActivity) throws IOException {


        return orderBillableActivityService.addOrderBillableActivity(orderId, orderBillableActivity);
    }

    @RequestMapping(method=RequestMethod.DELETE, value="/orders/{orderId}/billable-activities/{id}")
    public ResponseBodyWrapper<String> removeOrderBillableActivity(Long warehouseId,
                                                                     @PathVariable Long orderId,
                                                                     @PathVariable Long id) throws IOException {


        orderBillableActivityService.removeOrderBillableActivity(id);
        return ResponseBodyWrapper.success("order billable activity is removed");
    }


    @RequestMapping(method=RequestMethod.POST, value="/orders/{orderId}/billable-activities/{id}")
    public OrderBillableActivity changeOrderBillableActivity(Long warehouseId,
                                                                 @PathVariable Long orderId,
                                                                 @PathVariable Long id,
                                                                 @RequestBody OrderBillableActivity orderBillableActivity)  {


        return orderBillableActivityService.changeOrderBillableActivity(orderBillableActivity);
    }



    @RequestMapping(method=RequestMethod.POST, value="/orders/cancel-order")
    public OrderCancellationRequest cancelOrder(Long warehouseId,
                                                   @RequestParam(name = "orderId", required = false, defaultValue = "") Long orderId,
                                                   @RequestParam(name = "clientId", required = false, defaultValue = "") Long clientId,
                                                   @RequestParam(name = "clientName", required = false, defaultValue = "") String clientName,
                                                   @RequestParam(name = "orderNumber", required = false, defaultValue = "") String orderNumber) {


        return orderService.cancelOrder(warehouseId, orderId, clientId, clientName, orderNumber);


    }
    @RequestMapping(method=RequestMethod.POST, value="/orders/clear-order-cancelleation-request")
    public Order clearOrderCancellationRequest(Long warehouseId,
                                                   @RequestParam(name = "orderId", required = false, defaultValue = "") Long orderId,
                                                   @RequestParam(name = "clientId", required = false, defaultValue = "") Long clientId,
                                                   @RequestParam(name = "clientName", required = false, defaultValue = "") String clientName,
                                                   @RequestParam(name = "orderNumber", required = false, defaultValue = "") String orderNumber) {


        return orderService.clearOrderCancellationRequest(warehouseId, orderId, clientId, clientName, orderNumber);

    }

    @RequestMapping(method=RequestMethod.GET, value="/orders/quantity-in-order")
    @ClientValidationEndpoint
    public Long getQuantityInOrder(Long warehouseId,
                                   @RequestParam(name = "clientId", required = false, defaultValue = "") Long clientId,
                                   Long itemId,
                                   Long inventoryStatusId,
                                   @RequestParam(name = "color", required = false, defaultValue = "") String color,
                                   @RequestParam(name = "productSize", required = false, defaultValue = "") String productSize,
                                   @RequestParam(name = "style", required = false, defaultValue = "") String style,
                                   @RequestParam(name = "inventoryAttribute1", required = false, defaultValue = "") String inventoryAttribute1,
                                   @RequestParam(name = "inventoryAttribute2", required = false, defaultValue = "") String inventoryAttribute2,
                                   @RequestParam(name = "inventoryAttribute3", required = false, defaultValue = "") String inventoryAttribute3,
                                   @RequestParam(name = "inventoryAttribute4", required = false, defaultValue = "") String inventoryAttribute4,
                                   @RequestParam(name = "inventoryAttribute5", required = false, defaultValue = "") String inventoryAttribute5,
                                   boolean exactMatch, ClientRestriction clientRestriction) {


        return orderService.getQuantityInOrder(
                warehouseId, clientId, itemId,
                inventoryStatusId, color, productSize, style,
                inventoryAttribute1, inventoryAttribute2, inventoryAttribute3,
                inventoryAttribute4,inventoryAttribute5,
                exactMatch,
                clientRestriction);

    }


    @RequestMapping(method=RequestMethod.GET, value="/orders/open-order/count")
    @ClientValidationEndpoint
    public Integer getOpenOrderCount(Long warehouseId,
                                     ClientRestriction clientRestriction) {


        return orderService.getOpenOrderCount(
                warehouseId,
                clientRestriction);

    }
    @RequestMapping(method=RequestMethod.GET, value="/orders/today-order/count")
    @ClientValidationEndpoint
    public Integer getTodayOrderCount(Long warehouseId,
                                     ClientRestriction clientRestriction) {


        return orderService.getTodayOrderCount(
                warehouseId,
                clientRestriction);

    }
    @RequestMapping(method=RequestMethod.GET, value="/orders/today-complete-order/count")
    @ClientValidationEndpoint
    public Integer getTodayCompletedOrderCount(Long warehouseId,
                                      ClientRestriction clientRestriction) {


        return orderService.getTodayCompletedOrderCount(
                warehouseId,
                clientRestriction);

    }

    @RequestMapping(method=RequestMethod.POST, value="/orders/{id}/walmart-shipping-carton-labels/generate")
    public ReportHistory generateWalmartShippingCartonLabels(Long warehouseId,
                                                             @PathVariable Long id,
                                                             @RequestParam(name = "itemName", defaultValue = "", required = false) String itemName,
                                                             @RequestParam(name = "copies", defaultValue = "1", required = false) int copies,
            @RequestParam(name = "locale", defaultValue = "", required = false) String locale)   {


        return orderService.generateWalmartShippingCartonLabels(
                warehouseId,
                id, itemName, copies, locale);

    }

    @RequestMapping(method=RequestMethod.POST, value="/orders/{id}/walmart-shipping-carton-labels/generate-with-pallet-label")
    public List<ReportHistory> generateWalmartShippingCartonLabelsWithPalletLabels(Long warehouseId,
                                                             @PathVariable Long id,
                                                             @RequestParam(name = "itemName", defaultValue = "", required = false) String itemName,
                                                             @RequestParam(name = "copies", defaultValue = "1", required = false) int copies,
                                                             @RequestParam(name = "locale", defaultValue = "", required = false) String locale,
                                                                                   @RequestParam(name = "regeneratePalletLabels", defaultValue = "false", required = false) Boolean regeneratePalletLabels)   {


        return orderService.generateWalmartShippingCartonLabelsWithPalletLabels(
                warehouseId,
                id, copies, locale,
                regeneratePalletLabels);

    }


    @RequestMapping(method=RequestMethod.GET, value="/orders/{id}/walmart-shipping-carton-labels")
    public List<WalmartShippingCartonLabel> getWalmartShippingCartonLabels(Long warehouseId,
                                                             @PathVariable Long id,
                                                             @RequestParam(name = "itemName", defaultValue = "", required = false) String itemName,
                                                                           @RequestParam(name = "nonAssignedOnly", defaultValue = "true", required = false) Boolean nonAssignedOnly,
                                                                           @RequestParam(name = "nonPrintedOnly", defaultValue = "true", required = false) Boolean nonPrintedOnly)   {


        return orderService.getWalmartShippingCartonLabels(
                warehouseId,
                id, itemName,
                nonAssignedOnly, nonPrintedOnly);

    }

    @RequestMapping(method=RequestMethod.POST, value="/orders/{id}/target-shipping-carton-labels/generate-with-pallet-label/combined")
    public ReportHistory generateCombinedTargetShippingCartonLabelsWithPalletLabels(Long warehouseId,
                                                                                  @PathVariable Long id,
                                                                                  @RequestParam(name = "itemName", defaultValue = "", required = false) String itemName,
                                                                                  @RequestParam(name = "copies", defaultValue = "1", required = false) int copies,
                                                                                  @RequestParam(name = "locale", defaultValue = "", required = false) String locale,
                                                                                  @RequestParam(name = "regeneratePalletLabels", defaultValue = "false", required = false) Boolean regeneratePalletLabels)   {


        return orderService.generateCombinedTargetShippingCartonLabelsWithPalletLabels(
                warehouseId,
                id, copies, locale,
                regeneratePalletLabels);

    }


    @RequestMapping(method=RequestMethod.POST, value="/orders/{id}/target-shipping-carton-labels/generate-with-pallet-label")
    public List<ReportHistory> generateTargetShippingCartonLabelsWithPalletLabels(Long warehouseId,
                                                                                  @PathVariable Long id,
                                                                                  @RequestParam(name = "itemName", defaultValue = "", required = false) String itemName,
                                                                                  @RequestParam(name = "copies", defaultValue = "1", required = false) int copies,
                                                                                  @RequestParam(name = "locale", defaultValue = "", required = false) String locale,
                                                                                  @RequestParam(name = "regeneratePalletLabels", defaultValue = "false", required = false) Boolean regeneratePalletLabels)   {


        return orderService.generateTargetShippingCartonLabelsWithPalletLabels(
                warehouseId,
                id, copies, locale,
                regeneratePalletLabels);

    }

    @RequestMapping(method=RequestMethod.POST, value="/orders/{id}/target-shipping-carton-labels/generate")
    public ReportHistory generateTargetShippingCartonLabels(Long warehouseId,
                                                                  @PathVariable Long id,
                                                                  @RequestParam(name = "itemName", defaultValue = "", required = false) String itemName,
                                                                  @RequestParam(name = "copies", defaultValue = "1", required = false) int copies,
                                                                  @RequestParam(name = "locale", defaultValue = "", required = false) String locale)   {


        return orderService.generateTargetShippingCartonLabels(
                warehouseId,
                id, itemName, copies, locale);

    }

    @RequestMapping(method=RequestMethod.GET, value="/orders/{id}/target-shipping-carton-labels")
    public List<TargetShippingCartonLabel> getTargetShippingCartonLabels(Long warehouseId,
                                                                           @PathVariable Long id,
                                                                           @RequestParam(name = "itemName", defaultValue = "", required = false) String itemName,
                                                                           @RequestParam(name = "nonAssignedOnly", defaultValue = "true", required = false) Boolean nonAssignedOnly,
                                                                           @RequestParam(name = "nonPrintedOnly", defaultValue = "true", required = false) Boolean nonPrintedOnly)   {


        return orderService.getTargetShippingCartonLabels(
                warehouseId,
                id, itemName,
                nonAssignedOnly, nonPrintedOnly);

    }


    @BillableEndpoint
    @RequestMapping(value="/orders/{id}/get-manual-pick-quantity", method = RequestMethod.GET)
    public Long getPickableQuantityForManualPick(@PathVariable  Long id,
                                                 @RequestParam String lpn,
                                                 @RequestParam(name = "pickWholeLPN", required = false, defaultValue = "") Boolean pickWholeLPN) {
        logger.debug("======        Start to getPickableQuantityForManualPick pick   ========");
        logger.debug("=> orderId: {}", id);
        logger.debug("=> lpn: {}", lpn);
        logger.debug("=> pickWholeLPN: {}", pickWholeLPN);
        return orderService.getPickableQuantityForManualPick(id, lpn, pickWholeLPN);
    }

}

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

package com.garyzhangscm.cwms.inbound.service;


import com.garyzhangscm.cwms.inbound.clients.InventoryServiceRestemplateClient;
import com.garyzhangscm.cwms.inbound.clients.WarehouseLayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.inbound.exception.ReceiptOperationException;
import com.garyzhangscm.cwms.inbound.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.inbound.model.*;
import com.garyzhangscm.cwms.inbound.repository.CustomerReturnOrderLineRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class CustomerReturnOrderLineService {
    private static final Logger logger = LoggerFactory.getLogger(CustomerReturnOrderLineService.class);

    @Autowired
    private CustomerReturnOrderLineRepository customerReturnOrderLineRepository;

    @Autowired
    private CustomerReturnOrderService customerReturnOrderService;
    @Autowired
    private InboundQCConfigurationService inboundQCConfigurationService;

    @Autowired
    private InventoryServiceRestemplateClient inventoryServiceRestemplateClient;
    @Autowired
    private WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient;



    public CustomerReturnOrderLine findById(Long id) {
        return findById(id, true);
    }

    public CustomerReturnOrderLine findById(Long id, boolean includeDetails) {
        CustomerReturnOrderLine customerReturnOrderLine = customerReturnOrderLineRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("customer return order line not found by id: " + id));
        if (includeDetails) {
            loadAttribute(customerReturnOrderLine);
        }
        return customerReturnOrderLine;
    }
    public List<CustomerReturnOrderLine> findAll() {
        return findAll(true);
    }
    public List<CustomerReturnOrderLine> findAll(boolean includeDetails) {


        List<CustomerReturnOrderLine> customerReturnOrderLines = customerReturnOrderLineRepository.findAll();
        if (customerReturnOrderLines.size() > 0 && includeDetails) {
            loadAttribute(customerReturnOrderLines);
        }
        return customerReturnOrderLines;
    }

    public CustomerReturnOrderLine findByNaturalKey(Long warehouseId, Long receiptId, String number) {
        return customerReturnOrderLineRepository.findByNaturalKey(warehouseId, receiptId, number);

    }


    public void loadAttribute(List<CustomerReturnOrderLine> customerReturnOrderLines) {
        for(CustomerReturnOrderLine customerReturnOrderLine : customerReturnOrderLines) {
            loadAttribute(customerReturnOrderLine);
        }
    }

    public void loadAttribute(CustomerReturnOrderLine customerReturnOrderLine) {

        if (customerReturnOrderLine.getWarehouseId() != null && customerReturnOrderLine.getWarehouse() == null) {
            customerReturnOrderLine.setWarehouse(warehouseLayoutServiceRestemplateClient.getWarehouseById(
                    customerReturnOrderLine.getWarehouseId()));

        }
        // Load Item information
        if (customerReturnOrderLine.getItemId() != null && customerReturnOrderLine.getItem() == null) {
            customerReturnOrderLine.setItem(inventoryServiceRestemplateClient.getItemById(
                    customerReturnOrderLine.getItemId()));

        }
        // load the receipt number
        if (Objects.nonNull(customerReturnOrderLine.getCustomerReturnOrder())) {
            customerReturnOrderLine.setCustomerReturnOrderNumber(customerReturnOrderLine.getCustomerReturnOrder().getNumber());
            customerReturnOrderLine.setCustomerReturnOrderId(customerReturnOrderLine.getCustomerReturnOrder().getId());
        }

    }

    @Transactional
    public CustomerReturnOrderLine save(CustomerReturnOrderLine customerReturnOrderLine) {
        return customerReturnOrderLineRepository.save(customerReturnOrderLine);
    }

    @Transactional
    public CustomerReturnOrderLine saveOrUpdate(CustomerReturnOrderLine customerReturnOrderLine) {

        if (customerReturnOrderLine.getId() == null &&
                findByNaturalKey(customerReturnOrderLine.getWarehouseId(), customerReturnOrderLine.getCustomerReturnOrder().getId(),
                        customerReturnOrderLine.getNumber()) != null) {
            customerReturnOrderLine.setId(
                    findByNaturalKey(customerReturnOrderLine.getWarehouseId(),customerReturnOrderLine.getCustomerReturnOrder().getId(),
                            customerReturnOrderLine.getNumber()).getId());
        }
        if (Objects.isNull(customerReturnOrderLine.getId())) {
            // we are creating a new receipt line,
            // let's setup the qc quantity
            setupQCQuantity(
                    customerReturnOrderLine.getCustomerReturnOrder(),
                    customerReturnOrderLine
            );
        }
        return save(customerReturnOrderLine);
    }

    @Transactional
    public void delete(CustomerReturnOrderLine customerReturnOrderLine) {
        customerReturnOrderLineRepository.delete(customerReturnOrderLine);
    }
    @Transactional
    public void delete(Long id) {
        customerReturnOrderLineRepository.deleteById(id);
    }


    @Transactional
    public List<Inventory> receive(Long customerReturnOrderId, Long customerReturnOrderLineId,
                             List<Inventory> inventoryList){


        return inventoryList.stream().map(
                inventory -> receive(customerReturnOrderId, customerReturnOrderLineId, inventory)).collect(Collectors.toList());
    }
    @Transactional
    public Inventory receive(Long customerReturnOrderId, Long customerReturnOrderLineId,
                             Inventory inventory){
        // Receive inventory and save it on the receipt

        CustomerReturnOrder customerReturnOrder = customerReturnOrderService.findById(customerReturnOrderId);
        CustomerReturnOrderLine customerReturnOrderLine = findById(customerReturnOrderLineId);
        // If the inventory has location passed in, we will directly receive the inventory into
        // the location.
        // Otherwise, we will receive the location on the receipt and let the putaway logic
        // to decide where to put this LPN away

        // Validate if we can receive the inventory
        // 1. over receiving?
        // 3. unexpected item number?
        validateReceiving(customerReturnOrder, customerReturnOrderLine, inventory);
        boolean qcRequired = checkQCRequired(customerReturnOrder, customerReturnOrderLine, inventory);
        logger.debug("inventory {} received from customer return order line {} / {} needs QC? {}",
                inventory.getLpn(), customerReturnOrder.getNumber(), customerReturnOrderLine.getNumber(),
                qcRequired);
        if (qcRequired == true) {

            // qc is required, let's get the destination inventory status for QC
            // anc change the inventory automatically
            InboundQCConfiguration bestMatchedInboundQCConfiguration
                    = getBestMatchedInboundQCConfiguration(
                        customerReturnOrder, customerReturnOrderLine, inventory
                    );
            InventoryStatus toInventoryStatus =
                    bestMatchedInboundQCConfiguration.getToInventoryStatus();
            if (Objects.isNull(toInventoryStatus)) {
                toInventoryStatus = inventoryServiceRestemplateClient.getInventoryStatusById(
                        bestMatchedInboundQCConfiguration.getToInventoryStatusId()
                );
            }
            inventory.setInventoryStatus(toInventoryStatus);
            inventory.setInboundQCRequired(true);

        }
        else {

            inventory.setInboundQCRequired(false);
        }

        if (inventory.getLocation() == null) {
            Location location =
                    warehouseLayoutServiceRestemplateClient.getLocationByName(
                            customerReturnOrder.getWarehouseId(), customerReturnOrder.getNumber());
            inventory.setLocationId(location.getId());
            inventory.setLocation(location);
        }
        /**
        else {
            inventory.setVirtual(inventory.getLocation().getLocationGroup().getLocationGroupType().getVirtual());
        }
         **/

        // inventory should be always actual after it is received
        inventory.setVirtual(false);
        // Everytime when we check in a receipt, we will create a location with the same name so that
        // we can receive inventory on this receipt
        inventory.setCustomerReturnOrderId(customerReturnOrderId);
        inventory.setCustomerReturnOrderLineId(customerReturnOrderLineId);
        inventory.setWarehouseId(customerReturnOrder.getWarehouseId());

        logger.debug("Will receive inventory\n {}", inventory);
        Inventory newInventory =
                inventoryServiceRestemplateClient.receiveInventory(inventory, customerReturnOrder.getNumber());
        // Note here when we receive, the inventory may already consolidate with existing inventory
        // in the location and the newInventory may represent the inventory after consolidated.
        // so we need to calculate the received quantity on the line based off the original
        // inventory structure
        customerReturnOrderLine.setReceivedQuantity(customerReturnOrderLine.getReceivedQuantity() + inventory.getQuantity());
        if (qcRequired) {
            customerReturnOrderLine.setQcQuantityRequested(
                    customerReturnOrderLine.getQcQuantityRequested() +  inventory.getQuantity()
            );
        }
        save(customerReturnOrderLine);
        customerReturnOrder.setStatus(ReceiptStatus.RECEIVING);
        customerReturnOrderService.saveOrUpdate(customerReturnOrder);

        newInventory.setInboundQCRequired(qcRequired);
        return newInventory;
    }

    private boolean checkQCRequired(CustomerReturnOrder customerReturnOrder, CustomerReturnOrderLine customerReturnOrderLine,
                                    Inventory inventory) {

        Long qcQuantityNeeded =
                customerReturnOrderLine.getQcQuantity() > 0 ?
                        customerReturnOrderLine.getQcQuantity() :
                        (long)(customerReturnOrderLine.getExpectedQuantity() * customerReturnOrderLine.getQcPercentage() / 100);
        logger.debug("Customer return order line line {} / {}, receipt line qc: {} / {}, qc quantity needed? {}, qc quantity requested: {}",
                customerReturnOrder.getNumber(), customerReturnOrderLine.getNumber(),
                customerReturnOrderLine.getQcQuantity(), customerReturnOrderLine.getQcPercentage(),
                qcQuantityNeeded, customerReturnOrderLine.getQcQuantityRequested());
        if (customerReturnOrderLine.getQcQuantityRequested() >= qcQuantityNeeded) {
            return false;
        }

        return Objects.nonNull(
                getBestMatchedInboundQCConfiguration(
                        customerReturnOrder, customerReturnOrderLine, inventory
                )
        );

    }

    private InboundQCConfiguration getBestMatchedInboundQCConfiguration(
            CustomerReturnOrder customerReturnOrder, CustomerReturnOrderLine customerReturnOrderLine, Inventory inventory
    ) {

        Warehouse warehouse = customerReturnOrder.getWarehouse();
        if (Objects.isNull(warehouse)) {
            warehouse = warehouseLayoutServiceRestemplateClient.getWarehouseById(
                    customerReturnOrder.getWarehouseId()
            );
        }
        if (Objects.isNull(warehouse)) {
            // we should not arrive here！
            logger.debug("Can't get the QC configuration as we can't get the warehouse" +
                    "information from the customer return");
            logger.debug("=======   Customer Return ======= \n {}",
                    customerReturnOrder);
        }
        return
                inboundQCConfigurationService.getBestMatchedInboundQCConfiguration(
                        null,
                        Objects.isNull(customerReturnOrderLine.getItem().getItemFamily()) ? null :
                                customerReturnOrderLine.getItem().getItemFamily().getId(),
                        customerReturnOrderLine.getItemId(),
                        inventory.getInventoryStatus().getId(),
                        customerReturnOrder.getWarehouseId(),
                        warehouse.getCompany().getId()
                );

    }

    // validate whether we can receive inventory against this receipt line
    // 1. over receiving?
    // 3. unexpected item number?
    private void validateReceiving(CustomerReturnOrder customerReturnOrder,
            CustomerReturnOrderLine customerReturnOrderLine, Inventory inventory) {
        // unexpected item number?

        if (!customerReturnOrder.getAllowUnexpectedItem() &&
             !customerReturnOrderLine.getItemId().equals(
                     inventory.getItem().getId())) {
            throw ReceiptOperationException.raiseException("Unexpected item not allowed for this receipt");
        }

        // check how many more we can receive against this receipt line
        Long maxOverReceivingQuantityAllowedByQuantity = 0L;
        Long maxOverReceivingQuantityAllowedByPercentage = 0L;

        if (customerReturnOrderLine.getOverReceivingQuantity() > 0) {
            maxOverReceivingQuantityAllowedByQuantity = customerReturnOrderLine.getOverReceivingQuantity();
        }

        if (customerReturnOrderLine.getOverReceivingPercent() > 0) {
            maxOverReceivingQuantityAllowedByPercentage =
                    (long) (customerReturnOrderLine.getExpectedQuantity() * customerReturnOrderLine.getOverReceivingPercent() / 100);
        }
        Long maxOverReceivingQuantityAllowed = Math.max(
                maxOverReceivingQuantityAllowedByQuantity, maxOverReceivingQuantityAllowedByPercentage);

        // See should we receive this inventory, will the quantity maximum the total quantity allowed
        if (customerReturnOrderLine.getReceivedQuantity() + inventory.getQuantity() >
                customerReturnOrderLine.getExpectedQuantity() + maxOverReceivingQuantityAllowed) {
            if (maxOverReceivingQuantityAllowed == 0) {
                // over receiving is not allowed in this receipt line
                throw ReceiptOperationException.raiseException("Over receiving is not allowed");
            }
            if (maxOverReceivingQuantityAllowed > 0) {
                // over receiving is not allowed in this receipt line
                throw ReceiptOperationException.raiseException("Over receiving. The maximum you can receive for this line is " +
                        (customerReturnOrderLine.getExpectedQuantity() + maxOverReceivingQuantityAllowed - customerReturnOrderLine.getReceivedQuantity()));
            }
        }


    }

    public CustomerReturnOrderLine reverseReceivedInventory(Long customerReturnOrderId, Long customerReturnOrderLineId, Long quantity,
                                                Boolean inboundQCRequired, Boolean reverseQCQuantity) {
        CustomerReturnOrderLine customerReturnOrderLine = findById(customerReturnOrderLineId);

        customerReturnOrderLine.setReceivedQuantity(
                customerReturnOrderLine.getReceivedQuantity() - quantity > 0 ?
                        customerReturnOrderLine.getReceivedQuantity() - quantity : 0);
        if (Boolean.TRUE.equals(inboundQCRequired) &&
            Boolean.TRUE.equals(reverseQCQuantity)) {
            // we just reserved an inventory that needs QC, let's return
            // the qc quantity back
            customerReturnOrderLine.setQcQuantityRequested(
                    customerReturnOrderLine.getQcQuantityRequested() - quantity > 0 ?
                            customerReturnOrderLine.getQcQuantityRequested() - quantity  : 0);
        }

        return saveOrUpdate(customerReturnOrderLine);
    }

    /**
     * Setup qc quantity based on
     * 1. supplier
     * 2. item
     * 3. warehouse
     * 4. company
     * from most specific to most generic
     */
    public void setupQCQuantity(CustomerReturnOrder customerReturnOrder, CustomerReturnOrderLine customerReturnOrderLine) {

        logger.debug("Start to setup qc quantity for customer return order line {} / {}",
                customerReturnOrder.getNumber(), customerReturnOrderLine.getNumber());
        // default to the qc quantity to 0
        customerReturnOrderLine.setQcQuantity(0l);
        customerReturnOrderLine.setQcPercentage(0d);

        Warehouse warehouse = customerReturnOrder.getWarehouse();
        if (Objects.isNull(warehouse)) {
            warehouse = warehouseLayoutServiceRestemplateClient.getWarehouseById(
                    customerReturnOrder.getWarehouseId()
            );
        }
        if (Objects.isNull(warehouse)) {
            // we should not arrive here！
            logger.debug("Can't get the QC configuration as we can't get the warehouse" +
                    "information from the receipt");
            logger.debug("=======   Customer Return ======= \n {}",
                    customerReturnOrder);
            return;
        }
        Item item =
                Objects.nonNull(customerReturnOrderLine.getItem()) ? customerReturnOrderLine.getItem() :
                        inventoryServiceRestemplateClient.getItemById(customerReturnOrderLine.getItemId());

        InboundQCConfiguration inboundQCConfiguration =
                inboundQCConfigurationService.getBestMatchedInboundQCConfiguration(
                        null,
                        Objects.isNull(item.getItemFamily()) ? null : item.getItemFamily().getId(),
                        customerReturnOrderLine.getItemId(),
                        customerReturnOrder.getWarehouseId(),
                        warehouse.getCompany().getId()
                );
        if (Objects.isNull(inboundQCConfiguration)) {
            logger.debug("No inbound qc configuration is defined for the receipt line {} / {}",
                    customerReturnOrder.getNumber(), customerReturnOrderLine.getNumber());
            logger.debug("item {} / {}, warehouse {} / {}, company {} / {}",

                    customerReturnOrderLine.getItemId(),
                    Objects.isNull(customerReturnOrderLine.getItem()) ? "" : customerReturnOrderLine.getItem().getName(),
                    warehouse.getId(),
                    warehouse.getName(),
                    Objects.isNull(warehouse.getCompany()) ? "" : warehouse.getCompany().getId(),
                    Objects.isNull(warehouse.getCompany()) ? "" : warehouse.getCompany().getName());
            return;
        }
        // setup the qc quantity and percentage based on the configuration
        if (Objects.nonNull(inboundQCConfiguration.getQcQuantityPerReceipt())) {

            customerReturnOrderLine.setQcQuantity(inboundQCConfiguration.getQcQuantityPerReceipt());
        }
        if (Objects.nonNull(inboundQCConfiguration.getQcPercentage())) {

            customerReturnOrderLine.setQcPercentage(inboundQCConfiguration.getQcPercentage());
        }

    }

    /**
     *
     * Recalculate the qc quantity for the receipt line. We can specify the qc quantity and percentage, or let
     * the system run the configuration again to refresh the qc quantity required
     *
     * @param qcQuantity
     * @param qcPercentage
     * @return
     */
    public CustomerReturnOrderLine recalculateQCQuantity(Long customerReturnOrderLineId, Long qcQuantity, Double qcPercentage) {
        CustomerReturnOrderLine customerReturnOrderLine = findById(customerReturnOrderLineId);
        if (Objects.isNull(qcQuantity) && Objects.isNull(qcPercentage)) {
            // the user doesn't specify any field, let's re-run the configuration to get the
            // quantity or percentage

            setupQCQuantity(customerReturnOrderLine.getCustomerReturnOrder(), customerReturnOrderLine);
        }
        // if the user specify at least quantity of percentage, then update the field
        // based on the user's input
        else{
            if (Objects.nonNull(qcQuantity)){
                customerReturnOrderLine.setQcQuantity(qcQuantity);
            }
            if (Objects.nonNull(qcPercentage)){
                customerReturnOrderLine.setQcPercentage(qcPercentage);
            }
        }

        return saveOrUpdate(customerReturnOrderLine);
    }
}

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

package com.garyzhangscm.cwms.integration.controller;


import com.garyzhangscm.cwms.integration.ResponseBodyWrapper;
import com.garyzhangscm.cwms.integration.model.*;
import com.garyzhangscm.cwms.integration.service.IntegrationDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping(value = "/integration-data")
public class IntegrationDataController {


    private static final Logger logger = LoggerFactory.getLogger(IntegrationDataController.class);

    @Autowired
    private IntegrationDataService integrationDataService;


    //
    // Client Related
    //
    @RequestMapping(value="/clients", method = RequestMethod.GET)
    public List<? extends IntegrationClientData> getIntegrationClientData() {

        return integrationDataService.getClientData();
    }

    @RequestMapping(value="/clients/{id}", method = RequestMethod.GET)
    public IntegrationClientData getIntegrationClientData(@PathVariable Long id) {

        return integrationDataService.getClientData(id);
    }

    @RequestMapping(value="/clients", method = RequestMethod.PUT)
    public IntegrationClientData addIntegrationClientData(@RequestBody Client client) {

        return integrationDataService.addIntegrationClientData(client);
    }

    //
    // Integration - Customer
    //
    @RequestMapping(value="/customers", method = RequestMethod.GET)
    public List<? extends IntegrationCustomerData> getIntegrationCustomerData() {

        return integrationDataService.getCustomerData();
    }

    @RequestMapping(value="/customers/{id}", method = RequestMethod.GET)
    public IntegrationCustomerData getIntegrationCustomerData(@PathVariable Long id) {

        return integrationDataService.getCustomerData(id);
    }

    @RequestMapping(value="/customers", method = RequestMethod.PUT)
    public IntegrationCustomerData addIntegrationCustomerData(@RequestBody Customer customer) {

        return integrationDataService.addIntegrationCustomerData(customer);
    }

    //
    // Integration - Item
    //
    @RequestMapping(value="/items", method = RequestMethod.GET)
    public List<? extends IntegrationItemData> getIntegrationItemData() {

        return integrationDataService.getItemData();
    }

    @RequestMapping(value="/items/{id}", method = RequestMethod.GET)
    public IntegrationItemData getIntegrationItemData(@PathVariable Long id) {

        return integrationDataService.getItemData(id);
    }

    @RequestMapping(value="/items", method = RequestMethod.PUT)
    public IntegrationItemData addIntegrationItemData(@RequestBody Item item) {

        return integrationDataService.addIntegrationItemData(item);
    }

    //
    // Integration - Item Family
    //
    @RequestMapping(value="/item-families", method = RequestMethod.GET)
    public List<? extends IntegrationItemFamilyData> getIntegrationItemFamilyData() {

        return integrationDataService.getItemFamilyData();
    }

    @RequestMapping(value="/item-families/{id}", method = RequestMethod.GET)
    public IntegrationItemFamilyData getIntegrationItemFamilyData(@PathVariable Long id) {

        return integrationDataService.getItemFamilyData(id);
    }

    @RequestMapping(value="/item-families", method = RequestMethod.PUT)
    public IntegrationItemFamilyData addIntegrationItemFamilyData(@RequestBody ItemFamily itemFamily) {

        return integrationDataService.addIntegrationItemFamilyData(itemFamily);
    }

    //
    // Integration - Item Package Type
    //
    @RequestMapping(value="/item-package-types", method = RequestMethod.GET)
    public List<? extends IntegrationItemPackageTypeData> getIntegrationItemPackageTypeData() {

        return integrationDataService.getItemPackageTypeData();
    }

    @RequestMapping(value="/item-package-types/{id}", method = RequestMethod.GET)
    public IntegrationItemPackageTypeData getIntegrationItemPackageTypeData(@PathVariable Long id) {

        return integrationDataService.getItemPackageTypeData(id);
    }

    @RequestMapping(value="/item-package-types", method = RequestMethod.PUT)
    public IntegrationItemPackageTypeData addIntegrationItemPackageTypeData(@RequestBody ItemPackageType itemPackageType) {

        return integrationDataService.addIntegrationItemPackageTypeData(itemPackageType);
    }
    //
    // Integration - Item Unit Of Measure
    //
    @RequestMapping(value="/item-unit-of-measures", method = RequestMethod.GET)
    public List<? extends IntegrationItemUnitOfMeasureData> getIntegrationItemUnitOfMeasureData() {

        return integrationDataService.getItemUnitOfMeasureData();
    }

    @RequestMapping(value="/item-unit-of-measures/{id}", method = RequestMethod.GET)
    public IntegrationItemUnitOfMeasureData getIntegrationItemUnitOfMeasureData(@PathVariable Long id) {

        return integrationDataService.getItemUnitOfMeasureData(id);
    }

    @RequestMapping(value="/item-unit-of-measures", method = RequestMethod.PUT)
    public IntegrationItemUnitOfMeasureData addIntegrationItemUnitOfMeasureData(@RequestBody ItemUnitOfMeasure itemUnitOfMeasure) {

        return integrationDataService.addIntegrationItemUnitOfMeasureData(itemUnitOfMeasure);
    }


    //
    // Supplier Related
    //
    @RequestMapping(value="/suppliers", method = RequestMethod.GET)
    public List<? extends IntegrationSupplierData> getIntegrationSupplierData() {

        return integrationDataService.getSupplierData();
    }

    @RequestMapping(value="/suppliers/{id}", method = RequestMethod.GET)
    public IntegrationSupplierData getIntegrationSupplierData(@PathVariable Long id) {

        return integrationDataService.getSupplierData(id);
    }

    @RequestMapping(value="/suppliers", method = RequestMethod.PUT)
    public IntegrationSupplierData addIntegrationClientData(@RequestBody Supplier supplier) {

        return integrationDataService.addIntegrationSupplierData(supplier);
    }

    //
    // Order Related
    //
    @RequestMapping(value="/orders", method = RequestMethod.GET)
    public List<? extends IntegrationOrderData> getIntegrationOrderData() {

        return integrationDataService.getOrderData();
    }

    @RequestMapping(value="/orders/{id}", method = RequestMethod.GET)
    public IntegrationOrderData getIntegrationOrderData(@PathVariable Long id) {

        return integrationDataService.getOrderData(id);
    }
    @RequestMapping(value="/orders", method = RequestMethod.PUT)
    public IntegrationOrderData addIntegrationOrderData(@RequestBody Order order) {

        return integrationDataService.addOrderData(order);
    }


    //
    // Receipt Related
    //
    @RequestMapping(value="/receipts", method = RequestMethod.GET)
    public List<? extends IntegrationReceiptData> getIntegrationReceiptData() {

        return integrationDataService.getReceiptData();
    }

    @RequestMapping(value="/receipts/{id}", method = RequestMethod.GET)
    public IntegrationReceiptData getIntegrationReceiptData(@PathVariable Long id) {

        return integrationDataService.getReceiptData(id);
    }

    @RequestMapping(value="/receipts", method = RequestMethod.PUT)
    public IntegrationReceiptData addIntegrationReceiptData(@RequestBody Receipt receipt) {

        logger.debug("Start to add receipt: \n{}", receipt);
        return integrationDataService.addReceiptData(receipt);
    }

    //
    // Order Confirmation Related
    //
    @RequestMapping(value="/order-confirmations", method = RequestMethod.GET)
    public List<? extends IntegrationOrderConfirmationData> getIntegrationOrderConfirmationData(
            @RequestParam(name = "warehouseId", required = false, defaultValue = "") Long warehouseId,
            @RequestParam(name = "warehouseName", required = false, defaultValue = "") String warehouseName,
            @RequestParam(name = "number", required = false, defaultValue = "") String number) {

        return integrationDataService.getIntegrationOrderConfirmationData(warehouseId, warehouseName,
                number);
    }

    @RequestMapping(value="/order-confirmations/{id}", method = RequestMethod.GET)
    public IntegrationOrderConfirmationData getIntegrationOrderConfirmationData(@PathVariable Long id) {

        return integrationDataService.getIntegrationOrderConfirmationData(id);
    }

    @RequestMapping(value="/work-order", method = RequestMethod.PUT)
    public ResponseBodyWrapper saveIntegrationWorkOrderData(
            @RequestBody DBBasedWorkOrder dbBasedWorkOrder
    ){

        logger.debug("Start to save dbbased work order into database \n{}",
                dbBasedWorkOrder);
        integrationDataService.addIntegrationWorkOrderData(dbBasedWorkOrder);
        return ResponseBodyWrapper.success("success");
    }


    @RequestMapping(value="/item", method = RequestMethod.PUT)
    public ResponseBodyWrapper saveIntegrationItemData(
            @RequestBody DBBasedItem dbBasedItem
    ){

        logger.debug("Start to save dbBasedItem into database \n{}",
                dbBasedItem);
        integrationDataService.addIntegrationItemData(dbBasedItem);
        return ResponseBodyWrapper.success("success");
    }

    @RequestMapping(value="/item-package-type", method = RequestMethod.PUT)
    public ResponseBodyWrapper saveIntegrationItemPackageTypeData(
            @RequestBody DBBasedItemPackageType dbBasedItemPackageType
    ){

        logger.debug("Start to save dbBasedItemPackageType into database \n{}",
                dbBasedItemPackageType);
        integrationDataService.addIntegrationItemPackageTypeData(dbBasedItemPackageType);
        return ResponseBodyWrapper.success("success");
    }

    //
    // Work Order Confirmation Related
    //
    @RequestMapping(value="/work-order-confirmations", method = RequestMethod.GET)
    public List<? extends IntegrationWorkOrderConfirmationData> getIntegrationWorkOrderConfirmationData(
            @RequestParam(name = "warehouseId", required = false, defaultValue = "") Long warehouseId,
            @RequestParam(name = "warehouseName", required = false, defaultValue = "") String warehouseName,
            @RequestParam(name = "number", required = false, defaultValue = "") String number) {

        return integrationDataService.getIntegrationWorkOrderConfirmationData(warehouseId, warehouseName,
                number);
    }

    @RequestMapping(value="/work-order-confirmations/{id}", method = RequestMethod.GET)
    public IntegrationWorkOrderConfirmationData getIntegrationWorkOrderConfirmationData(@PathVariable Long id) {

        return integrationDataService.getIntegrationWorkOrderConfirmationData(id);
    }
    //
    // Receipt Confirmation Related
    //
    @RequestMapping(value="/receipt-confirmations", method = RequestMethod.GET)
    public List<? extends IntegrationReceiptConfirmationData> getIntegrationReceiptConfirmationData(
            @RequestParam(name = "warehouseId", required = false, defaultValue = "") Long warehouseId,
            @RequestParam(name = "warehouseName", required = false, defaultValue = "") String warehouseName,
            @RequestParam(name = "number", required = false, defaultValue = "") String number,
            @RequestParam(name = "clientId", required = false, defaultValue = "") Long clientId,
            @RequestParam(name = "clientName", required = false, defaultValue = "") String clientName,
            @RequestParam(name = "supplierId", required = false, defaultValue = "") Long supplierId,
            @RequestParam(name = "supplierName", required = false, defaultValue = "") String supplierName
    ) {

        return integrationDataService.getIntegrationReceiptConfirmationData(warehouseId, warehouseName,
                number, clientId, clientName,
                supplierId, supplierName);
    }

    @RequestMapping(value="/receipt-confirmations/{id}", method = RequestMethod.GET)
    public IntegrationReceiptConfirmationData getIntegrationReceiptConfirmationData(@PathVariable Long id) {

        return integrationDataService.getIntegrationReceiptConfirmationData(id);
    }

    //
    // Inventory Adjustment Confirmation Related
    //
    @RequestMapping(value="/inventory-adjustment-confirmations", method = RequestMethod.GET)
    public List<? extends IntegrationInventoryAdjustmentConfirmationData> getInventoryAdjustmentConfirmationData() {

        return integrationDataService.getInventoryAdjustmentConfirmationData();
    }

    @RequestMapping(value="/inventory-adjustment-confirmations/{id}", method = RequestMethod.GET)
    public IntegrationInventoryAdjustmentConfirmationData getInventoryAdjustmentConfirmationData(@PathVariable Long id) {

        return integrationDataService.getInventoryAdjustmentConfirmationData(id);
    }

    //
    // Inventory Attribute Change Confirmation Related
    //
    @RequestMapping(value="/inventory-attribute-change-confirmations", method = RequestMethod.GET)
    public List<? extends IntegrationInventoryAttributeChangeConfirmationData> getInventoryAttributeChangeConfirmationData() {
        return integrationDataService.getInventoryAttributeChangeConfirmationData();
    }
    @RequestMapping(value="/inventory-attribute-change-confirmations/{id}", method = RequestMethod.GET)
    public IntegrationInventoryAttributeChangeConfirmationData getInventoryAttributeChangeConfirmationData(Long id) {
        return integrationDataService.getInventoryAttributeChangeConfirmationData(id);
    }
}

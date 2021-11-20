package com.garyzhangscm.cwms.integration.service;

import com.garyzhangscm.cwms.integration.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;

import javax.transaction.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class IntegrationDataService {

    @Autowired
    private Integration integration;

    public List<? extends IntegrationClientData> getClientData(
            Long warehouseId, LocalDateTime startTime, LocalDateTime endTime, LocalDate date) {
        return integration.getClientData(
                warehouseId, startTime, endTime, date
        );
    }

    public IntegrationClientData getClientData(Long id) {
        return integration.getClientData(id);
    }

    public IntegrationClientData addIntegrationClientData(Client client) {

        return integration.addIntegrationClientData(client);
    }


    //
    // Integration - Customer
    //
    public List<? extends IntegrationCustomerData> getCustomerData(
            Long warehouseId, LocalDateTime startTime, LocalDateTime endTime, LocalDate date) {
        return integration.getCustomerData(
                warehouseId, startTime, endTime, date);
    }
    public IntegrationCustomerData getCustomerData(Long id) {
        return integration.getCustomerData(id);
    }
    public IntegrationCustomerData addIntegrationCustomerData(Customer customer) {
        return integration.addIntegrationCustomerData(customer);
    }
    public IntegrationCustomerData addIntegrationCustomerData(DBBasedCustomer dbBasedCustomer) {
        return integration.addIntegrationCustomerData(dbBasedCustomer);
    }



    //
    // Integration - Item
    //
    public List<? extends IntegrationItemData> getItemData(
            Long warehouseId, LocalDateTime startTime, LocalDateTime endTime, LocalDate date) {
        return integration.getItemData(
                warehouseId, startTime, endTime, date);
    }
    public IntegrationItemData getItemData(Long id) {
        return integration.getItemData(id);
    }

    @Transactional
    public IntegrationItemData addIntegrationItemData(Item item) {
        return integration.addIntegrationItemData(item);
    }


    //
    // Integration - Item Family
    //
    public List<? extends IntegrationItemFamilyData> getItemFamilyData(
            Long warehouseId, LocalDateTime startTime, LocalDateTime endTime, LocalDate date) {
        return integration.getItemFamilyData(
                warehouseId, startTime, endTime, date);
    }
    public IntegrationItemFamilyData getItemFamilyData(Long id) {
        return integration.getItemFamilyData(id);
    }
    public IntegrationItemFamilyData addIntegrationItemFamilyData(ItemFamily itemFamily) {
        return integration.addIntegrationItemFamilyData(itemFamily);
    }


    //
    // Integration - Item Package Type
    //
    public List<? extends IntegrationItemPackageTypeData> getItemPackageTypeData(
            Long warehouseId, LocalDateTime startTime, LocalDateTime endTime, LocalDate date) {
        return integration.getItemPackageTypeData(
                warehouseId, startTime, endTime, date);
    }
    public IntegrationItemPackageTypeData getItemPackageTypeData(Long id) {
        return integration.getItemPackageTypeData(id);
    }
    public IntegrationItemPackageTypeData addIntegrationItemPackageTypeData(ItemPackageType itemPackageType) {
        return integration.addIntegrationItemPackageTypeData(itemPackageType);
    }
    public IntegrationItemPackageTypeData addIntegrationItemPackageTypeData(IntegrationItemPackageTypeData integrationItemPackageTypeData) {
        return integration.addIntegrationItemPackageTypeData(integrationItemPackageTypeData);
    }


    //
    // Integration - Item Unit Of Measure
    //
    public List<? extends IntegrationItemUnitOfMeasureData> getItemUnitOfMeasureData(
            Long warehouseId, LocalDateTime startTime, LocalDateTime endTime, LocalDate date) {
        return integration.getItemUnitOfMeasureData(
                warehouseId, startTime, endTime, date);
    }
    public IntegrationItemUnitOfMeasureData getItemUnitOfMeasureData(Long id) {
        return integration.getItemUnitOfMeasureData(id);
    }
    public IntegrationItemUnitOfMeasureData addIntegrationItemUnitOfMeasureData(ItemUnitOfMeasure itemUnitOfMeasure) {
        return integration.addIntegrationItemUnitOfMeasureData(itemUnitOfMeasure);
    }

    //
    // Integration - Supplier
    //
    public List<? extends IntegrationSupplierData> getSupplierData(
            Long warehouseId, LocalDateTime startTime, LocalDateTime endTime, LocalDate date) {
        return integration.getSupplierData(
                warehouseId, startTime, endTime, date);
    }
    public IntegrationSupplierData getSupplierData(Long id) {
        return integration.getSupplierData(id);
    }
    public IntegrationSupplierData addIntegrationSupplierData(Supplier supplier) {
        return integration.addIntegrationSupplierData(supplier);
    }

    public IntegrationSupplierData addIntegrationSupplierData(DBBasedSupplier dbBasedSupplier) {
        return integration.addIntegrationSupplierData(dbBasedSupplier);
    }


    // Integration - Work order
    public IntegrationWorkOrderData addIntegrationWorkOrderData(IntegrationWorkOrderData workOrderData) {
        return integration.addIntegrationWorkOrderData(workOrderData);
    }

    // Integration - Item
    public IntegrationItemData addIntegrationItemData(IntegrationItemData itemData) {
        return integration.addIntegrationItemData(itemData);
    }

    //
    // Integration - Receipt and Receipt Line
    //
    public List<? extends IntegrationReceiptData> getReceiptData(
            Long warehouseId, LocalDateTime startTime, LocalDateTime endTime, LocalDate date) {
        return integration.getReceiptData(
                warehouseId, startTime, endTime, date);
    }
    public IntegrationReceiptData getReceiptData(Long id) {
        return integration.getReceiptData(id);
    }
    public IntegrationReceiptData addReceiptData(Receipt receipt) {
        return integration.addReceiptData(receipt);
    }
    public IntegrationReceiptData addReceiptData(DBBasedReceipt dbBasedReceipt) {
        return integration.addReceiptData(dbBasedReceipt);
    }

    //
    // Integration - Order and Order Line
    //
    public List<? extends IntegrationOrderData> getOrderData(
            Long warehouseId, LocalDateTime startTime, LocalDateTime endTime, LocalDate date) {
        return integration.getOrderData(
                warehouseId, startTime, endTime, date);
    }
    public IntegrationOrderData getOrderData(Long id) {
        return integration.getOrderData(id);
    }
    public IntegrationOrderData addOrderData(Order order) {
        return integration.addOrderData(order);
    }


    // Outbound
    // Integration sent to HOST


    // Order Confirmation Data
    public List<? extends IntegrationOrderConfirmationData> getIntegrationOrderConfirmationData(
            Long warehouseId, String warehouseName, String number,
            LocalDateTime startTime, LocalDateTime endTime, LocalDate date){
        return integration.getIntegrationOrderConfirmationData(warehouseId, warehouseName,
                number, startTime, endTime, date);
    }
    public IntegrationOrderConfirmationData getIntegrationOrderConfirmationData(Long id){
        return integration.getIntegrationOrderConfirmationData(id);
    }
    public IntegrationOrderConfirmationData sendIntegrationOrderConfirmationData(OrderConfirmation orderConfirmation){
        return integration.sendIntegrationOrderConfirmationData(orderConfirmation);
    }



    // Work Order Confirmation Data
    public List<? extends IntegrationWorkOrderConfirmationData> getIntegrationWorkOrderConfirmationData(
            Long warehouseId, String warehouseName, String number,
            LocalDateTime startTime, LocalDateTime endTime, LocalDate date){
        return integration.getIntegrationWorkOrderConfirmationData(warehouseId, warehouseName,
                number, startTime, endTime, date);
    }
    public IntegrationWorkOrderConfirmationData getIntegrationWorkOrderConfirmationData(Long id){
        return integration.getIntegrationWorkOrderConfirmationData(id);
    }
    public IntegrationWorkOrderConfirmationData sendIntegrationWorkOrderConfirmationData(WorkOrderConfirmation workOrderConfirmation){
        return integration.sendIntegrationWorkOrderConfirmationData(workOrderConfirmation);
    }
    // Receipt Confirmation Data
    public List<? extends IntegrationReceiptConfirmationData> getIntegrationReceiptConfirmationData(
            Long warehouseId, String warehouseName, String number, Long clientId, String clientName,
            Long supplierId, String supplierName, LocalDateTime startTime, LocalDateTime endTime, LocalDate date){
        return integration.getIntegrationReceiptConfirmationData(warehouseId, warehouseName,
                number, clientId, clientName,
                supplierId, supplierName, startTime, endTime, date);
    }
    public IntegrationReceiptConfirmationData getIntegrationReceiptConfirmationData(Long id){
        return integration.getIntegrationReceiptConfirmationData(id);
    }
    public IntegrationReceiptConfirmationData sendIntegrationReceiptConfirmationData(ReceiptConfirmation receiptConfirmation){
        return integration.sendIntegrationReceiptConfirmationData(receiptConfirmation);
    }


    // Inventory Adjustment Confirmation
    public List<? extends IntegrationInventoryAdjustmentConfirmationData> getInventoryAdjustmentConfirmationData(
            Long warehouseId, LocalDateTime startTime, LocalDateTime endTime, LocalDate date
    ) {
        return integration.getInventoryAdjustmentConfirmationData(
                warehouseId, startTime, endTime, date
        );
    }
    public IntegrationInventoryAdjustmentConfirmationData getInventoryAdjustmentConfirmationData(Long id) {
        return integration.getInventoryAdjustmentConfirmationData(id);
    }
    public IntegrationInventoryAdjustmentConfirmationData sendInventoryAdjustmentConfirmationData(InventoryAdjustmentConfirmation inventoryAdjustmentConfirmation) {
        return integration.sendInventoryAdjustmentConfirmationData(inventoryAdjustmentConfirmation);
    }
    // Inventory Attribute Change Confirmation
    public List<? extends IntegrationInventoryAttributeChangeConfirmationData> getInventoryAttributeChangeConfirmationData(
            Long warehouseId, LocalDateTime startTime, LocalDateTime endTime, LocalDate date) {
        return integration.getInventoryAttributeChangeConfirmationData(
                warehouseId, startTime, endTime, date);
    }
    public IntegrationInventoryAttributeChangeConfirmationData getInventoryAttributeChangeConfirmationData(Long id) {
        return integration.getInventoryAttributeChangeConfirmationData(id);
    }
    public IntegrationInventoryAttributeChangeConfirmationData sendInventoryAttributeChangeConfirmationData(InventoryAttributeChangeConfirmation inventoryAttributeChangeConfirmation) {
        return integration.sendInventoryAttributeChangeConfirmationData(inventoryAttributeChangeConfirmation);
    }

    public void saveIntegrationItemData(DBBasedItem dbBasedItem) {
    }

    public void saveIntegrationResult(IntegrationResult integrationResult) {
        integration.saveIntegrationResult(integrationResult);
    }
}

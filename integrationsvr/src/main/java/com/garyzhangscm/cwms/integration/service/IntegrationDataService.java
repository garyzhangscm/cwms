package com.garyzhangscm.cwms.integration.service;

import com.garyzhangscm.cwms.integration.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@Service
public class IntegrationDataService {

    @Autowired
    private Integration integration;

    public List<? extends IntegrationClientData> getClientData() {
        return integration.getClientData();
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
    public List<? extends IntegrationCustomerData> getCustomerData() {
        return integration.getCustomerData();
    }
    public IntegrationCustomerData getCustomerData(Long id) {
        return integration.getCustomerData(id);
    }
    public IntegrationCustomerData addIntegrationCustomerData(Customer customer) {
        return integration.addIntegrationCustomerData(customer);
    }


    //
    // Integration - Item
    //
    public List<? extends IntegrationItemData> getItemData() {
        return integration.getItemData();
    }
    public IntegrationItemData getItemData(Long id) {
        return integration.getItemData(id);
    }
    public IntegrationItemData addIntegrationItemData(Item item) {
        return integration.addIntegrationItemData(item);
    }


    //
    // Integration - Item Family
    //
    public List<? extends IntegrationItemFamilyData> getItemFamilyData() {
        return integration.getItemFamilyData();
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
    public List<? extends IntegrationItemPackageTypeData> getItemPackageTypeData() {
        return integration.getItemPackageTypeData();
    }
    public IntegrationItemPackageTypeData getItemPackageTypeData(Long id) {
        return integration.getItemPackageTypeData(id);
    }
    public IntegrationItemPackageTypeData addIntegrationItemPackageTypeData(ItemPackageType itemPackageType) {
        return integration.addIntegrationItemPackageTypeData(itemPackageType);
    }


    //
    // Integration - Item Unit Of Measure
    //
    public List<? extends IntegrationItemUnitOfMeasureData> getItemUnitOfMeasureData() {
        return integration.getItemUnitOfMeasureData();
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
    public List<? extends IntegrationSupplierData> getSupplierData() {
        return integration.getSupplierData();
    }
    public IntegrationSupplierData getSupplierData(Long id) {
        return integration.getSupplierData(id);
    }
    public IntegrationSupplierData addIntegrationSupplierData(Supplier supplier) {
        return integration.addIntegrationSupplierData(supplier);
    }


    //
    // Integration - Receipt and Receipt Line
    //
    public List<? extends IntegrationReceiptData> getReceiptData() {
        return integration.getReceiptData();
    }
    public IntegrationReceiptData getReceiptData(Long id) {
        return integration.getReceiptData(id);
    }
    public IntegrationReceiptData addReceiptData(Receipt receipt) {
        return integration.addReceiptData(receipt);
    }

    //
    // Integration - Order and Order Line
    //
    public List<? extends IntegrationOrderData> getOrderData() {
        return integration.getOrderData();
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
    public List<? extends IntegrationOrderConfirmationData> getIntegrationOrderConfirmationData(Long warehouseId, String warehouseName,
                                                                                                String number){
        return integration.getIntegrationOrderConfirmationData(warehouseId, warehouseName,
                number);
    }
    public IntegrationOrderConfirmationData getIntegrationOrderConfirmationData(Long id){
        return integration.getIntegrationOrderConfirmationData(id);
    }
    public IntegrationOrderConfirmationData sendIntegrationOrderConfirmationData(OrderConfirmation orderConfirmation){
        return integration.sendIntegrationOrderConfirmationData(orderConfirmation);
    }

    // Receipt Confirmation Data
    public List<? extends IntegrationReceiptConfirmationData> getIntegrationReceiptConfirmationData(Long warehouseId, String warehouseName,
                                                                                                    String number, Long clientId, String clientName,
                                                                                                    Long supplierId, String supplierName){
        return integration.getIntegrationReceiptConfirmationData(warehouseId, warehouseName,
                number, clientId, clientName,
                supplierId, supplierName);
    }
    public IntegrationReceiptConfirmationData getIntegrationReceiptConfirmationData(Long id){
        return integration.getIntegrationReceiptConfirmationData(id);
    }
    public IntegrationReceiptConfirmationData sendIntegrationReceiptConfirmationData(ReceiptConfirmation receiptConfirmation){
        return integration.sendIntegrationReceiptConfirmationData(receiptConfirmation);
    }


    // Inventory Adjustment Confirmation
    public List<? extends IntegrationInventoryAdjustmentConfirmationData> getInventoryAdjustmentConfirmationData() {
        return integration.getInventoryAdjustmentConfirmationData();
    }
    public IntegrationInventoryAdjustmentConfirmationData getInventoryAdjustmentConfirmationData(Long id) {
        return integration.getInventoryAdjustmentConfirmationData(id);
    }
    public IntegrationInventoryAdjustmentConfirmationData sendInventoryAdjustmentConfirmationData(InventoryAdjustmentConfirmation inventoryAdjustmentConfirmation) {
        return integration.sendInventoryAdjustmentConfirmationData(inventoryAdjustmentConfirmation);
    }
    // Inventory Attribute Change Confirmation
    public List<? extends IntegrationInventoryAttributeChangeConfirmationData> getInventoryAttributeChangeConfirmationData() {
        return integration.getInventoryAttributeChangeConfirmationData();
    }
    public IntegrationInventoryAttributeChangeConfirmationData getInventoryAttributeChangeConfirmationData(Long id) {
        return integration.getInventoryAttributeChangeConfirmationData(id);
    }
    public IntegrationInventoryAttributeChangeConfirmationData sendInventoryAttributeChangeConfirmationData(InventoryAttributeChangeConfirmation inventoryAttributeChangeConfirmation) {
        return integration.sendInventoryAttributeChangeConfirmationData(inventoryAttributeChangeConfirmation);
    }
}

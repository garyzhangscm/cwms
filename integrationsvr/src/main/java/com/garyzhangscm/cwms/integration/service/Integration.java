package com.garyzhangscm.cwms.integration.service;

import com.garyzhangscm.cwms.integration.model.*;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public interface Integration {

    void listen();

    void send();
    //
    // Integration - Client
    //
    List<? extends IntegrationClientData> getClientData(
            Long warehouseId, LocalDateTime startTime, LocalDateTime endTime, LocalDate date);
    IntegrationClientData getClientData(Long id);
    IntegrationClientData addIntegrationClientData(Client client);


    //
    // Integration - Customer
    //
    List<? extends IntegrationCustomerData> getCustomerData(
            Long warehouseId, LocalDateTime startTime, LocalDateTime endTime, LocalDate date);
    IntegrationCustomerData getCustomerData(Long id);
    IntegrationCustomerData addIntegrationCustomerData(Customer customer);
    IntegrationCustomerData addIntegrationCustomerData(DBBasedCustomer dbBasedCustomer);


    //
    // Integration - Item
    //
    List<? extends IntegrationItemData> getItemData(
            Long warehouseId, LocalDateTime startTime, LocalDateTime endTime, LocalDate date);
    IntegrationItemData getItemData(Long id);
    IntegrationItemData addIntegrationItemData(Item item);


    //
    // Integration - Item Family
    //
    List<? extends IntegrationItemFamilyData> getItemFamilyData(
            Long warehouseId, LocalDateTime startTime, LocalDateTime endTime, LocalDate date);
    IntegrationItemFamilyData getItemFamilyData(Long id);
    IntegrationItemFamilyData addIntegrationItemFamilyData(ItemFamily itemFamily);


    //
    // Integration - Item Package Type
    //
    List<? extends IntegrationItemPackageTypeData> getItemPackageTypeData(
            Long warehouseId, LocalDateTime startTime, LocalDateTime endTime, LocalDate date);
    IntegrationItemPackageTypeData getItemPackageTypeData(Long id);
    IntegrationItemPackageTypeData addIntegrationItemPackageTypeData(ItemPackageType itemPackageType);
    IntegrationItemPackageTypeData addIntegrationItemPackageTypeData(IntegrationItemPackageTypeData integrationItemPackageTypeData);


    //
    // Integration - Item Unit Of Measure
    //
    List<? extends IntegrationItemUnitOfMeasureData> getItemUnitOfMeasureData(
            Long warehouseId, LocalDateTime startTime, LocalDateTime endTime, LocalDate date);
    IntegrationItemUnitOfMeasureData getItemUnitOfMeasureData(Long id);
    IntegrationItemUnitOfMeasureData addIntegrationItemUnitOfMeasureData(ItemUnitOfMeasure itemUnitOfMeasure);

    //
    // Integration - Supplier
    //
    List<? extends IntegrationSupplierData> getSupplierData(
            Long warehouseId, LocalDateTime startTime, LocalDateTime endTime, LocalDate date);
    IntegrationSupplierData getSupplierData(Long id);
    IntegrationSupplierData addIntegrationSupplierData(Supplier supplier);
    IntegrationSupplierData addIntegrationSupplierData(DBBasedSupplier dbBasedSupplier);


    // Integration - Work order
    public IntegrationWorkOrderData addIntegrationWorkOrderData(IntegrationWorkOrderData workOrderData);
    public IntegrationItemData addIntegrationItemData(IntegrationItemData itemData);

    //
    // Integration - Receipt and Receipt Line
    //
    List<? extends IntegrationReceiptData> getReceiptData(
            Long warehouseId, LocalDateTime startTime, LocalDateTime endTime, LocalDate date);
    IntegrationReceiptData getReceiptData(Long id);
    IntegrationReceiptData addReceiptData(Receipt receipt);
    IntegrationReceiptData addReceiptData(DBBasedReceipt dbBasedReceipt);

    //
    // Integration - Order and Order Line
    //
    List<? extends IntegrationOrderData> getOrderData(
            Long warehouseId, LocalDateTime startTime, LocalDateTime endTime, LocalDate date);
    IntegrationOrderData getOrderData(Long id);
    IntegrationOrderData addOrderData(Order order);

    //
    // Integration - Work Order and Work Order Line
    //
    List<? extends IntegrationWorkOrderData> getWorkOrderData(
            Long warehouseId, LocalDateTime startTime, LocalDateTime endTime, LocalDate date);
    IntegrationWorkOrderData getWorkOrderData(Long id);
    IntegrationWorkOrderData addWorkOrderData(WorkOrder workOrder);

    //
    // Integration - BOM and BOM line
    //
    List<? extends IntegrationBillOfMaterialData> getBillOfMaterialData(
            Long warehouseId, LocalDateTime startTime, LocalDateTime endTime, LocalDate date);
    IntegrationBillOfMaterialData getBillOfMaterialData(Long id);
    IntegrationBillOfMaterialData addBillOfMaterialData(BillOfMaterial billOfMaterial);


    // Outbound
    // Integration sent to HOST

    // Order Confirmation Data
    List<? extends IntegrationOrderConfirmationData> getIntegrationOrderConfirmationData(
            Long warehouseId, String warehouseName, String number,
            LocalDateTime startTime, LocalDateTime endTime, LocalDate date);
    IntegrationOrderConfirmationData getIntegrationOrderConfirmationData(Long id);
    IntegrationOrderConfirmationData sendIntegrationOrderConfirmationData(OrderConfirmation orderConfirmation);

    // Work Order Confirmation Data
    List<? extends IntegrationWorkOrderConfirmationData> getIntegrationWorkOrderConfirmationData(
            Long warehouseId, String warehouseName, String number,
            LocalDateTime startTime, LocalDateTime endTime, LocalDate date);
    IntegrationWorkOrderConfirmationData getIntegrationWorkOrderConfirmationData(Long id);
    IntegrationWorkOrderConfirmationData sendIntegrationWorkOrderConfirmationData(WorkOrderConfirmation workOrderConfirmation);

    // Receipt Confirmation Data
    List<? extends IntegrationReceiptConfirmationData> getIntegrationReceiptConfirmationData(
            Long warehouseId, String warehouseName, String number, Long clientId, String clientName,
            Long supplierId, String supplierName, LocalDateTime startTime, LocalDateTime endTime, LocalDate date);
    IntegrationReceiptConfirmationData getIntegrationReceiptConfirmationData(Long id);
    IntegrationReceiptConfirmationData sendIntegrationReceiptConfirmationData(ReceiptConfirmation receiptConfirmation);

    // Inventory Adjustment Confirmation
    List<? extends IntegrationInventoryAdjustmentConfirmationData> getInventoryAdjustmentConfirmationData(

            Long warehouseId, LocalDateTime startTime, LocalDateTime endTime, LocalDate date
    );
    IntegrationInventoryAdjustmentConfirmationData getInventoryAdjustmentConfirmationData(Long id);
    IntegrationInventoryAdjustmentConfirmationData sendInventoryAdjustmentConfirmationData(InventoryAdjustmentConfirmation inventoryAdjustmentConfirmation);

    // Inventory Attribute Confirmation
    List<? extends IntegrationInventoryAttributeChangeConfirmationData> getInventoryAttributeChangeConfirmationData(
            Long warehouseId, LocalDateTime startTime, LocalDateTime endTime, LocalDate date);
    IntegrationInventoryAttributeChangeConfirmationData getInventoryAttributeChangeConfirmationData(Long id);
    IntegrationInventoryAttributeChangeConfirmationData sendInventoryAttributeChangeConfirmationData(InventoryAttributeChangeConfirmation inventoryAttributeChangeConfirmation);

    // Inventory Shipping Confirmation
    List<? extends IntegrationInventoryShippingConfirmationData> getInventoryShippingConfirmationData(
            Long warehouseId, LocalDateTime startTime, LocalDateTime endTime, LocalDate date);
    IntegrationInventoryShippingConfirmationData getInventoryShippingConfirmationData(Long id);
    IntegrationInventoryShippingConfirmationData sendInventoryShippingConfirmationData(InventoryAdjustmentConfirmation inventoryAdjustmentConfirmation);

    // save the integration result
    public void saveIntegrationResult(IntegrationResult integrationResult);
}

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
            Long warehouseId, LocalDateTime startTime, LocalDateTime endTime, LocalDate date, String statusList,
            Long id);
    IntegrationClientData getClientData(Long id);
    IntegrationClientData addIntegrationClientData(Client client);


    //
    // Integration - Customer
    //
    List<? extends IntegrationCustomerData> getCustomerData(
            Long warehouseId, LocalDateTime startTime, LocalDateTime endTime, LocalDate date, String statusList,
            Long id);
    IntegrationCustomerData getCustomerData(Long id);
    IntegrationCustomerData addIntegrationCustomerData(Customer customer);
    IntegrationCustomerData addIntegrationCustomerData(DBBasedCustomer dbBasedCustomer);


    //
    // Integration - Item
    //
    List<? extends IntegrationItemData> getItemData(
            String companyCode,
            Long warehouseId, LocalDateTime startTime, LocalDateTime endTime, LocalDate date, String statusList,
            Long id);
    IntegrationItemData getItemData(Long id);
    IntegrationItemData addIntegrationItemData(Item item);


    //
    // Integration - Item Family
    //
    List<? extends IntegrationItemFamilyData> getItemFamilyData(
            String companyCode,
            Long warehouseId, LocalDateTime startTime, LocalDateTime endTime, LocalDate date, String statusList,
            Long id);
    IntegrationItemFamilyData getItemFamilyData(Long id);
    IntegrationItemFamilyData addIntegrationItemFamilyData(ItemFamily itemFamily);


    //
    // Integration - Item Package Type
    //
    List<? extends IntegrationItemPackageTypeData> getItemPackageTypeData(
            String companyCode,
            Long warehouseId, LocalDateTime startTime, LocalDateTime endTime, LocalDate date, String statusList,
            Long id);
    IntegrationItemPackageTypeData getItemPackageTypeData(Long id);
    IntegrationItemPackageTypeData addIntegrationItemPackageTypeData(ItemPackageType itemPackageType);
    IntegrationItemPackageTypeData addIntegrationItemPackageTypeData(IntegrationItemPackageTypeData integrationItemPackageTypeData);


    //
    // Integration - Item Unit Of Measure
    //
    List<? extends IntegrationItemUnitOfMeasureData> getItemUnitOfMeasureData(
            String companyCode,
            Long warehouseId, LocalDateTime startTime, LocalDateTime endTime, LocalDate date, String statusList,
            Long id);
    IntegrationItemUnitOfMeasureData getItemUnitOfMeasureData(Long id);
    IntegrationItemUnitOfMeasureData addIntegrationItemUnitOfMeasureData(ItemUnitOfMeasure itemUnitOfMeasure);

    //
    // Integration - Supplier
    //
    List<? extends IntegrationSupplierData> getSupplierData(
            Long warehouseId, LocalDateTime startTime, LocalDateTime endTime, LocalDate date, String statusList,
            Long id);
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
            String companyCode,
            Long warehouseId, LocalDateTime startTime, LocalDateTime endTime, LocalDate date,
            String statusList,
            Long id);
    IntegrationReceiptData getReceiptData(Long id);
    IntegrationReceiptData addReceiptData(Receipt receipt);
    IntegrationReceiptData addReceiptData(DBBasedReceipt dbBasedReceipt);

    //
    // Integration - Purchase Order and Purchase Order Line
    //
    List<? extends IntegrationPurchaseOrderData> getPurchaseOrderData(
            String companyCode,
            Long warehouseId, LocalDateTime startTime, LocalDateTime endTime, LocalDate date,
            String statusList,
            Long id);
    IntegrationPurchaseOrderData getPurchaseOrderData(Long id);
    IntegrationPurchaseOrderData addPurchaseOrderData(PurchaseOrder purchaseOrder);
    IntegrationPurchaseOrderData addPurchaseOrderData(DBBasedPurchaseOrder dbBasedPurchaseOrder);


    //
    // Integration - Order and Order Line
    //
    List<? extends IntegrationOrderData> getOrderData(
            String companyCode,
            Long warehouseId, LocalDateTime startTime, LocalDateTime endTime, LocalDate date, String statusList,
            Long id);
    IntegrationOrderData getOrderData(Long id);
    IntegrationOrderData addOrderData(Order order);
    IntegrationOrderData addOrderData(DBBasedOrder dbBasedOrder);

    //
    // Integration - Work Order and Work Order Line
    //
    List<? extends IntegrationWorkOrderData> getWorkOrderData(
            String companyCode,
            Long warehouseId, LocalDateTime startTime, LocalDateTime endTime, LocalDate date, String statusList,
            Long id);
    IntegrationWorkOrderData getWorkOrderData(Long id);
    IntegrationWorkOrderData addWorkOrderData(WorkOrder workOrder);

    //
    // Integration - BOM and BOM line
    //
    List<? extends IntegrationBillOfMaterialData> getBillOfMaterialData(
            Long warehouseId, LocalDateTime startTime, LocalDateTime endTime, LocalDate date, String statusList,
            Long id);
    IntegrationBillOfMaterialData getBillOfMaterialData(Long id);
    IntegrationBillOfMaterialData addBillOfMaterialData(BillOfMaterial billOfMaterial);


    // Outbound
    // Integration sent to HOST

    // Order Confirmation Data
    List<? extends IntegrationOrderConfirmationData> getIntegrationOrderConfirmationData(
            Long warehouseId, String warehouseName, String number,
            LocalDateTime startTime, LocalDateTime endTime, LocalDate date,
            String statusList, Long id);
    IntegrationOrderConfirmationData getIntegrationOrderConfirmationData(Long id);
    IntegrationOrderConfirmationData resendOrderConfirmationData(Long id);
    IntegrationOrderConfirmationData sendIntegrationOrderConfirmationData(OrderConfirmation orderConfirmation);

    // Work Order Confirmation Data
    List<? extends IntegrationWorkOrderConfirmationData> getIntegrationWorkOrderConfirmationData(
            Long warehouseId, String warehouseName, String number,
            LocalDateTime startTime, LocalDateTime endTime, LocalDate date,
            String statusList, Long id);
    IntegrationWorkOrderConfirmationData getIntegrationWorkOrderConfirmationData(Long id);
    IntegrationWorkOrderConfirmationData resendWorkOrderConfirmationData(Long id);
    IntegrationWorkOrderConfirmationData sendIntegrationWorkOrderConfirmationData(WorkOrderConfirmation workOrderConfirmation);

    // Receipt Confirmation Data
    List<? extends IntegrationReceiptConfirmationData> getIntegrationReceiptConfirmationData(
            Long warehouseId, String warehouseName, String number, Long clientId, String clientName,
            Long supplierId, String supplierName, LocalDateTime startTime, LocalDateTime endTime, LocalDate date,
            String statusList, Long id);
    IntegrationReceiptConfirmationData getIntegrationReceiptConfirmationData(Long id);
    IntegrationReceiptConfirmationData resendReceiptConfirmationData(Long id);
    IntegrationReceiptConfirmationData sendIntegrationReceiptConfirmationData(ReceiptConfirmation receiptConfirmation);

    // Inventory Adjustment Confirmation
    List<? extends IntegrationInventoryAdjustmentConfirmationData> getInventoryAdjustmentConfirmationData(

            Long warehouseId, LocalDateTime startTime, LocalDateTime endTime, LocalDate date,
            String statusList, Long id
    );
    IntegrationInventoryAdjustmentConfirmationData getInventoryAdjustmentConfirmationData(Long id);
    IntegrationInventoryAdjustmentConfirmationData resendInventoryAdjustmentConfirmationData(Long id);
    IntegrationInventoryAdjustmentConfirmationData sendInventoryAdjustmentConfirmationData(InventoryAdjustmentConfirmation inventoryAdjustmentConfirmation);

    // Inventory Attribute Confirmation
    List<? extends IntegrationInventoryAttributeChangeConfirmationData> getInventoryAttributeChangeConfirmationData(
            Long warehouseId, LocalDateTime startTime, LocalDateTime endTime, LocalDate date,
            String statusList, Long id);
    IntegrationInventoryAttributeChangeConfirmationData getInventoryAttributeChangeConfirmationData(Long id);
    IntegrationInventoryAttributeChangeConfirmationData resendInventoryAttributeChangeConfirmationData(Long id);
    IntegrationInventoryAttributeChangeConfirmationData sendInventoryAttributeChangeConfirmationData(InventoryAttributeChangeConfirmation inventoryAttributeChangeConfirmation);

    // Inventory Shipping Confirmation
    List<? extends IntegrationInventoryShippingConfirmationData> getInventoryShippingConfirmationData(
            Long warehouseId, LocalDateTime startTime, LocalDateTime endTime, LocalDate date);
    IntegrationInventoryShippingConfirmationData getInventoryShippingConfirmationData(Long id);
    IntegrationInventoryShippingConfirmationData sendInventoryShippingConfirmationData(InventoryAdjustmentConfirmation inventoryAdjustmentConfirmation);

    // save the integration result
    public void saveIntegrationResult(IntegrationResult integrationResult);


    // resend the integration data
    IntegrationReceiptData resendReceiptData(Long id);
    IntegrationPurchaseOrderData resendPurchaseOrderData(Long id);
    IntegrationBillOfMaterialData resendBillOfMaterialData(Long id);
    IntegrationClientData resendClientData(Long id);
    IntegrationCustomerData resendCustomerData(Long id);
    IntegrationItemFamilyData resendItemFamilyData(Long id);
    IntegrationItemData resendItemData(Long id);
    IntegrationItemPackageTypeData resendItemPackageTypeData(Long id);
    IntegrationItemUnitOfMeasureData resendItemUnitOfMeasureData(Long id);
    IntegrationOrderData resendOrderData(Long id);
    IntegrationSupplierData resendSupplierData(Long id);
    IntegrationWorkOrderData resendWorkOrderData(Long id);

}

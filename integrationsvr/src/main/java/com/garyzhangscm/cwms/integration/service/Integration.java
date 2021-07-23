package com.garyzhangscm.cwms.integration.service;

import com.garyzhangscm.cwms.integration.model.*;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface Integration {

    void listen();

    //
    // Integration - Client
    //
    List<? extends IntegrationClientData> getClientData();
    IntegrationClientData getClientData(Long id);
    IntegrationClientData addIntegrationClientData(Client client);


    //
    // Integration - Customer
    //
    List<? extends IntegrationCustomerData> getCustomerData();
    IntegrationCustomerData getCustomerData(Long id);
    IntegrationCustomerData addIntegrationCustomerData(Customer customer);


    //
    // Integration - Item
    //
    List<? extends IntegrationItemData> getItemData();
    IntegrationItemData getItemData(Long id);
    IntegrationItemData addIntegrationItemData(Item item);


    //
    // Integration - Item Family
    //
    List<? extends IntegrationItemFamilyData> getItemFamilyData();
    IntegrationItemFamilyData getItemFamilyData(Long id);
    IntegrationItemFamilyData addIntegrationItemFamilyData(ItemFamily itemFamily);


    //
    // Integration - Item Package Type
    //
    List<? extends IntegrationItemPackageTypeData> getItemPackageTypeData();
    IntegrationItemPackageTypeData getItemPackageTypeData(Long id);
    IntegrationItemPackageTypeData addIntegrationItemPackageTypeData(ItemPackageType itemPackageType);
    IntegrationItemPackageTypeData addIntegrationItemPackageTypeData(IntegrationItemPackageTypeData integrationItemPackageTypeData);


    //
    // Integration - Item Unit Of Measure
    //
    List<? extends IntegrationItemUnitOfMeasureData> getItemUnitOfMeasureData();
    IntegrationItemUnitOfMeasureData getItemUnitOfMeasureData(Long id);
    IntegrationItemUnitOfMeasureData addIntegrationItemUnitOfMeasureData(ItemUnitOfMeasure itemUnitOfMeasure);

    //
    // Integration - Supplier
    //
    List<? extends IntegrationSupplierData> getSupplierData();
    IntegrationSupplierData getSupplierData(Long id);
    IntegrationSupplierData addIntegrationSupplierData(Supplier supplier);


    // Integration - Work order
    public IntegrationWorkOrderData addIntegrationWorkOrderData(IntegrationWorkOrderData workOrderData);
    public IntegrationItemData addIntegrationItemData(IntegrationItemData itemData);

    //
    // Integration - Receipt and Receipt Line
    //
    List<? extends IntegrationReceiptData> getReceiptData();
    IntegrationReceiptData getReceiptData(Long id);
    IntegrationReceiptData addReceiptData(Receipt receipt);

    //
    // Integration - Order and Order Line
    //
    List<? extends IntegrationOrderData> getOrderData();
    IntegrationOrderData getOrderData(Long id);
    IntegrationOrderData addOrderData(Order order);

    //
    // Integration - Work Order and Work Order Line
    //
    List<? extends IntegrationWorkOrderData> getWorkOrderData();
    IntegrationWorkOrderData getWorkOrderData(Long id);
    IntegrationWorkOrderData addWorkOrderData(WorkOrder workOrder);

    //
    // Integration - BOM and BOM line
    //
    List<? extends IntegrationBillOfMaterialData> getBillOfMaterialData();
    IntegrationBillOfMaterialData getBillOfMaterialData(Long id);
    IntegrationBillOfMaterialData addBillOfMaterialData(BillOfMaterial billOfMaterial);


    // Outbound
    // Integration sent to HOST

    // Order Confirmation Data
    List<? extends IntegrationOrderConfirmationData> getIntegrationOrderConfirmationData(Long warehouseId, String warehouseName,
                                                                                         String number);
    IntegrationOrderConfirmationData getIntegrationOrderConfirmationData(Long id);
    IntegrationOrderConfirmationData sendIntegrationOrderConfirmationData(OrderConfirmation orderConfirmation);

    // Work Order Confirmation Data
    List<? extends IntegrationWorkOrderConfirmationData> getIntegrationWorkOrderConfirmationData(Long warehouseId, String warehouseName,
                                                                                         String number);
    IntegrationWorkOrderConfirmationData getIntegrationWorkOrderConfirmationData(Long id);
    IntegrationWorkOrderConfirmationData sendIntegrationWorkOrderConfirmationData(WorkOrderConfirmation workOrderConfirmation);

    // Receipt Confirmation Data
    List<? extends IntegrationReceiptConfirmationData> getIntegrationReceiptConfirmationData(Long warehouseId, String warehouseName,
                                                                                             String number, Long clientId, String clientName,
                                                                                             Long supplierId, String supplierName);
    IntegrationReceiptConfirmationData getIntegrationReceiptConfirmationData(Long id);
    IntegrationReceiptConfirmationData sendIntegrationReceiptConfirmationData(ReceiptConfirmation receiptConfirmation);

    // Inventory Adjustment Confirmation
    List<? extends IntegrationInventoryAdjustmentConfirmationData> getInventoryAdjustmentConfirmationData();
    IntegrationInventoryAdjustmentConfirmationData getInventoryAdjustmentConfirmationData(Long id);
    IntegrationInventoryAdjustmentConfirmationData sendInventoryAdjustmentConfirmationData(InventoryAdjustmentConfirmation inventoryAdjustmentConfirmation);

    // Inventory Attribute Confirmation
    List<? extends IntegrationInventoryAttributeChangeConfirmationData> getInventoryAttributeChangeConfirmationData();
    IntegrationInventoryAttributeChangeConfirmationData getInventoryAttributeChangeConfirmationData(Long id);
    IntegrationInventoryAttributeChangeConfirmationData sendInventoryAttributeChangeConfirmationData(InventoryAttributeChangeConfirmation inventoryAttributeChangeConfirmation);

    // Inventory Shipping Confirmation
    List<? extends IntegrationInventoryShippingConfirmationData> getInventoryShippingConfirmationData();
    IntegrationInventoryShippingConfirmationData getInventoryShippingConfirmationData(Long id);
    IntegrationInventoryShippingConfirmationData sendInventoryShippingConfirmationData(InventoryAdjustmentConfirmation inventoryAdjustmentConfirmation);
}

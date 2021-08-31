package com.garyzhangscm.cwms.integration.service;


import com.garyzhangscm.cwms.integration.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * A DB based integraiton solution.
 * All data will be fetched from / saved into database
 */
@Service
@Profile({"DBBasedIntegration"})
public class DBBasedIntegration implements Integration{

    private static final Logger logger = LoggerFactory.getLogger(DBBasedIntegration.class);

    ////   Inbound
    @Autowired
    DBBasedClientIntegration dbBasedClientIntegration;

    @Autowired
    DBBasedCustomerIntegration dbBasedCustomerIntegration;



    @Autowired
    DBBasedItemFamilyIntegration dbBasedItemFamilyIntegration;
    @Autowired
    DBBasedItemIntegration dbBasedItemIntegration;
    @Autowired
    DBBasedItemPackageTypeIntegration dbBasedItemPackageTypeIntegration;
    @Autowired
    DBBasedItemUnitOfMeasureIntegration dbBasedItemUnitOfMeasureIntegration;

    @Autowired
    DBBasedSupplierIntegration dbBasedSupplierIntegration;

    @Autowired
    DBBasedReceiptIntegration dbBasedReceiptIntegration;

    @Autowired
    DBBasedOrderIntegration dbBasedOrderIntegration;


    @Autowired
    DBBasedWorkOrderIntegration dbBasedWorkOrderIntegration;


    @Autowired
    DBBasedBillOfMaterialIntegration dbBasedBillOfMaterialIntegration;

    ////// Outbound
    @Autowired
    DBBasedInventoryAdjustmentConfirmationIntegration dbBasedInventoryAdjustmentConfirmationIntegration;
    @Autowired
    DBBasedInventoryAttributeChangeConfirmationIntegration dbBasedInventoryAttributeChangeConfirmationIntegration;

    @Autowired
    DBBasedOrderConfirmationIntegration dbBasedOrderConfirmationIntegration;
    @Autowired
    DBBasedWorkOrderConfirmationIntegration dbBasedWorkOrderConfirmationIntegration;
    @Autowired
    DBBasedReceiptConfirmationIntegration dbBasedReceiptConfirmationIntegration;




    public void listen() {
        logger.debug("Will use DBBasedIntegration to process the inbound integration data");

        logger.debug("#1 Customer data");
        dbBasedCustomerIntegration.listen();

        logger.debug("#2 Supplier data");
        dbBasedSupplierIntegration.listen();

        logger.debug("#3 Client data");
        dbBasedClientIntegration.listen();

        logger.debug("#4 Item Family");
        dbBasedItemFamilyIntegration.listen();
        logger.debug("#5 Item");
        dbBasedItemIntegration.listen();
        logger.debug("#6 Item Package Type");
        dbBasedItemPackageTypeIntegration.listen();
        logger.debug("#7 Item Unit Of Measure");
        dbBasedItemUnitOfMeasureIntegration.listen();



        logger.debug("#8 Receipt");
        dbBasedReceiptIntegration.listen();

        logger.debug("#9 Sales Order");
        dbBasedOrderIntegration.listen();


        logger.debug("#10 Work  Order");
        dbBasedWorkOrderIntegration.listen();


        logger.debug("#11 Bill Of Material");
        dbBasedBillOfMaterialIntegration.listen();

    }


    @Override
    public void send() {

        logger.debug("Will use DBBasedIntegration to process the outbound integration data");

        logger.debug("#1 Customer data");
        dbBasedInventoryAdjustmentConfirmationIntegration.sendToHost();


        logger.debug("#2 order confirmation");
        dbBasedOrderConfirmationIntegration.sendToHost();
    }
    @Override
    public List<DBBasedClient> getClientData(
            Long warehouseId, LocalDateTime startTime, LocalDateTime endTime, LocalDate date) {
        return dbBasedClientIntegration.findAll(
                warehouseId, startTime, endTime, date);
    }

    @Override
    public DBBasedClient getClientData(Long id) {
        return dbBasedClientIntegration.findById(id);
    }
    @Override
    public  IntegrationClientData addIntegrationClientData(Client client) {


        return dbBasedClientIntegration.addIntegrationClientData(new DBBasedClient(client));
    }


    //
    // Integration - Customer
    //

    @Override
    public List<? extends IntegrationCustomerData> getCustomerData(
            Long warehouseId, LocalDateTime startTime, LocalDateTime endTime, LocalDate date) {
        return dbBasedCustomerIntegration.findAll(
                warehouseId, startTime, endTime, date);
    }

    @Override
    public IntegrationCustomerData getCustomerData(Long id) {
        return dbBasedCustomerIntegration.findById(id);
    }

    @Override
    public IntegrationCustomerData addIntegrationCustomerData(Customer customer) {
        return dbBasedCustomerIntegration.addIntegrationCustomerData(new DBBasedCustomer(customer));
    }

    //
    // Integration - Item
    //
    @Override
    public List<? extends IntegrationItemData> getItemData(
            Long warehouseId, LocalDateTime startTime, LocalDateTime endTime, LocalDate date) {
        return dbBasedItemIntegration.findAll(
                warehouseId, startTime, endTime, date);
    }

    @Override
    public IntegrationItemData getItemData(Long id) {
        return dbBasedItemIntegration.findById(id);
    }

    @Override
    public IntegrationItemData addIntegrationItemData(Item item) {
        DBBasedItem dbBasedItem = new DBBasedItem(item);
        logger.debug("Get dbBasedItem\n{}\n from item : \n{}",
                dbBasedItem, item);
        return dbBasedItemIntegration.addIntegrationItemData(dbBasedItem);
    }


    //
    // Integration - Item Family
    //
    @Override
    public List<? extends IntegrationItemFamilyData> getItemFamilyData(
            Long warehouseId, LocalDateTime startTime, LocalDateTime endTime, LocalDate date) {
        return dbBasedItemFamilyIntegration.findAll(
                warehouseId, startTime, endTime, date);
    }

    @Override
    public IntegrationItemFamilyData getItemFamilyData(Long id) {
        return dbBasedItemFamilyIntegration.findById(id);
    }

    @Override
    public IntegrationItemFamilyData addIntegrationItemFamilyData(ItemFamily itemFamily) {
        return dbBasedItemFamilyIntegration.addIntegrationItemFamilyData(new DBBasedItemFamily(itemFamily));
    }

    //
    // Integration - Item Package Type
    //
    @Override
    public List<? extends IntegrationItemPackageTypeData> getItemPackageTypeData(
            Long warehouseId, LocalDateTime startTime, LocalDateTime endTime, LocalDate date) {
        return dbBasedItemPackageTypeIntegration.findAll(
                warehouseId, startTime, endTime, date);
    }

    @Override
    public IntegrationItemPackageTypeData getItemPackageTypeData(Long id) {
        return dbBasedItemPackageTypeIntegration.findById(id);
    }

    @Override
    public IntegrationItemPackageTypeData addIntegrationItemPackageTypeData(ItemPackageType itemPackageType) {
        return dbBasedItemPackageTypeIntegration.addIntegrationItemPackageTypeData(new DBBasedItemPackageType(itemPackageType));
    }

    @Override
    public IntegrationItemPackageTypeData addIntegrationItemPackageTypeData(IntegrationItemPackageTypeData itemPackageType) {
        return dbBasedItemPackageTypeIntegration.addIntegrationItemPackageTypeData((DBBasedItemPackageType)itemPackageType);
    }

    //
    // Integration - Item Unit Of Measure
    //
    @Override
    public List<? extends IntegrationItemUnitOfMeasureData> getItemUnitOfMeasureData(
            Long warehouseId, LocalDateTime startTime, LocalDateTime endTime, LocalDate date) {
        return dbBasedItemUnitOfMeasureIntegration.findAll(
                warehouseId, startTime, endTime, date);
    }

    @Override
    public IntegrationItemUnitOfMeasureData getItemUnitOfMeasureData(Long id) {
        return dbBasedItemUnitOfMeasureIntegration.findById(id);
    }

    @Override
    public IntegrationItemUnitOfMeasureData addIntegrationItemUnitOfMeasureData(ItemUnitOfMeasure itemUnitOfMeasure) {
        return dbBasedItemUnitOfMeasureIntegration.addIntegrationItemUnitOfMeasureData(new DBBasedItemUnitOfMeasure(itemUnitOfMeasure));
    }

    //
    // Integration - Supplier
    //
    @Override
    public List<? extends IntegrationSupplierData> getSupplierData(
            Long warehouseId, LocalDateTime startTime, LocalDateTime endTime, LocalDate date) {
        return dbBasedSupplierIntegration.findAll(
                warehouseId, startTime, endTime, date);
    }

    @Override
    public IntegrationSupplierData getSupplierData(Long id) {
        return dbBasedSupplierIntegration.findById(id);
    }

    @Override
    public IntegrationSupplierData addIntegrationSupplierData(Supplier supplier) {
        return dbBasedSupplierIntegration.addIntegrationSupplierData(new DBBasedSupplier(supplier));
    }


    @Override
    public IntegrationWorkOrderData addIntegrationWorkOrderData(IntegrationWorkOrderData workOrderData) {
        return dbBasedWorkOrderIntegration.addIntegrationWorkOrderData((DBBasedWorkOrder) workOrderData);
    }
    @Override
    public IntegrationItemData addIntegrationItemData(IntegrationItemData itemData) {
        return dbBasedItemIntegration.addIntegrationItemData((DBBasedItem) itemData);
    }



    //
    // Integration - Receipt and Receipt Line
    //
    public List<? extends IntegrationReceiptData> getReceiptData(
            Long warehouseId, LocalDateTime startTime, LocalDateTime endTime, LocalDate date) {
        return dbBasedReceiptIntegration.findAll(
                warehouseId, startTime, endTime, date);
    }
    public IntegrationReceiptData getReceiptData(Long id) {
        return dbBasedReceiptIntegration.findById(id);
    }
    public IntegrationReceiptData addReceiptData(Receipt receipt) {

        return dbBasedReceiptIntegration.addIntegrationReceiptData(new DBBasedReceipt(receipt));

    }

    //
    // Integration - Order and Order Line
    //
    public List<? extends IntegrationOrderData> getOrderData(
            Long warehouseId, LocalDateTime startTime, LocalDateTime endTime, LocalDate date) {
        return dbBasedOrderIntegration.findAll(
                warehouseId, startTime, endTime, date);
    }
    public IntegrationOrderData getOrderData(Long id) {
        return dbBasedOrderIntegration.findById(id);
    }
    public IntegrationOrderData addOrderData(Order order) {
        return dbBasedOrderIntegration.addIntegrationOrderData(new DBBasedOrder(order));
    }


    //
    // Integration - Work Order and Work Order Line
    //
    @Override
    public List<? extends IntegrationWorkOrderData> getWorkOrderData(
            Long warehouseId, LocalDateTime startTime, LocalDateTime endTime, LocalDate date) {
        return dbBasedWorkOrderIntegration.findAll(
                warehouseId, startTime, endTime, date
        );
    }

    @Override
    public IntegrationWorkOrderData getWorkOrderData(Long id) {
        return null;
    }

    @Override
    public IntegrationWorkOrderData addWorkOrderData(WorkOrder workOrder) {
        return null;
    }

    //
    // Integration - BOM and BOM line
    //
    @Override
    public List<? extends IntegrationBillOfMaterialData> getBillOfMaterialData(
            Long warehouseId, LocalDateTime startTime, LocalDateTime endTime, LocalDate date) {
        return dbBasedBillOfMaterialIntegration.findAll(
                warehouseId, startTime, endTime, date
        );
    }

    @Override
    public IntegrationBillOfMaterialData getBillOfMaterialData(Long id) {
        return null;
    }

    @Override
    public IntegrationBillOfMaterialData addBillOfMaterialData(BillOfMaterial billOfMaterial) {
        return null;
    }


    // Outbound
    // Integration sent to HOST

    // Order Confirmation Data
    public List<? extends IntegrationOrderConfirmationData> getIntegrationOrderConfirmationData(Long warehouseId, String warehouseName,
                                                                                                String number, LocalDateTime startTime,
                                                                                                LocalDateTime endTime, LocalDate date){
        return dbBasedOrderConfirmationIntegration.findAll(warehouseId, warehouseName,
                number, startTime, endTime, date);
    }
    public IntegrationOrderConfirmationData getIntegrationOrderConfirmationData(Long id){
        return dbBasedOrderConfirmationIntegration.findById(id);
    }
    public IntegrationOrderConfirmationData sendIntegrationOrderConfirmationData(OrderConfirmation orderConfirmation){
        return dbBasedOrderConfirmationIntegration.sendIntegrationOrderConfirmationData(orderConfirmation);
    }

    @Override
    public List<? extends IntegrationWorkOrderConfirmationData> getIntegrationWorkOrderConfirmationData(
            Long warehouseId, String warehouseName, String number,
            LocalDateTime startTime, LocalDateTime endTime, LocalDate date) {
        return dbBasedWorkOrderConfirmationIntegration.findAll(warehouseId, warehouseName,
                number, startTime, endTime, date);
    }

    @Override
    public IntegrationWorkOrderConfirmationData getIntegrationWorkOrderConfirmationData(Long id) {
        return dbBasedWorkOrderConfirmationIntegration.findById(id);
    }

    @Override
    public IntegrationWorkOrderConfirmationData sendIntegrationWorkOrderConfirmationData(WorkOrderConfirmation workOrderConfirmation) {
        return dbBasedWorkOrderConfirmationIntegration.sendIntegrationWorkOrderConfirmationData(workOrderConfirmation);
    }

    // Receipt Confirmation Data
    public List<? extends IntegrationReceiptConfirmationData> getIntegrationReceiptConfirmationData(
            Long warehouseId, String warehouseName, String number, Long clientId, String clientName,
            Long supplierId, String supplierName, LocalDateTime startTime, LocalDateTime endTime, LocalDate date){

        return dbBasedReceiptConfirmationIntegration.findAll(warehouseId, warehouseName,
                number, clientId, clientName,
                supplierId, supplierName, startTime, endTime, date);
    }
    public IntegrationReceiptConfirmationData getIntegrationReceiptConfirmationData(Long id){

        return dbBasedReceiptConfirmationIntegration.findById(id);
    }
    public IntegrationReceiptConfirmationData sendIntegrationReceiptConfirmationData(ReceiptConfirmation receiptConfirmation){

        return dbBasedReceiptConfirmationIntegration.sendIntegrationReceiptConfirmationData(receiptConfirmation);
    }

    public List<? extends IntegrationInventoryAdjustmentConfirmationData> getInventoryAdjustmentConfirmationData(

            Long warehouseId, LocalDateTime startTime, LocalDateTime endTime, LocalDate date
    ) {
        return dbBasedInventoryAdjustmentConfirmationIntegration.findAll(
                warehouseId, startTime, endTime, date
        );
    }
    public IntegrationInventoryAdjustmentConfirmationData getInventoryAdjustmentConfirmationData(Long id) {
        return dbBasedInventoryAdjustmentConfirmationIntegration.findById(id);
    }
    public IntegrationInventoryAdjustmentConfirmationData sendInventoryAdjustmentConfirmationData(InventoryAdjustmentConfirmation inventoryAdjustmentConfirmation) {
        return dbBasedInventoryAdjustmentConfirmationIntegration.saveInventoryAdjustmentConfirmationData(inventoryAdjustmentConfirmation);
    }

    @Override
    public List<? extends IntegrationInventoryAttributeChangeConfirmationData> getInventoryAttributeChangeConfirmationData(
            Long warehouseId, LocalDateTime startTime, LocalDateTime endTime, LocalDate date) {
        return dbBasedInventoryAttributeChangeConfirmationIntegration.findAll(
                warehouseId, startTime, endTime, date);
    }

    @Override
    public IntegrationInventoryAttributeChangeConfirmationData getInventoryAttributeChangeConfirmationData(Long id) {
        return dbBasedInventoryAttributeChangeConfirmationIntegration.findById(id);
    }

    @Override
    public IntegrationInventoryAttributeChangeConfirmationData sendInventoryAttributeChangeConfirmationData(InventoryAttributeChangeConfirmation inventoryAttributeChangeConfirmation) {
        return dbBasedInventoryAttributeChangeConfirmationIntegration.sendInventoryAdjustmentConfirmationData(inventoryAttributeChangeConfirmation);
    }

    @Override
    public List<? extends IntegrationInventoryShippingConfirmationData> getInventoryShippingConfirmationData(
            Long warehouseId, LocalDateTime startTime, LocalDateTime endTime, LocalDate date) {
        return null;
    }

    @Override
    public IntegrationInventoryShippingConfirmationData getInventoryShippingConfirmationData(Long id) {
        return null;
    }

    @Override
    public IntegrationInventoryShippingConfirmationData sendInventoryShippingConfirmationData(InventoryAdjustmentConfirmation inventoryAdjustmentConfirmation) {
        return null;
    }


}

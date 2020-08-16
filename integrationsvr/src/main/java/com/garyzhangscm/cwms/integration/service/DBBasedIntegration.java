package com.garyzhangscm.cwms.integration.service;


import com.garyzhangscm.cwms.integration.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * A DB based integraiton solution.
 * All data will be fetched from / saved into database
 */
@Service
@Profile({"aws-dev","default", "dev"})
public class DBBasedIntegration implements Integration{

    private static final Logger logger = LoggerFactory.getLogger(DBBasedIntegration.class);

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
        logger.debug("Will use DBBasedIntegration to process the integration data");

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

    }

    @Override
    public List<DBBasedClient> getClientData() {
        return dbBasedClientIntegration.findAll();
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
    public List<? extends IntegrationCustomerData> getCustomerData() {
        return dbBasedCustomerIntegration.findAll();
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
    public List<? extends IntegrationItemData> getItemData() {
        return dbBasedItemIntegration.findAll();
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
    public List<? extends IntegrationItemFamilyData> getItemFamilyData() {
        return dbBasedItemFamilyIntegration.findAll();
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
    public List<? extends IntegrationItemPackageTypeData> getItemPackageTypeData() {
        return dbBasedItemPackageTypeIntegration.findAll();
    }

    @Override
    public IntegrationItemPackageTypeData getItemPackageTypeData(Long id) {
        return dbBasedItemPackageTypeIntegration.findById(id);
    }

    @Override
    public IntegrationItemPackageTypeData addIntegrationItemPackageTypeData(ItemPackageType itemPackageType) {
        return dbBasedItemPackageTypeIntegration.addIntegrationItemPackageTypeData(new DBBasedItemPackageType(itemPackageType));
    }

    //
    // Integration - Item Unit Of Measure
    //
    @Override
    public List<? extends IntegrationItemUnitOfMeasureData> getItemUnitOfMeasureData() {
        return dbBasedItemUnitOfMeasureIntegration.findAll();
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
    public List<? extends IntegrationSupplierData> getSupplierData() {
        return dbBasedSupplierIntegration.findAll();
    }

    @Override
    public IntegrationSupplierData getSupplierData(Long id) {
        return dbBasedSupplierIntegration.findById(id);
    }

    @Override
    public IntegrationSupplierData addIntegrationSupplierData(Supplier supplier) {
        return dbBasedSupplierIntegration.addIntegrationSupplierData(new DBBasedSupplier(supplier));
    }


    //
    // Integration - Receipt and Receipt Line
    //
    public List<? extends IntegrationReceiptData> getReceiptData() {
        return dbBasedReceiptIntegration.findAll();
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
    public List<? extends IntegrationOrderData> getOrderData() {
        return dbBasedOrderIntegration.findAll();
    }
    public IntegrationOrderData getOrderData(Long id) {
        return dbBasedOrderIntegration.findById(id);
    }
    public IntegrationOrderData addOrderData(Order order) {
        return dbBasedOrderIntegration.addIntegrationOrderData(new DBBasedOrder(order));
    }



    // Outbound
    // Integration sent to HOST

    // Order Confirmation Data
    public List<? extends IntegrationOrderConfirmationData> getIntegrationOrderConfirmationData(Long warehouseId, String warehouseName,
                                                                                                String number){
        return dbBasedOrderConfirmationIntegration.findAll(warehouseId, warehouseName,
                number);
    }
    public IntegrationOrderConfirmationData getIntegrationOrderConfirmationData(Long id){
        return dbBasedOrderConfirmationIntegration.findById(id);
    }
    public IntegrationOrderConfirmationData sendIntegrationOrderConfirmationData(OrderConfirmation orderConfirmation){
        return dbBasedOrderConfirmationIntegration.sendIntegrationOrderConfirmationData(orderConfirmation);
    }

    @Override
    public List<? extends IntegrationWorkOrderConfirmationData> getIntegrationWorkOrderConfirmationData(Long warehouseId, String warehouseName, String number) {
        return dbBasedWorkOrderConfirmationIntegration.findAll(warehouseId, warehouseName,
                number);
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
    public List<? extends IntegrationReceiptConfirmationData> getIntegrationReceiptConfirmationData(Long warehouseId, String warehouseName,
                                                                                                    String number, Long clientId, String clientName,
                                                                                                    Long supplierId, String supplierName){

        return dbBasedReceiptConfirmationIntegration.findAll(warehouseId, warehouseName,
                number, clientId, clientName,
                supplierId, supplierName);
    }
    public IntegrationReceiptConfirmationData getIntegrationReceiptConfirmationData(Long id){

        return dbBasedReceiptConfirmationIntegration.findById(id);
    }
    public IntegrationReceiptConfirmationData sendIntegrationReceiptConfirmationData(ReceiptConfirmation receiptConfirmation){

        return dbBasedReceiptConfirmationIntegration.sendIntegrationReceiptConfirmationData(receiptConfirmation);
    }

    public List<? extends IntegrationInventoryAdjustmentConfirmationData> getInventoryAdjustmentConfirmationData() {
        return dbBasedInventoryAdjustmentConfirmationIntegration.findAll();
    }
    public IntegrationInventoryAdjustmentConfirmationData getInventoryAdjustmentConfirmationData(Long id) {
        return dbBasedInventoryAdjustmentConfirmationIntegration.findById(id);
    }
    public IntegrationInventoryAdjustmentConfirmationData sendInventoryAdjustmentConfirmationData(InventoryAdjustmentConfirmation inventoryAdjustmentConfirmation) {
        return dbBasedInventoryAdjustmentConfirmationIntegration.sendInventoryAdjustmentConfirmationData(inventoryAdjustmentConfirmation);
    }

    @Override
    public List<? extends IntegrationInventoryAttributeChangeConfirmationData> getInventoryAttributeChangeConfirmationData() {
        return dbBasedInventoryAttributeChangeConfirmationIntegration.findAll();
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
    public List<? extends IntegrationInventoryShippingConfirmationData> getInventoryShippingConfirmationData() {
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

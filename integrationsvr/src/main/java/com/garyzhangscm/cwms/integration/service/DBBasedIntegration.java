package com.garyzhangscm.cwms.integration.service;


import com.garyzhangscm.cwms.integration.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.time.LocalDate;
import java.time.ZonedDateTime;
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
    DBBasedTrailerAppointmentIntegration dbBasedTrailerAppointmentIntegration;

    @Autowired
    DBBasedSupplierIntegration dbBasedSupplierIntegration;

    @Autowired
    DBBasedReceiptIntegration dbBasedReceiptIntegration;

    @Autowired
    DBBasedPurchaseOrderIntegration dbBasedPurchaseOrderIntegration;

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

        logger.debug("#12 Purchase Order");
        dbBasedPurchaseOrderIntegration.listen();

        logger.debug("#13 Trailer Appointment");
        dbBasedTrailerAppointmentIntegration.listen();


    }


    @Override
    public void send() {

        logger.debug("Will use DBBasedIntegration to process the outbound integration data");

        logger.debug("#1 inventory adjustment confirmation");
        dbBasedInventoryAdjustmentConfirmationIntegration.sendToHost();


        logger.debug("#2 order confirmation");
        dbBasedOrderConfirmationIntegration.sendToHost();


        logger.debug("#3 receipt confirmation");
        dbBasedReceiptConfirmationIntegration.sendToHost();
    }
    @Override
    public List<DBBasedClient> getClientData(
            Long warehouseId, ZonedDateTime startTime, ZonedDateTime endTime, LocalDate date,
            String statusList,
            Long id) {
        return dbBasedClientIntegration.findAll(
                warehouseId, startTime, endTime, date, statusList, id);
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
            Long warehouseId, ZonedDateTime startTime, ZonedDateTime endTime, LocalDate date,
            String statusList,
            Long id) {
        return dbBasedCustomerIntegration.findAll(
                warehouseId, startTime, endTime, date, statusList, id);
    }

    @Override
    public IntegrationCustomerData getCustomerData(Long id) {
        return dbBasedCustomerIntegration.findById(id);
    }

    @Override
    public IntegrationCustomerData addIntegrationCustomerData(Customer customer) {
        return dbBasedCustomerIntegration.addIntegrationCustomerData(new DBBasedCustomer(customer));
    }

    @Override
    public IntegrationCustomerData addIntegrationCustomerData(DBBasedCustomer dbBasedCustomer) {
        return dbBasedCustomerIntegration.addIntegrationCustomerData(dbBasedCustomer);
    }

    //
    // Integration - Item
    //
    @Override
    public List<? extends IntegrationItemData> getItemData(
            String companyCode,
            Long warehouseId, ZonedDateTime startTime, ZonedDateTime endTime, LocalDate date,
            String statusList,
            Long id) {
        return dbBasedItemIntegration.findAll(companyCode,
                warehouseId, startTime, endTime, date, statusList, id);
    }

    @Override
    public IntegrationItemData getItemData(Long id) {
        return dbBasedItemIntegration.findById(id);
    }

    @Override
    @Transactional
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
            String companyCode,
            Long warehouseId, ZonedDateTime startTime, ZonedDateTime endTime, LocalDate date,
            String statusList,
            Long id) {
        return dbBasedItemFamilyIntegration.findAll(companyCode,
                warehouseId, startTime, endTime, date, statusList, id);
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
            String companyCode,
            Long warehouseId, ZonedDateTime startTime, ZonedDateTime endTime, LocalDate date,
            String statusList,
            Long id) {
        return dbBasedItemPackageTypeIntegration.findAll(companyCode,
                warehouseId, startTime, endTime, date, statusList, id);
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
            String companyCode,
            Long warehouseId, ZonedDateTime startTime, ZonedDateTime endTime, LocalDate date,
            String statusList,
            Long id) {
        return dbBasedItemUnitOfMeasureIntegration.findAll(companyCode,
                warehouseId, startTime, endTime, date, statusList, id);
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
            Long warehouseId, ZonedDateTime startTime, ZonedDateTime endTime, LocalDate date,
            String statusList,
            Long id) {
        return dbBasedSupplierIntegration.findAll(
                warehouseId, startTime, endTime, date, statusList, id);
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
    public IntegrationSupplierData addIntegrationSupplierData(DBBasedSupplier dbBasedSupplier) {
        return dbBasedSupplierIntegration.addIntegrationSupplierData(dbBasedSupplier);
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
    public List<? extends IntegrationReceiptData> getReceiptData(String companyCode,
            Long warehouseId, ZonedDateTime startTime, ZonedDateTime endTime, LocalDate date,
            String statusList,
            Long id) {
        return dbBasedReceiptIntegration.findAll(
                companyCode, warehouseId, startTime, endTime, date, statusList, id);
    }
    public IntegrationReceiptData getReceiptData(Long id) {
        return dbBasedReceiptIntegration.findById(id);
    }
    public IntegrationReceiptData addReceiptData(Receipt receipt) {

        return dbBasedReceiptIntegration.addIntegrationReceiptData(new DBBasedReceipt(receipt));

    }
    public IntegrationReceiptData addReceiptData(DBBasedReceipt dbBasedReceipt) {

        return dbBasedReceiptIntegration.addIntegrationReceiptData(dbBasedReceipt);

    }
    //
    // Integration - Purchase Order and Purchase Order line
    //
    public List<? extends IntegrationPurchaseOrderData> getPurchaseOrderData(String companyCode,
                                                                 Long warehouseId, ZonedDateTime startTime,
                                                                             ZonedDateTime endTime, LocalDate date,
                                                                 String statusList,
                                                                 Long id) {
        return dbBasedPurchaseOrderIntegration.findAll(
                companyCode, warehouseId, startTime, endTime, date, statusList, id);
    }
    public IntegrationPurchaseOrderData getPurchaseOrderData(Long id) {
        return dbBasedPurchaseOrderIntegration.findById(id);
    }
    public IntegrationPurchaseOrderData addPurchaseOrderData(PurchaseOrder purchaseOrder) {

        return dbBasedPurchaseOrderIntegration.addIntegrationPurchaseOrderData(new DBBasedPurchaseOrder(purchaseOrder));

    }
    public IntegrationPurchaseOrderData addPurchaseOrderData(DBBasedPurchaseOrder dbBasedPurchaseOrder) {

        return dbBasedPurchaseOrderIntegration.addIntegrationPurchaseOrderData(dbBasedPurchaseOrder);

    }

    //
    // Integration - Order and Order Line
    //
    public List<? extends IntegrationOrderData> getOrderData(String companyCode,
            Long warehouseId, ZonedDateTime startTime, ZonedDateTime endTime, LocalDate date,
            String statusList,
            Long id) {
        return dbBasedOrderIntegration.findAll(
                companyCode, warehouseId, startTime, endTime, date, statusList, id);
    }
    public IntegrationOrderData getOrderData(Long id) {
        return dbBasedOrderIntegration.findById(id);
    }
    public IntegrationOrderData addOrderData(Order order) {
        return dbBasedOrderIntegration.addIntegrationOrderData(new DBBasedOrder(order));
    }
    public IntegrationOrderData addOrderData(DBBasedOrder dbBasedOrder) {
        return dbBasedOrderIntegration.addIntegrationOrderData(dbBasedOrder);
    }


    //
    // Integration - Work Order and Work Order Line
    //
    @Override
    public List<? extends IntegrationWorkOrderData> getWorkOrderData(
            String companyCode,
            Long warehouseId, ZonedDateTime startTime, ZonedDateTime endTime, LocalDate date,
            String statusList,
            Long id) {
        return dbBasedWorkOrderIntegration.findAll(companyCode,
                warehouseId, startTime, endTime, date, statusList, id
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
            Long warehouseId, ZonedDateTime startTime, ZonedDateTime endTime, LocalDate date,
            String statusList,
            Long id) {
        return dbBasedBillOfMaterialIntegration.findAll(
                warehouseId, startTime, endTime, date, statusList, id
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
                                                                                                String number, ZonedDateTime startTime,
                                                                                                ZonedDateTime endTime, LocalDate date,
                                                                                                String statusList, Long id){
        return dbBasedOrderConfirmationIntegration.findAll(warehouseId, warehouseName,
                number, startTime, endTime, date, statusList, id);
    }
    public List<? extends IntegrationOrderConfirmationData> getPendingIntegrationOrderConfirmationData(
            Long warehouseId, String companyCode, String warehouseName){
        return dbBasedOrderConfirmationIntegration.getPendingIntegrationOrderConfirmationData(
                warehouseId, companyCode, warehouseName
        );
    }
    public IntegrationOrderConfirmationData getIntegrationOrderConfirmationData(Long id){
        return dbBasedOrderConfirmationIntegration.findById(id);
    }
    public IntegrationOrderConfirmationData resendOrderConfirmationData(Long id){
        return dbBasedOrderConfirmationIntegration.resendOrderConfirmationData(id);
    }
    public IntegrationOrderConfirmationData sendIntegrationOrderConfirmationData(OrderConfirmation orderConfirmation){
        return dbBasedOrderConfirmationIntegration.sendIntegrationOrderConfirmationData(orderConfirmation);
    }

    @Override
    public List<? extends IntegrationWorkOrderConfirmationData> getIntegrationWorkOrderConfirmationData(
            Long warehouseId, String warehouseName, String number,
            ZonedDateTime startTime, ZonedDateTime endTime, LocalDate date,
            String statusList, Long id) {
        return dbBasedWorkOrderConfirmationIntegration.findAll(warehouseId, warehouseName,
                number, startTime, endTime, date, statusList, id);
    }
    @Override
    public List<? extends IntegrationWorkOrderConfirmationData> getPendingIntegrationWorkOrderConfirmationData(
            Long warehouseId, String companyCode, String warehouseName) {
        return dbBasedWorkOrderConfirmationIntegration.getPendingIntegrationWorkOrderConfirmationData(
                warehouseId, companyCode, warehouseName);
    }



    @Override
    public IntegrationWorkOrderConfirmationData getIntegrationWorkOrderConfirmationData(Long id) {
        return dbBasedWorkOrderConfirmationIntegration.findById(id);
    }

    @Override
    public IntegrationWorkOrderConfirmationData resendWorkOrderConfirmationData(Long id) {
        return dbBasedWorkOrderConfirmationIntegration.resendWorkOrderConfirmationData(id);
    }

    @Override
    public IntegrationWorkOrderConfirmationData sendIntegrationWorkOrderConfirmationData(WorkOrderConfirmation workOrderConfirmation) {
        return dbBasedWorkOrderConfirmationIntegration.sendIntegrationWorkOrderConfirmationData(workOrderConfirmation);
    }

    // Receipt Confirmation Data
    public List<? extends IntegrationReceiptConfirmationData> getIntegrationReceiptConfirmationData(
            Long warehouseId, String warehouseName, String number, Long clientId, String clientName,
            Long supplierId, String supplierName, ZonedDateTime startTime, ZonedDateTime endTime, LocalDate date,
            String statusList, Long id){

        return dbBasedReceiptConfirmationIntegration.findAll(warehouseId, warehouseName,
                number, clientId, clientName,
                supplierId, supplierName, startTime, endTime, date, statusList, id);
    }
    public  List<? extends IntegrationReceiptConfirmationData>  getPendingIntegrationReceiptConfirmationData(
            Long warehouseId, String companyCode, String warehouseName){

        return dbBasedReceiptConfirmationIntegration.getPendingIntegrationReceiptConfirmationData(
                warehouseId, companyCode, warehouseName
        );
    }
    public IntegrationReceiptConfirmationData getIntegrationReceiptConfirmationData(Long id){

        return dbBasedReceiptConfirmationIntegration.findById(id);
    }
    public IntegrationReceiptConfirmationData resendReceiptConfirmationData(Long id){

        return dbBasedReceiptConfirmationIntegration.resendReceiptConfirmationData(id);
    }
    public IntegrationReceiptConfirmationData sendIntegrationReceiptConfirmationData(ReceiptConfirmation receiptConfirmation){

        return dbBasedReceiptConfirmationIntegration.sendIntegrationReceiptConfirmationData(receiptConfirmation);
    }

    public List<? extends IntegrationInventoryAdjustmentConfirmationData> getInventoryAdjustmentConfirmationData(

            Long warehouseId, ZonedDateTime startTime, ZonedDateTime endTime, LocalDate date,
            String statusList, Long id
    ) {
        return dbBasedInventoryAdjustmentConfirmationIntegration.findAll(
                warehouseId, startTime, endTime, date, statusList, id
        );
    }

    public List<? extends IntegrationInventoryAdjustmentConfirmationData> getPendingInventoryAdjustmentConfirmationData(
            Long warehouseId, String companyCode, String warehouseName) {
        return dbBasedInventoryAdjustmentConfirmationIntegration.getPendingInventoryAdjustmentConfirmationData(
                warehouseId, companyCode, warehouseName
        );
    }


    public IntegrationInventoryAdjustmentConfirmationData getInventoryAdjustmentConfirmationData(Long id) {
        return dbBasedInventoryAdjustmentConfirmationIntegration.findById(id);
    }
    public IntegrationInventoryAdjustmentConfirmationData resendInventoryAdjustmentConfirmationData(Long id) {
        return dbBasedInventoryAdjustmentConfirmationIntegration.resendInventoryAdjustmentConfirmationData(id);
    }
    public IntegrationInventoryAdjustmentConfirmationData saveInventoryAdjustmentConfirmationResult(
            Long id, boolean succeed, String errorMessage
    ) {
        return dbBasedInventoryAdjustmentConfirmationIntegration.saveInventoryAdjustmentConfirmationResult(id, succeed, errorMessage);
    }
    public IntegrationInventoryAdjustmentConfirmationData sendInventoryAdjustmentConfirmationData(InventoryAdjustmentConfirmation inventoryAdjustmentConfirmation) {
        return dbBasedInventoryAdjustmentConfirmationIntegration.saveInventoryAdjustmentConfirmationData(inventoryAdjustmentConfirmation);
    }

    @Override
    public List<? extends IntegrationInventoryAttributeChangeConfirmationData> getInventoryAttributeChangeConfirmationData(
            Long warehouseId, ZonedDateTime startTime, ZonedDateTime endTime, LocalDate date,
            String statusList, Long id) {
        return dbBasedInventoryAttributeChangeConfirmationIntegration.findAll(
                warehouseId, startTime, endTime, date, statusList, id);
    }
    @Override
    public List<? extends IntegrationInventoryAttributeChangeConfirmationData> getPendingInventoryAttributeChangeConfirmationData(
            Long warehouseId, String companyCode, String warehouseName) {
        return dbBasedInventoryAttributeChangeConfirmationIntegration.getPendingInventoryAttributeChangeConfirmationData(
                warehouseId, companyCode, warehouseName);
    }


    @Override
    public IntegrationInventoryAttributeChangeConfirmationData getInventoryAttributeChangeConfirmationData(Long id) {
        return dbBasedInventoryAttributeChangeConfirmationIntegration.findById(id);
    }

    @Override
    public IntegrationInventoryAttributeChangeConfirmationData resendInventoryAttributeChangeConfirmationData(Long id) {
        return dbBasedInventoryAttributeChangeConfirmationIntegration.resendInventoryAttributeChangeConfirmationData(id);
    }

    @Override
    public IntegrationInventoryAttributeChangeConfirmationData sendInventoryAttributeChangeConfirmationData(InventoryAttributeChangeConfirmation inventoryAttributeChangeConfirmation) {
        return dbBasedInventoryAttributeChangeConfirmationIntegration.sendInventoryAdjustmentConfirmationData(inventoryAttributeChangeConfirmation);
    }

    @Override
    public List<? extends IntegrationInventoryShippingConfirmationData> getInventoryShippingConfirmationData(
            Long warehouseId, ZonedDateTime startTime, ZonedDateTime endTime, LocalDate date) {
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


    @Override
    public void saveIntegrationResult(IntegrationResult integrationResult) {
        logger.debug("Start to save integration result for {}, id: {}",
                integrationResult.getIntegrationType(),
                integrationResult.getIntegrationId());
        switch (integrationResult.getIntegrationType()) {
            case INTEGRATION_RECEIPT:
                dbBasedReceiptIntegration.saveIntegrationResult(integrationResult);
                break;
            case INTEGRATION_ITEM:
                dbBasedItemIntegration.saveIntegrationResult(integrationResult);
                break;
            case INTEGRATION_CLIENT:
                dbBasedClientIntegration.saveIntegrationResult(integrationResult);
                break;
            case INTEGRATION_CUSTOMER:
                dbBasedCustomerIntegration.saveIntegrationResult(integrationResult);
                break;
            case INTEGRATION_BILL_OF_MATERIAL:
                dbBasedBillOfMaterialIntegration.saveIntegrationResult(integrationResult);
                break;
            case INTEGRATION_ORDER:
                dbBasedOrderIntegration.saveIntegrationResult(integrationResult);
                break;
            case INTEGRATION_ITEM_FAMILY:
                dbBasedItemFamilyIntegration.saveIntegrationResult(integrationResult);
                break;
            case INTEGRATION_SUPPLIER:
                dbBasedSupplierIntegration.saveIntegrationResult(integrationResult);
                break;
            case INTEGRATION_WORK_ORDER:
                dbBasedWorkOrderIntegration.saveIntegrationResult(integrationResult);
                break;
            case INTEGRATION_PURCHASE_ORDER:
                dbBasedPurchaseOrderIntegration.saveIntegrationResult(integrationResult);
                break;
            case INTEGRATION_TRAILER_APPOINTMENT:
                dbBasedTrailerAppointmentIntegration.saveIntegrationResult(integrationResult);
                break;
        }
    }


    @Override
    public IntegrationReceiptData resendReceiptData(Long id) {
        return dbBasedReceiptIntegration.resendReceiptData(id);
    }

    @Override
    public IntegrationPurchaseOrderData resendPurchaseOrderData(Long id) {
        return dbBasedPurchaseOrderIntegration.resendPurchaseOrderData(id);
    }

    @Override
    public IntegrationBillOfMaterialData resendBillOfMaterialData(Long id) {
        return dbBasedBillOfMaterialIntegration.resendBillOfMaterialData(id);
    }
    @Override
    public IntegrationClientData resendClientData(Long id) {
        return dbBasedClientIntegration.resendClientData(id);
    }
    @Override
    public IntegrationCustomerData resendCustomerData(Long id) {
        return dbBasedCustomerIntegration.resendCustomerData(id);
    }
    @Override
    public IntegrationItemFamilyData resendItemFamilyData(Long id) {
        return dbBasedItemFamilyIntegration.resendItemFamilyData(id);
    }
    @Override
    public IntegrationItemData resendItemData(Long id) {
        return dbBasedItemIntegration.resendItemData(id);
    }
    @Override
    public IntegrationItemPackageTypeData resendItemPackageTypeData(Long id) {
        return dbBasedItemPackageTypeIntegration.resendItemPackageTypeData(id);
    }
    @Override
    public IntegrationItemUnitOfMeasureData resendItemUnitOfMeasureData(Long id) {
        return dbBasedItemUnitOfMeasureIntegration.resendItemUnitOfMeasureData(id);
    }
    @Override
    public IntegrationOrderData resendOrderData(Long id) {
        return dbBasedOrderIntegration.resendOrderData(id);
    }
    @Override
    public IntegrationSupplierData resendSupplierData(Long id) {
        return dbBasedSupplierIntegration.resendSupplierData(id);
    }
    @Override
    public IntegrationWorkOrderData resendWorkOrderData(Long id) {
        return dbBasedWorkOrderIntegration.resendWorkOrderData(id);
    }
}

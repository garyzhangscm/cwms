package com.garyzhangscm.cwms.integration.service;


import com.garyzhangscm.cwms.integration.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.List;

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



    public void listen() {
        logger.debug("Will use DBBasedIntegration to process the integration data");

        logger.debug("#1 Customer data");
        dbBasedCustomerIntegration.listen();

        logger.debug("#2 Supplier data");
        dbBasedSupplierIntegration.listen();

        logger.debug("#3 Client data");
        dbBasedClientIntegration.listen();

        logger.debug("#7 Item Unit Of Measure");
        dbBasedItemUnitOfMeasureIntegration.listen();

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
        return dbBasedItemIntegration.addIntegrationItemData(new DBBasedItem(item));
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


}

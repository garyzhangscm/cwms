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
}

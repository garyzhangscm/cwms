package com.garyzhangscm.cwms.integration.service;

import com.garyzhangscm.cwms.integration.model.IntegrationClientData;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface Integration {

    void listen();

    List<? extends IntegrationClientData> getClientData();

    <T> T getCustomerData();

    <T> T getItemData();

    <T> T getItemFamilyData();

    <T> T getItemPackageType();

    <T> T getItemUnitOfMeasure();

    <T> T getSupplierData();

}

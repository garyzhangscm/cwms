package com.garyzhangscm.cwms.integration.service;

import com.garyzhangscm.cwms.integration.model.IntegrationClientData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class IntegrationDataService {

    @Autowired
    private Integration integration;

    public List<? extends IntegrationClientData> getClientData() {
        return integration.getClientData();
    }

    public <T> T getCustomerData() {
        return null;
    }

    public <T> T getItemData() {
        return null;
    }

    public <T> T getItemFamilyData() {
        return null;
    }

    public <T> T getItemPackageType() {
        return null;
    }

    public <T> T getItemUnitOfMeasure() {
        return null;
    }

    public <T> T getSupplierData() {
        return null;
    }
}

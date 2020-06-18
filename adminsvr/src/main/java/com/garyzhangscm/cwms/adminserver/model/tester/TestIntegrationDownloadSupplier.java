package com.garyzhangscm.cwms.adminserver.model.tester;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.garyzhangscm.cwms.adminserver.clients.CommonServiceRestemplateClient;
import com.garyzhangscm.cwms.adminserver.clients.IntegrationServiceRestemplateClient;
import com.garyzhangscm.cwms.adminserver.clients.InventoryServiceRestemplateClient;
import com.garyzhangscm.cwms.adminserver.exception.TestFailException;
import com.garyzhangscm.cwms.adminserver.model.wms.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Service
public class TestIntegrationDownloadSupplier extends TestScenario{

    private static final Logger logger = LoggerFactory.getLogger(TestIntegrationDownloadSupplier.class);

    private final String[] supplierNames = {"TEST-SUPPLIER-A", "TEST-SUPPLIER-B", "TEST-SUPPLIER-C"};

    @Autowired
    private InventoryServiceRestemplateClient inventoryServiceRestemplateClient;
    @Autowired
    private CommonServiceRestemplateClient commonServiceRestemplateClient;
    @Autowired
    private IntegrationUtil integrationUtil;


    public TestIntegrationDownloadSupplier() {
        super(TestScenarioType.INTEGRATION_DOWNLOAD_SUPPLIER, 10300);

    }
    @Override
    public void runTest(Warehouse warehouse) {

            List<IntegrationData> integrationDataList = createSuppliers();
            integrationDataList.forEach(integrationData -> {
                logger.debug("Created supplier integration data: {}", integrationData.getId());
            });

            assertResult(integrationDataList);

    }

    private void assertResult(List<IntegrationData> integrationDataList) {

        // we will need to make sure
        // 1. all the integration data has been saved, immediately
        // 2. all the integration data has been processed, within certain time period
        // 3. data has been saved in certain table, within certain time period
        assertResultSaved(integrationDataList);
        assertResultProcessed(integrationDataList);
        assertResultConfirmed();
    }
    private void assertResultSaved(List<IntegrationData> integrationDataList) {
        integrationUtil.assertResultSaved(integrationDataList, Supplier.class);
    }
    private void assertResultProcessed(List<IntegrationData> integrationDataList)  {
       integrationUtil.assertResultProcessed(integrationDataList, Supplier.class);
    }
    private void assertResultConfirmed() {

        // Let's check from the inventory service to see if our item has been saved correctly
        // if we are here, we will assume we already passed assetResultProcessed()
        // which means the integration data has been processed correctly

        // We will allow about 3 minutes for the system to send and process the integration data
        int timeoutInTotal = 180000;
        int timeout = 2000;
        int i = 0;
        boolean allProcessed;

        while(i * timeout < timeoutInTotal) {
            i++;
            allProcessed = true;
            try {

                for (String supplierName : supplierNames) {

                    // Get the supplier information
                    Supplier  supplier = commonServiceRestemplateClient.getSupplierByName(supplierName);
                    if (Objects.isNull(supplier) || !supplier.getName().equals(supplierName)) {
                        allProcessed = false;
                        break;
                    }

                }
                if (allProcessed) {
                    // OK, all the integration has been process, sounds good
                    return;
                }
                else {

                    //
                    Thread.sleep(timeout);
                }

            } catch (InterruptedException e) {
                throw TestFailException.raiseException("Thread interrupted: " + e.getMessage());
            }
        }

        throw TestFailException.raiseException("All integration has been processed but we can't find the supplier after  " +
                (timeoutInTotal / timeout) + " seconds");

    }

    private List<IntegrationData> createSuppliers() {

        List<IntegrationData> integrationDataList = new ArrayList<>();
        Arrays.stream(supplierNames).forEach(supplierName ->
        {
            try {
                integrationDataList.add(createSupplier(supplierName));
            } catch (JsonProcessingException e) {
                throw TestFailException.raiseException("JsonProcessingException: " + e.getMessage());
            }
        });

        return integrationDataList;

    }

    private IntegrationData createSupplier(String supplierName) throws JsonProcessingException {
        logger.debug("Start to create supplier {}", supplierName);

        TesterDataSetAddress address = TesterDataSetAddress.getInstance();
        Supplier supplier = new Supplier(supplierName, supplierName,
                address.getContactorFirstname(), address.getContactorFirstname(),
                address.getAddressCountry(),address.getAddressState(),
                address.getAddressCounty(),address.getAddressCity(),address.getAddressDistrict(),
                address.getAddressLine1(), address.getAddressLine2(), address.getAddressPostcode());


        return integrationUtil.sendData(Supplier.class,supplier);

    }

}

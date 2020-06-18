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

import java.util.*;

@Service
public class TestIntegrationDownloadItemFamily extends TestScenario{

    private static final Logger logger = LoggerFactory.getLogger(TestIntegrationDownloadItemFamily.class);

    String[] itemFamilyNames = new String[] {"TEST_HIGH_VALUE", "FROZEN"};

    @Autowired
    private InventoryServiceRestemplateClient inventoryServiceRestemplateClient;
    @Autowired
    private CommonServiceRestemplateClient commonServiceRestemplateClient;
    @Autowired
    private IntegrationUtil integrationUtil;


    public TestIntegrationDownloadItemFamily() {
        super(TestScenarioType.INTEGRATION_DOWNLOAD_ITEM_FAMILY, 10000);

    }
    @Override
    public void runTest(Warehouse warehouse) {

            List<IntegrationData> integrationDataList = createItemFamilies(warehouse);
            integrationDataList.forEach(integrationData -> {
                logger.debug("Created item Family integration data: {}", integrationData.getId());
            });

            assertResult(integrationDataList, warehouse);

    }

    private void assertResult(List<IntegrationData> integrationDataList, Warehouse warehouse) {

        // we will need to make sure
        // 1. all the integration data has been saved, immediately
        // 2. all the integration data has been processed, within certain time period
        // 3. data has been saved in certain table, within certain time period
        assertResultSaved(integrationDataList);
        assertResultProcessed(integrationDataList);
        assertResultConfirmed(warehouse);



    }

    private void assertResultSaved(List<IntegrationData> integrationDataList) {
        integrationUtil.assertResultSaved(integrationDataList, ItemFamily.class);
    }
    private void assertResultProcessed(List<IntegrationData> integrationDataList)  {
        integrationUtil.assertResultProcessed(integrationDataList, ItemFamily.class);
    }

    private void assertResultConfirmed(Warehouse warehouse) {

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

                for (String itemFamilyName : itemFamilyNames) {

                    // Get the item family information
                    ItemFamily itemFamily = inventoryServiceRestemplateClient.getItemFamilyByName(warehouse.getId(), itemFamilyName);
                    if (Objects.isNull(itemFamily) || !itemFamily.getName().equals(itemFamilyName)) {
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

        throw TestFailException.raiseException("All integration has been processed but we can't find the item family after  " +
                (timeoutInTotal / timeout) + " seconds");

    }

    private List<IntegrationData> createItemFamilies(Warehouse warehouse) {

        List<IntegrationData> integrationDataList = new ArrayList<>();
        Arrays.stream(itemFamilyNames).forEach(itemFamilyName ->
        {
            try {
                integrationDataList.add(createItemFamily(warehouse, itemFamilyName));
            } catch (JsonProcessingException e) {
                throw TestFailException.raiseException("JsonProcessingException: " + e.getMessage());
            }
        });

        return integrationDataList;

    }

    private IntegrationData createItemFamily(Warehouse warehouse, String itemFamilyName) throws JsonProcessingException {
        logger.debug("Start to create item family {}", itemFamilyName);

        ItemFamily itemFamily = new ItemFamily(warehouse, itemFamilyName);


        return integrationUtil.sendData(ItemFamily.class, itemFamily);

    }

}

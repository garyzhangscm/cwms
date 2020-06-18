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
public class TestIntegrationDownloadClient extends TestScenario{

    private static final Logger logger = LoggerFactory.getLogger(TestIntegrationDownloadClient.class);

    private final String[] clientNames = {"TEST-CLIENT-A", "TEST-CLIENT-B", "TEST-CLIENT-C"};

    @Autowired
    private InventoryServiceRestemplateClient inventoryServiceRestemplateClient;
    @Autowired
    private CommonServiceRestemplateClient commonServiceRestemplateClient;
    @Autowired
    private IntegrationUtil integrationUtil;


    public TestIntegrationDownloadClient() {
        super(TestScenarioType.INTEGRATION_DOWNLOAD_CLIENT, 10400);

    }
    @Override
    public void runTest(Warehouse warehouse) {

            List<IntegrationData> integrationDataList = createClients();
            integrationDataList.forEach(integrationData -> {
                logger.debug("Created client integration data: {}", integrationData.getId());
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
        integrationUtil.assertResultSaved(integrationDataList, Client.class);
    }
    private void assertResultProcessed(List<IntegrationData> integrationDataList)  {
        integrationUtil.assertResultProcessed(integrationDataList, Client.class);
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

                for (String clientName : clientNames) {

                    // Get the supplier information
                    Client client = commonServiceRestemplateClient.getClientByName(clientName);
                    if (Objects.isNull(client) || !client.getName().equals(clientName)) {
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

        throw TestFailException.raiseException("All integration has been processed but we can't find the client after  " +
                (timeoutInTotal / timeout) + " seconds");

    }

    private List<IntegrationData> createClients() {

        List<IntegrationData> integrationDataList = new ArrayList<>();
        Arrays.stream(clientNames).forEach(clientName ->
        {
            try {
                integrationDataList.add(createClient( clientName));
            } catch (JsonProcessingException e) {
                throw TestFailException.raiseException("JsonProcessingException: " + e.getMessage());
            }
        });

        return integrationDataList;

    }

    private IntegrationData createClient(String clientName) throws JsonProcessingException {
        logger.debug("Start to create client {}", clientName);

        TesterDataSetAddress address = TesterDataSetAddress.getInstance();
        Client client = new Client(clientName, clientName,
                address.getContactorFirstname(), address.getContactorFirstname(),
                address.getAddressCountry(),address.getAddressState(),
                address.getAddressCounty(),address.getAddressCity(),address.getAddressDistrict(),
                address.getAddressLine1(), address.getAddressLine2(), address.getAddressPostcode());


        return integrationUtil.sendData(Client.class,client);

    }

}

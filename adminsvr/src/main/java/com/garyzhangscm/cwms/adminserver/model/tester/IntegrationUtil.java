package com.garyzhangscm.cwms.adminserver.model.tester;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.garyzhangscm.cwms.adminserver.clients.IntegrationServiceRestemplateClient;
import com.garyzhangscm.cwms.adminserver.exception.TestFailException;
import com.garyzhangscm.cwms.adminserver.model.wms.*;
import org.hibernate.criterion.Order;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class IntegrationUtil {


    @Autowired
    private IntegrationServiceRestemplateClient integrationServiceRestemplateClient;

    private static Map<Class, String> subUrls = new HashMap<>();
    static {
        subUrls.put(Item.class, "items");
        subUrls.put(ItemFamily.class, "item-families");
        subUrls.put(Supplier.class, "suppliers");
        subUrls.put(Customer.class, "customers");
        subUrls.put(Client.class, "clients");
        subUrls.put(Receipt.class, "receipts");
        subUrls.put(Order.class, "orders");
        subUrls.put(ReceiptConfirmation.class, "receipt-confirmations");
        subUrls.put(OrderConfirmation.class, "order-confirmations");
    }

    public IntegrationData getData(String subUrl, Long id) {
        return integrationServiceRestemplateClient.getData(subUrl, id);
    }

    public IntegrationData getData(Class clazz, Long id) {
        return getData(subUrls.get(clazz), id);
    }

    public List<IntegrationData> getDataByParams(String subUrl, Map<String, String> params) {
        return integrationServiceRestemplateClient.getDataByParams(subUrl, params);
    }

    public List<IntegrationData> getDataByParams(Class clazz, Map<String, String> params) {
        return getDataByParams(subUrls.get(clazz), params);
    }
    public <T> IntegrationData sendData(String subUrl, T data) throws JsonProcessingException {
        return integrationServiceRestemplateClient.sendData(subUrl, data);
    }

    public <T> IntegrationData sendData(Class clazz, T data) throws JsonProcessingException {
        return sendData(subUrls.get(clazz), data);
    }


    public void assertResultSaved(List<IntegrationData> integrationDataList, Class clazz) {
        integrationDataList.forEach(integrationData -> {
            // make sure we at least have the integration with the right id

            IntegrationData savedIntegrationData
                    = getData(clazz, integrationData.getId());
            if (Objects.isNull(savedIntegrationData)) {
                throw TestFailException.raiseException("Can't find integration data of type: " +
                        clazz +", by id " + integrationData.getId());
            }
        });
    }

    public void assertResultProcessed(List<IntegrationData> integrationDataList, Class clazz)  {
        // We will allow about 3 minutes for the system to process the integration data
        int timeoutInTotal = 180000;
        int timeout = 2000;
        int i = 0;
        boolean allProcessed;
        while(i * timeout < timeoutInTotal) {
            i++;
            try {
                allProcessed = true;
                for (IntegrationData integrationData : integrationDataList) {

                    IntegrationData savedIntegrationData
                            = getData(clazz, integrationData.getId());
                    if (Objects.isNull(savedIntegrationData)) {
                        throw TestFailException.raiseException("Can't find integration data of type: " +
                                clazz +", by id " + integrationData.getId());
                    }
                    if (!savedIntegrationData.getStatus().equals(IntegrationStatus.COMPLETED) &&
                            !savedIntegrationData.getStatus().equals(IntegrationStatus.ERROR)) {
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

        throw TestFailException.raiseException("Some integration of type: " + clazz +
                " has not been processed after  " + (timeoutInTotal / 1000 ) + " seconds");
    }


}

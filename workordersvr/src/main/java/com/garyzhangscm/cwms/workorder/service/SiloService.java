package com.garyzhangscm.cwms.workorder.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.garyzhangscm.cwms.workorder.clients.InventoryServiceRestemplateClient;
import com.garyzhangscm.cwms.workorder.clients.SiloRestemplateClient;
import com.garyzhangscm.cwms.workorder.exception.WorkOrderException;
import com.garyzhangscm.cwms.workorder.model.*;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class SiloService {
    private static final Logger logger = LoggerFactory.getLogger(SiloService.class);


    @Autowired
    private SiloRestemplateClient siloRestemplateClient;

    @Autowired
    private SiloConfigurationService siloConfigurationService;

    @Autowired
    private SiloDeviceAPICallHistoryService siloDeviceAPICallHistoryService;

    @Autowired
    private InventoryServiceRestemplateClient inventoryServiceRestemplateClient;

    private static String lastToken = "";
    private static LocalDateTime lastTokenGeneratedTime;

    @Qualifier("getObjMapper")
    @Autowired
    private ObjectMapper objectMapper;


    private void refreshToken(String token) {
        lastToken = token;
        lastTokenGeneratedTime = LocalDateTime.now();
    }


    private String getToken(Long warehouseId) {
        // get the last login token, We will refresh the token once every 5 minutes
        if (Strings.isNotBlank(lastToken) && Objects.nonNull(lastTokenGeneratedTime) &&
                lastTokenGeneratedTime.isAfter(LocalDateTime.now().minusMinutes(5))) {
            // we already get the token and it is generated within last 5 minutes
            logger.debug("get the cached token: {}", lastToken);
            return lastToken;
        }

        String tokenResponse = siloRestemplateClient.loginSilo(warehouseId);
        try {

            if (Strings.isNotBlank(tokenResponse)) {
                SiloLoginResponseWrapper siloLoginResponseWrapper
                        = objectMapper.readValue(tokenResponse, SiloLoginResponseWrapper.class);

                if (Objects.isNull(siloLoginResponseWrapper)) {
                    throw WorkOrderException.raiseException("Can't login SILO system");
                }
                refreshToken(siloLoginResponseWrapper.getToken());
                return siloLoginResponseWrapper.getToken();

            }
        }catch (JsonProcessingException e) {
            e.printStackTrace();
            logger.debug("can't pass the sile information");
        }
        throw WorkOrderException.raiseException("Can't login SILO system");

    }


    /**
     * Sync device status from web API endpoint every 5 minutes
     */
    @Scheduled(fixedDelay = 300000)
    public void syncDeviceStaus() {

        logger.debug("start to sync silo device status");
        List<SiloConfiguration> sileEnabledWarehouses = siloConfigurationService.findSiloEnabledWarehouse();
        logger.debug("Find {} warehouse that has silo enabled!",
                sileEnabledWarehouses.size());
        sileEnabledWarehouses.forEach(
                siloConfiguration -> getSiloDevices(siloConfiguration.getWarehouseId(), true)
        );
    }

    private void syncDeviceStaus(Long warehouseId) {

        logger.debug("start to sync silo device status for warehouse {}", warehouseId);
        // if token is not passed in, login the default user and get the token
        String token = "";
        try {
            token = getToken(warehouseId);
        }
        catch (Exception ex) {

            ex.printStackTrace();
            logger.debug("Error while getting silo token for warehouse {}. \nError message: {} \n " +
                    "We will ignore the error but don't sync data for this warehouse", warehouseId,
                    ex.getMessage());
        }

        if (Strings.isBlank(token)) {

            logger.debug("We can't get the token for the current warehouse {}, skip",
                    warehouseId);
            // throw WorkOrderException.raiseException("Can't login SILO system");
            return;
        }

        SiloDeviceResponseWrapper siloDevicesResponseWrapper =  siloRestemplateClient.getSiloDevices(
                warehouseId, token);

        refreshToken(siloDevicesResponseWrapper.getToken());

        logger.debug("Get {} silo devices", siloDevicesResponseWrapper.getSiloDevices().size());

        siloDevicesResponseWrapper.getSiloDevices().forEach(
                siloDevice -> logger.debug("=========   Silo Device: {} ===========\n", siloDevice.getName(),
                        siloDevice)
        );
    }

    public List<SiloDevice> getSiloDevices(Long warehouseId, boolean refresh) {
        // first of all, see if the silo system is enabled in the warehouse
        SiloConfiguration siloConfiguration = siloConfigurationService.findByWarehouseId(warehouseId);
        if (Objects.isNull(siloConfiguration) || !siloConfiguration.isEnabled()) {
            throw WorkOrderException.raiseException("Silo System is not enabled in the current warehouse");
        }

        // see if we will need to refresh and get the latest data from the web API Endpoint,
        // or we get from the cached one, which we fetch the data once every 5 minutes
        if (refresh) {
            syncDeviceStaus(warehouseId);
        }

        // let's get the latest batch of silo, we will get by
        // the device history's web_api_call_timestamp
        List<SiloDeviceAPICallHistory> siloDeviceAPICallHistories =
                siloDeviceAPICallHistoryService.getLatestBatchOfSiloDeviceAPICallHistory(warehouseId);

        // if setup , we will get the silo device's material field from the inventory in the WMS
        // instead of the material value from the remote SILO system

        return siloDeviceAPICallHistories.stream()
                .map(siloDeviceAPICallHistory -> {
                    // setup the item name if not done so
                    if (siloConfiguration.getInventoryInformationFromWMS() && Strings.isBlank(siloDeviceAPICallHistory.getItemName())) {
                        return setupItemName(warehouseId, siloDeviceAPICallHistory);
                    }
                    else {
                        return siloDeviceAPICallHistory;
                    }
                })
                .map(siloDeviceAPICallHistory -> new SiloDevice(siloDeviceAPICallHistory))
                .sorted(Comparator.comparing(SiloDevice::getName)).collect(Collectors.toList());

    }

    private SiloDeviceAPICallHistory setupItemName(Long warehouseId,
                                                   SiloDeviceAPICallHistory siloDeviceAPICallHistory) {
            // we will get item from WMS
            logger.debug("start to get item information from location of warehouse {}, name {}",
                    warehouseId, siloDeviceAPICallHistory.getName());
            Item item = inventoryServiceRestemplateClient.getLastItemFromSiloLocation(
                    warehouseId, siloDeviceAPICallHistory.getName()
            );

            if (Objects.nonNull(item)) {
                logger.debug("We find the latest item {} from location {}",
                        item.getName(), siloDeviceAPICallHistory.getName());
                siloDeviceAPICallHistory.setItemName(item.getName());
                return siloDeviceAPICallHistoryService.save(siloDeviceAPICallHistory);
            }

            return siloDeviceAPICallHistory;
    }


}

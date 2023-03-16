package com.garyzhangscm.cwms.workorder.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.garyzhangscm.cwms.workorder.clients.SiloRestemplateClient;
import com.garyzhangscm.cwms.workorder.exception.WorkOrderException;
import com.garyzhangscm.cwms.workorder.model.*;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class SiloService {
    private static final Logger logger = LoggerFactory.getLogger(SiloService.class);


    @Autowired
    private SiloRestemplateClient siloRestemplateClient;

    private static String lastToken = "";
    private static LocalDateTime lastTokenGeneratedTime;

    @Qualifier("getObjMapper")
    @Autowired
    private ObjectMapper objectMapper;

    public List<SiloDevice> getSiloMonitor(Long warehouseId, String token) {
        // if token is not passed in, login the default user and get the token
        if (Strings.isBlank(token)) {
            token = getToken();
        }

        if (Strings.isBlank(token)) {

            logger.debug("Token is not passed in and we are not able to login with the default user");
            throw WorkOrderException.raiseException("Can't login SILO system");
        }

        SiloDeviceResponseWrapper siloDevicesResponseWrapper =  siloRestemplateClient.getSiloDevices(token);

        refreshToken(siloDevicesResponseWrapper.getToken());

        logger.debug("Get {} silo devices", siloDevicesResponseWrapper.getSiloDevices().size());

        siloDevicesResponseWrapper.getSiloDevices().forEach(
                    siloDevice -> logger.debug("=========   Silo Device： {} ===========\n", siloDevice.getName(),
                            siloDevice)
            );

        return siloDevicesResponseWrapper.getSiloDevices();
    }

    private void refreshToken(String token) {
        lastToken = token;
        lastTokenGeneratedTime = LocalDateTime.now();
    }


    private String getToken() {
        if (Strings.isNotBlank(lastToken) && Objects.nonNull(lastTokenGeneratedTime) &&
                lastTokenGeneratedTime.isAfter(LocalDateTime.now().minusMinutes(5))) {
            // we already get the token and it is generated within last 5 minutes
            logger.debug("get the cached token: {}", lastToken);
            return lastToken;
        }

        String tokenResponse = siloRestemplateClient.loginSilo();
        try {

            if (Strings.isNotBlank(tokenResponse)) {
                SiloLoginResponseWrapper siloLoginResponseWrapper
                        = objectMapper.readValue(tokenResponse, SiloLoginResponseWrapper.class);

                if (Objects.isNull(siloLoginResponseWrapper)) {
                    throw WorkOrderException.raiseException("Can't login SILO system");
                }
                return siloLoginResponseWrapper.getToken();

            }
        }catch (JsonProcessingException e) {
            e.printStackTrace();
            logger.debug("can't pass the sile information");
        }
        throw WorkOrderException.raiseException("Can't login SILO system");

    }
}

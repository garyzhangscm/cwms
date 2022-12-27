package com.garyzhangscm.cwms.outbound.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.garyzhangscm.cwms.outbound.clients.ShipEngineRestemplateClient;
import com.garyzhangscm.cwms.outbound.model.shipengine.*;
import com.garyzhangscm.cwms.outbound.model.shipengine.Package;
import com.shipengine.ShipEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
 *  https://www.shipengine.com/
 * an integration solution for shipping with UPS / USPS / Fedex / etc
 * we will use this to simplify the process to print shipping label
 * and track the package
 * please refer to https://github.com/ShipEngine/shipengine-java
 * for how to use the java SDK
* */
@Service
public class ShipEngineService {

    private static final Logger logger = LoggerFactory.getLogger(ShipEngineService.class);

    @Value("${parcel.shipEngine.apiKey}")
    private String apiKey;

    @Autowired
    private ShipEngineRestemplateClient shipEngineRestemplateClient;

    @Bean
    public ShipEngine shipEngine() {

        return  new ShipEngine(new HashMap<String, Object>() {{
            put("apiKey", apiKey);
            put("pageSize", 75);
            put("retries", 3);
            put("timeout", 8000);
        }});
    }

    public HashMap<String, String> getValidCarriers() {
        return new HashMap<String, String>() {{
            put("se-3750097", "Sandbox-USPS");
            put("se-3750098", "Sandbox-UPS");
            put("se-3750099", "Sandbox-FEDEX");
        }};
    }

    public void getShippingRates() throws JsonProcessingException {
        RateRequest rateRequest = new RateRequest(
                new RateOption(
                        List.of("se-3750097", "se-3750098", "se-3750099")
                ),
                new Shipment(
                        "no_validation",
                        // ship to address
                        new Address(
                                "Brian Wang",
                                "",
                                "555-5555-5555",
                                "14540 Manchester Ave",
                                "Chino",
                                "CA",
                                "91710",
                                "US",
                                "yes"
                        ),
                        // ship from address
                        new Address(
                                "Xiangxiang Wang",
                                "",
                                "666-6666-6666",
                                "14575 Manchester Ave",
                                "Chino",
                                "CA",
                                "91710",
                                "US",
                                "yes"

                        ),
                        List.of(
                                new Package(
                                        new Weight(
                                                1.0,
                                                "ounce"
                                        )
                                )
                        )

                )
        );
        String result = shipEngineRestemplateClient.getRate(rateRequest);

        logger.debug("get result from rate request: \n{}", result);

   }

}

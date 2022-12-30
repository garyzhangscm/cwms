/**
 * Copyright 2019
 *
 * @author gzhang
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.garyzhangscm.cwms.outbound.controller;

import com.easypost.exception.EasyPostException;
import com.easypost.model.Event;
import com.easypost.model.Rate;
import com.easypost.model.Shipment;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.garyzhangscm.cwms.outbound.ResponseBodyWrapper;
import com.garyzhangscm.cwms.outbound.model.Order;
import com.garyzhangscm.cwms.outbound.model.ParcelPackage;
import com.garyzhangscm.cwms.outbound.service.EasyPostService;
import com.garyzhangscm.cwms.outbound.service.ParcelPackageService;
import com.garyzhangscm.cwms.outbound.service.ShipEngineService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@RestController
public class EasyPostController {

    private static final Logger logger = LoggerFactory.getLogger(EasyPostController.class);


    @Qualifier("getObjMapper")
    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EasyPostService easyPostService;


    @RequestMapping(value="/parcel/easy-post/webhook", method = RequestMethod.POST)
    public ResponseBodyWrapper processWebhook(@RequestHeader Map<String, Object> headers,
                                              @RequestBody String webhookEvent) throws JsonProcessingException, EasyPostException {

        logger.debug("start to process easy post webhook");
        logger.debug("get event from easypost webhook \n{}", webhookEvent);


        // Event event = objectMapper.readValue(webhookEvent, Event.class);

        logger.debug("start to validate against header ");
        headers.entrySet().forEach(
                entry -> logger.debug(">> {} : {}" , entry.getKey(), entry.getValue())
        );

        if (headers.containsKey("x-hmac-signature") && !headers.containsKey("X-Hmac-Signature")) {
            // the standard easy post client request a header named X-Hmac-Signature
            // but the header may only contains x-hmac-signature
            headers.put("X-Hmac-Signature", headers.get("x-hmac-signature"));
        }


        logger.debug("header container key X-Hmac-Signature: {}", headers.containsKey("X-Hmac-Signature"));
        logger.debug("header container key x-hmac-signature: {}", headers.containsKey("x-hmac-signature"));

        Event event = easyPostService.validateWebhook(
                webhookEvent.getBytes(StandardCharsets.UTF_8),
                headers
        );

        logger.debug("convert the string to webhook event");
        logger.debug(">> description: {}", event.getDescription());
        logger.debug(">> result: {}", event.getResult());
        logger.debug(">> previousAttributes: {}", event.getPreviousAttributes());
        logger.debug(">> pendingUrls: {}", event.getPendingUrls());
        logger.debug(">> completedUrls: {}", event.getCompletedUrls());


        easyPostService.processWebhookEvent(event);

        return ResponseBodyWrapper.success("webhook processed!");
    }

}

package com.garyzhangscm.cwms.quickbook.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.garyzhangscm.cwms.quickbook.WebhookResponseWrapper;
import com.garyzhangscm.cwms.quickbook.service.SecurityService;
import com.garyzhangscm.cwms.quickbook.service.queue.QueueService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.intuit.ipp.util.StringUtils;

/**
 * Controller class for the webhooks endpoint
 *
 * @author dderose
 *
 */
@RestController
public class WebhooksController {

    private static final Logger logger = LoggerFactory.getLogger(WebhooksController.class);

    private static final String SIGNATURE = "intuit-signature";
    private static final String SUCCESS = "Success";
    private static final String ERROR = "Error";

    @Autowired
    SecurityService securityService;

    @Autowired
    private QueueService queueService;

    /**
     * Method to receive webhooks event notification
     * 1. Validates payload
     * 2. Adds it to a queue
     * 3. Sends success response back
     *
     * Note: Queue processing occurs in an async thread
     *
     * @param signature
     * @param payload
     * @return
     */
    @RequestMapping(value = "/webhooks", method = RequestMethod.POST)
    public ResponseEntity<WebhookResponseWrapper> webhooks(@RequestHeader(SIGNATURE) String signature, @RequestBody String payload) throws JsonProcessingException {

        logger.debug("start to process webhook");
        logger.debug("signature >> {}", signature);
        logger.debug("payload >> {}", payload);
        // if signature is empty return 401
        if (!StringUtils.hasText(signature)) {
            return new ResponseEntity<>(new WebhookResponseWrapper(ERROR), HttpStatus.FORBIDDEN);
        }

        // if payload is empty, don't do anything
        if (!StringUtils.hasText(payload)) {
            new ResponseEntity<>(new WebhookResponseWrapper(SUCCESS), HttpStatus.OK);
        }

        logger.debug("start to verify the web hook request");

        //if request valid - push to queue
        if (securityService.isRequestValid(signature, payload)) {
            logger.debug("webhook request passed validation, push payload to queue");
            // add the payload to the queue
            // QueueProcessor will dequeue the message and process
            queueService.add(payload);
        } else {
            return new ResponseEntity<>(new WebhookResponseWrapper(ERROR), HttpStatus.FORBIDDEN);
        }

        logger.info("response sent ");
        return new ResponseEntity<>(new WebhookResponseWrapper(SUCCESS), HttpStatus.OK);
    }

}
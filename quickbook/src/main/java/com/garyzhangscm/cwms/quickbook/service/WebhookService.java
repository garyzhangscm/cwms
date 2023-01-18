package com.garyzhangscm.cwms.quickbook.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.garyzhangscm.cwms.quickbook.model.WebhookStatus;
import com.garyzhangscm.cwms.quickbook.service.queue.QueueService;
import com.intuit.ipp.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class WebhookService {


	@Autowired
	SecurityService securityService;
	@Autowired
	private QueueService queueService;
	@Autowired
	private QuickBookWebhookHistoryService quickBookWebhookHistoryService;

	private static final Logger logger = LoggerFactory.getLogger(WebhookService.class);

	public boolean processWebhook(String signature, String payload) throws JsonProcessingException {
		// if signature is empty return false
		if (!StringUtils.hasText(signature)) {
			return false;
		}

		// if payload is empty, don't do anything
		if (!StringUtils.hasText(payload)) {
			return true;
		}

		logger.debug("start to verify the web hook request");

		//if request valid - push to queue
		if (securityService.isRequestValid(signature, payload)) {
			logger.debug("webhook request passed validation, push payload to queue");
			// add the payload to the queue
			// QueueProcessor will dequeue the message and process
			// we will no long add the payload to the queue.
			// instead we will just save the payload and relay on the
			// scheduled job to process the payload
			// queueService.add(payload);
			quickBookWebhookHistoryService.addNewWebhookRequest(signature, payload);

		} else {
			quickBookWebhookHistoryService.addNewWebhookRequest(signature, payload,
					WebhookStatus.ERROR, "Payload is not valid according to the signature");
			return false;
		}

		logger.info("response sent ");
		return true;
	}
}

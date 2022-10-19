package com.garyzhangscm.cwms.quickbook.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.garyzhangscm.cwms.quickbook.WebhookResponseWrapper;
import com.garyzhangscm.cwms.quickbook.controller.WebhooksController;
import com.garyzhangscm.cwms.quickbook.exception.MissingInformationException;
import com.garyzhangscm.cwms.quickbook.exception.OAuthFailException;
import com.garyzhangscm.cwms.quickbook.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.quickbook.model.QuickBookOnlineConfiguration;
import com.garyzhangscm.cwms.quickbook.model.QuickBookOnlineToken;
import com.garyzhangscm.cwms.quickbook.model.QuickBookWebhookHistory;
import com.garyzhangscm.cwms.quickbook.model.WebhookStatus;
import com.garyzhangscm.cwms.quickbook.repository.QuickBookOnlineTokenRepository;
import com.garyzhangscm.cwms.quickbook.service.queue.QueueService;
import com.intuit.ipp.util.StringUtils;
import com.intuit.oauth2.client.OAuth2PlatformClient;
import com.intuit.oauth2.config.Environment;
import com.intuit.oauth2.config.OAuth2Config;
import com.intuit.oauth2.data.BearerTokenResponse;
import com.intuit.oauth2.exception.OAuthException;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

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

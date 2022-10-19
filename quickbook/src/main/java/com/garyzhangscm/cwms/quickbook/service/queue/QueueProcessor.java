package com.garyzhangscm.cwms.quickbook.service.queue;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import com.garyzhangscm.cwms.quickbook.model.QuickBookOnlineToken;
import com.garyzhangscm.cwms.quickbook.model.QuickBookWebhookHistory;
import com.garyzhangscm.cwms.quickbook.model.WebhookStatus;
import com.garyzhangscm.cwms.quickbook.service.QuickBookOnlineTokenService;
import com.garyzhangscm.cwms.quickbook.service.QuickBookWebhookHistoryService;
import com.garyzhangscm.cwms.quickbook.service.qbo.QBODataService;
import com.garyzhangscm.cwms.quickbook.service.qbo.WebhooksServiceFactory;
import com.intuit.ipp.data.Entity;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.intuit.ipp.data.EventNotification;
import com.intuit.ipp.data.WebhooksEvent;
import com.intuit.ipp.services.WebhooksService;
import com.intuit.ipp.util.DateUtils;


/**
 * Callable task to process the queue
 * 1. Retrieves the payload from the queue
 * 2. Converts json to object
 * 3. Queries CompanyConfig table to get the last CDC performed time for the realmId
 * 4. Performs CDC for all the subscribed entities using the lastCDCTime retrieved in step 3
 * 5. Updates the CompanyConfig table with the last CDC performed time for the realmId - time when step 4 was performed
 * 
 * @author dderose
 *
 */
@Service
public class QueueProcessor implements Callable<Object> {

	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(QueueService.class);
	@Autowired
	@Qualifier("CdcAPI")
	private QBODataService cdcService;
	
	@Autowired
	private QueueService queueService;
	
	@Autowired
	private QuickBookOnlineTokenService quickBookOnlineTokenService;
	@Autowired
	private QuickBookWebhookHistoryService quickBookWebhookHistoryService;
	
	@Autowired
	WebhooksServiceFactory webhooksServiceFactory;
	
	public static final String DATE_yyyyMMddTHHmmssSSSZ = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
	
	@Override
	public Object call() throws Exception { 

		logger.debug("start to process webhook request queue");
		while (!queueService.getQueue().isEmpty()) {
			//remove item from queue
			String payload = queueService.getQueue().poll();
			logger.info("processing payload: Queue Size:" + queueService.getQueue().size());
			payload = payload.trim().replace("\n", "").replace("\r", "");
			logger.info(" payload: \n {}",  payload);
			
			// create webhooks service
			WebhooksService service = webhooksServiceFactory.getWebhooksService();


			// get the webhook history so we will update it later on
			QuickBookWebhookHistory quickBookWebhookHistory =
					quickBookWebhookHistoryService.findPendingWebhookRequest(payload);
			// we will get the realmId from the notification and then get
			// the warehouse id and company Id from the realmid. We will then save
			// the 3 ids along with the webhook
			logger.debug("start to process quickbook history with payload \n {}", payload);
			if (Objects.isNull(quickBookWebhookHistory)) {

				logger.debug("can't find any webhook hsitory information with payload \n {}", payload);
			}
			else {

				logger.debug("quickBookWebhookHistory / id: {}\n",
						quickBookWebhookHistory.getId());
				logger.debug("quickBookWebhookHistory / payload: {}\n",
						quickBookWebhookHistory.getPayload());
				logger.debug("quickBookWebhookHistory / signature: {}\n",
						quickBookWebhookHistory.getSignature());
			}
			Long warehouseId = null;
			Long companyId = null;
			String realmId = "";
			//Convert payload to obj
			WebhooksEvent event = service.getWebhooksEvent(payload);
			try {

				for (EventNotification eventNotification : event.getEventNotifications()) {

					// get the company config
					List<QuickBookOnlineToken> quickBookOnlineTokens
							= quickBookOnlineTokenService.getByRealmId(eventNotification.getRealmId());
					// loop through each  token and process the message
					for (QuickBookOnlineToken quickBookOnlineToken : quickBookOnlineTokens) {

						List<Entity> entities =
								eventNotification.getDataChangeEvent().getEntities().stream().filter(
										entity -> cdcService.isRegistered(entity.getName())
								).collect(Collectors.toList());
						for (Entity changedEntity : entities) {

							// perform cdc with last updated timestamp and subscribed entities
							String cdcTimestamp = DateUtils.getStringFromDateTime(DateUtils.getCurrentDateTime());

							//We will call by entity so that we can save history information for each entity
							// cdcService.callDataService(eventNotification, quickBookOnlineToken);
							cdcService.callDataService(changedEntity, quickBookOnlineToken);

							// update cdcTimestamp in companyconfig
							quickBookOnlineToken.setLastCDCCallTime(cdcTimestamp);
							quickBookOnlineTokenService.save(quickBookOnlineToken);

							// create one webhook history for each reaml and
							// entity combination
							quickBookWebhookHistoryService.save(
									new QuickBookWebhookHistory(
											quickBookOnlineToken.getCompanyId(),
											quickBookOnlineToken.getWarehouseId(),
											quickBookOnlineToken.getRealmId(),
											quickBookWebhookHistory.getSignature(),
											quickBookWebhookHistory.getPayload(),  // copy the signature and payload from the original history
											changedEntity.getName(),
											WebhookStatus.COMPLETE,
											"",
											LocalDateTime.now()
									)
							);

						}

					}

				}

				// update the original webhook to complete
				quickBookWebhookHistory.setStatus(WebhookStatus.COMPLETE);
				quickBookWebhookHistory.setErrorMessage("");
				quickBookWebhookHistory.setProcessedTime(LocalDateTime.now());

				quickBookWebhookHistoryService.save(quickBookWebhookHistory);

			}
			catch (Exception ex) {
				ex.printStackTrace();
				quickBookWebhookHistory.setCompanyId(companyId);
				quickBookWebhookHistory.setWarehouseId(warehouseId);
				quickBookWebhookHistory.setRealmId(realmId);
				quickBookWebhookHistory.setStatus(WebhookStatus.ERROR);
				quickBookWebhookHistory.setErrorMessage(ex.getMessage());
				quickBookWebhookHistory.setProcessedTime(LocalDateTime.now());

				quickBookWebhookHistoryService.save(quickBookWebhookHistory);
			}

		}
		
		return null;
	}


}

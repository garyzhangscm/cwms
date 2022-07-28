package com.garyzhangscm.cwms.quickbook.service.queue;

import java.util.concurrent.Callable;

import com.garyzhangscm.cwms.quickbook.model.QuickBookOnlineToken;
import com.garyzhangscm.cwms.quickbook.service.QuickBookOnlineTokenService;
import com.garyzhangscm.cwms.quickbook.service.qbo.QBODataService;
import com.garyzhangscm.cwms.quickbook.service.qbo.WebhooksServiceFactory;
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
	WebhooksServiceFactory webhooksServiceFactory;
	
	public static final String DATE_yyyyMMddTHHmmssSSSZ = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
	
	@Override
	public Object call() throws Exception { 

		logger.debug("start to process webhook request queue");
		while (!queueService.getQueue().isEmpty()) {
			//remove item from queue
			String payload = queueService.getQueue().poll();
			logger.info("processing payload: Queue Size:" + queueService.getQueue().size());
			logger.info(" payload: \n {}",  payload);
			
			// create webhooks service
			WebhooksService service = webhooksServiceFactory.getWebhooksService();
			
			//Convert payload to obj
			WebhooksEvent event = service.getWebhooksEvent(payload);
			for (EventNotification eventNotification : event.getEventNotifications()) {

				// get the company config
				QuickBookOnlineToken quickBookOnlineToken
						= quickBookOnlineTokenService.getByRealmId(eventNotification.getRealmId());

				// perform cdc with last updated timestamp and subscribed entities
				String cdcTimestamp = DateUtils.getStringFromDateTime(DateUtils.getCurrentDateTime());
				cdcService.callDataService(eventNotification, quickBookOnlineToken);

				// update cdcTimestamp in companyconfig
				quickBookOnlineToken.setLastCDCCallTime(cdcTimestamp);
				quickBookOnlineTokenService.save(quickBookOnlineToken);
			}

		}
		
		return null;
	}


}

package com.garyzhangscm.cwms.quickbook.service.qbo;

import java.util.Arrays;
import java.util.List;

import com.garyzhangscm.cwms.quickbook.model.QuickBookOnlineToken;
import com.intuit.ipp.data.EventNotification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.intuit.ipp.services.DataService;
import com.intuit.ipp.util.Logger;

/**
 * Class for implementing QBO Query api 
 * 
 * @author dderose
 *
 */
@Service(value="QueryAPI")
public class QueryService implements QBODataService {
	
	private static final org.slf4j.Logger LOG = Logger.getLogger();
	
	@Autowired
    DataServiceFactory dataServiceFactory;


	private static final String WEBHOOKS_SUBSCRIBED_ENTITES = "Invoice,Customer,Vendor,Item,PurchaseOrder";

	@Override
	public void callDataService(EventNotification eventNotification, QuickBookOnlineToken quickBookOnlineToken) throws Exception {
		
		// create data service
		DataService service = dataServiceFactory.getDataService(quickBookOnlineToken);
			
		try {
			LOG.info("Calling Query API ");
			String query = "select * from ";
			//Build query list for each subscribed entities
			List<String> subscribedEntities = Arrays.asList(getWebhooksSubscribedEntites().split(","));
			subscribedEntities.forEach(entity -> executeQuery(query + entity, service)) ;
			
		} catch (Exception ex) {
			LOG.error("Error loading app configs" , ex.getCause());
		}
		
	}
	
	/**
	 * Call executeQuery api for each entity
	 * 
	 * @param query
	 * @param service
	 */
	public void executeQuery(String query, DataService service) {
		try {
			LOG.info("Executing Query " + query);
			service.executeQuery(query);
			LOG.info(" Query complete" );
		} catch (Exception ex) {
			LOG.error("Error loading app configs" , ex.getCause());
		}
	}

	private String getWebhooksSubscribedEntites() {
		return WEBHOOKS_SUBSCRIBED_ENTITES;
	}
}

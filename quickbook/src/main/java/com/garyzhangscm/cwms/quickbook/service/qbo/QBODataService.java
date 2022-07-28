package com.garyzhangscm.cwms.quickbook.service.qbo;


import com.garyzhangscm.cwms.quickbook.model.QuickBookOnlineToken;
import com.intuit.ipp.data.EventNotification;

/**
 * Interface holding methods to call QBP Dataservice api
 * 
 * @author dderose
 *
 */
public interface QBODataService {
	
	public void callDataService(EventNotification eventNotification, QuickBookOnlineToken quickBookOnlineToken) throws Exception;

}

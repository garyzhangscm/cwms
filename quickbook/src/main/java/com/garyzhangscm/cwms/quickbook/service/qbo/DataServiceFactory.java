package com.garyzhangscm.cwms.quickbook.service.qbo;

import com.garyzhangscm.cwms.quickbook.controller.QuickBookOnlineTokenController;
import com.garyzhangscm.cwms.quickbook.exception.SystemFatalException;
import com.garyzhangscm.cwms.quickbook.model.AppConfig;
import com.garyzhangscm.cwms.quickbook.model.QuickBookOnlineToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.intuit.ipp.core.Context;
import com.intuit.ipp.core.ServiceType;
import com.intuit.ipp.exception.FMSException;
import com.intuit.ipp.security.IAuthorizer;
import com.intuit.ipp.security.OAuth2Authorizer;
import com.intuit.ipp.services.DataService;
import com.intuit.ipp.util.Config;

/**
 * 
 * @author dderose
 *
 */
@Service
public class DataServiceFactory {

	private static final Logger logger = LoggerFactory.getLogger(QuickBookOnlineTokenController.class);

	@Autowired
	AppConfig appConfig;
	
	/**
	 * Initializes DataService for a given app/company profile
	 *
	 * @return
	 * @throws FMSException
	 */
	public DataService getDataService(QuickBookOnlineToken quickBookOnlineToken) throws FMSException {
		logger.debug("start to get data service");
		//set custom config, this should be commented for prod
		logger.debug("QBO url: {}", appConfig.getQboUrl());
		Config.setProperty(Config.BASE_URL_QBO, appConfig.getQboUrl());
		
		//create oauth object based on OAuth type
		IAuthorizer oauth; 
		if(appConfig.getOAuthType().equals("1")) {
			// OAuth 1 is not support
			throw SystemFatalException.raiseException("OAuth 1 is not support to connrect to quick book online");
			// oauth = new OAuthAuthorizer(appConfig.getConsumerKey(), appConfig.getConsumerSecret(), companyConfig.getAccessToken(), companyConfig.getAccessTokenSecret());
		} else {
			oauth = new OAuth2Authorizer(quickBookOnlineToken.getToken());
		}
		//create context
		Context context = new Context(oauth, appConfig.getAppToken(), ServiceType.QBO, quickBookOnlineToken.getRealmId());
		
		//create dataservice
		return new DataService(context);
	}

}

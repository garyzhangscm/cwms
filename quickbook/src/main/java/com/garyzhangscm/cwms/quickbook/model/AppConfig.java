package com.garyzhangscm.cwms.quickbook.model;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;

/**
 * Entity to store app configs
 * 
 * @author dderose
 *
 */
@Configuration
public class AppConfig {
	
	@Autowired
    Environment env;

	public String getAppToken() {
		// return env.getProperty("app.token");
		// Oauth2 doesn't need the app token
		return "";
	}

	public String getConsumerKey() {
		//return env.getProperty("consumer.key");
		// Oauth2 doesn't need the consumer key
		return "";
	}

	public String getConsumerSecret() {
		// return env.getProperty("consumer.secret");
		// Oauth2 doesn't need the consumer secret
		return "";
	}

	public String getQboUrl() {

		// return env.getProperty("quickbook.qbo.url");
		// return "https://sandbox-quickbooks.api.intuit.com/v3/company";
		return "https://quickbooks.api.intuit.com/v3/company";
	}

	//Flag to determine if app is OAuth1 or 2
	public String getOAuthType() {
		// return env.getProperty("oauth.type");
		// we only support OAuth2 for now
		return "2";
	}
	
}

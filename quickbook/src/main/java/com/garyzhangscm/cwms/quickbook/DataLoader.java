package com.garyzhangscm.cwms.quickbook;

import com.garyzhangscm.cwms.quickbook.service.qbo.QBODataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import com.intuit.ipp.util.Logger;

/**
 * @author dderose
 *
 */
@Component
@Configuration
@PropertySource(value="classpath:/application.properties", ignoreResourceNotFound=true)
public class DataLoader implements ApplicationListener<ContextRefreshedEvent> {
	
	private static final org.slf4j.Logger LOG = Logger.getLogger();
	
	@Autowired
    private Environment env;

	@Autowired
	@Qualifier("QueryAPI")
	private QBODataService queryService;

	@Override
	public void onApplicationEvent(ContextRefreshedEvent event) {
		
		// Load CompanyConfig table with realmIds and access tokens
		// loadCompanyConfig();
		
		//get list of companyConfigs
		/**
		Iterable<CompanyConfig> companyConfigs = companyConfigService.getAllCompanyConfigs();
		
		//run findQuery for all entities for each realmId
		// and update the timestamp in the database
		// This is done so that the app is synced before it listens to event notifications
		for (CompanyConfig config : companyConfigs) {
			try {
				String lastQueryTimestamp = DateUtils.getStringFromDateTime(DateUtils.getCurrentDateTime());
				queryService.callDataService(config);
				
				//update timestamp data in table
				config.setLastCdcTimestamp(lastQueryTimestamp);
				// companyConfigService.save(config);
				
			} catch (Exception ex) {
				LOG.error("Error loading company configs" , ex.getCause());
			}
		}
		 */
			
	}


}

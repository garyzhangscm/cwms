package com.garyzhangscm.cwms.quickbook.service.qbo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.garyzhangscm.cwms.quickbook.controller.QuickBookOnlineTokenController;
import com.garyzhangscm.cwms.quickbook.model.*;
import com.intuit.ipp.data.EventNotification;
import com.intuit.ipp.exception.FMSException;
import com.intuit.ipp.services.CDCQueryResult;
import com.intuit.ipp.services.QueryResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.intuit.ipp.core.IEntity;
import com.intuit.ipp.services.DataService;

/**
 * Class for implementing the QBO CDC api
 * 
 * @author dderose
 *
 */
@Service(value="CdcAPI")
public class CDCService implements QBODataService {

	private static final Logger logger = LoggerFactory.getLogger(QuickBookOnlineTokenController.class);
	
	@Autowired
    DataServiceFactory dataServiceFactory;
	@Autowired
	@Qualifier("getObjMapper")
	private ObjectMapper objectMapper;

	private static final String WEBHOOKS_SUBSCRIBED_ENTITES = "Invoice,Customer,Vendor,Item,PurchaseOrder";

	@Override
	public void callDataService(EventNotification eventNotification, QuickBookOnlineToken quickBookOnlineToken) throws Exception {

		logger.info("Calling CDC from CDCService");
		// create data service
		DataService service = dataServiceFactory.getDataService(quickBookOnlineToken);
			
		try {
			eventNotification.getDataChangeEvent().getEntities().stream().filter(
					entity -> isRegistered(entity.getName())
			).forEach(
					changedEntity -> {


						String intuitQuery = "SELECT * FROM " + changedEntity.getName() + " where id = '"  + changedEntity.getId() + "'";
						logger.debug("start to get the entity of type " + changedEntity.getName()
								+ " by query \n {}", intuitQuery);

						try {
							QueryResult queryResult = service.executeQuery(intuitQuery);

							processCDCQueryResults(EntityChangeOperation.valueOf(changedEntity.getOperation()),
									changedEntity.getName(), queryResult);
						} catch (FMSException e) {
							e.printStackTrace();
						}
					}
			);
/*

			// build entity list for cdc based on entities subscribed for webhooks
			List<String> subscribedEntities = Arrays.asList(getWebhooksSubscribedEntites().split(","));
			List<IEntity> entities = new ArrayList<>();
			for (String subscribedEntity : subscribedEntities) {
				Class<?> className = Class.forName("com.intuit.ipp.data." + subscribedEntity);
				IEntity entity = (IEntity) className.newInstance();
				entities.add(entity);
			}

			List<CDCQueryResult> cdcQueryResults = service.executeCDCQuery(entities, quickBookOnlineToken.getLastCDCCallTime());
 */
			
		} catch (Exception ex) {
			ex.printStackTrace();
			logger.error("Error while calling CDC" , ex.getCause());
		}
		
	}

	private void processCDCQueryResults(List<CDCQueryResult> cdcQueryResults) {
		cdcQueryResults.forEach(
				cdcQueryResult -> {
					processCDCQueryResults(cdcQueryResult);
				}
		);
	}

	private void processCDCQueryResults(CDCQueryResult cdcQueryResult) {
		cdcQueryResult.getQueryResults().forEach(
				(type, queryResult) -> {
					switch (type) {
						case "Invoice":
							processCDCQueryInvoiceResult(queryResult);
							break;

						case "Customer":
							processCDCQueryCustomerResult(queryResult);
							break;

					}
				}
		);
	}

	private void processCDCQueryResults(EntityChangeOperation operation, String type, QueryResult queryResult)   {
		logger.debug("start to porecess query result with type {}, operation {}", type,
				operation);
		try {
			logger.debug("content: \n {}",
					new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(queryResult));
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
		switch (type) {
			case "Invoice":
				processCDCQueryInvoiceResult(queryResult);
				break;

			case "Customer":
				processCDCQueryCustomerResult(queryResult);
				break;

			case "Item":
				processCDCQueryItemResult(queryResult);
				break;
			case "PurchaseOrder":
				processCDCQueryPurchaseOrderResult(queryResult);
				break;
		}
	}

	private void processCDCQueryPurchaseOrderResult(QueryResult queryResult) {

		queryResult.getEntities().forEach(
				iEntity -> {

					try {
						PurchaseOrder purchaseOrder
								= objectMapper.readValue(
								new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(iEntity)
								, PurchaseOrder.class);

						logger.debug("start to process purchase order \n{}", purchaseOrder);
					} catch (JsonProcessingException e) {
						e.printStackTrace();
					}

				}

		);
	}
	private void processCDCQueryItemResult(QueryResult queryResult) {

		queryResult.getEntities().forEach(
				iEntity -> {

					try {
						Item item
								= objectMapper.readValue(
								new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(iEntity)
								, Item.class);

						logger.debug("start to process item \n{}", item);
					} catch (JsonProcessingException e) {
						e.printStackTrace();
					}

				}

		);
	}

	private void processCDCQueryCustomerResult(QueryResult queryResult) {

		queryResult.getEntities().forEach(
				iEntity -> {

					try {
						Customer customer
								= objectMapper.readValue(
								new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(iEntity)
								, Customer.class);

						logger.debug("start to process customer \n{}", customer);
					} catch (JsonProcessingException e) {
						e.printStackTrace();
					}

				}

		);
	}

	private void processCDCQueryInvoiceResult(QueryResult queryResult) {
		queryResult.getEntities().forEach(
				iEntity -> {

					try {
						Invoice invoice
								= objectMapper.readValue(
								    new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(iEntity)
								    , Invoice.class);

						logger.debug("start to process invoice \n {}", invoice);
					} catch (JsonProcessingException e) {
						e.printStackTrace();
					}

				}

		);
	}

	private String getWebhooksSubscribedEntites() {
		return WEBHOOKS_SUBSCRIBED_ENTITES;
	}

	private boolean isRegistered(String name) {
		return Arrays.stream(WEBHOOKS_SUBSCRIBED_ENTITES.split(",")).anyMatch(
				entity -> entity.equalsIgnoreCase(name)
		);
	}
}

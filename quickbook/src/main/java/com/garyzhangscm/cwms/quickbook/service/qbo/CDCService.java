package com.garyzhangscm.cwms.quickbook.service.qbo;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.garyzhangscm.cwms.quickbook.clients.IntegrationServiceRestemplateClient;
import com.garyzhangscm.cwms.quickbook.controller.QuickBookOnlineTokenController;
import com.garyzhangscm.cwms.quickbook.model.*;
import com.garyzhangscm.cwms.quickbook.service.*;
import com.intuit.ipp.data.Entity;
import com.intuit.ipp.data.EventNotification;
import com.intuit.ipp.exception.FMSException;
import com.intuit.ipp.services.CDCQueryResult;
import com.intuit.ipp.services.QueryResult;
import org.apache.logging.log4j.util.Strings;
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

	private static final Logger logger = LoggerFactory.getLogger(CDCService.class);
	
	@Autowired
    DataServiceFactory dataServiceFactory;
	@Autowired
	@Qualifier("getObjMapper")
	private ObjectMapper objectMapper;

	@Autowired
	private ItemIntegrationService itemIntegrationService;
	@Autowired
	private QuickBookOnlineTokenService quickBookOnlineTokenService;

	@Autowired
	private VendorIntegrationService vendorIntegrationService;
	@Autowired
	private PurchaseOrderIntegrationService purchaseOrderIntegrationService;
	@Autowired
	private CustomerIntegrationService customerIntegrationService;
	@Autowired
	private OutboundOrderIntegrationService outboundOrderIntegrationService;

	private static final String WEBHOOKS_SUBSCRIBED_ENTITES = "Invoice,Customer,Vendor,Item,PurchaseOrder";

	@Override
	public void callDataService(Entity entity, QuickBookOnlineToken quickBookOnlineToken) throws Exception {

		DataService service = dataServiceFactory.getDataService(quickBookOnlineToken);

		logger.info("Calling CDC from CDCService for entity : {}", entity.getName());

		String intuitQuery = "SELECT * FROM " + entity.getName() + " where id = '"  + entity.getId() + "'";
		logger.debug("start to get the entity of type " + entity.getName()
				+ " by query \n {}", intuitQuery);

		QueryResult queryResult = service.executeQuery(intuitQuery);

		processCDCQueryResults(EntityChangeOperation.valueOf(entity.getOperation()),
				entity.getName(),
				queryResult, quickBookOnlineToken.getCompanyId(),
				quickBookOnlineToken.getWarehouseId());
	}
	@Override
	public void callDataService(EventNotification eventNotification, QuickBookOnlineToken quickBookOnlineToken) throws Exception {

		logger.info("Calling CDC from CDCService");


		List<Entity> entities =
					eventNotification.getDataChangeEvent().getEntities().stream().filter(
							entity -> isRegistered(entity.getName())
					).collect(Collectors.toList());
		for (Entity changedEntity : entities) {
				callDataService(changedEntity, quickBookOnlineToken);
		}
		
	}

	/**
	 * Sync full set of certain entity from quickbook into wms
	 * only suggest for go live use, not for daily use
	 * @param warehouseId
	 * @param entityName
	 */
	public int syncEntity(Long warehouseId, String entityName, int syncTransactionDays) {

		// see how many entity we processed
		int affectEntityNumber = 0;
		// get the company config
		QuickBookOnlineToken quickBookOnlineToken
				= quickBookOnlineTokenService.getByWarehouseId(warehouseId);
		try {
			DataService service = dataServiceFactory.getDataService(quickBookOnlineToken);

			String intuitQuery = getSyncEntityQuery(entityName, syncTransactionDays);
			logger.debug("start to get the entity of type " + entityName +
					" by query \n {}, syncTransactionDays: {}", intuitQuery,
					syncTransactionDays);

			QueryResult queryResult = service.executeQuery(intuitQuery);
			affectEntityNumber = queryResult.getEntities().size();

			processCDCQueryResults(EntityChangeOperation.Create,
					entityName,
					queryResult, quickBookOnlineToken.getCompanyId(),
					quickBookOnlineToken.getWarehouseId());

		} catch (Exception ex) {
			ex.printStackTrace();
			logger.error("Error while calling CDC to sync entity " + entityName , ex.getCause());
		}
		logger.debug("We get {} number of entity {} from quickbook ", entityName);
		return affectEntityNumber;
	}
	private String getSyncEntityQuery(String entityName, int syncTransactionDays) {

		String intuitQuery = "SELECT * FROM " + entityName;

		// if this is a transactional data, we will only download the data
		// if it is changed in the past certain days
		if (entityName.equalsIgnoreCase("Invoice") ||
				entityName.equalsIgnoreCase("PurchaseOrder")) {

			intuitQuery += " where MetaData.CreateTime >= '" + LocalDateTime.now().minusDays(syncTransactionDays) + "'";
		}

		return intuitQuery;
	}

	private void processCDCQueryResults(EntityChangeOperation operation, String type, QueryResult queryResult,
										Long companyId, Long warehouseId)   {
		logger.debug("start to porecess query result with type {}, operation {}, companyId: {}, warehouseId: {}",
				type,
				operation,
				companyId,
				warehouseId);
		try {
			logger.debug("content: \n {}",
					new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(queryResult));
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
		switch (type) {
			case "Invoice":
				processCDCQueryInvoiceResult(queryResult, companyId, warehouseId);
				break;

			case "Customer":
				processCDCQueryCustomerResult(queryResult, companyId, warehouseId);
				break;

			case "Item":
				processCDCQueryItemResult(queryResult, companyId, warehouseId);
				break;
			case "PurchaseOrder":
				processCDCQueryPurchaseOrderResult(queryResult, companyId, warehouseId);
				break;
			case "Vendor":
				processCDCQueryVendorResult(queryResult, companyId, warehouseId);
				break;
		}
	}

	private void processCDCQueryVendorResult(QueryResult queryResult,
													Long companyId, Long warehouseId) {

		queryResult.getEntities().forEach(
				iEntity -> {

					try {
						Vendor vendor
								= objectMapper.readValue(
								new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(iEntity)
								, Vendor.class);

						logger.debug("start to process vendor \n{}", vendor);
						vendorIntegrationService.sendIntegrationData(vendor, companyId, warehouseId);
					} catch (JsonProcessingException e) {
						e.printStackTrace();
					}

				}

		);
	}

	private void processCDCQueryPurchaseOrderResult(QueryResult queryResult,
													Long companyId, Long warehouseId) {

		queryResult.getEntities().forEach(
				iEntity -> {

					try {
						PurchaseOrder purchaseOrder
								= objectMapper.readValue(
								new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(iEntity)
								, PurchaseOrder.class);

						logger.debug("start to process purchase order \n{}", purchaseOrder);
						purchaseOrderIntegrationService.sendIntegrationData(
								purchaseOrder, companyId, warehouseId
						);
					} catch (JsonProcessingException e) {
						e.printStackTrace();
					}

				}

		);
	}
	private void processCDCQueryItemResult(QueryResult queryResult,
										   Long companyId, Long warehouseId) {

		queryResult.getEntities().forEach(
				iEntity -> {

					try {
						Item item
								= objectMapper.readValue(
								new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(iEntity)
								, Item.class);

						// setup the missing field
						logger.debug("start to process item \n{}", item);
						itemIntegrationService.sendIntegrationData(item, companyId, warehouseId);
					} catch (JsonProcessingException e) {
						e.printStackTrace();
					}

				}

		);
	}

	private void processCDCQueryCustomerResult(QueryResult queryResult,
											   Long companyId, Long warehouseId) {

		queryResult.getEntities().forEach(
				iEntity -> {

					try {
						Customer customer
								= objectMapper.readValue(
								new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(iEntity)
								, Customer.class);

						logger.debug("start to process customer \n{}", customer);
						customerIntegrationService.sendIntegrationData(customer,
								companyId, warehouseId);
					} catch (JsonProcessingException e) {
						e.printStackTrace();
					}

				}

		);
	}

	private void processCDCQueryInvoiceResult(QueryResult queryResult,
											  Long companyId, Long warehouseId) {
		queryResult.getEntities().forEach(
				iEntity -> {

					try {
						Invoice invoice
								= objectMapper.readValue(
								    new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(iEntity)
								    , Invoice.class);

						logger.debug("start to process invoice \n {}", invoice);
						outboundOrderIntegrationService.sendIntegrationData(invoice,
								companyId, warehouseId);
					} catch (JsonProcessingException e) {
						e.printStackTrace();
					}

				}

		);
	}

	private String getWebhooksSubscribedEntites() {
		return WEBHOOKS_SUBSCRIBED_ENTITES;
	}

	@Override
	public boolean isRegistered(String name) {
		return Arrays.stream(WEBHOOKS_SUBSCRIBED_ENTITES.split(",")).anyMatch(
				entity -> entity.equalsIgnoreCase(name)
		);
	}
}

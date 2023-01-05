package com.garyzhangscm.cwms.outbound.service;

import com.easypost.exception.EasyPostException;
import com.easypost.exception.General.MissingParameterError;
import com.easypost.exception.General.SignatureVerificationError;
import com.easypost.http.Constant;
import com.easypost.model.Event;
import com.easypost.model.Rate;
import com.easypost.model.Shipment;
import com.easypost.service.EasyPostClient;
import com.easypost.utils.Cryptography;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.garyzhangscm.cwms.outbound.clients.WarehouseLayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.outbound.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.outbound.exception.ShippingException;
import com.garyzhangscm.cwms.outbound.model.EasyPostConfiguration;
import com.garyzhangscm.cwms.outbound.model.Order;
import com.garyzhangscm.cwms.outbound.model.Warehouse;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/*
 * https://www.easypost.com/
 *
 * an integration solution for shipping with UPS / USPS / Fedex / etc
 * we will use this to simplify the process to print shipping label
 * and track the package
 * please refer to https://github.com/EasyPost/easypost-java
 * for how to use the java SDK
* */
@Service
public class EasyPostService {

    private static final Logger logger = LoggerFactory.getLogger(EasyPostService.class);

    // @Value("${parcel.easyPost.apiKey}")
    // private String apiKey;

    // @Value("${parcel.easyPost.webhookSecret}")
    // private String webhookSecret;


    @Autowired
    private OrderService orderService;
    @Autowired
    private ParcelPackageService parcelPackageService;
    @Autowired
    private WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient;

    @Autowired
    private EasyPostConfigurationService easyPostConfigurationService;


    public EasyPostClient easyPostClient(String apiKey) throws MissingParameterError {
        return new EasyPostClient(apiKey);

    }

    private String getAPIKey(Long warehouseId) {
        EasyPostConfiguration easyPostConfiguration =
                easyPostConfigurationService.findByWarehouseId(warehouseId, false);

        if (Objects.isNull(easyPostConfiguration) || Strings.isBlank(easyPostConfiguration.getApiKey())) {
            throw ResourceNotFoundException.raiseException("please setup the API key for easy post first");
        }
        return easyPostConfiguration.getApiKey();

    }
    private String getWebhookSecret(Long warehouseId) {
        EasyPostConfiguration easyPostConfiguration =
                easyPostConfigurationService.findByWarehouseId(warehouseId, false);

        if (Objects.isNull(easyPostConfiguration) || Strings.isBlank(easyPostConfiguration.getWebhookSecret())) {
            throw ResourceNotFoundException.raiseException("please setup the webhook secret for easy post first");
        }
        return easyPostConfiguration.getWebhookSecret();

    }


    public void getShippingRates(Long warehouseId) throws JsonProcessingException, EasyPostException {

        Map<String, Object> fromAddressMap = new HashMap<String, Object>();
        fromAddressMap.put("company", "EasyPost");
        fromAddressMap.put("street1", "417 MONTGOMERY ST");
        fromAddressMap.put("street2", "FLOOR 5");
        fromAddressMap.put("city", "SAN FRANCISCO");
        fromAddressMap.put("state", "CA");
        fromAddressMap.put("country", "US");
        fromAddressMap.put("zip", "94104");
        fromAddressMap.put("phone", "415-123-4567");

        Map<String, Object> toAddressMap = new HashMap<String, Object>();
        toAddressMap.put("name", "Dr. Steve Brule");
        toAddressMap.put("street1", "179 N Harbor Dr");
        toAddressMap.put("city", "Redondo Beach");
        toAddressMap.put("state", "CA");
        toAddressMap.put("country", "US");
        toAddressMap.put("zip", "90277");
        toAddressMap.put("phone", "310-808-5243");

        Map<String, Object> parcelMap = new HashMap<String, Object>();
        parcelMap.put("weight", 22.9);
        parcelMap.put("height", 12.1);
        parcelMap.put("width", 8);
        parcelMap.put("length", 19.8);

        Map<String, Object> shipmentMap = new HashMap<String, Object>();
        shipmentMap.put("from_address", fromAddressMap);
        shipmentMap.put("to_address", toAddressMap);
        shipmentMap.put("parcel", parcelMap);

        EasyPostClient easyPostClient = easyPostClient(getAPIKey(warehouseId));
        Shipment shipment = easyPostClient.shipment.create(shipmentMap);

        Shipment boughtShipment = easyPostClient.shipment.buy(shipment.getId(), shipment.lowestRate());

        System.out.println(boughtShipment.prettyPrint());

   }

    /**
     * Create the easy post shipment from the order and the size of the package
     * the from address is the warehouse address
     * the to address is the order's ship to address
     * @param warehouseId
     * @param orderId
     * @param length
     * @param weight
     * @param height
     * @return
     */
    public Shipment createEasyPostShipment(Long warehouseId, Long orderId,
                                           Double length, Double width, Double height, Double weight) throws EasyPostException {
        Order order = orderService.findById(orderId);
        Warehouse warehouse = warehouseLayoutServiceRestemplateClient.getWarehouseById(warehouseId);

        EasyPostConfiguration easyPostConfiguration = easyPostConfigurationService.findByWarehouseId(warehouseId, false);
        

        // ship from address
        Map<String, Object> fromAddressMap = getShipFromAddress(easyPostConfiguration, warehouse);
        // ship from address
        Map<String, Object> returnAddressMap = getReturnAddress(easyPostConfiguration, warehouse);


        Map<String, Object> toAddressMap = new HashMap<String, Object>();
        toAddressMap.put("name", order.getShipToContactorFirstname() + " " + order.getShipToContactorLastname());
        toAddressMap.put("street1", order.getShipToAddressLine1());
        toAddressMap.put("street2", order.getShipToAddressLine2());
        toAddressMap.put("city", order.getShipToAddressCity());
        toAddressMap.put("state", order.getShipToAddressState());
        toAddressMap.put("country", order.getShipToAddressCountry());
        toAddressMap.put("zip", order.getShipToAddressPostcode());
        toAddressMap.put("phone", "");

        Map<String, Object> parcelMap = new HashMap<String, Object>();
        parcelMap.put("weight", weight);
        parcelMap.put("height", height);
        parcelMap.put("width", width);
        parcelMap.put("length", length);

        List<String> carrierAccountNumbers =  getCarrierAccountNumbers(easyPostConfiguration);

        Map<String, Object> shipmentMap = new HashMap<String, Object>();
        shipmentMap.put("from_address", fromAddressMap);
        shipmentMap.put("to_address", toAddressMap);
        shipmentMap.put("return_address", returnAddressMap);
        shipmentMap.put("parcel", parcelMap);
        shipmentMap.put("carrier_accounts ", carrierAccountNumbers);


        logger.debug("start to create shipment with parameters \n{}", shipmentMap);

        try {
            return easyPostClient(getAPIKey(warehouseId)).shipment.create(shipmentMap);
        }
        catch(com.easypost.exception.EasyPostException ex) {
            throw ShippingException.raiseException("Error while create Easy Post shipment: " + ex.getMessage());
        }
    }

    private List<String> getCarrierAccountNumbers(EasyPostConfiguration easyPostConfiguration) {
        return easyPostConfiguration.getCarriers().stream().map(
                easyPostCarrier -> easyPostCarrier.getAccountNumber()
        ).collect(Collectors.toList());
    }

    private Map<String, Object> getShipFromAddress(EasyPostConfiguration easyPostConfiguration, Warehouse warehouse) {

        Map<String, Object> fromAddressMap =new HashMap<String, Object>();

        // get the ship from address from warehouse or the configuration
        // based on the setup
        if (Boolean.TRUE.equals(easyPostConfiguration.getUseWarehouseAddressAsShipFromFlag())) {
            fromAddressMap.put("name", warehouse.getContactorFirstname() + " " + warehouse.getContactorLastname());
            fromAddressMap.put("company", warehouse.getCompany().getName());
            fromAddressMap.put("street1", warehouse.getAddressLine1());
            fromAddressMap.put("street2", warehouse.getAddressLine2());
            fromAddressMap.put("city", warehouse.getAddressCity());
            fromAddressMap.put("state", warehouse.getAddressState());
            fromAddressMap.put("country", warehouse.getAddressCountry());
            fromAddressMap.put("zip", warehouse.getAddressPostcode());
            fromAddressMap.put("phone", "");
        }
        else {

            fromAddressMap.put("name", easyPostConfiguration.getContactorFirstname() + " " + easyPostConfiguration.getContactorLastname());
            fromAddressMap.put("company", warehouse.getCompany().getName());
            fromAddressMap.put("street1", easyPostConfiguration.getAddressLine1());
            fromAddressMap.put("street2", easyPostConfiguration.getAddressLine2());
            fromAddressMap.put("city", easyPostConfiguration.getAddressCity());
            fromAddressMap.put("state", easyPostConfiguration.getAddressState());
            fromAddressMap.put("country", easyPostConfiguration.getAddressCountry());
            fromAddressMap.put("zip", easyPostConfiguration.getAddressPostcode());
            fromAddressMap.put("phone", "");
        }
        return fromAddressMap;


    }


    private Map<String, Object> getReturnAddress(EasyPostConfiguration easyPostConfiguration, Warehouse warehouse) {

        Map<String, Object> returnAddressMap =new HashMap<String, Object>();

        // get the ship from address from warehouse or the configuration
        // based on the setup
        if (Boolean.TRUE.equals(easyPostConfiguration.getUseWarehouseAddressAsShipFromFlag())) {
            returnAddressMap.put("name", warehouse.getContactorFirstname() + " " + warehouse.getContactorLastname());
            returnAddressMap.put("company", warehouse.getCompany().getName());
            returnAddressMap.put("street1", warehouse.getAddressLine1());
            returnAddressMap.put("street2", warehouse.getAddressLine2());
            returnAddressMap.put("city", warehouse.getAddressCity());
            returnAddressMap.put("state", warehouse.getAddressState());
            returnAddressMap.put("country", warehouse.getAddressCountry());
            returnAddressMap.put("zip", warehouse.getAddressPostcode());
            returnAddressMap.put("phone", "");
        }
        else {

            returnAddressMap.put("name", easyPostConfiguration.getReturnContactorFirstname() + " " + easyPostConfiguration.getReturnContactorLastname());
            returnAddressMap.put("company", warehouse.getCompany().getName());
            returnAddressMap.put("street1", easyPostConfiguration.getReturnAddressLine1());
            returnAddressMap.put("street2", easyPostConfiguration.getReturnAddressLine2());
            returnAddressMap.put("city", easyPostConfiguration.getReturnAddressCity());
            returnAddressMap.put("state", easyPostConfiguration.getReturnAddressState());
            returnAddressMap.put("country", easyPostConfiguration.getReturnAddressCountry());
            returnAddressMap.put("zip", easyPostConfiguration.getReturnAddressPostcode());
            returnAddressMap.put("phone", "");
        }
        return returnAddressMap;


    }
    /**
     * Confirm easy post shipment with  rate
     * @return
     * @throws EasyPostException
     */
    public Shipment confirmEasyPostShipment(Long warehouseId, Long orderId, String shipmentId, Rate rate) throws EasyPostException {

        // request the shipping label from easy post
        Shipment boughtShipment = easyPostClient(getAPIKey(warehouseId)).shipment.buy(shipmentId, rate);

        // save the data to database
        Order order = orderService.findById(orderId);
        parcelPackageService.addParcelPackage(warehouseId, order, boughtShipment);


        // logger.debug("bought shipment \n {}", boughtShipment);
        return boughtShipment;
    }
    public Event validateWebhook(Long warehouseId, byte[] eventBody, Map<String, Object> headers) throws EasyPostException {
        return easyPostClient(getAPIKey(warehouseId)).webhook.validateWebhook(eventBody, headers, getWebhookSecret(warehouseId));
    }

    public void processWebhookEvent(Event event) {
        logger.debug("start to process webhook event with {}", event.getDescription());
        logger.debug("event.getDescription().equalsIgnoreCase(tracker.updated): {}",
                event.getDescription().equalsIgnoreCase("tracker.updated"));
        if (event.getDescription().equalsIgnoreCase("tracker.updated")) {
            // update the tracking information
            logger.debug("start to change the tracker {}'s status to {}",
                    event.getResult().get("tracking_code"),
                    event.getResult().get("status"));
            parcelPackageService.updateTracker(event.getResult().get("tracking_code").toString(),
                    event.getResult().get("status").toString());
        }
    }
}

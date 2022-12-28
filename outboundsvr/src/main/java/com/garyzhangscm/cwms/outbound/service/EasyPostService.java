package com.garyzhangscm.cwms.outbound.service;

import com.easypost.exception.EasyPostException;
import com.easypost.exception.General.MissingParameterError;
import com.easypost.model.Rate;
import com.easypost.model.Shipment;
import com.easypost.service.EasyPostClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.garyzhangscm.cwms.outbound.clients.WarehouseLayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.outbound.model.Order;
import com.garyzhangscm.cwms.outbound.model.Warehouse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

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

    @Value("${parcel.easyPost.apiKey}")
    private String apiKey;

    @Autowired
    private OrderService orderService;
    @Autowired
    private ParcelPackageService parcelPackageService;
    @Autowired
    private WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient;


    @Bean
    public EasyPostClient easyPostClient() throws MissingParameterError {
        return new EasyPostClient(apiKey);

    }


    public void getShippingRates() throws JsonProcessingException, EasyPostException {

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

        Shipment shipment = easyPostClient().shipment.create(shipmentMap);

        Shipment boughtShipment = easyPostClient().shipment.buy(shipment.getId(), shipment.lowestRate());

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

        // from address is the warehouse's address
        Map<String, Object> fromAddressMap = new HashMap<String, Object>();

        fromAddressMap.put("company", warehouse.getCompany().getName());
        fromAddressMap.put("street1", warehouse.getAddressLine1());
        fromAddressMap.put("street2", warehouse.getAddressLine2());
        fromAddressMap.put("city", warehouse.getAddressCity());
        fromAddressMap.put("state", warehouse.getAddressState());
        fromAddressMap.put("country", warehouse.getAddressCountry());
        fromAddressMap.put("zip", warehouse.getAddressPostcode());
        fromAddressMap.put("phone", "");

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

        Map<String, Object> shipmentMap = new HashMap<String, Object>();
        shipmentMap.put("from_address", fromAddressMap);
        shipmentMap.put("to_address", toAddressMap);
        shipmentMap.put("parcel", parcelMap);

        return easyPostClient().shipment.create(shipmentMap);
    }


    /**
     * Confirm easy post shipment with  rate
     * @return
     * @throws EasyPostException
     */
    public Shipment confirmEasyPostShipment(Long warehouseId, Long orderId, String shipmentId, Rate rate) throws EasyPostException {

        // request the shipping label from easy post
        Shipment boughtShipment = easyPostClient().shipment.buy(shipmentId, rate);

        // save the data to database
        Order order = orderService.findById(orderId);
        parcelPackageService.addParcelPackage(warehouseId, order, boughtShipment);


        // logger.debug("bought shipment \n {}", boughtShipment);
        return boughtShipment;
    }
}

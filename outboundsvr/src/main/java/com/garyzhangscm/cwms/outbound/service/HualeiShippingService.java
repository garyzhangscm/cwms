/**
 * Copyright 2018
 *
 * @author gzhang
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.garyzhangscm.cwms.outbound.service;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.garyzhangscm.cwms.outbound.clients.CommonServiceRestemplateClient;
import com.garyzhangscm.cwms.outbound.clients.HualeiRestemplateClient;
import com.garyzhangscm.cwms.outbound.exception.ExceptionCode;
import com.garyzhangscm.cwms.outbound.exception.GenericException;
import com.garyzhangscm.cwms.outbound.exception.OrderOperationException;
import com.garyzhangscm.cwms.outbound.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.outbound.model.FileUploadResult;
import com.garyzhangscm.cwms.outbound.model.Order;
import com.garyzhangscm.cwms.outbound.model.OrderLineCSVWrapper;
import com.garyzhangscm.cwms.outbound.model.OrderStatus;
import com.garyzhangscm.cwms.outbound.model.hualei.*;
import com.garyzhangscm.cwms.outbound.repository.HualeiProductRepository;
import com.garyzhangscm.cwms.outbound.repository.HualeiShipmentRequestRepository;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.codehaus.jackson.map.ser.std.ObjectArraySerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import javax.persistence.*;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


@Service
public class HualeiShippingService {
    private static final Logger logger = LoggerFactory.getLogger(HualeiShippingService.class);

    @Autowired
    private OrderService orderService;
    @Autowired
    private HualeiConfigurationService hualeiConfigurationService;
    @Autowired
    private CommonServiceRestemplateClient commonServiceRestemplateClient;
    @Autowired
    private HualeiRestemplateClient hualeiRestemplateClient;
    @Autowired
    private HualeiShipmentRequestRepository hualeiShipmentRequestRepository;
    @Autowired
    private UnitService unitService;

    public ShipmentRequest[] sendHualeiShippingRequest(Long warehouseId,
                                                       String productId, // hualei product id
                                                       Long orderId,
                                                       double length,
                                                       double width,
                                                       double height,
                                                       double weight,
                                                       int packageCount,
                                                       String itemName,
                                                       Long quantity,
                                                       Double unitCost,
                                                       String lengthUnit,
                                                       String weightUnit) {

        return sendHualeiShippingRequest(
                warehouseId, productId, orderService.findById(orderId),
                length, width, height, weight, packageCount,
                itemName, quantity, unitCost,
                lengthUnit, weightUnit);
    }
    public ShipmentRequest[] sendHualeiShippingRequest(Long warehouseId,
                                                       String productId, // hualei product id
                                                       Order order,
                                                       double length,
                                                       double width,
                                                       double height,
                                                       double weight,
                                                       int packageCount,
                                                       String itemName,
                                                       Long quantity,
                                                       Double unitCost,
                                                       String lengthUnit,
                                                       String weightUnit) {
        HualeiConfiguration hualeiConfiguration =
                hualeiConfigurationService.findByWarehouse(warehouseId);

        if (Objects.isNull(hualeiConfiguration)) {
            throw OrderOperationException.raiseException("hualei system is not setup");
        }

        // setup the default value if the value is not passed in
        if (length <= 1) {
            length = 1;
        }
        if (width <= 1) {
            width = 1;
        }
        if (height <= 1) {
            height = 1;
        }
        if (weight <= 1) {
            weight = 1;
        }


        if (Strings.isBlank(itemName)) {
            itemName = hualeiConfiguration.getDefaultSku();
        }
        if (Objects.isNull(quantity) || quantity <= 1) {
            quantity = 1l;
        }
        if (Objects.isNull(unitCost) || unitCost <= 1) {
            unitCost = 1.0;
        }


        ShipmentRequest[] shipmentRequests = new ShipmentRequest[packageCount];

        // init identical shipment request and save it.
        // we will send the request and get the response in a separate thread
        for (int i = 0; i < packageCount; i++) {

            shipmentRequests[i] =
                    hualeiShipmentRequestRepository.save(
                            generateHualeiShipmentRequest(
                                warehouseId, productId, hualeiConfiguration, order, length, width, height, weight,
                                    itemName, quantity, unitCost,
                                    lengthUnit, weightUnit
                            )
                    );
        }

        // start a new thread to request the label one by one
        new Thread(() -> {
            for (int i = 0; i < packageCount; i++) {
                    ShipmentRequest shipmentRequest = shipmentRequests[i];
                    logger.debug("start to send #{} hualei shipment request: \n{}",
                            i, shipmentRequest);

                    ShipmentResponse shipmentResponse = hualeiRestemplateClient.sendHualeiShippingRequest(hualeiConfiguration,
                            shipmentRequest);


                    shipmentRequest.setShipmentResponse(shipmentResponse);
                    shipmentResponse.setShipmentRequest(shipmentRequest);
                    shipmentResponse.setWarehouseId(warehouseId);
                    shipmentResponse.getChildList().forEach(
                            shipmentResponseChild -> {
                                shipmentResponseChild.setShipmentResponse(shipmentResponse);
                                shipmentResponseChild.setWarehouseId(warehouseId);
                            }
                    );
                    logger.debug("save hualei's shipment response\n {}", shipmentResponse);

                    hualeiShipmentRequestRepository.save(shipmentRequest);
            }
        }).start();


        return shipmentRequests;
    }

    private ShipmentRequest generateHualeiShipmentRequest(Long warehouseId,
                                                          String productId, // hualei product id
                                                          HualeiConfiguration hualeiConfiguration,
                                                          Order order,
                                                          double length,
                                                          double width,
                                                          double height,
                                                          double weight,
                                                          String itemName,
                                                          Long quantity,
                                                          Double unitCost,
                                                          String lengthUnit,
                                                          String weightUnit) {
        ShipmentRequest shipmentRequest = new ShipmentRequest();

        shipmentRequest.setWarehouseId(warehouseId);
        shipmentRequest.setGetTrackingNumber("1");
        shipmentRequest.setStatus(ShipmentRequestStatus.REQUESTED);
        shipmentRequest.setOrder(order);

        ShipmentRequestParameters shipmentRequestParameters =
                generateShipmentRequestParameters(
                        warehouseId, productId, hualeiConfiguration, order,
                        length, width, height, weight,
                        itemName, quantity, unitCost,
                        lengthUnit, weightUnit
                );
        shipmentRequestParameters.setShipmentRequest(shipmentRequest);
        shipmentRequest.setShipmentRequestParameters(shipmentRequestParameters);


        return shipmentRequest;
    }

    private ShipmentRequestParameters generateShipmentRequestParameters(Long warehouseId,
                                                                        String productId, // hualei product id
                                                                        HualeiConfiguration hualeiConfiguration,
                                                                        Order order,
                                                                        double length,
                                                                        double width,
                                                                        double height,
                                                                        double weight,
                                                                        String itemName,
                                                                        Long quantity,
                                                                        Double unitCost,
                                                                        String lengthUnit,
                                                                        String weightUnit) {
        ShipmentRequestParameters shipmentRequestParameters = new ShipmentRequestParameters();
        shipmentRequestParameters.setWarehouseId(warehouseId);

        shipmentRequestParameters.setCargoType(hualeiConfiguration.getDefaultCargoType());

        shipmentRequestParameters.setConsigneeAddress(order.getShipToAddressLine1());
        shipmentRequestParameters.setConsigneeCity(order.getShipToAddressCity());
        shipmentRequestParameters.setConsigneeName(
                order.getShipToContactorFirstname() + " " + order.getShipToContactorLastname()
        );
        shipmentRequestParameters.setConsigneePostcode(order.getShipToAddressPostcode());
        shipmentRequestParameters.setConsigneeState(order.getShipToAddressState());
        shipmentRequestParameters.setConsigneeTelephone("0000000000");
        shipmentRequestParameters.setCountry(order.getShipToAddressCountry());

        shipmentRequestParameters.setCustomerId(hualeiConfiguration.getCustomerId());
        shipmentRequestParameters.setCustomerUserid(hualeiConfiguration.getCustomerUserid());
        shipmentRequestParameters.setCustomsClearance(hualeiConfiguration.getDefaultCustomsClearance());
        shipmentRequestParameters.setCustomsDeclaration(hualeiConfiguration.getDefaultCustomsDeclaration());
        shipmentRequestParameters.setDutyType(hualeiConfiguration.getDefaultDutyType());
        shipmentRequestParameters.setShipFrom(hualeiConfiguration.getDefaultFrom());
        shipmentRequestParameters.setIsFba(hualeiConfiguration.getDefaultIsFba());
        shipmentRequestParameters.setOrderCustomerInvoiceCode(
                commonServiceRestemplateClient.getNextNumber(warehouseId, "hualei-order-customer-invoice-code")
        );
        // order piece: how many boxes in the orderVolumeParam
        shipmentRequestParameters.setOrderPiece(1);
        shipmentRequestParameters.setOrderReturnSign(hualeiConfiguration.getDefaultOrderReturnSign());
        shipmentRequestParameters.setProductId(productId);
        shipmentRequestParameters.setWeight(weight);


        String shippingCartonNumber =
                commonServiceRestemplateClient.getNextNumber(warehouseId, "shipping-cartonization-number");

        ShipmentRequestOrderVolumeParameters orderVolumeParam
                =  generateShipmentRequestOrderVolumeParameters(
                warehouseId, shippingCartonNumber, length, width, height, weight,
                lengthUnit, weightUnit,
                hualeiConfiguration.getLengthUnit(),
                hualeiConfiguration.getWeightUnit()
        );
        orderVolumeParam.setShipmentRequestParameters(shipmentRequestParameters);
        shipmentRequestParameters.addOrderVolumeParam(orderVolumeParam);

        ShipmentRequestOrderInvoiceParameters orderInvoiceParam
                =  generateShipmentRequestOrderInvoiceParameters(
                        warehouseId,
                        shippingCartonNumber,
                        hualeiConfiguration.getDefaultHsCode(),
                        weight,
                        itemName, quantity, unitCost
                    );
        orderInvoiceParam.setShipmentRequestParameters(shipmentRequestParameters);
        shipmentRequestParameters.addOrderInvoiceParam(orderInvoiceParam);

        return shipmentRequestParameters;

    }

    private ShipmentRequestOrderInvoiceParameters generateShipmentRequestOrderInvoiceParameters(
            Long warehouseId,
            String shippingCartonNumber, String hsCode, double weight,
            String itemName, Long quantity, Double unitCost) {

        ShipmentRequestOrderInvoiceParameters shipmentRequestOrderInvoiceParameters =
                new ShipmentRequestOrderInvoiceParameters();
        shipmentRequestOrderInvoiceParameters.setWarehouseId(warehouseId);
        shipmentRequestOrderInvoiceParameters.setBoxNo(shippingCartonNumber);
        shipmentRequestOrderInvoiceParameters.setHsCode(hsCode);
        shipmentRequestOrderInvoiceParameters.setInvoiceAmount(quantity * unitCost);
        shipmentRequestOrderInvoiceParameters.setInvoicePieces(quantity.intValue());
        shipmentRequestOrderInvoiceParameters.setInvoiceTitle(itemName);
        shipmentRequestOrderInvoiceParameters.setInvoiceWeight(weight);
        shipmentRequestOrderInvoiceParameters.setSku(itemName);
        shipmentRequestOrderInvoiceParameters.setSkuCode(itemName);

        return shipmentRequestOrderInvoiceParameters;
    }

    private ShipmentRequestOrderVolumeParameters generateShipmentRequestOrderVolumeParameters(
            Long warehouseId, String shippingCartonNumber,
            double length, double width, double height, double weight,
            String lengthUnit, String weightUnit,
            String requiredLengthUnit, String requiredWeightUnit) {
        ShipmentRequestOrderVolumeParameters shipmentRequestOrderVolumeParameters =
                new ShipmentRequestOrderVolumeParameters();

        shipmentRequestOrderVolumeParameters.setWarehouseId(warehouseId);
        shipmentRequestOrderVolumeParameters.setBoxNo(shippingCartonNumber);
        shipmentRequestOrderVolumeParameters.setChildNo(shippingCartonNumber);

        // convert the length / width / height and weight based on the
        // pass in unit and required unit.
        // if the length unit passed in match with the length unit required,
        // then

        shipmentRequestOrderVolumeParameters.setVolumeLength(
                unitService.convertLength(warehouseId, length, lengthUnit, requiredLengthUnit));
        shipmentRequestOrderVolumeParameters.setVolumeWidth(
                unitService.convertLength(warehouseId, width, lengthUnit, requiredLengthUnit));
        shipmentRequestOrderVolumeParameters.setVolumeHeight(
                unitService.convertLength(warehouseId, height, lengthUnit, requiredLengthUnit));
        shipmentRequestOrderVolumeParameters.setLengthUnit(requiredLengthUnit);

        shipmentRequestOrderVolumeParameters.setVolumeWeight(
                unitService.convertWeight(warehouseId, weight, lengthUnit, requiredWeightUnit));
        shipmentRequestOrderVolumeParameters.setWeightUnit(requiredWeightUnit);
        return shipmentRequestOrderVolumeParameters;
    }


    public File getShippingLabelFile(Long warehouseId,
                                     Long orderId,
                                     String productId,
                                     String hualeiOrderId) {
        // download from the web, we will need to get the response based on the order id
        HualeiConfiguration hualeiConfiguration = hualeiConfigurationService.findByWarehouse(warehouseId);
        HualeiShippingLabelFormatByProduct hualeiShippingLabelFormatByProduct =
                hualeiConfiguration.getHualeiShippingLabelFormatByProducts().stream().filter(
                        existingHualeiShippingLabelFormatByProduct -> existingHualeiShippingLabelFormatByProduct.getProductId().equalsIgnoreCase(productId)
                ).findFirst()
                        .orElseThrow(() -> ResourceNotFoundException.raiseException("Hualei configuration not found for product id " + productId));


        File file = hualeiRestemplateClient.getHualeiShippingLabelFile(warehouseId, orderId,
                hualeiConfiguration,
                hualeiShippingLabelFormatByProduct.getShippingLabelFormat(), hualeiOrderId);

        if (Objects.isNull(file)) {
            throw OrderOperationException.raiseException("can't download the shipping label by order id " + hualeiOrderId);
        }
        return file;

    }
}

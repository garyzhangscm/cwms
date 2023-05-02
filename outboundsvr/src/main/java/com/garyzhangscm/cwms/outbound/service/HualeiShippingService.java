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
import com.garyzhangscm.cwms.outbound.model.Order;
import com.garyzhangscm.cwms.outbound.model.OrderStatus;
import com.garyzhangscm.cwms.outbound.model.hualei.*;
import com.garyzhangscm.cwms.outbound.repository.HualeiProductRepository;
import com.garyzhangscm.cwms.outbound.repository.HualeiShipmentRequestRepository;
import org.apache.commons.lang.StringUtils;
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

    public ShipmentResponse sendHualeiShippingRequest(Long warehouseId,
                                                      String productId, // hualei product id
                                                      Long orderId,
                                                      double length,
                                                      double width,
                                                      double height,
                                                      double weight) {

        return sendHualeiShippingRequest(
                warehouseId, productId, orderService.findById(orderId),
                length, width, height, weight);
    }
    public ShipmentResponse sendHualeiShippingRequest(Long warehouseId,
                                                      String productId, // hualei product id
                                                      Order order,
                                                      double length,
                                                      double width,
                                                      double height,
                                                      double weight) {
        HualeiConfiguration hualeiConfiguration =
                hualeiConfigurationService.findByWarehouse(warehouseId);
        if (Objects.isNull(hualeiConfiguration)) {
            throw OrderOperationException.raiseException("hualei system is not setup");
        }

        ShipmentRequest shipmentRequest = generateHualeiShipmentRequest(
                warehouseId, productId, hualeiConfiguration, order, length, width, height, weight
        );

        logger.debug("start to send hualei shipment request: \n{}");
        logger.debug(shipmentRequest.toString());

        logger.debug("Save shipment request");
        shipmentRequest = hualeiShipmentRequestRepository.save(shipmentRequest);

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


        shipmentRequest = hualeiShipmentRequestRepository.save(shipmentRequest);

        return shipmentRequest.getShipmentResponse();
    }

    private ShipmentRequest generateHualeiShipmentRequest(Long warehouseId,
                                                          String productId, // hualei product id
                                                          HualeiConfiguration hualeiConfiguration,
                                                          Order order,
                                                          double length,
                                                          double width,
                                                          double height,
                                                          double weight) {
        ShipmentRequest shipmentRequest = new ShipmentRequest();

        shipmentRequest.setWarehouseId(warehouseId);
        shipmentRequest.setGetTrackingNumber("1");
        shipmentRequest.setOrder(order);

        ShipmentRequestParameters shipmentRequestParameters =
                generateShipmentRequestParameters(
                        warehouseId, productId, hualeiConfiguration, order,
                        length, width, height, weight
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
                                                                        double weight) {
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
        shipmentRequestParameters.setOrderPiece(1);
        shipmentRequestParameters.setOrderReturnSign(hualeiConfiguration.getDefaultOrderReturnSign());
        shipmentRequestParameters.setProductId(productId);
        shipmentRequestParameters.setWeight(weight);


        String shippingCartonNumber =
                commonServiceRestemplateClient.getNextNumber(warehouseId, "shipping-cartonization-number");

        ShipmentRequestOrderVolumeParameters orderVolumeParam
                =  generateShipmentRequestOrderVolumeParameters(
                warehouseId, shippingCartonNumber, length, width, height, weight
        );
        orderVolumeParam.setShipmentRequestParameters(shipmentRequestParameters);
        shipmentRequestParameters.addOrderVolumeParam(orderVolumeParam);

        ShipmentRequestOrderInvoiceParameters orderInvoiceParam
                =  generateShipmentRequestOrderInvoiceParameters(
                warehouseId,
                shippingCartonNumber,
                hualeiConfiguration.getDefaultHsCode(),
                hualeiConfiguration.getDefaultInvoiceTitle(),
                hualeiConfiguration.getDefaultSku(),
                hualeiConfiguration.getDefaultSkuCode(),
                weight);
        orderInvoiceParam.setShipmentRequestParameters(shipmentRequestParameters);
        shipmentRequestParameters.addOrderInvoiceParam(orderInvoiceParam);

        return shipmentRequestParameters;

    }

    private ShipmentRequestOrderInvoiceParameters generateShipmentRequestOrderInvoiceParameters(
            Long warehouseId,
            String shippingCartonNumber, String hsCode, String invoiceTitle,
            String sku, String skuCode, double weight) {

        ShipmentRequestOrderInvoiceParameters shipmentRequestOrderInvoiceParameters =
                new ShipmentRequestOrderInvoiceParameters();
        shipmentRequestOrderInvoiceParameters.setWarehouseId(warehouseId);
        shipmentRequestOrderInvoiceParameters.setBoxNo(shippingCartonNumber);
        shipmentRequestOrderInvoiceParameters.setHsCode(hsCode);
        shipmentRequestOrderInvoiceParameters.setInvoiceAmount(1.0);
        shipmentRequestOrderInvoiceParameters.setInvoicePieces(1);
        shipmentRequestOrderInvoiceParameters.setInvoiceTitle(invoiceTitle);
        shipmentRequestOrderInvoiceParameters.setInvoiceWeight(weight);
        shipmentRequestOrderInvoiceParameters.setSku(sku);
        shipmentRequestOrderInvoiceParameters.setSkuCode(skuCode);

        return shipmentRequestOrderInvoiceParameters;
    }

    private ShipmentRequestOrderVolumeParameters generateShipmentRequestOrderVolumeParameters(
            Long warehouseId, String shippingCartonNumber,
            double length, double width, double height, double weight) {
        ShipmentRequestOrderVolumeParameters shipmentRequestOrderVolumeParameters =
                new ShipmentRequestOrderVolumeParameters();

        shipmentRequestOrderVolumeParameters.setWarehouseId(warehouseId);
        shipmentRequestOrderVolumeParameters.setBoxNo(shippingCartonNumber);
        shipmentRequestOrderVolumeParameters.setChildNo(shippingCartonNumber);

        shipmentRequestOrderVolumeParameters.setVolumeLength(length);
        shipmentRequestOrderVolumeParameters.setVolumeWidth(width);
        shipmentRequestOrderVolumeParameters.setVolumeHeight(height);
        shipmentRequestOrderVolumeParameters.setVolumeWeight(weight);
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

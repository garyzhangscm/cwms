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

import com.garyzhangscm.cwms.outbound.clients.CommonServiceRestemplateClient;
import com.garyzhangscm.cwms.outbound.clients.HualeiRestemplateClient;
import com.garyzhangscm.cwms.outbound.exception.OrderOperationException;
import com.garyzhangscm.cwms.outbound.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.outbound.model.*;
import com.garyzhangscm.cwms.outbound.model.hualei.*;
import com.garyzhangscm.cwms.outbound.repository.HualeiShipmentRequestRepository;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;


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
    @Autowired
    private HualeiProductService hualeiProductService;
    @Autowired
    private ParcelPackageService parcelPackageService;

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
                                                       String weightUnit,
                                                       Boolean parcelInsured,
                                                       Double parcelInsuredAmount,
                                                       Boolean parcelSignatureRequired) {

        return sendHualeiShippingRequest(
                warehouseId, productId, orderService.findById(orderId),
                length, width, height, weight, packageCount,
                itemName, quantity, unitCost,
                lengthUnit, weightUnit,
                parcelInsured, parcelInsuredAmount, parcelSignatureRequired);
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
                                                       String weightUnit,
                                                       Boolean parcelInsured,
                                                       Double parcelInsuredAmount,
                                                       Boolean parcelSignatureRequired) {
        HualeiConfiguration hualeiConfiguration =
                hualeiConfigurationService.findByWarehouse(warehouseId);

        if (Objects.isNull(hualeiConfiguration)) {
            throw OrderOperationException.raiseException("hualei system is not setup");
        }

        HualeiProduct hualeiProduct = hualeiProductService.findByProductId(warehouseId, productId);

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
                                    lengthUnit, weightUnit,
                                    parcelInsured, parcelInsuredAmount, parcelSignatureRequired
                            )
                    );
        }

        // start a new thread to request the label one by one
        double finalLength = length;
        double finalWidth = width;
        double finalHeight = height;
        double finalWeight = weight;
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

                    /**
                    if (Strings.isBlank(shipmentResponse.getMessage())) {
                        // if message is empty, then we don't have any error
                        saveTrackingNumber(warehouseId, order, productId, shipmentResponse,
                                hualeiProduct.getCarrierId(), hualeiProduct.getCarrierServiceLevelId(),
                                finalLength, finalWidth, finalHeight, lengthUnit, finalWeight, weightUnit);
                    }
                     **/
                    addPackage(warehouseId, order, productId, shipmentResponse,
                        hualeiProduct.getCarrierId(), hualeiProduct.getCarrierServiceLevelId(),
                        finalLength, finalWidth, finalHeight, lengthUnit, finalWeight, weightUnit);
                    hualeiShipmentRequestRepository.save(shipmentRequest);
            }
        }).start();


        return shipmentRequests;
    }

    private void addPackage(Long warehouseId,
                                    Order order,
                                    String productId,
                                    ShipmentResponse shipmentResponse,
                                    Long carrierId,
                                    Long carrierServiceLevelId,
                                    double length,
                                    double width,
                                    double height,
                                    String lengthUnit,
                                    double weight,
                                    String weightUnit) {

        logger.debug("start to add tracking number {} for order {}, with carrier id {}, " +
                "service level id {}",
                shipmentResponse.getTrackingNumber(),
                order.getNumber(), carrierId, carrierServiceLevelId);
        logger.debug("volume: {} x {} x {}, weight: {}",
                unitService.convertLength(warehouseId, length, lengthUnit),
                unitService.convertLength(warehouseId, width, lengthUnit),
                unitService.convertLength(warehouseId, height, lengthUnit),
                unitService.convertWeight(warehouseId, weight, weightUnit)
                );

        parcelPackageService.addParcelPackage(warehouseId, order, productId,
                carrierId, carrierServiceLevelId,
                // if the message is blank, then there's no error and we can use the tracking number.
                // if the message has value, it indicates that there's error with the call and
                // the tracking number field will be the reference number that we passed to hualei
                Strings.isBlank(shipmentResponse.getMessage()) ? shipmentResponse.getTrackingNumber() : null,
                shipmentResponse.getOrderId(),
                unitService.convertLength(warehouseId, length, lengthUnit),
                unitService.convertLength(warehouseId, weight, lengthUnit),
                unitService.convertLength(warehouseId, height, lengthUnit),
                unitService.convertWeight(warehouseId, weight, weightUnit)
                );
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
                                                          String weightUnit,
                                                          Boolean parcelInsured,
                                                          Double parcelInsuredAmount,
                                                          Boolean parcelSignatureRequired) {
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
                        lengthUnit, weightUnit,
                        parcelInsured, parcelInsuredAmount, parcelSignatureRequired
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
                                                                        String weightUnit,
                                                                        Boolean parcelInsured,
                                                                        Double parcelInsuredAmount,
                                                                        Boolean parcelSignatureRequired) {
        ShipmentRequestParameters shipmentRequestParameters = new ShipmentRequestParameters();
        shipmentRequestParameters.setWarehouseId(warehouseId);

        shipmentRequestParameters.setCargoType(hualeiConfiguration.getDefaultCargoType());

        shipmentRequestParameters.setConsigneeAddress(order.getShipToAddressLine1());
        shipmentRequestParameters.setConsigneeCity(order.getShipToAddressCity());
        shipmentRequestParameters.setConsigneeName(
                order.getShipToContactorFirstname() + " " + order.getShipToContactorLastname()
        );
        shipmentRequestParameters.setConsigneeTelephone(order.getShipToContactorPhoneNumber());
        shipmentRequestParameters.setConsigneePostcode(order.getShipToAddressPostcode());
        shipmentRequestParameters.setConsigneeState(order.getShipToAddressState());
        // shipmentRequestParameters.setConsigneeTelephone("0000000000");
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


        shipmentRequestParameters.setWeight(
                unitService.convertWeight(warehouseId, weight, weightUnit, hualeiConfiguration.getWeightUnit()));
        shipmentRequestParameters.setWeightUnit(hualeiConfiguration.getWeightUnit());
        // shipmentRequestParameters.setWeight(weight);

        String note = "";

        if (Boolean.TRUE.equals(parcelInsured) && Objects.nonNull(parcelInsuredAmount) && parcelInsuredAmount > 0) {
            note += "Insured(Amount: $" + parcelInsuredAmount +").";
        }
        if (Boolean.TRUE.equals(parcelSignatureRequired)) {
            note += " Signature Required!";
        }
        shipmentRequestParameters.setNote(note);


        String shippingCartonNumber =
                commonServiceRestemplateClient.getNextNumber(warehouseId, "shipping-cartonization-number");

        // convert the length / width / height and weight based on the
        // current unit used in the system and the required unit by hualei

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
                        itemName, quantity, unitCost,
                        weightUnit,
                        hualeiConfiguration.getWeightUnit()
                    );
        orderInvoiceParam.setShipmentRequestParameters(shipmentRequestParameters);
        shipmentRequestParameters.addOrderInvoiceParam(orderInvoiceParam);

        return shipmentRequestParameters;

    }

    private ShipmentRequestOrderInvoiceParameters generateShipmentRequestOrderInvoiceParameters(
            Long warehouseId,
            String shippingCartonNumber, String hsCode, double weight,
            String itemName, Long quantity, Double unitCost,
            String weightUnit, String requiredWeightUnit) {

        ShipmentRequestOrderInvoiceParameters shipmentRequestOrderInvoiceParameters =
                new ShipmentRequestOrderInvoiceParameters();
        shipmentRequestOrderInvoiceParameters.setWarehouseId(warehouseId);
        shipmentRequestOrderInvoiceParameters.setBoxNo(shippingCartonNumber);
        shipmentRequestOrderInvoiceParameters.setHsCode(hsCode);
        shipmentRequestOrderInvoiceParameters.setInvoiceAmount(quantity * unitCost);
        shipmentRequestOrderInvoiceParameters.setInvoicePieces(quantity.intValue());
        shipmentRequestOrderInvoiceParameters.setInvoiceTitle(itemName);

        shipmentRequestOrderInvoiceParameters.setInvoiceWeight(
                unitService.convertWeight(warehouseId, weight, weightUnit, requiredWeightUnit));
        shipmentRequestOrderInvoiceParameters.setWeightUnit(requiredWeightUnit);


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
                unitService.convertWeight(warehouseId, weight, weightUnit, requiredWeightUnit));
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

    public List<HualeiTrackStatusResponseData> refreshHualeiPackageStatus(String[] trackingNumberArray, HualeiConfiguration hualeiConfiguration) {

        String trackingNumbers = Arrays.stream(trackingNumberArray).filter(trackingNumber -> Strings.isNotBlank(trackingNumber))
                .collect(Collectors.joining(","));
        return hualeiRestemplateClient.refreshHualeiPackageStatus(trackingNumbers, hualeiConfiguration);


    }

    public List<HualeiTrackNumberResponseData> refreshHualeiPackageTrackingNumber(String[] orderIdArray, HualeiConfiguration hualeiConfiguration) {

        String orderIds = Arrays.stream(orderIdArray).filter(orderId -> Strings.isNotBlank(orderId))
                .collect(Collectors.joining(","));
        return hualeiRestemplateClient.refreshHualeiPackageTrackingNumbers(orderIds, hualeiConfiguration);


    }
}

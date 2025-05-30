/**
 * Copyright 2019
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

package com.garyzhangscm.cwms.outbound.controller;

import com.easypost.exception.EasyPostException;
import com.easypost.model.Rate;
import com.easypost.model.Shipment;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.garyzhangscm.cwms.outbound.ResponseBodyWrapper;
import com.garyzhangscm.cwms.outbound.model.*;
import com.garyzhangscm.cwms.outbound.service.*;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.List;

@RestController
public class ParcelShippingController {

    private static final Logger logger = LoggerFactory.getLogger(ParcelShippingController.class);

    @Autowired
    ShipEngineService shipEngineService;
    @Autowired
    EasyPostService easyPostService;

    @Autowired
    private ParcelPackageService parcelPackageService;
    @Autowired
    private FileService fileService;
    @Autowired
    private UploadFileService uploadFileService;
    @Autowired
    private OrderDocumentService orderDocumentService;

    @RequestMapping(value="/parcel/ship-engine/rate", method = RequestMethod.GET)
    public ResponseBodyWrapper getShipEngineRate(@RequestParam Long warehouseId) throws JsonProcessingException {
        logger.debug("Start to find the best rate for the shipment from warehouse {}", warehouseId);

        shipEngineService.getShippingRates();
        return ResponseBodyWrapper.success("get ship engine rate!");
    }


    @RequestMapping(value="/parcel/easy-post/shipment", method = RequestMethod.POST)
    public Shipment createEasyPostShipment(@RequestParam Long warehouseId,
                                           @RequestParam Long orderId,
                                           @RequestParam Double length,
                                           @RequestParam Double width,
                                           @RequestParam Double height,
                                           @RequestParam Double weight)
            throws EasyPostException {

        return easyPostService.createEasyPostShipment(warehouseId,
                orderId, length, width, height, weight);
    }
    @RequestMapping(value="/parcel/easy-post/shipment-confirm", method = RequestMethod.POST)
    public Shipment confirmEasyPostShipment(@RequestParam Long warehouseId,
                                            @RequestParam Long orderId,
                                            @RequestParam String shipmentId,
                                            @RequestBody Rate rate) throws JsonProcessingException, EasyPostException {

        return easyPostService.confirmEasyPostShipment(warehouseId, orderId,
                shipmentId, rate);
    }

    @RequestMapping(value="/parcel/packages", method = RequestMethod.GET)
    public List<ParcelPackage> findParcelPackages(@RequestParam Long warehouseId,
                                                  @RequestParam(name = "orderId", required = false) Long orderId,
                                                  @RequestParam(name = "orderNumber", required = false) String orderNumber,
                                                  @RequestParam(name = "trackingCode", required = false) String trackingCode,
                                                  @RequestParam(name = "undeliveredPackageOnly", required = false) Boolean undeliveredPackageOnly,
                                                  @RequestParam(name = "emptyTrackingCodeOnly", required = false) Boolean emptyTrackingCodeOnly,
                                                  @RequestParam(name = "requestSystem", required = false, defaultValue = "") String requestSystemName,
                                                  @RequestParam(name = "hualeiOrderId", required = false) String hualeiOrderId) {
        ParcelPackageRequestSystem requestSystem =
                Strings.isBlank(requestSystemName) ? null : ParcelPackageRequestSystem.valueOf(requestSystemName);
        return parcelPackageService.findAll(warehouseId, orderId, orderNumber, trackingCode,
                undeliveredPackageOnly, emptyTrackingCodeOnly, requestSystem, hualeiOrderId);
    }

    @RequestMapping(value="/parcel/packages", method = RequestMethod.PUT)
    public ParcelPackage addParcelPackage(@RequestParam Long warehouseId,
                                          @RequestParam Long orderId,
                                          @RequestBody ParcelPackage parcelPackage) {
        return parcelPackageService.addParcelPackage(warehouseId, orderId, parcelPackage);
    }

    @BillableEndpoint
    @RequestMapping(method=RequestMethod.POST, value="/parcel/packages/upload")
    public ResponseBodyWrapper uploadParcelPackage(Long companyId,
                                                   Long warehouseId,
                                                   @RequestParam(name = "ignoreUnknownFields", defaultValue = "false", required = false) Boolean ignoreUnknownFields,
                                                         @RequestParam("file") MultipartFile file) throws IOException {

        try {

            File localFile = uploadFileService.convertToCSVFile(
                    companyId, warehouseId, "parcel-packages", fileService.saveFile(file), ignoreUnknownFields);

            String fileUploadProgressKey = parcelPackageService.saveParcelPackageData(warehouseId, localFile);
            return  ResponseBodyWrapper.success(fileUploadProgressKey);
        }
        catch (Exception ex) {
            return new ResponseBodyWrapper(-1, ex.getMessage(), "");
        }

    }
    @RequestMapping(method=RequestMethod.GET, value="/parcel/packages/upload/progress")
    public ResponseBodyWrapper getParcelPackageFileUploadProgress(Long warehouseId,
                                                                        String key) {



        return  ResponseBodyWrapper.success(
                String.format("%.2f",parcelPackageService.getFileUploadProgress(key)));
    }
    @RequestMapping(method=RequestMethod.GET, value="/parcel/packages/upload/result")
    public List<FileUploadResult> getParcelPackageFileUploadResult(Long warehouseId,
                                                                         String key) {


        return parcelPackageService.getFileUploadResult(warehouseId, key);
    }

    @RequestMapping(value="/parcel/packages/{id}", method = RequestMethod.DELETE)
    public ResponseBodyWrapper<String> removeParcelPackage(@RequestParam Long warehouseId,
                                                           @PathVariable Long id) {
        parcelPackageService.removeParcelPackage(warehouseId, id);

        return ResponseBodyWrapper.success("parcel package with id " + id + " is removed!");
    }

    @RequestMapping(value="/parcel/packages/{warehouseId}/{orderId}/labels/upload", method = RequestMethod.POST)
    public ResponseBodyWrapper<String> uploadParcelLabel(@PathVariable Long warehouseId,
                                                         @PathVariable Long orderId,
                                                         @RequestParam("file") MultipartFile file) throws IOException {

        return ResponseBodyWrapper.success(orderDocumentService.uploadOrderDocument(warehouseId, orderId, file));
    }
}

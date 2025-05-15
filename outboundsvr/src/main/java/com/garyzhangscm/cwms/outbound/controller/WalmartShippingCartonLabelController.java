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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.garyzhangscm.cwms.outbound.ResponseBodyWrapper;
import com.garyzhangscm.cwms.outbound.model.*;
import com.garyzhangscm.cwms.outbound.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;

@RestController
public class WalmartShippingCartonLabelController {

    private static final Logger logger = LoggerFactory.getLogger(WalmartShippingCartonLabelController.class);

    @Autowired
    WalmartShippingCartonLabelService walmartShippingCartonLabelService;
    @Autowired
    FileService fileService;
    @Autowired
    private UploadFileService uploadFileService;


    @ClientValidationEndpoint
    @RequestMapping(value="/walmart-shipping-carton-labels", method = RequestMethod.GET)
    public List<WalmartShippingCartonLabel> findAllWalmartShippingCartonLabel(
            @RequestParam Long warehouseId,
            @RequestParam(name="SSCC18", required = false, defaultValue = "") String SSCC18,
            @RequestParam(name="SSCC18s", required = false, defaultValue = "") String SSCC18s,
            @RequestParam(name="poNumber", required = false, defaultValue = "") String poNumber,
            @RequestParam(name="type", required = false, defaultValue = "") String type,
            @RequestParam(name="dept", required = false, defaultValue = "") String dept,
            @RequestParam(name="itemNumber", required = false, defaultValue = "") String itemNumber,
            @RequestParam(name="palletPickLabelContentId", required = false, defaultValue = "") Long palletPickLabelContentId,
            @RequestParam(name="notPrinted", required = false, defaultValue = "") Boolean notPrinted,
            @RequestParam(name="notAssignedToPalletPickLabel", required = false, defaultValue = "") Boolean notAssignedToPalletPickLabel,
            @RequestParam(name="count", required = false, defaultValue = "") Integer count) {
        return walmartShippingCartonLabelService.findAll(warehouseId,
                SSCC18,SSCC18s, poNumber, type, dept, itemNumber,
                palletPickLabelContentId, notPrinted, notAssignedToPalletPickLabel, count);
    }



    @RequestMapping(value="/walmart-shipping-carton-labels/{id}", method = RequestMethod.GET)
    public WalmartShippingCartonLabel findWalmartShippingCartonLabel(@PathVariable Long id) {
        return walmartShippingCartonLabelService.findById(id);
    }

    @BillableEndpoint
    @RequestMapping(method=RequestMethod.POST, value="/walmart-shipping-carton-labels/upload")
    public ResponseBodyWrapper updateWalmartShippingCartonLabels(Long companyId, Long warehouseId,
                                                                 @RequestParam(name = "ignoreUnknownFields", defaultValue = "false", required = false) Boolean ignoreUnknownFields,
                                            @RequestParam("file") MultipartFile file) throws IOException {

        try {

            File localFile = uploadFileService.convertToCSVFile(
                    companyId, warehouseId, "walmart-shipping-carton-labels", fileService.saveFile(file), ignoreUnknownFields);

            String fileUploadProgressKey = walmartShippingCartonLabelService.updateWalmartShippingCartonLabels(warehouseId, localFile);
            return  ResponseBodyWrapper.success(fileUploadProgressKey);
        }
        catch (Exception ex) {
            return new ResponseBodyWrapper(-1, ex.getMessage(), "");
        }
    }
    @RequestMapping(method=RequestMethod.GET, value="/walmart-shipping-carton-labels/upload/progress")
    public ResponseBodyWrapper getWalmartShippingCartonLabelsFileUploadProgress(Long warehouseId,
                                                            String key) throws IOException {



        return  ResponseBodyWrapper.success(
                String.format("%.2f",walmartShippingCartonLabelService.getWalmartShippingCartonLabelsFileUploadProgress(key)));
    }
    @RequestMapping(method=RequestMethod.GET, value="/walmart-shipping-carton-labels/upload/result")
    public List<FileUploadResult> getWalmartShippingCartonLabelsFileUploadResult(Long warehouseId,
                                                             String key) throws IOException {


        return walmartShippingCartonLabelService.getWalmartShippingCartonLabelsFileUploadResult(warehouseId, key);
    }

    @RequestMapping(method=RequestMethod.POST, value="/walmart-shipping-carton-labels/generate-label")
    public ReportHistory generateWalmartShippingCartonLabels(Long warehouseId,
                                                             String SSCC18s,
                                                             @RequestParam(name = "copies", defaultValue = "1", required = false) int copies,
                                                             @RequestParam(name = "locale", defaultValue = "", required = false) String locale)   {


        return walmartShippingCartonLabelService.generateWalmartShippingCartonLabels(
                warehouseId, SSCC18s, copies, locale);

    }
}

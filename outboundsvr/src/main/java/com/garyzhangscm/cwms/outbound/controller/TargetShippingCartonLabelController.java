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

import com.garyzhangscm.cwms.outbound.ResponseBodyWrapper;
import com.garyzhangscm.cwms.outbound.model.*;
import com.garyzhangscm.cwms.outbound.service.FileService;
import com.garyzhangscm.cwms.outbound.service.TargetShippingCartonLabelService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.List;

@RestController
public class TargetShippingCartonLabelController {

    private static final Logger logger = LoggerFactory.getLogger(TargetShippingCartonLabelController.class);

    @Autowired
    TargetShippingCartonLabelService targetShippingCartonLabelService;
    @Autowired
    FileService fileService;


    @ClientValidationEndpoint
    @RequestMapping(value="/target-shipping-carton-labels", method = RequestMethod.GET)
    public List<TargetShippingCartonLabel> findAllTargetShippingCartonLabel(
            @RequestParam Long warehouseId,
            @RequestParam(name="SSCC18", required = false, defaultValue = "") String SSCC18,
            @RequestParam(name="SSCC18s", required = false, defaultValue = "") String SSCC18s,
            @RequestParam(name="poNumber", required = false, defaultValue = "") String poNumber,
            @RequestParam(name="itemNumber", required = false, defaultValue = "") String itemNumber,
            @RequestParam(name="palletPickLabelContentId", required = false, defaultValue = "") Long palletPickLabelContentId,
            @RequestParam(name="notPrinted", required = false, defaultValue = "") Boolean notPrinted,
            @RequestParam(name="notAssignedToPalletPickLabel", required = false, defaultValue = "") Boolean notAssignedToPalletPickLabel,
            @RequestParam(name="count", required = false, defaultValue = "") Integer count) {
        return targetShippingCartonLabelService.findAll(warehouseId,
                SSCC18,SSCC18s, poNumber,   itemNumber,
                palletPickLabelContentId, notPrinted, notAssignedToPalletPickLabel, count);
    }



    @RequestMapping(value="/target-shipping-carton-labels/{id}", method = RequestMethod.GET)
    public TargetShippingCartonLabel findTargetShippingCartonLabel(@PathVariable Long id) {
        return targetShippingCartonLabelService.findById(id);
    }

    @BillableEndpoint
    @RequestMapping(method=RequestMethod.POST, value="/target-shipping-carton-labels/upload")
    public ResponseBodyWrapper updateTargetShippingCartonLabels(Long warehouseId,
                                            @RequestParam("file") MultipartFile file) throws IOException {


        File localFile = fileService.processUploadedFile("target-shipping-carton-labels", file);
        try {
            fileService.validateCSVFile(warehouseId, "target-shipping-carton-labels", localFile);
        }
        catch (Exception ex) {
            return new ResponseBodyWrapper(-1, ex.getMessage(), "");
        }
        String fileUploadProgressKey = targetShippingCartonLabelService.updateTargetShippingCartonLabels(warehouseId, localFile);
        return  ResponseBodyWrapper.success(fileUploadProgressKey);
    }
    @RequestMapping(method=RequestMethod.GET, value="/target-shipping-carton-labels/upload/progress")
    public ResponseBodyWrapper getTargetShippingCartonLabelsFileUploadProgress(Long warehouseId,
                                                            String key) throws IOException {



        return  ResponseBodyWrapper.success(
                String.format("%.2f",targetShippingCartonLabelService.getTargetShippingCartonLabelsFileUploadProgress(key)));
    }
    @RequestMapping(method=RequestMethod.GET, value="/target-shipping-carton-labels/upload/result")
    public List<FileUploadResult> getTargetShippingCartonLabelsFileUploadResult(Long warehouseId,
                                                             String key) throws IOException {


        return targetShippingCartonLabelService.getTargetShippingCartonLabelsFileUploadResult(warehouseId, key);
    }

    @RequestMapping(method=RequestMethod.POST, value="/target-shipping-carton-labels/generate-label")
    public ReportHistory generateTargetShippingCartonLabels(Long warehouseId,
                                                             String SSCC18s,
                                                             @RequestParam(name = "copies", defaultValue = "1", required = false) int copies,
                                                             @RequestParam(name = "locale", defaultValue = "", required = false) String locale)   {


        return targetShippingCartonLabelService.generateTargetShippingCartonLabels(
                warehouseId, SSCC18s, copies, locale);

    }

    @RequestMapping(value="/target-shipping-carton-labels/{id}", method = RequestMethod.DELETE)
    public ResponseBodyWrapper<String> removeTargetShippingCartonLabel(@RequestParam Long warehouseId,
                                                                       @PathVariable Long id) {
        targetShippingCartonLabelService.removeTargetShippingCartonLabel(id);

        return ResponseBodyWrapper.success("target shipping carton label with id " + id + " is removed");
    }

    @RequestMapping(value="/target-shipping-carton-labels/remove-by-id-list", method = RequestMethod.DELETE)
    public ResponseBodyWrapper<String> removeTargetShippingCartonLabels(@RequestParam Long warehouseId,
                                                                        @RequestParam String ids) {
        targetShippingCartonLabelService.removeTargetShippingCartonLabels(ids);

        return ResponseBodyWrapper.success("target shipping carton labels with id list " + ids + " are removed");
    }
}

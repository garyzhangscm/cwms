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
import com.garyzhangscm.cwms.outbound.model.BillableEndpoint;
import com.garyzhangscm.cwms.outbound.model.FileUploadResult;
import com.garyzhangscm.cwms.outbound.model.TrailerAppointment;
import com.garyzhangscm.cwms.outbound.service.FileService;
import com.garyzhangscm.cwms.outbound.service.TrailerAppointmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.List;

@RestController
public class TrailerAppointmentController {
    @Autowired
    TrailerAppointmentService trailerAppointmentService;

    @Autowired
    private FileService fileService;

    @BillableEndpoint
    @RequestMapping(value="/trailer-appointments/{id}/assign-stops-shipments-orders", method = RequestMethod.POST)
    public TrailerAppointment assignStopShipmentOrdersToTrailerAppointment(
            @PathVariable Long id,
            @RequestParam Long warehouseId,
            @RequestParam(name = "stopIdList", required = false, defaultValue = "") String stopIdList,
            @RequestParam(name = "shipmentIdList", required = false, defaultValue = "") String shipmentIdList,
            @RequestParam(name = "orderIdList", required = false, defaultValue = "") String orderIdList) {
        return trailerAppointmentService.assignStopShipmentOrdersToTrailerAppointment(
                id, stopIdList, shipmentIdList, orderIdList
        );
    }

    @BillableEndpoint
    @RequestMapping(value="/trailer-appointments/{id}/allocate", method = RequestMethod.POST)
    public TrailerAppointment allocateTrailerAppointment(
            @PathVariable Long id,
            @RequestParam Long warehouseId) {
        return trailerAppointmentService.allocateTrailerAppointment(warehouseId, id);
    }

    @BillableEndpoint
    @RequestMapping(value="/trailer-appointments/{id}/complete", method = RequestMethod.POST)
    public TrailerAppointment completeTrailerAppointment(
            @PathVariable Long id,
            @RequestParam Long warehouseId) {
        return trailerAppointmentService.completeTrailerAppointment(warehouseId, id);
    }

    @BillableEndpoint
    @RequestMapping(method=RequestMethod.POST, value="/trailer-appointments/shipping/upload")
    public ResponseBodyWrapper uploadShippingTrailerAppointments(Long warehouseId,
                                                                 @RequestParam("file") MultipartFile file) throws IOException {


        File localFile = fileService.saveFile(file);
        fileService.validateCSVFile(warehouseId, "shipping-trailer-appointment", localFile);
        String fileUploadProgressKey = trailerAppointmentService.saveShippingTrailerAppointmentData(warehouseId, localFile);
        return  ResponseBodyWrapper.success(fileUploadProgressKey);
    }

    @RequestMapping(method=RequestMethod.GET, value="/trailer-appointments/shipping/upload/progress")
    public ResponseBodyWrapper getShippingTrailerAppoitnmentFileUploadProgress(Long warehouseId,
                                                                               String key) throws IOException {



        return  ResponseBodyWrapper.success(
                String.format("%.2f",trailerAppointmentService.getShippingTrailerAppoitnmentFileUploadProgress(key)));
    }
    @RequestMapping(method=RequestMethod.GET, value="/trailer-appointments/shipping/upload/result")
    public List<FileUploadResult> getShippingTrailerAppointmentFileUploadResult(Long warehouseId,
                                                                                String key) throws IOException {


        return trailerAppointmentService.getShippingTrailerAppointmentFileUploadResult(warehouseId, key);
    }

}

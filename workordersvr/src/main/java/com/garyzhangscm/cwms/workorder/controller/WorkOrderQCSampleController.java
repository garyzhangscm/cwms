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

package com.garyzhangscm.cwms.workorder.controller;


import com.garyzhangscm.cwms.workorder.ResponseBodyWrapper;
import com.garyzhangscm.cwms.workorder.model.BillableEndpoint;
import com.garyzhangscm.cwms.workorder.model.Mould;
import com.garyzhangscm.cwms.workorder.model.WorkOrderQCSample;
import com.garyzhangscm.cwms.workorder.service.MouldService;
import com.garyzhangscm.cwms.workorder.service.WorkOrderQCSampleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

@RestController
public class WorkOrderQCSampleController {
    @Autowired
    WorkOrderQCSampleService workOrderQCSampleService;

    @RequestMapping(value="/qc-samples", method = RequestMethod.GET)
    public List<WorkOrderQCSample> findAllWorkOrderQCSamples(
            @RequestParam Long warehouseId,
            @RequestParam(name="number", required = false, defaultValue = "") String number,
            @RequestParam(name="productionLineAssignmentId", required = false, defaultValue = "") Long productionLineAssignmentId) {
        return workOrderQCSampleService.findAll(warehouseId, number, productionLineAssignmentId);
    }



    @BillableEndpoint
    @RequestMapping(value="/qc-samples", method = RequestMethod.PUT)
    public WorkOrderQCSample addWorkOrderQCSample(@RequestBody WorkOrderQCSample workOrderQCSample) {
        return workOrderQCSampleService.addWorkOrderQCSample(workOrderQCSample);
    }


    @RequestMapping(value="/qc-samples/{id}", method = RequestMethod.GET)
    public WorkOrderQCSample findWorkOrderQCSample(@PathVariable Long id) {

        return workOrderQCSampleService.findById(id);
    }

    @BillableEndpoint
    @RequestMapping(value="/qc-samples/{id}", method = RequestMethod.POST)
    public WorkOrderQCSample changeWorkOrderQCSample(@PathVariable Long id,
                                               @RequestBody WorkOrderQCSample workOrderQCSample){
        return workOrderQCSampleService.changeWorkOrderQCSample(id, workOrderQCSample);
    }

    @BillableEndpoint
    @RequestMapping(value="/qc-samples/{id}", method = RequestMethod.DELETE)
    public void removeWorkOrderQCSample(@PathVariable Long id) {
        workOrderQCSampleService.delete(id);
    }


    @RequestMapping(value="/qc-samples/images/{warehouseId}/{productionLineAssignmentId}/{fileName}", method = RequestMethod.GET)
    public ResponseEntity<Resource> getWorkOrderQCSampleImage(@PathVariable Long warehouseId,
                                                              @PathVariable Long productionLineAssignmentId,
                                                              @PathVariable String fileName) throws FileNotFoundException {

        File imageFile = workOrderQCSampleService.getWorkOrderQCSampleImage(warehouseId, productionLineAssignmentId, fileName);

        InputStreamResource resource
                = new InputStreamResource(new FileInputStream(imageFile));
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment;fileName=" + fileName)
                .contentLength(imageFile.length())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);

    }

    @BillableEndpoint
    @RequestMapping(method=RequestMethod.POST, value="/qc-samples/{productionLineAssignmentId}/images")
    public ResponseBodyWrapper uploadQCSampleImage(
            @PathVariable Long productionLineAssignmentId,
            @RequestParam("file") MultipartFile file) throws IOException {


        String filePath = workOrderQCSampleService.uploadQCSampleImage(productionLineAssignmentId, file);
        return  ResponseBodyWrapper.success(filePath);
    }



}

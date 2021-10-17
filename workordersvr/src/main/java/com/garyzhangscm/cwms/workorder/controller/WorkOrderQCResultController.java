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
import com.garyzhangscm.cwms.workorder.model.WorkOrderQCResult;
import com.garyzhangscm.cwms.workorder.model.WorkOrderQCSample;
import com.garyzhangscm.cwms.workorder.service.WorkOrderQCResultService;
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
public class WorkOrderQCResultController {
    @Autowired
    WorkOrderQCResultService workOrderQCResultService;

    @RequestMapping(value="/qc-results", method = RequestMethod.GET)
    public List<WorkOrderQCResult> findAllWorkOrderQCResults(
            @RequestParam Long warehouseId,
            @RequestParam(name="number", required = false, defaultValue = "") String number,
            @RequestParam(name="workOrderSampleNumber", required = false, defaultValue = "") String workOrderSampleNumber,
            @RequestParam(name="productionLineAssignmentId", required = false, defaultValue = "") Long productionLineAssignmentId,
            @RequestParam(name="productionLineId", required = false, defaultValue = "") Long productionLineId,
            @RequestParam(name="workOrderId", required = false, defaultValue = "") Long workOrderId,
            @RequestParam(name="workOrderNumber", required = false, defaultValue = "") String workOrderNumber) {
        return workOrderQCResultService.findAll(warehouseId, number,
                workOrderSampleNumber, productionLineAssignmentId,
                productionLineId, workOrderId, workOrderNumber);
    }



    @BillableEndpoint
    @RequestMapping(value="/qc-results", method = RequestMethod.PUT)
    public WorkOrderQCResult addWorkOrderQCResult(@RequestBody WorkOrderQCResult workOrderQCResult) {
        return workOrderQCResultService.addWorkOrderQCResult(workOrderQCResult);
    }


    @RequestMapping(value="/qc-results/{id}", method = RequestMethod.GET)
    public WorkOrderQCResult findWorkOrderQCResult(@PathVariable Long id) {

        return workOrderQCResultService.findById(id);
    }


    @BillableEndpoint
    @RequestMapping(value="/qc-results/{id}", method = RequestMethod.DELETE)
    public void removeWorkOrderQCResult(@PathVariable Long id) {
        workOrderQCResultService.delete(id);
    }





}

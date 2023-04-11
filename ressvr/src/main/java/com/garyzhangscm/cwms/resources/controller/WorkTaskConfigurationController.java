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

package com.garyzhangscm.cwms.resources.controller;

import com.garyzhangscm.cwms.resources.ResponseBodyWrapper;
import com.garyzhangscm.cwms.resources.exception.RequestValidationFailException;
import com.garyzhangscm.cwms.resources.model.BillableEndpoint;
import com.garyzhangscm.cwms.resources.model.WorkTask;
import com.garyzhangscm.cwms.resources.model.WorkTaskConfiguration;
import com.garyzhangscm.cwms.resources.service.WorkTaskConfigurationService;
import com.garyzhangscm.cwms.resources.service.WorkTaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;

@RestController
public class WorkTaskConfigurationController {
    @Autowired
    WorkTaskConfigurationService workTaskConfigurationService;

    @RequestMapping(value="/work-task-configurations", method = RequestMethod.GET)
    public List<WorkTaskConfiguration> findAllWorkTasks(@RequestParam Long warehouseId,
                                                        @RequestParam(name = "sourceLocationGroupTypeId", required = false, defaultValue = "") Long sourceLocationGroupTypeId,
                                                        @RequestParam(name = "sourceLocationGroupTypeName", required = false, defaultValue = "") String sourceLocationGroupTypeName,
                                                        @RequestParam(name = "sourceLocationGroupId", required = false, defaultValue = "") Long sourceLocationGroupId,
                                                        @RequestParam(name = "sourceLocationGroupName", required = false, defaultValue = "") String sourceLocationGroupName,
                                                        @RequestParam(name = "sourceLocationId", required = false, defaultValue = "") Long sourceLocationId,
                                                        @RequestParam(name = "sourceLocationName", required = false, defaultValue = "") String sourceLocationName,
                                                        @RequestParam(name = "destinationLocationGroupTypeId", required = false, defaultValue = "") Long destinationLocationGroupTypeId,
                                                        @RequestParam(name = "destinationLocationGroupTypeName", required = false, defaultValue = "") String destinationLocationGroupTypeName,
                                                        @RequestParam(name = "destinationLocationGroupId", required = false, defaultValue = "") Long destinationLocationGroupId,
                                                        @RequestParam(name = "destinationLocationGroupName", required = false, defaultValue = "") String destinationLocationGroupName,
                                                        @RequestParam(name = "destinationLocationId", required = false, defaultValue = "") Long destinationLocationId,
                                                        @RequestParam(name = "destinationLocationName", required = false, defaultValue = "") String destinationLocationName,
                                                        @RequestParam(name = "workTaskType", required = false, defaultValue = "") String workTaskType,
                                                        @RequestParam(name = "operationTypeName", required = false, defaultValue = "") String operationTypeName) {
        return workTaskConfigurationService.findAll(warehouseId,
                sourceLocationGroupTypeId, sourceLocationGroupTypeName,
                sourceLocationGroupId, sourceLocationGroupName,
                sourceLocationId, sourceLocationName,
                destinationLocationGroupTypeId, destinationLocationGroupTypeName,
                destinationLocationGroupId, destinationLocationGroupName,
                destinationLocationId, destinationLocationName,
                workTaskType, operationTypeName);
    }


    @RequestMapping(value="/work-task-configurations/{id}", method = RequestMethod.GET)
    public WorkTaskConfiguration findWorkTaskConfiguration(@PathVariable Long id) {
        return workTaskConfigurationService.findById(id);
    }


    @BillableEndpoint
    @RequestMapping(value="/work-task-configurations/{id}", method = RequestMethod.DELETE)
    public ResponseBodyWrapper<String> removeWorkTaskConfiguration(@RequestParam Long warehouseId,
                                                           @PathVariable Long id) {
        workTaskConfigurationService.remove(id);

        return ResponseBodyWrapper.success("work task configuration " + id + " is removed!");
    }

    @BillableEndpoint
    @RequestMapping(value="/work-task-configurations", method = RequestMethod.PUT)
    public WorkTaskConfiguration addWorkTaskConfiguration(Long warehouseId,
                                @RequestBody WorkTaskConfiguration workTaskConfiguration) {
        return workTaskConfigurationService.addWorkTaskConfiguration(workTaskConfiguration);
    }

    @BillableEndpoint
    @RequestMapping(value="/work-task-configurations/{id}", method = RequestMethod.POST)
    public WorkTaskConfiguration changeWorkTaskConfiguration(Long warehouseId,
                                                             @PathVariable Long id, @RequestBody WorkTaskConfiguration workTaskConfiguration) {
        if (Objects.nonNull(workTaskConfiguration.getId()) && !Objects.equals(workTaskConfiguration.getId(), id)) {
            throw RequestValidationFailException.raiseException(
                    "id(in URI): " + id + "; workTaskConfiguration.getId(): " + workTaskConfiguration.getId());
        }
        return workTaskConfigurationService.changeWorkTaskConfiguration(id, workTaskConfiguration);
    }

}

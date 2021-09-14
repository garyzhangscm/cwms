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

package com.garyzhangscm.cwms.common.controller;

import com.garyzhangscm.cwms.common.exception.RequestValidationFailException;
import com.garyzhangscm.cwms.common.model.BillableEndpoint;
import com.garyzhangscm.cwms.common.model.Carrier;
import com.garyzhangscm.cwms.common.model.WorkTask;
import com.garyzhangscm.cwms.common.service.CarrierService;
import com.garyzhangscm.cwms.common.service.WorkTaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class WorkTaskController {
    @Autowired
    WorkTaskService workTaskService;

    @RequestMapping(value="/work-tasks", method = RequestMethod.GET)
    public List<WorkTask> findAllWorkTasks(@RequestParam Long warehouseId,
                                           @RequestParam(name = "number", required = false, defaultValue = "") String number,
                                           @RequestParam(name = "workType", required = false, defaultValue = "") String workType,
                                           @RequestParam(name = "workStatus", required = false, defaultValue = "") String workStatus,
                                           @RequestParam(name = "lpn", required = false, defaultValue = "") String lpn,
                                           @RequestParam(name = "sourceLocationName", required = false, defaultValue = "") String sourceLocationName,
                                           @RequestParam(name = "destinationLocationName", required = false, defaultValue = "") String destinationLocationName,
                                           @RequestParam(name = "assignedUserName", required = false, defaultValue = "") String assignedUserName,
                                           @RequestParam(name = "assignedRoleName", required = false, defaultValue = "") String assignedRoleName,
                                           @RequestParam(name = "assignedWorkingTeamName", required = false, defaultValue = "") String assignedWorkingTeamName,
                                           @RequestParam(name = "currentUserName", required = false, defaultValue = "") String currentUserName,
                                           @RequestParam(name = "completeUserName", required = false, defaultValue = "") String completeUserName,
                                           @RequestParam(name = "workTaskIds", required = false, defaultValue = "") String workTaskIds) {
        return workTaskService.findAll(warehouseId, number,
                workType, workStatus, lpn,
                sourceLocationName, destinationLocationName,
                assignedUserName, assignedRoleName, assignedWorkingTeamName,
                currentUserName, completeUserName, workTaskIds);
    }

    @BillableEndpoint
    @RequestMapping(value="/work-tasks", method = RequestMethod.DELETE)
    public List<WorkTask> removeAllWorkTasks(@RequestParam Long warehouseId,
                                           @RequestParam(name = "number", required = false, defaultValue = "") String number,
                                           @RequestParam(name = "workType", required = false, defaultValue = "") String workType,
                                           @RequestParam(name = "workStatus", required = false, defaultValue = "") String workStatus,
                                           @RequestParam(name = "lpn", required = false, defaultValue = "") String lpn,
                                           @RequestParam(name = "sourceLocationName", required = false, defaultValue = "") String sourceLocationName,
                                           @RequestParam(name = "destinationLocationName", required = false, defaultValue = "") String destinationLocationName,
                                           @RequestParam(name = "assignedUserName", required = false, defaultValue = "") String assignedUserName,
                                           @RequestParam(name = "assignedRoleName", required = false, defaultValue = "") String assignedRoleName,
                                           @RequestParam(name = "assignedWorkingTeamName", required = false, defaultValue = "") String assignedWorkingTeamName,
                                           @RequestParam(name = "currentUserName", required = false, defaultValue = "") String currentUserName,
                                           @RequestParam(name = "completeUserName", required = false, defaultValue = "") String completeUserName,
                                           @RequestParam(name = "workTaskIds", required = false, defaultValue = "") String workTaskIds) {
        return workTaskService.removeAllWorkTasks(warehouseId, number,
                workType, workStatus, lpn,
                sourceLocationName, destinationLocationName,
                assignedUserName, assignedRoleName, assignedWorkingTeamName,
                currentUserName, completeUserName, workTaskIds);
    }

    @RequestMapping(value="/work-tasks/{id}", method = RequestMethod.GET)
    public WorkTask findWorkTask(@PathVariable Long id) {
        return workTaskService.findById(id);
    }


    @BillableEndpoint
    @RequestMapping(value="/work-tasks/{id}", method = RequestMethod.DELETE)
    public WorkTask removeWorkTask(@PathVariable Long id) {
        return workTaskService.removeWorkTask(id);
    }

    @BillableEndpoint
    @RequestMapping(value="/work-tasks", method = RequestMethod.POST)
    public WorkTask addWorkTask(@RequestBody WorkTask workTask) {
        return workTaskService.addWorkTask(workTask);
    }

    @BillableEndpoint
    @RequestMapping(value="/work-tasks/{id}", method = RequestMethod.PUT)
    public WorkTask changeWorkTask(@PathVariable Long id, @RequestBody WorkTask workTask) {
        if (workTask.getId() != null && workTask.getId() != id) {
            throw RequestValidationFailException.raiseException(
                    "id(in URI): " + id + "; workTask.getId(): " + workTask.getId());
        }
        return workTaskService.changeWorkTask(workTask);
    }

    @BillableEndpoint
    @RequestMapping(value="/work-tasks/assignment", method = RequestMethod.POST)
    public List<WorkTask> assignWorkTasks(@RequestParam String workTaskIds,
                                         @RequestParam(name = "username", required = false, defaultValue = "") String username,
                                         @RequestParam(name = "rolename", required = false, defaultValue = "") String rolename,
                                         @RequestParam(name = "workingTeamName", required = false, defaultValue = "") String workingTeamName) {
        return workTaskService.assignWorkTasks(workTaskIds, username, rolename, workingTeamName);
    }

    @BillableEndpoint
    @RequestMapping(value="/work-tasks/deassignment", method = RequestMethod.POST)
    public List<WorkTask> deassignWorkTasks(@RequestParam String workTaskIds) {
        return workTaskService.deassignWorkTasks(workTaskIds);
    }

}

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


import com.garyzhangscm.cwms.workorder.model.*;
import com.garyzhangscm.cwms.workorder.service.MouldService;
import com.garyzhangscm.cwms.workorder.service.WorkOrderLaborService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class WorkOrderLaborController {
    @Autowired
    WorkOrderLaborService workOrderLaborService;

    @RequestMapping(value="/labors", method = RequestMethod.GET)
    public List<WorkOrderLabor> findAllLabors(@RequestParam Long warehouseId,
                                              @RequestParam(name="productionLineId", required = false, defaultValue = "") Long productionLineId,
                                              @RequestParam(name="workOrderLaborStatus", required = false, defaultValue = "") String workOrderLaborStatus,
                                              @RequestParam(name="username", required = false, defaultValue = "") String username) {
        return workOrderLaborService.findAll(warehouseId, productionLineId, workOrderLaborStatus, username);
    }


    @RequestMapping(value="/labor/checked_in_production_lines", method = RequestMethod.GET)
    public List<ProductionLine> findAllCheckedInProductionLines(@RequestParam Long warehouseId,
                                                                @RequestParam String username) {
        return workOrderLaborService.findAllCheckedInProductionLines(warehouseId, username);
    }

    @RequestMapping(value="/labor/checked_in_users", method = RequestMethod.GET)
    public List<User> findAllCheckedInUsers(@RequestParam Long warehouseId,
                                                @RequestParam Long productionLineId) {
        return workOrderLaborService.findAllCheckedInUsers(warehouseId, productionLineId);
    }


    @BillableEndpoint
    @RequestMapping(value="/labor/check_in_user", method = RequestMethod.POST)
    public WorkOrderLabor checkInUser(@RequestParam Long warehouseId,
                                    @RequestParam Long productionLineId,
                                    @RequestParam String username,
                                    @RequestParam String currentUsername) {
        return workOrderLaborService.checkInUser(warehouseId, productionLineId, username, currentUsername);
    }

    @BillableEndpoint
    @RequestMapping(value="/labor/check_out_user", method = RequestMethod.POST)
    public WorkOrderLabor checkOutUser(@RequestParam Long warehouseId,
                                      @RequestParam Long productionLineId,
                                      @RequestParam String username,
                                      @RequestParam String currentUsername) {
        return workOrderLaborService.checkOutUser(warehouseId, productionLineId, username, currentUsername);
    }


    @BillableEndpoint
    @RequestMapping(value="/labors", method = RequestMethod.PUT)
    public WorkOrderLabor addLabors(@RequestBody WorkOrderLabor workOrderLabor,
                                    String currentUsername) {
        return workOrderLaborService.addLabor(workOrderLabor, currentUsername);
    }


    @RequestMapping(value="/labors/{id}", method = RequestMethod.GET)
    public WorkOrderLabor findLabor(@PathVariable Long id) {

        return workOrderLaborService.findById(id);
    }

    @BillableEndpoint
    @RequestMapping(value="/labors/{id}", method = RequestMethod.POST)
    public WorkOrderLabor changeLabor(@PathVariable Long id,
                                      @RequestBody WorkOrderLabor workOrderLabor,
                                      String currentUsername){
        return workOrderLaborService.changeLabor(id, workOrderLabor, currentUsername);
    }

    @BillableEndpoint
    @RequestMapping(value="/labors/{id}", method = RequestMethod.DELETE)
    public void removeLabor(@PathVariable Long id) {
        workOrderLaborService.delete(id);
    }


}

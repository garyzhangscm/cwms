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
import com.garyzhangscm.cwms.workorder.service.WorkOrderKPIService;
import com.garyzhangscm.cwms.workorder.service.WorkOrderLineService;
import com.garyzhangscm.cwms.workorder.service.WorkOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class WorkOrderKPIController {
    @Autowired
    WorkOrderKPIService workOrderKPIService;


    @RequestMapping(value="/work-order-kpis", method = RequestMethod.GET)
    public List<WorkOrderKPI> findAllWorkOrderKPIs(@RequestParam Long warehouseId,
                                                   @RequestParam(name="workOrderNumber", required = false, defaultValue = "") String workOrderNumber,
                                                   @RequestParam(name="username", required = false, defaultValue = "") String username,
                                                   @RequestParam(name="workingTeamName", required = false, defaultValue = "") String workingTeamName) {
        return workOrderKPIService.findAll(warehouseId, null, username, workingTeamName,  workOrderNumber);
    }

    @BillableEndpoint
    @RequestMapping(value="/work-order-kpis", method = RequestMethod.POST)
    public WorkOrderKPI addWorkOrderKPI(@RequestBody WorkOrderKPI workOrderKPI) {
        return workOrderKPIService.save(workOrderKPI);
    }


    @RequestMapping(value="/work-order-kpis/{id}", method = RequestMethod.GET)
    public WorkOrderKPI findWorkOrderKPI(@PathVariable Long id) {
        return workOrderKPIService.findById(id);
    }

    @BillableEndpoint
    @RequestMapping(value="/work-order-kpis/{id}", method = RequestMethod.DELETE)
    public void removeWorkOrderKPI(@PathVariable Long id) {
         workOrderKPIService.delete(id);
    }

    @BillableEndpoint
    @RequestMapping(value="/work-order-kpis/{id}", method = RequestMethod.PUT)
    public WorkOrderKPI changeWorkOrderKPI(@RequestBody WorkOrderKPI workOrderKPI){
        return workOrderKPIService.save(workOrderKPI);
    }




}

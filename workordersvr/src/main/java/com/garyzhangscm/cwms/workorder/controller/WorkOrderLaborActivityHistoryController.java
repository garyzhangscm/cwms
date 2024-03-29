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


import com.garyzhangscm.cwms.workorder.model.BillableEndpoint;
import com.garyzhangscm.cwms.workorder.model.WorkOrderLabor;
import com.garyzhangscm.cwms.workorder.model.WorkOrderLaborActivityHistory;
import com.garyzhangscm.cwms.workorder.service.WorkOrderLaborActivityHistoryService;
import com.garyzhangscm.cwms.workorder.service.WorkOrderLaborService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class WorkOrderLaborActivityHistoryController {
    @Autowired
    WorkOrderLaborActivityHistoryService workOrderLaborActivityHistoryService;

    @RequestMapping(value="/labor-activity-history", method = RequestMethod.GET)
    public List<WorkOrderLaborActivityHistory> findAllActivityHistory(@RequestParam Long warehouseId,
                                                                      @RequestParam(name="productionLineId", required = false, defaultValue = "") Long productionLineId,
                                                                      @RequestParam(name="username", required = false, defaultValue = "") String username) {
        return workOrderLaborActivityHistoryService.findAll(warehouseId, productionLineId, username);
    }





}

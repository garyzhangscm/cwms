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
import com.garyzhangscm.cwms.workorder.model.*;
import com.garyzhangscm.cwms.workorder.service.WorkOrderConfigurationService;
import com.garyzhangscm.cwms.workorder.service.WorkOrderLineService;
import com.garyzhangscm.cwms.workorder.service.WorkOrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
public class WorkOrderConfigurationController {

    private static final Logger logger = LoggerFactory.getLogger(WorkOrderConfigurationController.class);


    @Autowired
    WorkOrderConfigurationService workOrderConfigurationService;



    @RequestMapping(value="/work-order-configuration", method = RequestMethod.GET)
    public WorkOrderConfiguration findAllWorkOrderConfiguration(@RequestParam Long companyId,
                                                                @RequestParam Long warehouseId) {
        return workOrderConfigurationService.getWorkOrderConfiguration(companyId, warehouseId);
    }

    @BillableEndpoint
    @RequestMapping(value="/work-order-configuration", method = RequestMethod.POST)
    public WorkOrderConfiguration changeWorkOrderConfiguration(@RequestBody WorkOrderConfiguration workOrderConfiguration) {
        return workOrderConfigurationService.changeWorkOrderConfiguration(workOrderConfiguration);
    }





}

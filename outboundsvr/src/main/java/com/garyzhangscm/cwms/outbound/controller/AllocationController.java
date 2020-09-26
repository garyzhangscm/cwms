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


import com.garyzhangscm.cwms.outbound.model.AllocationResult;
import com.garyzhangscm.cwms.outbound.model.WorkOrder;
import com.garyzhangscm.cwms.outbound.service.AllocationConfigurationService;
import com.garyzhangscm.cwms.outbound.service.AllocationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;


@RestController
public class AllocationController {

    @Autowired
    private AllocationConfigurationService allocationConfigurationService;
    @Autowired
    private AllocationService allocationService;

    @RequestMapping(value="/allocation/work-order", method = RequestMethod.POST)
    public AllocationResult allocateWorkOrder(@RequestBody WorkOrder workOrder) {
        // return allocationConfigurationService.allocateWorkOrder(workOrder);
        return allocationService.allocate(workOrder);
    }




}

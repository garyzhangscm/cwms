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

import com.garyzhangscm.cwms.outbound.model.BillableEndpoint;
import com.garyzhangscm.cwms.outbound.model.Stop;
import com.garyzhangscm.cwms.outbound.model.TrailerAppointment;
import com.garyzhangscm.cwms.outbound.service.StopService;
import com.garyzhangscm.cwms.outbound.service.TrailerAppointmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class TrailerAppointmentController {
    @Autowired
    TrailerAppointmentService trailerAppointmentService;

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




}

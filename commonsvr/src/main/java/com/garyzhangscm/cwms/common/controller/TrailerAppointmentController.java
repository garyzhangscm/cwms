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

import com.garyzhangscm.cwms.common.clients.WarehouseLayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.common.exception.MissingInformationException;
import com.garyzhangscm.cwms.common.exception.RequestValidationFailException;
import com.garyzhangscm.cwms.common.model.BillableEndpoint;
import com.garyzhangscm.cwms.common.model.Supplier;
import com.garyzhangscm.cwms.common.model.TrailerAppointment;
import com.garyzhangscm.cwms.common.service.SupplierService;
import com.garyzhangscm.cwms.common.service.TrailerAppointmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@RestController
public class TrailerAppointmentController {
    @Autowired
    TrailerAppointmentService trailerAppointmentService;

    @RequestMapping(value="/trailer-appointments", method = RequestMethod.GET)
    public List<TrailerAppointment> findAllTrailerAppointments(
            @RequestParam Long warehouseId,
            @RequestParam(name = "number", required = false, defaultValue = "") String number,
            @RequestParam(name = "type", required = false, defaultValue = "") String type,
            @RequestParam(name = "status", required = false, defaultValue = "") String status,
            @RequestParam(name = "startTime", required = false, defaultValue = "") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(name = "endTime", required = false, defaultValue = "") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)  LocalDateTime endTime,
            @RequestParam(name = "date", required = false, defaultValue = "") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
            ) {


        return trailerAppointmentService.findAll(warehouseId, number, type, status, startTime,
                endTime, date);
    }

    @RequestMapping(value="/trailer-appointments/{id}", method = RequestMethod.GET)
    public TrailerAppointment findTrailerAppointment(@PathVariable Long id) {
        return trailerAppointmentService.findById(id);
    }
    @RequestMapping(value="/trailer-appointments/{id}/complete", method = RequestMethod.POST)
    public TrailerAppointment completeTrailerAppointment(@PathVariable Long id) {
        return trailerAppointmentService.completeTrailerAppointment(id);
    }




}

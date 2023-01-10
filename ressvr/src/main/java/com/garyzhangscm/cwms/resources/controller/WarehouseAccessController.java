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
import com.garyzhangscm.cwms.resources.model.Alert;
import com.garyzhangscm.cwms.resources.model.BillableEndpoint;
import com.garyzhangscm.cwms.resources.model.WarehouseAccess;
import com.garyzhangscm.cwms.resources.service.AlertService;
import com.garyzhangscm.cwms.resources.service.WarehouseAccessService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;

@RestController
public class WarehouseAccessController {

    @Autowired
    WarehouseAccessService warehouseAccessService;

    @RequestMapping(value="/warehouse-access", method = RequestMethod.GET)
    public List<WarehouseAccess> findAllWarehouseAccesses(@RequestParam Long warehouseId,
                                               @RequestParam(value = "username", defaultValue = "", required = false) String username,
                                               @RequestParam(value = "userId", defaultValue = "", required = false) Long userId) {
        return warehouseAccessService.findAll(warehouseId, username, userId);
    }

    @RequestMapping(value="/warehouse-access/{id}", method = RequestMethod.GET)
    public WarehouseAccess getWarehouseAccess(@RequestParam Long warehouseId,
                                              @PathVariable Long id) {
        return warehouseAccessService.findById(id);
    }

    @RequestMapping(value="/warehouse-access/has-access", method = RequestMethod.GET)
    public Boolean hasAccess(@RequestParam Long warehouseId,
                             @RequestParam Long userId) {
        return warehouseAccessService.hasAccess(warehouseId, userId);
    }

    @RequestMapping(value="/warehouse-access", method = RequestMethod.PUT)
    public WarehouseAccess addAccess(@RequestParam Long warehouseId,
                                     @RequestBody WarehouseAccess warehouseAccess) {
        return warehouseAccessService.addAccess(warehouseAccess);
    }



    @RequestMapping(value="/warehouse-access", method = RequestMethod.DELETE)
    public ResponseBodyWrapper removeAccess(@RequestParam Long warehouseId,
                                            @PathVariable Long userId) {
        warehouseAccessService.removeAccess(warehouseId, userId);
        return ResponseBodyWrapper.success("warehouse  " + warehouseId + " is removed from user " + userId );
    }

}

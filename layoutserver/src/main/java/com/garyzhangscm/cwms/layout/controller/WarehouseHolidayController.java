/**
 * Copyright 2018
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

package com.garyzhangscm.cwms.layout.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.garyzhangscm.cwms.layout.ResponseBodyWrapper;
import com.garyzhangscm.cwms.layout.model.BillableEndpoint;
import com.garyzhangscm.cwms.layout.model.WarehouseConfiguration;
import com.garyzhangscm.cwms.layout.model.WarehouseHoliday;
import com.garyzhangscm.cwms.layout.service.WarehouseConfigurationService;
import com.garyzhangscm.cwms.layout.service.WarehouseHolidayService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class WarehouseHolidayController {

    private static final Logger logger = LoggerFactory.getLogger(WarehouseHolidayController.class);
    @Autowired
    WarehouseHolidayService warehouseHolidayService;


    @RequestMapping(value="/warehouse-holidays", method=RequestMethod.GET)
    public List<WarehouseHoliday> getWarehouseHolidays(
            @RequestParam(name = "companyId", required = false, defaultValue = "") Long companyId,
            @RequestParam(name = "companyCode", required = false, defaultValue = "") String companyCode,
            @RequestParam(name = "warehouseId", required = false, defaultValue = "") Long warehouseId,
            @RequestParam(name = "warehouseName", required = false, defaultValue = "") String warehouseName) {

        return warehouseHolidayService.findAll(companyId, companyCode, warehouseId, warehouseName);
    }

    @RequestMapping(value="/warehouse-holidays/by-year/{year}", method=RequestMethod.GET)
    public List<WarehouseHoliday> getWWarehouseHolidayByYear(
            @PathVariable String year, @RequestParam Long warehouseId) {

        return warehouseHolidayService.findByWarehouseAndYear(warehouseId, year);
    }
    @BillableEndpoint
    @RequestMapping(value="/warehouse-holidays", method=RequestMethod.PUT)
    public WarehouseHoliday addWarehouseHoliday(@RequestParam Long warehouseId,
                                   @RequestBody WarehouseHoliday warehouseHoliday) {
        return warehouseHolidayService.addWarehouseHoliday(warehouseHoliday);
    }

    @BillableEndpoint
    @RequestMapping(value="/warehouse-holidays/{id}", method=RequestMethod.DELETE)
    public ResponseBodyWrapper removeWarehouseHoliday(@RequestParam Long warehouseId,
                                                      @PathVariable Long id) {
        warehouseHolidayService.delete(id);
        return ResponseBodyWrapper.success("holiday is removed!");
    }




}

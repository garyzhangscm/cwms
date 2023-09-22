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


import com.fasterxml.jackson.core.JsonProcessingException;
import com.garyzhangscm.cwms.workorder.model.ItemProductivityReport;
import com.garyzhangscm.cwms.workorder.service.ItemProductivityReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class ItemProductivityReportController {
    @Autowired
    ItemProductivityReportService itemProductivityReportService;

    @RequestMapping(value="/item-productivity-report/current-shift", method = RequestMethod.GET)
    public List<ItemProductivityReport> findAllMoulds(@RequestParam Long warehouseId,
                                     @RequestParam(name="itemName", required = false, defaultValue = "") String itemName,
                                     @RequestParam(name="itemFamilyName", required = false, defaultValue = "") String itemFamilyName) throws JsonProcessingException {
        return itemProductivityReportService.getItemProductivityReportForCurrentShiftWithCache(warehouseId, itemFamilyName, itemName);
    }


}

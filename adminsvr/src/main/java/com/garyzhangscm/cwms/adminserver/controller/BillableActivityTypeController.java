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

package com.garyzhangscm.cwms.adminserver.controller;

import com.garyzhangscm.cwms.adminserver.ResponseBodyWrapper;
import com.garyzhangscm.cwms.adminserver.model.BillableActivityType;
import com.garyzhangscm.cwms.adminserver.service.BillableActivityTypeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class BillableActivityTypeController {
    @Autowired
    BillableActivityTypeService billableActivityTypeService;

    @RequestMapping(value="/billable-activity-types", method = RequestMethod.GET)
    public List<BillableActivityType> findAllBillableActivityTypes(@RequestParam Long warehouseId,
                                                                   @RequestParam(name="name", required = false, defaultValue = "") String name,
                                                                   @RequestParam(name="description", required = false, defaultValue = "") String description) {
        return billableActivityTypeService.findAll(warehouseId, name, description);
    }

    @RequestMapping(value="/billable-activity-types/{id}", method = RequestMethod.GET)
    public BillableActivityType getBillableActivityType(@PathVariable Long id) {
        return billableActivityTypeService.findById(id);
    }

    @RequestMapping(value="/billable-activity-types/{id}", method = RequestMethod.DELETE)
    @Caching(
            evict = {
                    @CacheEvict(cacheNames = "InboundService_BillableActivityType", allEntries = true),
            }
    )
    public ResponseBodyWrapper<String> removeInventoryStatus(@PathVariable Long id,
                                                             @RequestParam Long warehouseId) {

        billableActivityTypeService.delete(id);
        return ResponseBodyWrapper.success("Billable activity type is removed!");
    }

    @RequestMapping(value="/billable-activity-types", method = RequestMethod.PUT)
    public BillableActivityType createBillableActivityType(@RequestParam Long warehouseId,
                                                 @RequestBody BillableActivityType billableActivityType) {
        return billableActivityTypeService.createBillableActivityType(billableActivityType);
    }

    @RequestMapping(method=RequestMethod.POST, value="/billable-activity-types/{id}")
    @Caching(
            evict = {
                    @CacheEvict(cacheNames = "InboundService_BillableActivityType", allEntries = true),
            }
    )
    public BillableActivityType changeBillableActivityType(@PathVariable long id,
                                                 @RequestParam Long warehouseId,
                                                 @RequestBody BillableActivityType billableActivityType) {

        return billableActivityTypeService.changeBillableActivityType(billableActivityType);
    }
}

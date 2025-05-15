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


import com.garyzhangscm.cwms.layout.exception.RequestValidationFailException;
import com.garyzhangscm.cwms.layout.model.*;
import com.garyzhangscm.cwms.layout.service.LocationGroupService;
import com.garyzhangscm.cwms.layout.service.PickZoneService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class PickZoneController {

    private static final Logger logger = LoggerFactory.getLogger(PickZoneController.class);
    @Autowired
    PickZoneService pickZoneService;


    @RequestMapping(method=RequestMethod.GET, value="/pick-zones")
    public List<PickZone> listPickZones(@RequestParam Long warehouseId,
                                        @RequestParam(name = "name", required = false, defaultValue = "") String name) {
        return pickZoneService.findAll(warehouseId, name);
    }


    @RequestMapping(method=RequestMethod.GET, value="/pick-zones/{id}")
    public PickZone getPickZone(@PathVariable long id) {
        return pickZoneService.findById(id);
    }

    @BillableEndpoint
    @RequestMapping(method=RequestMethod.POST, value="/pick-zones")
    public PickZone addPickZone(@RequestBody PickZone pickZone) {
        return pickZoneService.addPickZone(pickZone);
    }

    @BillableEndpoint
    @RequestMapping(method=RequestMethod.PUT, value="/pick-zones/{id}")
    @Caching(
            evict = {
                    @CacheEvict(cacheNames = "AdminService_PickZone", allEntries = true),
                    @CacheEvict(cacheNames = "InboundService_PickZone", allEntries = true),
                    @CacheEvict(cacheNames = "InventoryService_PickZone", allEntries = true),
                    @CacheEvict(cacheNames = "OutboundService_PickZone", allEntries = true),
                    @CacheEvict(cacheNames = "ResourceService_PickZone", allEntries = true),
            }
    )
    public PickZone changePickZone(@PathVariable long id,
                                   @RequestBody PickZone pickZone) {

        return pickZoneService.saveOrUpdate(pickZone);
    }


    @BillableEndpoint
    @RequestMapping(method=RequestMethod.DELETE, value="/pick-zones/{id}")
    @Caching(
            evict = {
                    @CacheEvict(cacheNames = "AdminService_PickZone", allEntries = true),
                    @CacheEvict(cacheNames = "InboundService_PickZone", allEntries = true),
                    @CacheEvict(cacheNames = "InventoryService_PickZone", allEntries = true),
                    @CacheEvict(cacheNames = "OutboundService_PickZone", allEntries = true),
                    @CacheEvict(cacheNames = "ResourceService_PickZone", allEntries = true),
            }
    )
    public void removePickZone(@PathVariable long id) {
        pickZoneService.removePickZone(id);
    }



}

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

import com.garyzhangscm.cwms.layout.Exception.GenericException;
import com.garyzhangscm.cwms.layout.model.Location;
import com.garyzhangscm.cwms.layout.model.LocationGroup;
import com.garyzhangscm.cwms.layout.service.LocationGroupService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class LocationGroupController {

    private static final Logger logger = LoggerFactory.getLogger(LocationGroupController.class);
    @Autowired
    LocationGroupService locationGroupService;


    @RequestMapping(method=RequestMethod.GET, value="/locationgroups")
    public List<LocationGroup> listLocationGroups(@RequestParam Long warehouseId,
                                                  @RequestParam(name = "locationGroupTypes", required = false, defaultValue = "") String locationGroupTypes,
                                                  @RequestParam(name = "name", required = false, defaultValue = "") String name) {
        return locationGroupService.findAll(warehouseId, locationGroupTypes, name);
    }
    @RequestMapping(method=RequestMethod.GET, value="/locationgroups/{id}")
    public LocationGroup getLocationGroup(@PathVariable long id) {
        return locationGroupService.findById(id);
    }

    @RequestMapping(method=RequestMethod.POST, value="/locationgroups")
    public LocationGroup addLocationGroups(@RequestBody LocationGroup locationGroup) {
        return locationGroupService.save(locationGroup);
    }
    @RequestMapping(method=RequestMethod.PUT, value="/locationgroups/{id}")
    public LocationGroup changeLocationGroups(@PathVariable long id,
                                              @RequestBody LocationGroup locationGroup) {
        if (locationGroup.getId() != null && locationGroup.getId() != id) {
            throw new GenericException(10000, "ID in the URL doesn't match with the data passed in the request");
        }
        return locationGroupService.save(locationGroup);
    }


    @RequestMapping(method=RequestMethod.DELETE, value="/locationgroups")
    public void removeLocationGroups(@RequestParam(name = "locationGroupIds", required = false, defaultValue = "") String locationGroupIds) {
        locationGroupService.delete(locationGroupIds);
    }


    // Reserve a location. This is normally to reserve hop locations for certain inventory
    @RequestMapping(method=RequestMethod.PUT, value="/locationgroups/{id}/reserve")
    public Location reserveLocation(@PathVariable Long id,
                                    @RequestParam(name = "reservedCode") String reservedCode,
                                    @RequestParam(name = "pendingSize") Double pendingSize,
                                    @RequestParam(name = "pendingQuantity") Long pendingQuantity,
                                    @RequestParam(name = "pendingPalletQuantity") Integer pendingPalletQuantity) {

        logger.debug("========     Start to reserve location with parameters:   =======");
        logger.debug("id： {}", id);
        logger.debug("reservedCode： {}", reservedCode);
        logger.debug("pendingSize： {}", pendingSize);
        logger.debug("pendingQuantity： {}", pendingQuantity);
        logger.debug("pendingPalletQuantity： {}", pendingPalletQuantity);
        return locationGroupService.reserveLocation(id, reservedCode, pendingSize, pendingQuantity, pendingPalletQuantity);
    }



}

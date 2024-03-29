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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Caching;
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
                                                  @RequestParam(name = "locationGroups", required = false, defaultValue = "") String locationGroups,
                                                  @RequestParam(name = "name", required = false, defaultValue = "") String name) {
        logger.debug("Will list all groups by \n warehouseId {}\n locationGroupTypes{}\n name{}\n",
                warehouseId, locationGroupTypes, name);
        return locationGroupService.findAll(warehouseId, locationGroupTypes, locationGroups, name);
    }

    @RequestMapping(method=RequestMethod.GET, value="/locationgroups/qc")
    public List<LocationGroup> getQCLocationGroups(@RequestParam Long warehouseId) {
        logger.debug("Will list qc location groups by  warehouseId {} ",
                warehouseId );
        return locationGroupService.getQCLocationGroups(warehouseId);
    }

    @RequestMapping(method=RequestMethod.GET, value="/locationgroups/storage")
    public List<LocationGroup> getStorageLocationGroup(@RequestParam Long warehouseId) {
        logger.debug("Will list storage location groups by  warehouseId {} ",
                warehouseId );
        return locationGroupService.getStorageLocationGroup(warehouseId);
    }

    @RequestMapping(method=RequestMethod.GET, value="/locationgroups/{id}")
    public LocationGroup getLocationGroup(@PathVariable long id) {
        return locationGroupService.findById(id);
    }

    @RequestMapping(method=RequestMethod.GET, value="/locationgroups/{id}/inventory-consolidation-strategy")
    public InventoryConsolidationStrategy getInventoryConsolidationStrategy(@PathVariable long id) {
        return locationGroupService.findById(id).getInventoryConsolidationStrategy();
    }

    @BillableEndpoint
    @RequestMapping(method=RequestMethod.POST, value="/locationgroups")
    public LocationGroup addLocationGroups(@RequestBody LocationGroup locationGroup) {
        return locationGroupService.addLocationGroups(locationGroup);
    }

    @BillableEndpoint
    @RequestMapping(method=RequestMethod.PUT, value="/locationgroups/{id}")
    @Caching(
            evict = {
                    @CacheEvict(cacheNames = "AdminService_LocationGroup", allEntries = true),
                    @CacheEvict(cacheNames = "InboundService_LocationGroup", allEntries = true),
                    @CacheEvict(cacheNames = "InventoryService_LocationGroup", allEntries = true),
                    @CacheEvict(cacheNames = "OutboundService_LocationGroup", allEntries = true),
                    @CacheEvict(cacheNames = "ResourceService_LocationGroup", allEntries = true),
            }
    )
    public LocationGroup changeLocationGroups(@PathVariable long id,
                                              @RequestBody LocationGroup locationGroup) {
        if (locationGroup.getId() != null && locationGroup.getId() != id) {
            throw RequestValidationFailException.raiseException(
                    "id(in URI): " + id + "; locationGroup.getId(): " + locationGroup.getId());
        }
        return locationGroupService.saveOrUpdate(locationGroup);
    }


    @BillableEndpoint
    @RequestMapping(method=RequestMethod.DELETE, value="/locationgroups/{id}")
    @Caching(
            evict = {
                    @CacheEvict(cacheNames = "AdminService_LocationGroup", allEntries = true),
                    @CacheEvict(cacheNames = "InboundService_LocationGroup", allEntries = true),
                    @CacheEvict(cacheNames = "InventoryService_LocationGroup", allEntries = true),
                    @CacheEvict(cacheNames = "OutboundService_LocationGroup", allEntries = true),
                    @CacheEvict(cacheNames = "ResourceService_LocationGroup", allEntries = true),
            }
    )
    public void removeLocationGroup(@PathVariable long id) {
        locationGroupService.removeLocationGroup(id);
    }

    @BillableEndpoint
    @RequestMapping(method=RequestMethod.DELETE, value="/locationgroups")
    @Caching(
            evict = {
                    @CacheEvict(cacheNames = "AdminService_LocationGroup", allEntries = true),
                    @CacheEvict(cacheNames = "InboundService_LocationGroup", allEntries = true),
                    @CacheEvict(cacheNames = "InventoryService_LocationGroup", allEntries = true),
                    @CacheEvict(cacheNames = "OutboundService_LocationGroup", allEntries = true),
                    @CacheEvict(cacheNames = "ResourceService_LocationGroup", allEntries = true),
            }
    )
    public void removeLocationGroups(@RequestParam(name = "locationGroupIds", required = false, defaultValue = "") String locationGroupIds) {
        locationGroupService.delete(locationGroupIds);
    }




    // Reserve a location. This is normally to reserve hop locations for certain inventory
    @BillableEndpoint
    @RequestMapping(method=RequestMethod.PUT, value="/locationgroups/{id}/reserve")
    @Caching(
            evict = {
                    @CacheEvict(cacheNames = "AdminService_LocationGroup", allEntries = true),
                    @CacheEvict(cacheNames = "InboundService_LocationGroup", allEntries = true),
                    @CacheEvict(cacheNames = "InventoryService_LocationGroup", allEntries = true),
                    @CacheEvict(cacheNames = "OutboundService_LocationGroup", allEntries = true),
                    @CacheEvict(cacheNames = "ResourceService_LocationGroup", allEntries = true),
            }
    )
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


    @RequestMapping(method=RequestMethod.GET, value="/locationgroups/utilization/storage")
    public List<StorageLocationGroupUtilization> getStorageLocationGroupUtilization(
            Long warehouseId) {
        return locationGroupService.getStorageLocationGroupUtilization(warehouseId);
    }

}

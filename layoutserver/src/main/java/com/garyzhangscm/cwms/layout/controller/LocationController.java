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

import com.garyzhangscm.cwms.layout.ResponseBodyWrapper;
import com.garyzhangscm.cwms.layout.model.Location;
import com.garyzhangscm.cwms.layout.service.FileService;
import com.garyzhangscm.cwms.layout.service.LocationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.ws.rs.Path;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.List;

@RestController
public class LocationController {

    private static final Logger logger = LoggerFactory.getLogger(LocationController.class);
    @Autowired
    LocationService locationService;

    @Autowired
    FileService fileService;

    @RequestMapping(value="/locations/{id}", method = RequestMethod.GET)
    public Location getLocationById(@PathVariable Long id) {
        return locationService.findById(id);
    }


    @RequestMapping(value="/locations/logic/{locationType}", method = RequestMethod.GET)
    public Location getLogicalLocation(@PathVariable String locationType,
                                       @RequestParam Long warehouseId) {
        return locationService.findLogicLocation(locationType, warehouseId);
    }


    @RequestMapping(method=RequestMethod.POST, value="/locations/upload")
    public ResponseBodyWrapper uploadLocations(@RequestParam("file") MultipartFile file) throws IOException {


        File localFile = fileService.saveFile(file);
        List<Location> locations = locationService.loadLocationData(localFile);
        return  ResponseBodyWrapper.success(locations.size() + "");
    }

    @RequestMapping(method=RequestMethod.GET, value="/locations")
    public List<Location> findLocations(@RequestParam Long warehouseId,
                                        @RequestParam(name = "locationGroupTypeIds", required = false, defaultValue = "") String locationGroupTypeIds,
                                        @RequestParam(name = "locationGroupIds", required = false, defaultValue = "") String locationGroupIds,
                                        @RequestParam(name = "name", required = false, defaultValue = "") String name,
                                        @RequestParam(name = "beginSequence", required = false, defaultValue = "") Long beginSequence,
                                        @RequestParam(name = "endSequence", required = false, defaultValue = "") Long endSequence,
                                        @RequestParam(name = "sequenceType", required = false, defaultValue = "") String sequenceType,
                                        @RequestParam(name = "includeEmptyLocation", required = false, defaultValue = "true") Boolean includeEmptyLocation,
                                        @RequestParam(name = "emptyLocationOnly", required = false, defaultValue = "false") Boolean emptyLocationOnly,
                                        @RequestParam(name = "minEmptyCapacity", required = false, defaultValue = "0.0") Double minEmptyCapacity,
                                        @RequestParam(name = "pickableLocationOnly", required = false, defaultValue = "false") Boolean pickableLocationOnly,
                                        @RequestParam(name = "includeDisabledLocation", required = false, defaultValue = "false") Boolean includeDisabledLocation,
                                        @RequestParam(name = "maxResultCount", required =  false, defaultValue =  "0") Integer maxResultCount) {

        StringBuilder params = new StringBuilder()
                .append("Start to find location with params:")
                .append("\nwarehouseId: ").append(warehouseId)
                .append("\nlocationGroupTypeIds: ").append(locationGroupTypeIds)
                .append("\nlocationGroupIds: ").append(locationGroupIds)
                .append("\nname: ").append(name)
                .append("\nbeginSequence: ").append(beginSequence)
                .append("\nendSequence: ").append(endSequence)
                .append("\nsequenceType: ").append(sequenceType)
                .append("\nincludeEmptyLocation: ").append(includeEmptyLocation)
                .append("\nemptyLocationOnly: ").append(emptyLocationOnly)
                .append("\nminEmptyCapacity: ").append(minEmptyCapacity)
                .append("\npickableLocationOnly: ").append(pickableLocationOnly)
                .append("\nincludeDisabledLocation: ").append(includeDisabledLocation)
                .append("\nmaxResultCount: ").append(maxResultCount);

        logger.debug(params.toString());


        List<Location> locations = locationService.findAll(
                warehouseId,
                locationGroupTypeIds, locationGroupIds, name,
                beginSequence, endSequence, sequenceType,
                includeEmptyLocation, emptyLocationOnly, minEmptyCapacity,pickableLocationOnly,  includeDisabledLocation);

        logger.debug(">> Find {} locations", locations.size());
        if (locations.size() == 0) {
            return locations;
        }

        int returnResultCout = locations.size();
        if (maxResultCount > 0) {
            returnResultCout = Math.min(maxResultCount, locations.size());
        }
        logger.debug(">> Will only return {} locations", returnResultCout);
        return locations.subList(0, returnResultCout);
    }

    @RequestMapping(method=RequestMethod.GET, value="/locations/dock")
    public List<Location> findDockLocations(@RequestParam Long warehouseId,
                                            @RequestParam(name = "empty", required = false, defaultValue = "false") boolean emptyDockOnly) {

        return locationService.getDockLocations(warehouseId, emptyDockOnly);
    }

    @RequestMapping(method=RequestMethod.POST, value="/locations/dock/{id}/dispatch-trailer")
    public Location dispatchTrailerFromDock(@PathVariable Long id){
        return locationService.moveTrailerFromDock(id);
    }
    @RequestMapping(method=RequestMethod.POST, value="/locations/dock/{id}/check-in-trailer")
    public Location checkInTrailerAtDock(@PathVariable Long id,
                                         @RequestParam Long trailerId){
        return locationService.checkInTrailerAtDock(id, trailerId);
    }

    @RequestMapping(method=RequestMethod.DELETE, value="/locations")
    public ResponseBodyWrapper removeLocations(@RequestParam("locationIds") String locationIds) {

        locationService.delete(locationIds);
        return  ResponseBodyWrapper.success(locationIds);
    }


    @RequestMapping(method=RequestMethod.PUT, value="/locations/{id}")
    public Location updateLocation(@PathVariable Long id,
                                              @RequestParam(name = "enabled", defaultValue = "", required = false) Boolean enabled,
                                              @RequestParam(name = "inventoryQuantity", defaultValue = "", required = false) Long inventoryQuantity,
                                              @RequestParam(name = "inventorySize", defaultValue = "", required = false) Double inventorySize) {

        Location location = locationService.findById(id);

        if (enabled != null) {
            location.setEnabled(enabled);
        }
        if (inventoryQuantity != null && inventorySize != null) {
            location.setCurrentVolume(locationService.getLocationVolume(inventoryQuantity, inventorySize));
        }
        return locationService.saveOrUpdate(location);
    }


    @RequestMapping(method=RequestMethod.POST, value="/locations")
    public Location addLocation(@RequestBody Location location) {

        return locationService.saveOrUpdate(location);
    }

    // Reserve a location. This is normally to reserve hop locations for certain inventory
    @RequestMapping(method=RequestMethod.PUT, value="/locations/{id}/reserve")
    public Location reserveLocation(@PathVariable Long id,
                                    @RequestParam(name = "reservedCode") String reservedCode) {


        return locationService.reserveLocation(id, reservedCode);
    }

    // Reserve a location. This is normally to reserve hop locations for certain inventory
    @RequestMapping(method=RequestMethod.PUT, value="/locations/{id}/reserveWithVolume")
    public Location reserveLocation(@PathVariable Long id,
                                    @RequestParam(name = "reservedCode") String reservedCode,
                                    @RequestParam(name = "pendingSize") Double pendingSize,
                                    @RequestParam(name = "pendingQuantity") Long pendingQuantity,
                                    @RequestParam(name = "pendingPalletQuantity") Integer pendingPalletQuantity) {


        return locationService.reserveLocation(id, reservedCode, pendingSize, pendingQuantity, pendingPalletQuantity);
    }

    // Allocate a final destination for a inventory and update the pending volume of the
    // location
    @RequestMapping(method=RequestMethod.PUT, value="/locations/{id}/allocate")
    public Location allocateLocation(@PathVariable Long id,
                                     @RequestParam(name = "inventorySize") Double inventorySize) {

        logger.debug("Start to allocate location with id {}, inventory size is : {}", id, inventorySize);
        return locationService.allocateLocation(id, inventorySize);
    }


    @RequestMapping(method=RequestMethod.POST, value="/locations/{id}/pending-volume")
    public Location changePendingVolume(@PathVariable Long id,
                                        @RequestParam(name = "reduce", required = false, defaultValue = "0.0") Double reducedPendingVolume,
                                        @RequestParam(name = "increase", required = false, defaultValue = "0.0") Double increasedPendingVolume) {

        return locationService.changePendingVolume(id, reducedPendingVolume, increasedPendingVolume);
    }

    @RequestMapping(method=RequestMethod.POST, value="/locations/{id}/volume")
    public Location changeVolume(@PathVariable Long id,
                                        @RequestParam(name = "reduce", required = false, defaultValue = "0.0") Double reducedVolume,
                                        @RequestParam(name = "increase", required = false, defaultValue = "0.0") Double increasedVolume) {

        return locationService.changeLocationVolume(id, reducedVolume, increasedVolume);
    }


    @RequestMapping(method=RequestMethod.POST, value="/locations/{id}/lock")
    public Location processLocationLock(@PathVariable Long id,
                                 @RequestParam Boolean locked) {

        return locationService.processLocationLock(id, locked);
    }


}

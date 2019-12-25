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

    @RequestMapping(value="/location/{id}", method = RequestMethod.GET)
    public Location getLocationById(@PathVariable Long id) {
        return locationService.findById(id);
    }


    @RequestMapping(value="/location/logic/{locationType}", method = RequestMethod.GET)
    public Location getLogicalLocation(@PathVariable String locationType) {
        return locationService.findLogicLocation(locationType);
    }


    @RequestMapping(method=RequestMethod.POST, value="/locations/upload")
    public ResponseBodyWrapper uploadLocations(@RequestParam("file") MultipartFile file) throws IOException {


        File localFile = fileService.saveFile(file);
        List<Location> locations = locationService.loadLocationData(localFile);
        return  ResponseBodyWrapper.success(locations.size() + "");
    }

    @RequestMapping(method=RequestMethod.GET, value="/locations")
    public List<Location> findLocations(@RequestParam(name = "location_group_type_ids", required = false, defaultValue = "") String locationGroupTypeIds,
                                        @RequestParam(name = "location_group_ids", required = false, defaultValue = "") String locationGroupIds,
                                        @RequestParam(name = "name", required = false, defaultValue = "") String name,
                                        @RequestParam(name = "begin_sequence", required = false, defaultValue = "") Long beginSequence,
                                        @RequestParam(name = "end_sequence", required = false, defaultValue = "") Long endSequence,
                                        @RequestParam(name = "sequence_type", required = false, defaultValue = "") String sequenceType,
                                        @RequestParam(name = "include_empty_location", required = false, defaultValue = "true") Boolean includeEmptyLocation,
                                        @RequestParam(name = "include_disabled_location", required = false, defaultValue = "false") Boolean includeDisabledLocation) {

        return locationService.findAll(locationGroupTypeIds, locationGroupIds, name,
                beginSequence, endSequence, sequenceType,
                includeEmptyLocation, includeDisabledLocation);
    }

    @RequestMapping(method=RequestMethod.DELETE, value="/location")
    public ResponseBodyWrapper removeLocations(@RequestParam("location_ids") String locationIds) {

        locationService.delete(locationIds);
        return  ResponseBodyWrapper.success(locationIds);
    }


    @RequestMapping(method=RequestMethod.PUT, value="/location/{id}")
    public Location updateLocation(@PathVariable Long id,
                                              @RequestParam(name = "enabled", defaultValue = "", required = false) Boolean enabled,
                                              @RequestParam(name = "inventory_quantity", defaultValue = "", required = false) Long inventoryQuantity,
                                              @RequestParam(name = "inventory_size", defaultValue = "", required = false) Double inventorySize) {

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

}

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

import com.garyzhangscm.cwms.layout.model.BillableEndpoint;
import com.garyzhangscm.cwms.layout.model.LocationGroupType;
import com.garyzhangscm.cwms.layout.service.LocationGroupTypeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Caching;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class LocationGroupTypeController {

    @Autowired
    LocationGroupTypeService locationGroupTypeService;


    @RequestMapping(method=RequestMethod.GET, value="/locationgrouptypes")
    public List<LocationGroupType> listLocationGroupTypes(@RequestParam(name="name", required = false, defaultValue = "") String name,
                                                          @RequestParam(name="shippingStage", required = false, defaultValue = "") Boolean shippingStage) {
        return locationGroupTypeService.findAll(name, shippingStage);
    }

    @RequestMapping(method=RequestMethod.GET, value="/locationgrouptypes/{id}")
    public LocationGroupType getLocationGroupType(@PathVariable Long id) {
        return locationGroupTypeService.findById(id);
    }

    @BillableEndpoint
    @RequestMapping(method=RequestMethod.POST, value="/locationgrouptypes")
    public LocationGroupType addLocationGroupTypes(@RequestBody LocationGroupType locationGroupType) {
        return locationGroupTypeService.saveOrUpdate(locationGroupType);
    }


    @RequestMapping(method=RequestMethod.GET, value="/locationgrouptypes/storage-locations")
    public List<LocationGroupType> getStorageLocationTypes() {
        return locationGroupTypeService.getStorageLocationGroupTypes();
    }
}

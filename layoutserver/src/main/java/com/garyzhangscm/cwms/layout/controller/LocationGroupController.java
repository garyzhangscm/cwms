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
import com.garyzhangscm.cwms.layout.model.LocationGroup;
import com.garyzhangscm.cwms.layout.service.LocationGroupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class LocationGroupController {

    @Autowired
    LocationGroupService locationGroupService;


    @RequestMapping(method=RequestMethod.GET, value="/locationgroups")
    public List<LocationGroup> listLocationGroups(@RequestParam(name = "location_group_types", required = false, defaultValue = "") String locationGroupTypes) {
        return locationGroupService.listLocationGroupsByTypes(locationGroupTypes);
    }

    @RequestMapping(method=RequestMethod.POST, value="/locationgroups")
    public LocationGroup addLocationGroups(@RequestBody LocationGroup locationGroup) {
        return locationGroupService.save(locationGroup);
    }
    @RequestMapping(method=RequestMethod.PUT, value="/locationgroup/{id}")
    public LocationGroup changeLocationGroups(@PathVariable long id,
                                              @RequestBody LocationGroup locationGroup) {
        if (locationGroup.getId() != null && locationGroup.getId() != id) {
            throw new GenericException(10000, "ID in the URL doesn't match with the data passed in the request");
        }
        return locationGroupService.save(locationGroup);
    }


    @RequestMapping(method=RequestMethod.DELETE, value="/locationgroup")
    public void removeLocationGroups(@RequestParam(name = "location_group_ids", required = false, defaultValue = "") String locationGroupIds) {
        locationGroupService.delete(locationGroupIds);
    }



}

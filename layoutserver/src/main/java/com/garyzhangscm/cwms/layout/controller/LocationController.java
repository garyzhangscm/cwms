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
import com.garyzhangscm.cwms.layout.service.LocationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

@RestController
public class LocationController {

    @Autowired
    LocationService locationService;


    @RequestMapping(method=RequestMethod.POST, value="/locations/upload")
    public List<Location> listLocationGroups(@RequestParam("file") MultipartFile file) throws IOException {


        String destination = "/upload/tmp/"  + System.currentTimeMillis() + "_" + file.getOriginalFilename();
        File localFile = new File(destination);

        if (!localFile.getParentFile().exists()) {
            localFile.getParentFile().mkdirs();
        }
        if (!localFile.exists()) {
            localFile.createNewFile();
        }
        file.transferTo(localFile);
        return locationService.findAll();
    }

    @RequestMapping(method=RequestMethod.GET, value="/locations")
    public List<Location> findLocationGroups(@RequestParam(name = "location_group_types", required = false, defaultValue = "") String locationGroupTypes,
                                             @RequestParam(name = "location_groups", required = false, defaultValue = "") String locationGroups) {
        return locationService.findAll();
    }


}

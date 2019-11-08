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

package com.garyzhangscm.cwms.layout.service;

import com.garyzhangscm.cwms.layout.model.Location;
import com.garyzhangscm.cwms.layout.model.LocationGroup;
import com.garyzhangscm.cwms.layout.repository.LocationGroupRepository;
import com.garyzhangscm.cwms.layout.repository.LocationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class LocationService {
    @Autowired
    private LocationRepository locationRepository;

    public Location findById(Long id) {
        return locationRepository.findById(id).orElse(null);
    }

    public List<Location> findAll() {

        return locationRepository.findAll();
    }

    public List<Location> findAll(long[] locationGroupIdArray) {

        List<Long> locationGroupList = Arrays.stream(locationGroupIdArray).boxed().collect( Collectors.toList());
        return locationRepository.findByLocationGroups(locationGroupList);
    }

    public List<Location> listLocationsByTypes(String locationGroups) {
        if (locationGroups.isEmpty()) {
            return findAll();
        }
        else {
            long[] locationGroupArray = Arrays.asList(locationGroups.split(",")).stream().mapToLong(Long::parseLong).toArray();
            return findAll(locationGroupArray);
        }
    }

    public Location findByName(String name){
        return locationRepository.findByName(name);
    }

    public Location save(Location location) {
        return locationRepository.save(location);
    }

    public void delete(Location location) {
        locationRepository.delete(location);
    }
    public void delete(Long id) {
        locationRepository.deleteById(id);
    }

    public void delete(String locationGroupIds) {
        // remove a list of location groups based upon the id passed in
        if (!locationGroupIds.isEmpty()) {
            long[] locationGroupIdArray = Arrays.asList(locationGroupIds.split(",")).stream().mapToLong(Long::parseLong).toArray();
            for(long id : locationGroupIdArray) {
                delete(id);
            }
        }

    }

}

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

import com.garyzhangscm.cwms.layout.model.LocationGroup;
import com.garyzhangscm.cwms.layout.model.LocationGroupType;
import com.garyzhangscm.cwms.layout.repository.LocationGroupRepository;
import com.garyzhangscm.cwms.layout.repository.LocationGroupTypeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class LocationGroupService {
    @Autowired
    private LocationGroupRepository locationGroupRepository;

    public LocationGroup findById(Long id) {
        return locationGroupRepository.findById(id).orElse(null);
    }

    public List<LocationGroup> findAll() {

        return locationGroupRepository.findAll();
    }

    public List<LocationGroup> findAll(long[] locationGroupTypeIdArray) {

        List<Long> locationGroupTypeList = Arrays.stream(locationGroupTypeIdArray).boxed().collect( Collectors.toList());
        return locationGroupRepository.findByLocationGroupTypes(locationGroupTypeList);
    }

    public List<LocationGroup> listLocationGroupsByTypes(String locationGroupTypes) {
        if (locationGroupTypes.isEmpty()) {
            return findAll();
        }
        else {
            long[] locationGroupTypeArray = Arrays.asList(locationGroupTypes.split(",")).stream().mapToLong(Long::parseLong).toArray();
            return findAll(locationGroupTypeArray);
        }
    }

    public LocationGroup findByName(String name){
        return locationGroupRepository.findByName(name);
    }

    public LocationGroup save(LocationGroup locationGroup) {
        return locationGroupRepository.save(locationGroup);
    }

    public void delete(LocationGroup locationGroup) {
        locationGroupRepository.delete(locationGroup);
    }
    public void delete(Long id) {
        locationGroupRepository.deleteById(id);
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

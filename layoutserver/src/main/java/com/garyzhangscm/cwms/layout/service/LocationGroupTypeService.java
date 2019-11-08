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

import com.garyzhangscm.cwms.layout.model.LocationGroupType;
import com.garyzhangscm.cwms.layout.model.Warehouse;
import com.garyzhangscm.cwms.layout.repository.LocationGroupTypeRepository;
import com.garyzhangscm.cwms.layout.repository.WarehouseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LocationGroupTypeService {
    @Autowired
    private LocationGroupTypeRepository locationGroupTypeRepository;

    public LocationGroupType findById(Long id) {
        return locationGroupTypeRepository.findById(id).orElse(null);
    }

    public List<LocationGroupType> findAll() {

        return locationGroupTypeRepository.findAll();
    }

    public LocationGroupType findByName(String name){
        return locationGroupTypeRepository.findByName(name);
    }

    public LocationGroupType save(LocationGroupType locationGroupType) {
        return locationGroupTypeRepository.save(locationGroupType);
    }

    public void delete(LocationGroupType locationGroupType) {
        locationGroupTypeRepository.delete(locationGroupType);
    }
    public void delete(Long id) {
        locationGroupTypeRepository.deleteById(id);
    }

}

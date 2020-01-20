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

import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.garyzhangscm.cwms.layout.model.LocationGroupCSVWrapper;
import com.garyzhangscm.cwms.layout.model.LocationGroupType;
import com.garyzhangscm.cwms.layout.model.Warehouse;
import com.garyzhangscm.cwms.layout.repository.LocationGroupTypeRepository;
import com.garyzhangscm.cwms.layout.repository.WarehouseRepository;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class LocationGroupTypeService implements TestDataInitiableService {

    private static final Logger logger = LoggerFactory.getLogger(LocationGroupTypeService.class);

    @Autowired
    private LocationGroupTypeRepository locationGroupTypeRepository;
    @Autowired
    private FileService fileService;

    @Value("${fileupload.test-data.location-group-types:location_group_types.csv}")
    String testDataFile;

    public LocationGroupType findById(Long id) {
        return locationGroupTypeRepository.findById(id).orElse(null);
    }

    public List<LocationGroupType> findAll(String name) {

        if (StringUtils.isBlank(name)) {
            return locationGroupTypeRepository.findAll();
        }
        else {
            LocationGroupType locationGroupType = findByName(name);
            if (locationGroupType != null) {
                return Arrays.asList(new LocationGroupType[]{locationGroupType});
            }
            else {
                return new ArrayList<>();
            }
        }
    }

    public LocationGroupType findByName(String name){
        return locationGroupTypeRepository.findByName(name);
    }

    public LocationGroupType save(LocationGroupType locationGroupType) {
        return locationGroupTypeRepository.save(locationGroupType);
    }
    public LocationGroupType saveOrUpdate(LocationGroupType locationGroupType) {
        if (findByName(locationGroupType.getName()) != null) {
            locationGroupType.setId(findByName(locationGroupType.getName()).getId());
        }
        return save(locationGroupType);
    }

    public void delete(LocationGroupType locationGroupType) {
        locationGroupTypeRepository.delete(locationGroupType);
    }
    public void delete(Long id) {
        locationGroupTypeRepository.deleteById(id);
    }
    public List<LocationGroupType> loadData(File file) throws IOException {

        CsvSchema schema = CsvSchema.builder().
                addColumn("name").
                addColumn("description").
                addColumn("fourWallInventory").
                addColumn("virtual").
                addColumn("receivingStage").
                addColumn("shippingStage").
                addColumn("dock").
                addColumn("yard").
                addColumn("storage").
                addColumn("pickupAndDeposit").
                addColumn("trailer").
                build().withHeader();
        return fileService.loadData(file, schema, LocationGroupType.class);
    }
    public List<LocationGroupType> loadData(InputStream inputStream) throws IOException {

        CsvSchema schema = CsvSchema.builder().
                addColumn("name").
                addColumn("description").
                addColumn("fourWallInventory").
                addColumn("virtual").
                addColumn("receivingStage").
                addColumn("shippingStage").
                addColumn("dock").
                addColumn("yard").
                addColumn("storage").
                addColumn("pickupAndDeposit").
                addColumn("trailer").
                build().withHeader();

        return fileService.loadData(inputStream, schema, LocationGroupType.class);
    }

    public void initTestData() {
        try {
            InputStream inputStream = new ClassPathResource(testDataFile).getInputStream();
            List<LocationGroupType> locationGroupTypes = loadData(inputStream);
            locationGroupTypes.stream().forEach(locationGroupType -> saveOrUpdate(locationGroupType));
        } catch (IOException ex) {
            logger.debug("Exception while load test data: {}", ex.getMessage());
        }
    }



}

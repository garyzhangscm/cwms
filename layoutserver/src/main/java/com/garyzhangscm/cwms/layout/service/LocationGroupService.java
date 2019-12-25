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
import com.garyzhangscm.cwms.layout.model.LocationGroup;
import com.garyzhangscm.cwms.layout.model.LocationGroupCSVWrapper;
import com.garyzhangscm.cwms.layout.model.LocationGroupType;
import com.garyzhangscm.cwms.layout.model.Warehouse;
import com.garyzhangscm.cwms.layout.repository.LocationGroupRepository;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@CacheConfig(cacheNames = "location_groups")
public class LocationGroupService implements TestDataInitiableService {

    private static final Logger logger = LoggerFactory.getLogger(LocationGroupService.class);
    @Autowired
    private LocationGroupRepository locationGroupRepository;
    @Autowired
    private LocationGroupTypeService locationGroupTypeService;
    @Autowired
    private FileService fileService;

    @Value("${fileupload.test-data.location-groups:location_groups.csv}")
    String testDataFile;

    @Cacheable
    public LocationGroup findById(Long id) {
        return locationGroupRepository.findById(id).orElse(null);
    }

    public List<LocationGroup> findAll() {

        return locationGroupRepository.findAll();
    }

    public List<LocationGroup> findAll(String locationGroupTypes, String name) {

        if (!StringUtils.isBlank(name)) {
            LocationGroup locationGroup = findByName(name);
            if (locationGroup != null) {
                return Arrays.asList(new LocationGroup[]{locationGroup});
            }
            else {
                return new ArrayList<>();
            }
        }
        else {
            return listLocationGroupsByTypes(locationGroupTypes);

        }

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

    public LocationGroup saveOrUpdate(LocationGroup locationGroup) {
        if (findByName(locationGroup.getName()) != null) {
            locationGroup.setId(findByName(locationGroup.getName()).getId());
        }
        return save(locationGroup);
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
    public List<LocationGroupCSVWrapper> loadData(File file) throws IOException {

        CsvSchema schema = CsvSchema.builder().
                addColumn("name").
                addColumn("description").
                addColumn("locationGroupType").
                addColumn("pickable").
                addColumn("storable").
                addColumn("countable").
                build().withHeader();
        return fileService.loadData(file, schema, LocationGroupCSVWrapper.class);
    }
    public List<LocationGroupCSVWrapper> loadData(InputStream inputStream) throws IOException {

        CsvSchema schema = CsvSchema.builder().
                addColumn("name").
                addColumn("description").
                addColumn("locationGroupType").
                addColumn("pickable").
                addColumn("storable").
                addColumn("countable").
                build().withHeader();

        return fileService.loadData(inputStream, schema, LocationGroupCSVWrapper.class);
    }

    public void initTestData() {
        try {
            InputStream inputStream = new ClassPathResource(testDataFile).getInputStream();
            List<LocationGroupCSVWrapper> locationGroupCSVWrappers = loadData(inputStream);
            locationGroupCSVWrappers.stream().forEach(locationGroupCSVWrapper -> {
                try {
                    saveOrUpdate(convertFromWrapper(locationGroupCSVWrapper));
                } catch(Exception ex) {
                    logger.debug("Exception while saving location group " + locationGroupCSVWrapper.getName());
                }
            });
        } catch (IOException ex) {
            logger.debug("Exception while load test data: {}", ex.getMessage());
        }
    }


    private LocationGroup convertFromWrapper(LocationGroupCSVWrapper locationGroupCSVWrapper) {
        LocationGroup locationGroup = new LocationGroup();
        locationGroup.setName(locationGroupCSVWrapper.getName());
        locationGroup.setDescription(locationGroupCSVWrapper.getDescription());
        locationGroup.setPickable(locationGroupCSVWrapper.getPickable());
        locationGroup.setCountable(locationGroupCSVWrapper.getCountable());
        locationGroup.setStorable(locationGroupCSVWrapper.getStorable());

        logger.debug("locationGroupCSVWrapper.getLocationGroupType().isEmpty()? " + locationGroupCSVWrapper.getLocationGroupType().isEmpty());
        if (!locationGroupCSVWrapper.getLocationGroupType().isEmpty()) {
            logger.debug("locationGroupCSVWrapper.getLocationGroupType():" + locationGroupCSVWrapper.getLocationGroupType());
            LocationGroupType locationGroupType = locationGroupTypeService.findByName(locationGroupCSVWrapper.getLocationGroupType().trim());
            logger.debug("locationGroupType == null ? : " + (locationGroupType==null));
            if (locationGroupType != null) {
                logger.debug("locationGroupType.getId():" + locationGroupType.getId());
            }
            locationGroup.setLocationGroupType(locationGroupType);
        }
        return locationGroup;

    }
}

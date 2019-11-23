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

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.garyzhangscm.cwms.layout.model.*;
import com.garyzhangscm.cwms.layout.repository.LocationGroupRepository;
import com.garyzhangscm.cwms.layout.repository.LocationRepository;
import org.apache.commons.lang.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class LocationService implements TestDataInitiableService {

    private static final Logger logger = LoggerFactory.getLogger(LocationService.class);
    @Autowired
    private LocationRepository locationRepository;

    @Autowired
    private LocationGroupService locationGroupService;
    @Autowired
    private FileService fileService;

    @Value("${fileupload.test-data.locations:locations.csv}")
    String testDataFile;

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
    public Location saveOrUpdate(Location location) {
        if (findByName(location.getName()) != null) {
            location.setId(findByName(location.getName()).getId());
        }
        return save(location);
    }

    public void delete(Location location) {
        locationRepository.delete(location);
    }
    public void delete(Long id) {
        locationRepository.deleteById(id);
    }

    @Transactional
    public void delete(String locationIds) {
        // remove a list of location groups based upon the id passed in
        if (!locationIds.isEmpty()) {
            long[] locationIdArray = Arrays.asList(locationIds.split(",")).stream().mapToLong(Long::parseLong).toArray();
            List<Long> locationIdList = Arrays.asList(ArrayUtils.toObject(locationIdArray));
            locationRepository.deleteByLocationIds(locationIdList);
        }
    }

    public List<Location> loadLocationData(File file) throws IOException {
        List<LocationCSVWrapper> locationCSVWrappers = loadData(file);
        return locationCSVWrappers.stream().map(locationCSVWrapper -> convertFromWrapper(locationCSVWrapper)).collect(Collectors.toList());
    }

    public List<LocationCSVWrapper> loadData(File file) throws IOException {
        CsvSchema schema = CsvSchema.builder().
                addColumn("name").
                addColumn("aisle").
                addColumn("x").
                addColumn("y").
                addColumn("z").
                addColumn("length").
                addColumn("width").
                addColumn("height").
                addColumn("pickSequence").
                addColumn("putawaySequence").
                addColumn("countSequence").
                addColumn("capacity").
                addColumn("fillPercentage").
                addColumn("locationGroup").
                addColumn("enabled").
                build().withHeader();
        return fileService.loadData(file, schema, LocationCSVWrapper.class);
    }
    public List<LocationCSVWrapper> loadData(InputStream inputStream) throws IOException {

        CsvSchema schema = CsvSchema.builder().
                addColumn("name").
                addColumn("aisle").
                addColumn("x").
                addColumn("y").
                addColumn("z").
                addColumn("length").
                addColumn("width").
                addColumn("height").
                addColumn("pickSequence").
                addColumn("putawaySequence").
                addColumn("countSequence").
                addColumn("capacity").
                addColumn("fillPercentage").
                addColumn("locationGroup").
                addColumn("enabled").
                build().withHeader();

        return fileService.loadData(inputStream, schema, LocationCSVWrapper.class);
    }

    public void initTestData() {
        try {
            InputStream inputStream = new ClassPathResource(testDataFile).getInputStream();
            List<LocationCSVWrapper> locationCSVWrappers = loadData(inputStream);
            locationCSVWrappers.stream().forEach(locationCSVWrapper -> saveOrUpdate(convertFromWrapper(locationCSVWrapper)));
        } catch (IOException ex) {
            logger.debug("Exception while load test data: {}", ex.getMessage());
        }
    }

    private Location convertFromWrapper(LocationCSVWrapper locationCSVWrapper) {
        Location location = new Location();
        location.setName(locationCSVWrapper.getName());
        location.setX(locationCSVWrapper.getX());
        location.setY(locationCSVWrapper.getY());
        location.setZ(locationCSVWrapper.getZ());
        location.setAisle(locationCSVWrapper.getAisle());
        location.setLength(locationCSVWrapper.getLength());
        location.setWidth(locationCSVWrapper.getWidth());
        location.setHeight(locationCSVWrapper.getHeight());
        location.setPickSequence(locationCSVWrapper.getPickSequence());
        location.setPutawaySequence(locationCSVWrapper.getPutawaySequence());
        location.setCountSequence(locationCSVWrapper.getCountSequence());
        location.setCapacity(locationCSVWrapper.getCapacity());
        location.setFillPercentage(locationCSVWrapper.getFillPercentage());
        location.setEnabled(locationCSVWrapper.getEnabled());

        if (!locationCSVWrapper.getLocationGroup().isEmpty()) {
            LocationGroup locationGroup = locationGroupService.findByName(locationCSVWrapper.getLocationGroup());
            location.setLocationGroup(locationGroup);
        }
        return location;

    }

}

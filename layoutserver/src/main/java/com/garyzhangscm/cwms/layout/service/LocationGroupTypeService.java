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
import com.garyzhangscm.cwms.layout.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.layout.model.LocationGroupType;
import com.garyzhangscm.cwms.layout.repository.LocationGroupTypeRepository;
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
import java.util.stream.Collectors;

@Service
public class LocationGroupTypeService implements TestDataInitiableService {

    private static final Logger logger = LoggerFactory.getLogger(LocationGroupTypeService.class);

    @Autowired
    private LocationGroupTypeRepository locationGroupTypeRepository;
    @Autowired
    private CompanyService companyService;
    @Autowired
    private FileService fileService;

    @Value("${fileupload.test-data.location-group-types:location_group_types}")
    String testDataFile;

    public LocationGroupType findById(Long id) {
        return locationGroupTypeRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("location group type not found by id: " + id));
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

    public List<LocationGroupType> getContainerLocationGroupType() {
        return findAll("").stream().filter(locationGroupType -> locationGroupType.getContainer() == true).collect(Collectors.toList());
    }

    public List<LocationGroupType> getRFLocationGroupType() {
        return findAll("").stream().filter(locationGroupType -> locationGroupType.getRf() == true).collect(Collectors.toList());
    }


    public LocationGroupType findByName(String name){
        return locationGroupTypeRepository.findByName(name);
    }

    public LocationGroupType save(LocationGroupType locationGroupType) {
        logger.debug("Will save location group type: {}", locationGroupType);
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
                addColumn("productionLine").
                addColumn("productionLineInbound").
                addColumn("productionLineOutbound").
                addColumn("grid").
                addColumn("container").
                addColumn("packingStation").
                addColumn("shippedParcel").
                addColumn("shippedOrder").
                addColumn("rf").
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
                addColumn("productionLine").
                addColumn("productionLineInbound").
                addColumn("productionLineOutbound").
                addColumn("grid").
                addColumn("container").
                addColumn("packingStation").
                addColumn("shippedParcel").
                addColumn("shippedOrder").
                addColumn("rf").
                build().withHeader();

        return fileService.loadData(inputStream, schema, LocationGroupType.class);
    }

    public void initTestData(Long companyId, String warehouseName) {
        logger.debug(">>Start to init location group type for warehouse {}", warehouseName);
        try {

            String companyCode = companyService.findById(companyId).getCode();

            String testDataFileName = StringUtils.isBlank(warehouseName) ?
                    testDataFile + ".csv" :
                    testDataFile + "-" + companyCode + "-" + warehouseName + ".csv";
            logger.debug("Start to init location group type from {}", testDataFileName);
            InputStream inputStream = new ClassPathResource(testDataFileName).getInputStream();
            List<LocationGroupType> locationGroupTypes = loadData(inputStream);
            locationGroupTypes.stream().forEach(locationGroupType -> saveOrUpdate(locationGroupType));
        } catch (IOException ex) {
            logger.debug("Exception while load test data: {}", ex.getMessage());
        }
    }



}

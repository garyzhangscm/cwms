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

package com.garyzhangscm.cwms.common.service;

import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.garyzhangscm.cwms.common.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.common.model.Carrier;
import com.garyzhangscm.cwms.common.model.CarrierServiceLevel;
import com.garyzhangscm.cwms.common.model.CarrierServiceLevelCSVWrapper;
import com.garyzhangscm.cwms.common.model.CarrierServiceLevelType;
import com.garyzhangscm.cwms.common.repository.CarrierServiceLevelRepository;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

@Service
public class CarrierServiceLevelService implements  TestDataInitiableService{

    private static final Logger logger = LoggerFactory.getLogger(CarrierServiceLevelService.class);

    @Autowired
    private CarrierServiceLevelRepository carrierServiceLevelRepository;
    @Autowired
    private CarrierService carrierService;
    @Autowired
    private FileService fileService;

    @Value("${fileupload.test-data.carrier-service-levels:carrier-service-levels}")
    String testDataFile;

    public CarrierServiceLevel findById(Long id) {
        return carrierServiceLevelRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("carrier service level not found by id: " + id));
    }

    public List<CarrierServiceLevel> findAll( ) {
        return carrierServiceLevelRepository.findAll();
    }

    // Natural Key: carrier and service level name
    public CarrierServiceLevel findByNatrualKey(String carrierName, String name){
        return carrierServiceLevelRepository.findByNatrualKey(carrierName, name);
    }

    @Transactional
    public CarrierServiceLevel save(CarrierServiceLevel carrierServiceLevel) {

        return carrierServiceLevelRepository.save(carrierServiceLevel);
    }
    // Save when the client's name doesn't exists
    // update when the client already exists
    @Transactional
    public CarrierServiceLevel saveOrUpdate(CarrierServiceLevel carrierServiceLevel) {
        if (carrierServiceLevel.getId() == null && findByNatrualKey(carrierServiceLevel.getCarrier().getName(), carrierServiceLevel.getName()) != null) {
            carrierServiceLevel.setId(findByNatrualKey(carrierServiceLevel.getCarrier().getName(), carrierServiceLevel.getName()).getId());
        }
        return save(carrierServiceLevel);
    }

    @Transactional
    public void delete(CarrierServiceLevel carrierServiceLevel) {
        carrierServiceLevelRepository.delete(carrierServiceLevel);
    }
    @Transactional
    public void delete(Long id) {
        carrierServiceLevelRepository.deleteById(id);
    }

    @Transactional
    public void delete(String carrierServiceLevelIds) {
        // remove a list of location groups based upon the id passed in
        if (!carrierServiceLevelIds.isEmpty()) {
            long[] carrierServiceLevelIdArray = Arrays.asList(carrierServiceLevelIds.split(",")).stream().mapToLong(Long::parseLong).toArray();
            for(long id : carrierServiceLevelIdArray) {
                delete(id);
            }
        }
    }

    public List<CarrierServiceLevelCSVWrapper> loadData(String fileName) throws IOException {
        return loadData(new File(fileName));
    }

    public List<CarrierServiceLevelCSVWrapper> loadData(File file) throws IOException {

        CsvSchema schema = CsvSchema.builder().
                addColumn("carrier").
                addColumn("name").
                addColumn("description").
                addColumn("type").
                build().withHeader();

        return fileService.loadData(file, schema, CarrierServiceLevelCSVWrapper.class);
    }

    @Transactional
    public List<CarrierServiceLevelCSVWrapper> loadData(InputStream inputStream) throws IOException {

        CsvSchema schema = CsvSchema.builder().
                addColumn("carrier").
                addColumn("name").
                addColumn("description").
                addColumn("type").
                build().withHeader();

        return fileService.loadData(inputStream, schema, CarrierServiceLevelCSVWrapper.class);
    }

    @Transactional
    public void initTestData(String warehouseName) {
        try {

            String testDataFileName = StringUtils.isBlank(warehouseName) ?
                    testDataFile + ".csv" :
                    testDataFile + "-" + warehouseName + ".csv";
            InputStream inputStream = new ClassPathResource(testDataFileName).getInputStream();
            List<CarrierServiceLevelCSVWrapper> carrierServiceLevelCSVWrappers = loadData(inputStream);
            carrierServiceLevelCSVWrappers.stream().forEach(carrierServiceLevelCSVWrapper -> saveOrUpdate(convertFromWrapper(carrierServiceLevelCSVWrapper)));
        }
        catch(IOException ex) {
            logger.debug("Exception while load test data: {}", ex.getMessage());
        }
    }

    private CarrierServiceLevel convertFromWrapper(CarrierServiceLevelCSVWrapper carrierServiceLevelCSVWrapper) {
        CarrierServiceLevel carrierServiceLevel = new CarrierServiceLevel();
        carrierServiceLevel.setName(carrierServiceLevelCSVWrapper.getName());
        carrierServiceLevel.setDescription(carrierServiceLevelCSVWrapper.getDescription());
        carrierServiceLevel.setType(CarrierServiceLevelType.valueOf(carrierServiceLevelCSVWrapper.getType()));

        Carrier carrier = carrierService.findByName(carrierServiceLevelCSVWrapper.getCarrier());
        if (carrier != null) {
            carrierServiceLevel.setCarrier(carrier);
        }
        return carrierServiceLevel;

    }

}

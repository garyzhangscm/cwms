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
import com.garyzhangscm.cwms.common.model.Carrier;
import com.garyzhangscm.cwms.common.model.Client;
import com.garyzhangscm.cwms.common.repository.CarrierRepository;
import com.garyzhangscm.cwms.common.repository.ClientRepository;
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
import java.util.Arrays;
import java.util.List;

@Service
public class CarrierService implements  TestDataInitiableService{

    private static final Logger logger = LoggerFactory.getLogger(CarrierService.class);

    @Autowired
    private CarrierRepository carrierRepository;
    @Autowired
    private FileService fileService;

    @Value("${fileupload.test-data.carriers:carriers}")
    String testDataFile;

    public Carrier findById(Long id) {
        return carrierRepository.findById(id).orElse(null);
    }

    public List<Carrier> findAll(String name) {

        if (StringUtils.isBlank(name)) {
            return carrierRepository.findAll();
        }
        else {
            return Arrays.asList(new Carrier[]{findByName(name)});
        }
    }

    public Carrier findByName(String name){
        return carrierRepository.findByName(name);
    }

    public Carrier save(Carrier carrier) {
        return carrierRepository.save(carrier);
    }
    // Save when the client's name doesn't exists
    // update when the client already exists
    public Carrier saveOrUpdate(Carrier carrier) {
        if (carrier.getId() == null && findByName(carrier.getName()) != null) {
            carrier.setId(findByName(carrier.getName()).getId());
        }
        return save(carrier);
    }

    public void delete(Carrier carrier) {
        carrierRepository.delete(carrier);
    }
    public void delete(Long id) {
        carrierRepository.deleteById(id);
    }

    public void delete(String carrierIds) {
        // remove a list of location groups based upon the id passed in
        if (!carrierIds.isEmpty()) {
            long[] carrierIdArray = Arrays.asList(carrierIds.split(",")).stream().mapToLong(Long::parseLong).toArray();
            for(long id : carrierIdArray) {
                delete(id);
            }
        }
    }

    public List<Carrier> loadData(String fileName) throws IOException {
        return loadData(new File(fileName));
    }

    public List<Carrier> loadData(File file) throws IOException {

        CsvSchema schema = CsvSchema.builder().
                addColumn("name").
                addColumn("description").
                addColumn("contactorFirstname").
                addColumn("contactorLastname").
                addColumn("addressCountry").
                addColumn("addressState").
                addColumn("addressCounty").
                addColumn("addressCity").
                addColumn("addressDistrict").
                addColumn("addressLine1").
                addColumn("addressLine2").
                addColumn("addressPostcode").
                build().withHeader();

        return fileService.loadData(file, schema, Carrier.class);
    }

    public List<Carrier> loadData(InputStream inputStream) throws IOException {

        CsvSchema schema = CsvSchema.builder().
                addColumn("name").
                addColumn("description").
                addColumn("contactorFirstname").
                addColumn("contactorLastname").
                addColumn("addressCountry").
                addColumn("addressState").
                addColumn("addressCounty").
                addColumn("addressCity").
                addColumn("addressDistrict").
                addColumn("addressLine1").
                addColumn("addressLine2").
                addColumn("addressPostcode").
                build().withHeader();

        return fileService.loadData(inputStream, schema, Carrier.class);
    }

    public void initTestData(String warehouseName) {
        try {
            String testDataFileName = StringUtils.isBlank(warehouseName) ?
                    testDataFile + ".csv" :
                    testDataFile + "-" + warehouseName + ".csv";
            InputStream inputStream = new ClassPathResource(testDataFileName).getInputStream();
            List<Carrier> carriers = loadData(inputStream);
            carriers.stream().forEach(carrier -> saveOrUpdate(carrier));
        }
        catch(IOException ex) {
            logger.debug("Exception while load test data: {}", ex.getMessage());
        }
    }
}

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
import com.garyzhangscm.cwms.layout.model.LocationCSVWrapper;
import com.garyzhangscm.cwms.layout.model.Warehouse;
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
public class WarehouseService implements TestDataInitiableService {

    private static final Logger logger = LoggerFactory.getLogger(WarehouseService.class);

    @Autowired
    private WarehouseRepository warehouseRepository;
    @Autowired
    private FileService fileService;

    @Value("${fileupload.test-data.warehouses:warehouses.csv}")
    String testDataFile;

    public Warehouse findById(Long id) {
        return warehouseRepository.findById(id).orElse(null);
    }

    public List<Warehouse> findAll() {
        return findAll("");
    }
    public List<Warehouse> findAll(String name) {

        if (StringUtils.isBlank(name)) {
            return warehouseRepository.findAll();
        }
        else {
            return Arrays.asList(new Warehouse[]{findByName(name)});
        }
    }

    public Warehouse findByName(String name){
        return warehouseRepository.findByName(name);
    }

    public Warehouse save(Warehouse warehouse) {
        return warehouseRepository.save(warehouse);
    }

    public Warehouse saveOrUpdate(Warehouse warehouse) {
        if (warehouse.getId() == null && findByName(warehouse.getName()) != null) {
            warehouse.setId(findByName(warehouse.getName()).getId());
        }
        return save(warehouse);
    }

    public void delete(Warehouse warehouse) {
        warehouseRepository.delete(warehouse);
    }
    public void delete(Long id) {
        warehouseRepository.deleteById(id);
    }
    public List<Warehouse> loadData(File file) throws IOException {

        CsvSchema schema = CsvSchema.builder().
                addColumn("name").
                addColumn("size").
                addColumn("addressCountry").
                addColumn("addressState").
                addColumn("addressCounty").
                addColumn("addressCity").
                addColumn("addressDistrict").
                addColumn("addressLine1").
                addColumn("addressLine2").
                addColumn("addressPostcode").
                build().withHeader();
        return fileService.loadData(file, schema, Warehouse.class);
    }

    public List<Warehouse> loadData(InputStream inputStream) throws IOException {

        CsvSchema schema = CsvSchema.builder().
                addColumn("name").
                addColumn("size").
                addColumn("addressCountry").
                addColumn("addressState").
                addColumn("addressCounty").
                addColumn("addressCity").
                addColumn("addressDistrict").
                addColumn("addressLine1").
                addColumn("addressLine2").
                addColumn("addressPostcode").
                build().withHeader();

        return fileService.loadData(inputStream, schema, Warehouse.class);
    }

    public void initTestData(String warehouseName) {
        try {

            String testDataFileName = StringUtils.isBlank(warehouseName) ?
                    testDataFile + ".csv" :
                    testDataFile + "-" + warehouseName + ".csv";
            InputStream inputStream = new ClassPathResource(testDataFileName).getInputStream();
            List<Warehouse> warehouses = loadData(inputStream);
            warehouses.stream().forEach(warehouse -> saveOrUpdate(warehouse));
        } catch (IOException ex) {
            logger.debug("Exception while load test data: {}", ex.getMessage());
        }
    }


}

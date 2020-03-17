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
import com.garyzhangscm.cwms.common.model.Client;
import com.garyzhangscm.cwms.common.model.Supplier;
import com.garyzhangscm.cwms.common.repository.ClientRepository;
import com.garyzhangscm.cwms.common.repository.SupplierRepository;
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
public class SupplierService implements  TestDataInitiableService{
    private static final Logger logger = LoggerFactory.getLogger(SupplierService.class);

    @Autowired
    private SupplierRepository supplierRepository;
    @Autowired
    private FileService fileService;

    @Value("${fileupload.test-data.clients:suppliers}")
    String testDataFile;

    public Supplier findById(Long id) {
        return supplierRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("supplier not found by id: " + id));
    }

    public List<Supplier> findAll(String name) {

        if (StringUtils.isBlank(name)) {
            return supplierRepository.findAll();
        }
        else {
            return Arrays.asList(new Supplier[]{findByName(name)});
        }
    }

    public Supplier findByName(String name){
        return supplierRepository.findByName(name);
    }

    @Transactional
    public Supplier save(Supplier supplier) {
        return supplierRepository.save(supplier);
    }

    // Save when the supplier's name doesn't exists
    // update when the supplier already exists
    @Transactional
    public Supplier saveOrUpdate(Supplier supplier) {
        if (supplier.getId() == null && findByName(supplier.getName()) != null) {
            supplier.setId(findByName(supplier.getName()).getId());
        }
        return save(supplier);
    }

    @Transactional
    public void delete(Supplier supplier) {
        supplierRepository.delete(supplier);
    }
    @Transactional
    public void delete(Long id) {
        supplierRepository.deleteById(id);
    }

    @Transactional
    public void delete(String supplierIds) {
        // remove a list of suppliers based upon the id passed in
        if (!supplierIds.isEmpty()) {
            long[] supplierIdArray = Arrays.asList(supplierIds.split(",")).stream().mapToLong(Long::parseLong).toArray();
            for(long id : supplierIdArray) {
                delete(id);
            }
        }

    }
    public List<Supplier> loadData(File file) throws IOException {
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
        return fileService.loadData(file, schema, Supplier.class);
    }

    public List<Supplier> loadData(InputStream inputStream) throws IOException {

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

        return fileService.loadData(inputStream, schema, Supplier.class);
    }

    @Transactional
    public void initTestData(String warehouseName) {
        try {
            String testDataFileName = StringUtils.isBlank(warehouseName) ?
                    testDataFile + ".csv" :
                    testDataFile + "-" + warehouseName + ".csv";
            InputStream inputStream = new ClassPathResource(testDataFileName).getInputStream();
            List<Supplier> suppliers = loadData(inputStream);
            suppliers.stream().forEach(supplier -> saveOrUpdate(supplier));
        } catch (IOException ex) {
            logger.debug("Exception while load test data: {}", ex.getMessage());
        }
    }

}

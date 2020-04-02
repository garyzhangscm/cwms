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
import com.garyzhangscm.cwms.common.model.Customer;
import com.garyzhangscm.cwms.common.repository.CustomerRepository;
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
public class CustomerService implements  TestDataInitiableService{
    private static final Logger logger = LoggerFactory.getLogger(CustomerService.class);

    @Autowired
    private CustomerRepository customerRepository;
    @Autowired
    private FileService fileService;

    @Value("${fileupload.test-data.customers:customers}")
    String testDataFile;

    public Customer findById(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("customer not found by id: " + id));
    }

    public List<Customer> findAll(String name) {

        if (StringUtils.isBlank(name)) {
            return customerRepository.findAll();
        }
        else {
            return Arrays.asList(new Customer[]{findByName(name)});
        }
    }

    public Customer findByName(String name){
        return customerRepository.findByName(name);
    }

    @Transactional
    public Customer save(Customer customer) {
        return customerRepository.save(customer);
    }

    // Save when the supplier's name doesn't exists
    // update when the supplier already exists
    @Transactional
    public Customer saveOrUpdate(Customer customer) {
        if (customer.getId() == null && findByName(customer.getName()) != null) {
            customer.setId(findByName(customer.getName()).getId());
        }
        return save(customer);
    }

    @Transactional
    public void delete(Customer customer) {
        customerRepository.delete(customer);
    }
    @Transactional
    public void delete(Long id) {
        customerRepository.deleteById(id);
    }

    @Transactional
    public void delete(String customerIds) {
        // remove a list of suppliers based upon the id passed in
        if (!customerIds.isEmpty()) {
            long[] customerIdArray = Arrays.asList(customerIds.split(",")).stream().mapToLong(Long::parseLong).toArray();
            for(long id : customerIdArray) {
                delete(id);
            }
        }

    }
    public List<Customer> loadData(File file) throws IOException {
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
        return fileService.loadData(file, schema, Customer.class);
    }

    public List<Customer> loadData(InputStream inputStream) throws IOException {

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

        return fileService.loadData(inputStream, schema, Customer.class);
    }

    @Transactional
    public void initTestData(String warehouseName) {
        try {
            String testDataFileName = StringUtils.isBlank(warehouseName) ?
                    testDataFile + ".csv" :
                    testDataFile + "-" + warehouseName + ".csv";
            InputStream inputStream = new ClassPathResource(testDataFileName).getInputStream();
            List<Customer> customers = loadData(inputStream);
            customers.stream().forEach(customer -> saveOrUpdate(customer));
        } catch (IOException ex) {
            logger.debug("Exception while load test data: {}", ex.getMessage());
        }
    }


}

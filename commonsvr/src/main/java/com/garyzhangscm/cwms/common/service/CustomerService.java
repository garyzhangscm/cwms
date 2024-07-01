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
import com.garyzhangscm.cwms.common.clients.OutbuondServiceRestemplateClient;
import com.garyzhangscm.cwms.common.clients.WarehouseLayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.common.exception.RequestValidationFailException;
import com.garyzhangscm.cwms.common.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.common.model.*;
import com.garyzhangscm.cwms.common.repository.CustomerRepository;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.criteria.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

@Service
public class CustomerService implements  TestDataInitiableService{
    private static final Logger logger = LoggerFactory.getLogger(CustomerService.class);

    @Autowired
    private CustomerRepository customerRepository;
    @Autowired
    private FileService fileService;
    @Autowired
    private WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient;
    @Autowired
    private OutbuondServiceRestemplateClient outbuondServiceRestemplateClient;

    @Value("${fileupload.test-data.customers:customers}")
    String testDataFile;

    public Customer findById(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("customer not found by id: " + id));
    }

    public List<Customer> findAll(Long companyId,
                                  Long warehouseId,
                                  String name) {
        List<Customer> customers = customerRepository.findAll(
                (Root<Customer> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();

                    predicates.add(criteriaBuilder.equal(root.get("companyId"), companyId));

                    if (StringUtils.isNotBlank(name)) {
                        if (name.contains("*")) {
                            predicates.add(criteriaBuilder.like(root.get("name"), name.replaceAll("\\*", "%")));
                        }
                        else {
                            predicates.add(criteriaBuilder.equal(root.get("name"), name));
                        }
                    }
                    Predicate[] p = new Predicate[predicates.size()];

                    // special handling for warehouse id
                    // if warehouse id is passed in, then return both the warehouse level item
                    // and the company level item information.
                    // otherwise, return the company level item information
                    Predicate predicate = criteriaBuilder.and(predicates.toArray(p));
                    if (Objects.nonNull(warehouseId)) {
                        return criteriaBuilder.and(predicate,
                                criteriaBuilder.or(
                                        criteriaBuilder.equal(root.get("warehouseId"), warehouseId),
                                        criteriaBuilder.isNull(root.get("warehouseId"))));
                    }
                    else  {
                        return criteriaBuilder.and(predicate,criteriaBuilder.isNull(root.get("warehouseId")));
                    }
                }
                ,
                Sort.by(Sort.Direction.ASC, "warehouseId", "name")
        );
        // we may get duplicated record from the above query when we pass in the warehouse id
        // if so, we may need to remove the company level item if we have the warehouse level item
        if (Objects.nonNull(warehouseId)) {
            removeDuplicatedCustomerRecords(customers);
        }
        return customers;
    }
    /**
     * Remove teh duplicated customer record. If we have 2 record with the same customer name
     * but different warehouse, then we will remove the one without any warehouse information
     * from the result
     * @param customers
     */
    private void removeDuplicatedCustomerRecords(List<Customer> customers) {
        Iterator<Customer> customerIterator = customers.listIterator();
        Set<String> customerProcessed = new HashSet<>();
        while(customerIterator.hasNext()) {
            Customer customer = customerIterator.next();

            if (customerProcessed.contains(customer.getName()) &&
                    Objects.isNull(customer.getWarehouseId())) {
                // ok, we already processed the item and the current
                // record is a company level item, then we will remove
                // this record from the result
                customerIterator.remove();
            }
            customerProcessed.add(customer.getName());
        }
    }
    public Customer findByName(Long companyId, Long warehouseId, String name){
        List<Customer> customers = findAll(companyId, warehouseId, name);
        // we should only get one customer with specific company and name combination
        if (customers.isEmpty()) {
            return null;
        }
        else {
            return customers.get(0);
        }
    }

    @Transactional
    public Customer save(Customer customer) {
        return customerRepository.save(customer);
    }

    // Save when the supplier's name doesn't exists
    // update when the supplier already exists
    @Transactional
    public Customer saveOrUpdate(Customer customer) {
        if (customer.getId() == null && findByName(customer.getWarehouseId(), customer.getWarehouseId(), customer.getName()) != null) {
            customer.setId(findByName(customer.getWarehouseId(), customer.getWarehouseId(), customer.getName()).getId());
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
    public void removeCustomers(String customerIds) {
        // remove a list of suppliers based upon the id passed in
        if (!customerIds.isEmpty()) {
            long[] customerIdArray = Arrays.asList(customerIds.split(",")).stream().mapToLong(Long::parseLong).toArray();
            for(long id : customerIdArray) {
                removeCustomer(id);
            }
        }

    }

    @Transactional
    public void removeCustomer(Long id) {
        Customer customer = findById(id);
        validateCustomerForRemoval(customer, id);
        delete(id);
    }

    private void validateCustomerForRemoval(Customer customer, Long id) {
        // make sure there's no order tie to this customer

        int orderCount = outbuondServiceRestemplateClient.getOrderCountForCustomer(
                customer.getWarehouseId(), id);
        if (orderCount > 0) {
            throw RequestValidationFailException.raiseException("Can't remove the customer " +
                    customer.getName() + " as there's order existing");
        }
    }

    public List<CustomerCSVWrapper> loadData(File file) throws IOException {
        CsvSchema schema = CsvSchema.builder().
                addColumn("company").
                addColumn("warehouse").
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
        return fileService.loadData(file, schema, CustomerCSVWrapper.class);
    }

    public List<CustomerCSVWrapper> loadData(InputStream inputStream) throws IOException {

        CsvSchema schema = CsvSchema.builder().
                addColumn("company").
                addColumn("warehouse").
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

        return fileService.loadData(inputStream, schema, CustomerCSVWrapper.class);
    }

    @Transactional
    public void initTestData(Long companyId, String warehouseName) {
        try {
            String companyCode = warehouseLayoutServiceRestemplateClient.getCompanyById(companyId).getCode();

            String testDataFileName = StringUtils.isBlank(warehouseName) ?
                    testDataFile + ".csv" :
                    testDataFile + "-" + companyCode + "-" + warehouseName + ".csv";

            InputStream inputStream = new ClassPathResource(testDataFileName).getInputStream();
            List<CustomerCSVWrapper> customerCSVWrappers = loadData(inputStream);
            customerCSVWrappers.stream().forEach(customerCSVWrapper -> saveOrUpdate(convertFromWrapper(customerCSVWrapper)));
        } catch (IOException ex) {
            logger.debug("Exception while load test data: {}", ex.getMessage());
        }
    }
    private Customer convertFromWrapper(CustomerCSVWrapper customerCSVWrapper) {
        Customer customer = new Customer();

        BeanUtils.copyProperties(customerCSVWrapper, customer);
        Warehouse warehouse =warehouseLayoutServiceRestemplateClient.getWarehouseByName(
                customerCSVWrapper.getCompany(), customerCSVWrapper.getWarehouse()
        );
        customer.setWarehouseId(warehouse.getId());
        return customer;

    }
}

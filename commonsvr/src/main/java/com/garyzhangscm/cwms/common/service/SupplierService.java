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
import com.garyzhangscm.cwms.common.clients.WarehouseLayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.common.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.common.model.*;
import com.garyzhangscm.cwms.common.repository.SupplierRepository;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Service
public class SupplierService implements  TestDataInitiableService{
    private static final Logger logger = LoggerFactory.getLogger(SupplierService.class);

    @Autowired
    private SupplierRepository supplierRepository;

    @Autowired
    private FileService fileService;
    @Autowired
    private WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient;

    @Value("${fileupload.test-data.clients:suppliers}")
    String testDataFile;

    public Supplier findById(Long id) {
        return supplierRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("supplier not found by id: " + id));
    }

    public List<Supplier> findAll( Long warehouseId,
                                  String name) {
        return supplierRepository.findAll(
                (Root<Supplier> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();


                    if (Objects.nonNull(warehouseId)) {
                        predicates.add(criteriaBuilder.equal(root.get("warehouseId"), warehouseId));
                    }


                    if (StringUtils.isNotBlank(name)) {
                        predicates.add(criteriaBuilder.equal(root.get("name"), name));
                    }
                    Predicate[] p = new Predicate[predicates.size()];
                    return criteriaBuilder.and(predicates.toArray(p));
                }
        ,
                Sort.by(Sort.Direction.ASC, "description"));
    }

    public Supplier findByName(Long warehouseId, String name){
        return supplierRepository.findByName(warehouseId, name);
    }

    @Transactional
    public Supplier save(Supplier supplier) {
        return supplierRepository.save(supplier);
    }

    // Save when the supplier's name doesn't exists
    // update when the supplier already exists
    @Transactional
    public Supplier saveOrUpdate(Supplier supplier) {
        if (supplier.getId() == null && findByName(supplier.getWarehouseId(), supplier.getName()) != null) {
            supplier.setId(findByName(supplier.getWarehouseId(), supplier.getName()).getId());
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
    public List<SupplierCSVWrapper> loadData(File file) throws IOException {
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
        return fileService.loadData(file, schema, SupplierCSVWrapper.class);
    }

    public List<SupplierCSVWrapper> loadData(InputStream inputStream) throws IOException {

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

        return fileService.loadData(inputStream, schema, SupplierCSVWrapper.class);
    }

    @Transactional
    public void initTestData(Long companyId, String warehouseName) {
        try {
            String companyCode = warehouseLayoutServiceRestemplateClient.getCompanyById(companyId).getCode();

            String testDataFileName = StringUtils.isBlank(warehouseName) ?
                    testDataFile + ".csv" :
                    testDataFile + "-" + companyCode + "-" + warehouseName + ".csv";

            InputStream inputStream = new ClassPathResource(testDataFileName).getInputStream();
            List<SupplierCSVWrapper> supplierCSVWrappers = loadData(inputStream);
            supplierCSVWrappers.stream().forEach(supplierCSVWrapper -> saveOrUpdate(convertFromWrapper(supplierCSVWrapper)));
        } catch (IOException ex) {
            logger.debug("Exception while load test data: {}", ex.getMessage());
        }
    }
    private Supplier convertFromWrapper(SupplierCSVWrapper supplierCSVWrapper) {
        Supplier supplier = new Supplier();

        BeanUtils.copyProperties(supplierCSVWrapper, supplier);
        Warehouse warehouse =warehouseLayoutServiceRestemplateClient.getWarehouseByName(
                supplierCSVWrapper.getCompany(), supplierCSVWrapper.getWarehouse()
        );
        supplier.setWarehouseId(warehouse.getId());
        return supplier;

    }

}

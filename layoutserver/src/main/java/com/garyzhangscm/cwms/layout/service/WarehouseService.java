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
import com.garyzhangscm.cwms.layout.model.*;
import com.garyzhangscm.cwms.layout.repository.WarehouseRepository;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import javax.persistence.criteria.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Service
public class WarehouseService implements TestDataInitiableService {

    private static final Logger logger = LoggerFactory.getLogger(WarehouseService.class);

    @Autowired
    private WarehouseRepository warehouseRepository;
    @Autowired
    private CompanyService companyService;
    @Autowired
    private FileService fileService;

    @Value("${fileupload.test-data.warehouses:warehouses.csv}")
    String testDataFile;

    public Warehouse findById(Long id) {
        return findById(id, true);
    }
    public Warehouse findById(Long id, boolean loadAttribute) {
        Warehouse warehouse = warehouseRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("warehouse not found by id: " + id));
        if(loadAttribute) {
            loadAttribute(warehouse);
        }
        return warehouse;
    }

    public List<Warehouse> findAll() {
        return findAll(null, null, null);
    }

    public List<Warehouse> findAll(Long companyId,
                                   String companyCode,
                                   String name) {
        return findAll(companyId, companyCode, name, true);
    }

    public List<Warehouse> findAll(Long companyId,
                                   String companyCode,
                                   String name,
                                   boolean loadAttribute) {

        List<Warehouse> warehouses =  warehouseRepository.findAll(
                (Root<Warehouse> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();



                    if (Objects.nonNull(companyId)) {
                        Join<Warehouse, Company> joinCompany = root.join("company", JoinType.INNER);

                        predicates.add(criteriaBuilder.equal(joinCompany.get("id"), companyId));
                    }

                    if (StringUtils.isNotBlank(companyCode)) {
                        Join<Warehouse, Company> joinCompany = root.join("company", JoinType.INNER);

                        predicates.add(criteriaBuilder.equal(joinCompany.get("code"), companyCode));
                    }


                    if (StringUtils.isNotBlank(name)) {
                        predicates.add(criteriaBuilder.equal(root.get("name"), name));
                    }
                    Predicate[] p = new Predicate[predicates.size()];
                    return criteriaBuilder.and(predicates.toArray(p));
                }
        );

        if (warehouses.size() > 0 && loadAttribute) {
            loadAttribute(warehouses);
        }
        return warehouses;
    }

    private void loadAttribute(List<Warehouse> warehouses) {
        warehouses.forEach(this::loadAttribute);
    }

    private void loadAttribute(Warehouse warehouse) {

        warehouse.setCompanyId(warehouse.getCompany().getId());
    }
    public Warehouse findByName(String companyCode, String name){
        return findByName(companyService.findByCode(companyCode).getId(), name);
    }
    public Warehouse findByName(Long companyId, String name){
        return warehouseRepository.findByName(companyId, name);
    }

    public Warehouse save(Warehouse warehouse) {
        return warehouseRepository.save(warehouse);
    }

    public Warehouse saveOrUpdate(Warehouse warehouse) {
        if (warehouse.getId() == null && findByName(warehouse.getCompany().getId(), warehouse.getName()) != null) {
            warehouse.setId(findByName(warehouse.getCompany().getId(), warehouse.getName()).getId());
        }
        return save(warehouse);
    }

    public void delete(Warehouse warehouse) {
        warehouseRepository.delete(warehouse);
    }
    public void delete(Long id) {
        warehouseRepository.deleteById(id);
    }
    public List<WarehouseCSVWrapper> loadData(File file) throws IOException {

        CsvSchema schema = CsvSchema.builder().
                addColumn("company").
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
        return fileService.loadData(file, schema, WarehouseCSVWrapper.class);
    }

    public List<WarehouseCSVWrapper> loadData(InputStream inputStream) throws IOException {

        CsvSchema schema = CsvSchema.builder().
                addColumn("company").
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

        return fileService.loadData(inputStream, schema, WarehouseCSVWrapper.class);
    }

    public void initTestData(Long companyId, String warehouseName) {
        try {
            String companyCode = companyService.findById(companyId).getCode();

            String testDataFileName = StringUtils.isBlank(warehouseName) ?
                    testDataFile + ".csv" :
                    testDataFile + "-" + companyCode + "-" + warehouseName + ".csv";
            InputStream inputStream = new ClassPathResource(testDataFileName).getInputStream();
            List<WarehouseCSVWrapper> warehouseCSVWrappers = loadData(inputStream);
            warehouseCSVWrappers.stream().forEach(warehouseCSVWrapper -> saveOrUpdate(convertFromWrapper(warehouseCSVWrapper)));
        } catch (IOException ex) {
            logger.debug("Exception while load test data: {}", ex.getMessage());
        }
    }


    private Warehouse convertFromWrapper(WarehouseCSVWrapper warehouseCSVWrapper) {
        Warehouse warehouse = new Warehouse();
        BeanUtils.copyProperties(warehouseCSVWrapper, warehouse);

        warehouse.setCompany(companyService.findByCode(warehouseCSVWrapper.getCompany()));

        return warehouse;

    }

    public Warehouse changeWarehouse(long id, Warehouse warehouse) {
        Warehouse existingWarehouse = findById(id);
        BeanUtils.copyProperties(warehouse, existingWarehouse, "id", "name", "company");
        return saveOrUpdate(existingWarehouse);
    }

    public Warehouse addWarehouses(Long companyId, Warehouse warehouse) {
        Company company = companyService.findById(companyId);
        warehouse.setCompany(company);
        return saveOrUpdate(warehouse);
    }
}

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
import com.garyzhangscm.cwms.common.clients.InboundServiceRestemplateClient;
import com.garyzhangscm.cwms.common.clients.WarehouseLayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.common.exception.GenericException;
import com.garyzhangscm.cwms.common.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.common.exception.SupplierException;
import com.garyzhangscm.cwms.common.model.*;
import com.garyzhangscm.cwms.common.repository.SupplierRepository;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.util.Strings;
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
public class SupplierService implements  TestDataInitiableService{
    private static final Logger logger = LoggerFactory.getLogger(SupplierService.class);

    @Autowired
    private SupplierRepository supplierRepository;

    @Autowired
    private FileService fileService;
    @Autowired
    private WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient;
    @Autowired
    private InboundServiceRestemplateClient inboundServiceRestemplateClient;

    @Value("${fileupload.test-data.clients:suppliers}")
    String testDataFile;

    public Supplier findById(Long id) {
        return supplierRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("supplier not found by id: " + id));
    }

    public List<Supplier> findAll( Long companyId, Long warehouseId,
                                  String name, String quickbookListId) {
        List<Supplier> suppliers = supplierRepository.findAll(
                (Root<Supplier> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();

                    predicates.add(criteriaBuilder.equal(root.get("companyId"), companyId));
                    if (StringUtils.isNotBlank(name)) {
                        if (name.contains("%")) {
                            predicates.add(criteriaBuilder.like(root.get("name"), name));
                        }
                        else {
                            predicates.add(criteriaBuilder.equal(root.get("name"), name));
                        }
                    }
                    if (Strings.isNotBlank(quickbookListId)) {

                        predicates.add(criteriaBuilder.equal(root.get("quickbookListId"), quickbookListId));
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
                Sort.by(Sort.Direction.ASC, "warehouseId", "description", "name")
        );

        // we may get duplicated record from the above query when we pass in the warehouse id
        // if so, we may need to remove the company level item if we have the warehouse level item
        if (Objects.nonNull(warehouseId)) {
            removeDuplicatedSupplierRecords(suppliers);
        }
        return suppliers;
    }

    /**
     * Remove teh duplicated supplier record. If we have 2 record with the same supplier name
     * but different warehouse, then we will remove the one without any warehouse information
     * from the result
     * @param suppliers
     */
    private void removeDuplicatedSupplierRecords(List<Supplier> suppliers) {
        Iterator<Supplier> supplierIterator = suppliers.listIterator();
        Set<String> supplierProcessed = new HashSet<>();
        while(supplierIterator.hasNext()) {
            Supplier supplier = supplierIterator.next();

            if (supplierProcessed.contains(supplier.getName()) &&
                    Objects.isNull(supplier.getWarehouseId())) {
                // ok, we already processed the item and the current
                // record is a company level item, then we will remove
                // this record from the result
                supplierIterator.remove();
            }
            supplierProcessed.add(supplier.getName());
        }
    }

    public Supplier findByName(Long companyId, Long warehouseId, String name){
        List<Supplier> suppliers = findAll(companyId, warehouseId, name, "");
        // we should only get one customer with specific company and name combination
        if (suppliers.isEmpty()) {
            return null;
        }
        else {
            return suppliers.get(0);
        }
    }

    @Transactional
    public Supplier save(Supplier supplier) {
        return supplierRepository.save(supplier);
    }

    // Save when the supplier's name doesn't exists
    // update when the supplier already exists
    @Transactional
    public Supplier saveOrUpdate(Supplier supplier) {
        if (supplier.getId() == null && findByName(supplier.getCompanyId(), supplier.getWarehouseId(), supplier.getName()) != null) {
            supplier.setId(findByName(supplier.getCompanyId(), supplier.getWarehouseId(), supplier.getName()).getId());
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
    public void delete(Long warehouseId, String supplierIds) {
        // remove a list of suppliers based upon the id passed in
        logger.debug("start to remove suppliers with id list {}", supplierIds);
        if (!supplierIds.isEmpty()) {
            long[] supplierIdArray = Arrays.asList(supplierIds.split(",")).stream().mapToLong(Long::parseLong).toArray();
            validateSuppliersForDelete(warehouseId, supplierIdArray);
            for(long id : supplierIdArray) {
                logger.debug("will remove supplier with id {}", supplierIds);
                delete(id);
            }
        }

    }

    /**
     * If there's receipt against the supplier, we won't allow the user to remove the supplier
     * @param supplierId
     */
    private void validateSupplierForDelete(Long warehouseId,Long supplierId) {
        int receiptCountBySupplier = inboundServiceRestemplateClient.getReceiptCountBySupplier(warehouseId, supplierId);
        if (receiptCountBySupplier > 0) {
            // show the name to the user
            Supplier supplier = findById(supplierId);
            throw SupplierException.raiseException("There's existing receipt against this supplier "
             +  supplier.getId() + " / " + supplier.getName() + ", fail to remove it");
        }

    }
    private void validateSuppliersForDelete(Long warehouseId, long[] supplierIdArray) {
        for (long supplierId : supplierIdArray) {
            validateSupplierForDelete(warehouseId, supplierId);
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

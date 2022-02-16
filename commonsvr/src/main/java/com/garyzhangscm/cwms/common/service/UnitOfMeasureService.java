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
import com.garyzhangscm.cwms.common.repository.UnitOfMeasureRepository;
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
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

@Service
public class UnitOfMeasureService implements  TestDataInitiableService{
    private static final Logger logger = LoggerFactory.getLogger(UnitOfMeasureService.class);

    @Autowired
    private UnitOfMeasureRepository unitOfMeasureRepository;
    @Autowired
    private FileService fileService;
    @Autowired
    private WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient;

    @Value("${fileupload.test-data.unit-of-measures:unit_of_meansures}")
    String testDataFile;

    public UnitOfMeasure findById(Long id) {
        return unitOfMeasureRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("unit of measure not found by id: " + id));}

    public List<UnitOfMeasure> findAll(Long companyId, Long warehouseId, String name,
                                       Boolean companyUnitOfMeasure,
                                       Boolean warehouseSpecificUnitOfMeasure) {
        List<UnitOfMeasure> unitOfMeasures =  unitOfMeasureRepository.findAll(
                (Root<UnitOfMeasure> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
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

                    Predicate[] p = new Predicate[predicates.size()];

                    // special handling for warehouse id
                    // if warehouse id is passed in, then return both the warehouse level item
                    // and the company level item information.
                    // otherwise, return the company level item information
                    Predicate predicate = criteriaBuilder.and(predicates.toArray(p));

                    if (Objects.nonNull(warehouseId) && Boolean.TRUE.equals(warehouseSpecificUnitOfMeasure)) {
                        // return the item that specific at the warehouse level
                        return criteriaBuilder.and(
                                predicate,
                                criteriaBuilder.equal(root.get("warehouseId"), warehouseId));

                    }
                    else if (Objects.nonNull(warehouseId) && !Boolean.TRUE.equals(companyUnitOfMeasure)) {
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
                // we may get duplicated record from the above query when we pass in the warehouse id
                // if so, we may need to remove the company level item if we have the warehouse level item
                Sort.by(Sort.Direction.DESC, "warehouseId")
        );

        // we may get duplicated record from the above query when we pass in the warehouse id
        // if so, we may need to remove the company level item if we have the warehouse level item
        if (Objects.nonNull(warehouseId)) {
            removeDuplicatedUnitOfMeasureRecords(unitOfMeasures);
        }
        return unitOfMeasures;

    }

    /**
     * Remove teh duplicated unit of measure record. If we have 2 record with the same unit of measure name
     * but different warehouse, then we will remove the one without any warehouse information
     * from the result
     * @param unitOfMeasures
     */
    private void removeDuplicatedUnitOfMeasureRecords(List<UnitOfMeasure> unitOfMeasures) {
        Iterator<UnitOfMeasure> unitOfMeasureIterator = unitOfMeasures.listIterator();
        Set<String> unitOfMeasureProcessed = new HashSet<>();
        while(unitOfMeasureIterator.hasNext()) {
            UnitOfMeasure unitOfMeasure = unitOfMeasureIterator.next();

            if (unitOfMeasureProcessed.contains(unitOfMeasure.getName()) &&
                    Objects.isNull(unitOfMeasure.getWarehouseId())) {
                // ok, we already processed the item and the current
                // record is a company level item, then we will remove
                // this record from the result
                unitOfMeasureIterator.remove();
            }
            unitOfMeasureProcessed.add(unitOfMeasure.getName());
        }
    }

    public UnitOfMeasure findByName(Long warehouseId, String name){
        return unitOfMeasureRepository.findByName(warehouseId, name);
    }

    @Transactional
    public UnitOfMeasure save(UnitOfMeasure unitOfMeasure) {
        return unitOfMeasureRepository.save(unitOfMeasure);
    }

    // Save when the supplier's name doesn't exists
    // update when the supplier already exists
    @Transactional
    public UnitOfMeasure saveOrUpdate(UnitOfMeasure unitOfMeasure) {
        logger.debug("Will save or update unit of measure: {}", unitOfMeasure.getName());
        if (unitOfMeasure.getId() == null &&
                findByName(unitOfMeasure.getWarehouseId(), unitOfMeasure.getName()) != null) {
            unitOfMeasure.setId(
                    findByName(unitOfMeasure.getWarehouseId(), unitOfMeasure.getName()).getId());
        }
        return save(unitOfMeasure);
    }
    @Transactional
    public void delete(UnitOfMeasure unitOfMeasure) {
        unitOfMeasureRepository.delete(unitOfMeasure);
    }
    @Transactional
    public void delete(Long id) {
        unitOfMeasureRepository.deleteById(id);
    }

    @Transactional
    public void delete(String unitOfMeasureIds) {
        // remove a list of location groups based upon the id passed in
        if (!unitOfMeasureIds.isEmpty()) {
            long[] unitOfMeasureIdArray = Arrays.asList(unitOfMeasureIds.split(",")).stream().mapToLong(Long::parseLong).toArray();
            for(long id : unitOfMeasureIdArray) {
                delete(id);
            }
        }
    }

    public List<UnitOfMeasureCSVWrapper> loadData(InputStream inputStream) throws IOException {

        CsvSchema schema = CsvSchema.builder().
                addColumn("company").
                addColumn("warehouse").
                addColumn("name").
                addColumn("description").
                build().withHeader();

        return fileService.loadData(inputStream, schema, UnitOfMeasureCSVWrapper.class);
    }

    @Transactional
    public void initTestData(Long companyId, String warehouseName) {
        try {
            String companyCode = warehouseLayoutServiceRestemplateClient.getCompanyById(companyId).getCode();

            String testDataFileName = StringUtils.isBlank(warehouseName) ?
                    testDataFile + ".csv" :
                    testDataFile + "-" + companyCode + "-" + warehouseName + ".csv";

            InputStream inputStream = new ClassPathResource(testDataFileName).getInputStream();
            List<UnitOfMeasureCSVWrapper> unitOfMeasureCSVWrappers = loadData(inputStream);
            unitOfMeasureCSVWrappers.stream().forEach(unitOfMeasureCSVWrapper -> saveOrUpdate(convertFromWrapper(unitOfMeasureCSVWrapper)));
        } catch (IOException ex) {
            logger.debug("Exception while load test data: {}", ex.getMessage());
        }
    }
    private UnitOfMeasure convertFromWrapper(UnitOfMeasureCSVWrapper unitOfMeasureCSVWrapper) {
        UnitOfMeasure unitOfMeasure = new UnitOfMeasure();

        BeanUtils.copyProperties(unitOfMeasureCSVWrapper, unitOfMeasure);
        Warehouse warehouse =warehouseLayoutServiceRestemplateClient.getWarehouseByName(
                unitOfMeasureCSVWrapper.getCompany(), unitOfMeasureCSVWrapper.getWarehouse()
        );
        unitOfMeasure.setWarehouseId(warehouse.getId());
        return unitOfMeasure;

    }

}

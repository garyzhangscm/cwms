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
import com.garyzhangscm.cwms.common.repository.CarrierRepository;
import com.garyzhangscm.cwms.common.repository.UnitRepository;
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

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Service
public class UnitService implements  TestDataInitiableService{

    private static final Logger logger = LoggerFactory.getLogger(UnitService.class);

    @Autowired
    private UnitRepository unitRepository;
    @Autowired
    private FileService fileService;

    @Value("${fileupload.test-data.units:units}")
    String testDataFile;

    public Unit findById(Long id) {
        return unitRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("unit not found by id: " + id));
    }

    public List<Unit> findAll(
            String name, String type) {

        return unitRepository.findAll(
                (Root<Unit> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();

                    if (StringUtils.isNotBlank(name)) {
                        predicates.add(criteriaBuilder.equal(root.get("name"), name));
                    }
                    if (StringUtils.isNotBlank(type)) {
                        predicates.add(criteriaBuilder.equal(root.get("type"), UnitType.valueOf(type)));
                    }
                    Predicate[] p = new Predicate[predicates.size()];
                    return criteriaBuilder.and(predicates.toArray(p));
                }
                ,
                Sort.by(Sort.Direction.ASC, "name")
        );
    }

    public Unit findByName(String name){
        return unitRepository.findByName(name);
    }

    public List<Unit> findByType(String type){
        return unitRepository.findByType(UnitType.valueOf(type));
    }



    @Transactional
    public Unit save(Unit unit) {
        return unitRepository.save(unit);
    }

    @Transactional
    public Unit saveOrUpdate(Unit unit) {
        if (unit.getId() == null &&
                findByName(unit.getName()) != null) {
            unit.setId(findByName( unit.getName()).getId());
        }
        return save(unit);
    }

    @Transactional
    public void delete(Unit unit) {
        unitRepository.delete(unit);
    }
    @Transactional
    public void delete(Long id) {
        unitRepository.deleteById(id);
    }

    public List<Unit> loadData(InputStream inputStream) throws IOException {

        CsvSchema schema = CsvSchema.builder().
                addColumn("type").
                addColumn("name").
                addColumn("description").
                addColumn("ratio").
                addColumn("baseUnitFlag").
                build().withHeader();

        return fileService.loadData(inputStream, schema, Unit.class);
    }

    @Transactional
    public void initTestData(Long companyId, String warehouseName) {
        try {

            String testDataFileName = testDataFile + ".csv";

            InputStream inputStream = new ClassPathResource(testDataFileName).getInputStream();
            List<Unit> units = loadData(inputStream);
            units.stream().forEach(unit -> saveOrUpdate(unit));
        } catch (IOException ex) {
            logger.debug("Exception while load test data: {}", ex.getMessage());
        }
    }
}

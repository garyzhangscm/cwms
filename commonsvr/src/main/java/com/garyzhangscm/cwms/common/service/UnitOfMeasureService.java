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
import com.garyzhangscm.cwms.common.model.Supplier;
import com.garyzhangscm.cwms.common.model.UnitOfMeasure;
import com.garyzhangscm.cwms.common.repository.UnitOfMeasureRepository;
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
public class UnitOfMeasureService implements  TestDataInitiableService{
    private static final Logger logger = LoggerFactory.getLogger(UnitOfMeasureService.class);

    @Autowired
    private UnitOfMeasureRepository unitOfMeasureRepository;
    @Autowired
    private FileService fileService;

    @Value("${fileupload.test-data.unit-of-measures:unit_of_meansures}")
    String testDataFile;

    public UnitOfMeasure findById(Long id) {
        return unitOfMeasureRepository.findById(id).orElse(null);
    }

    public List<UnitOfMeasure> findAll() {

        return unitOfMeasureRepository.findAll();
    }

    public UnitOfMeasure findByName(String name){
        return unitOfMeasureRepository.findByName(name);
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
        if (unitOfMeasure.getId() == null && findByName(unitOfMeasure.getName()) != null) {
            unitOfMeasure.setId(findByName(unitOfMeasure.getName()).getId());
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

    public List<UnitOfMeasure> loadData(InputStream inputStream) throws IOException {

        CsvSchema schema = CsvSchema.builder().
                addColumn("name").
                addColumn("description").
                build().withHeader();

        return fileService.loadData(inputStream, schema, UnitOfMeasure.class);
    }

    @Transactional
    public void initTestData(String warehouseName) {
        try {
            String testDataFileName = StringUtils.isBlank(warehouseName) ?
                    testDataFile + ".csv" :
                    testDataFile + "-" + warehouseName + ".csv";

            InputStream inputStream = new ClassPathResource(testDataFileName).getInputStream();
            List<UnitOfMeasure> unitOfMeasures = loadData(inputStream);
            unitOfMeasures.stream().forEach(unitOfMeasure -> saveOrUpdate(unitOfMeasure));
        } catch (IOException ex) {
            logger.debug("Exception while load test data: {}", ex.getMessage());
        }
    }


}

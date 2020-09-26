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
import com.garyzhangscm.cwms.common.repository.ReasonCodeRepository;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
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
public class ReasonCodeService implements  TestDataInitiableService{

    private static final Logger logger = LoggerFactory.getLogger(ReasonCodeService.class);

    @Autowired
    private ReasonCodeRepository reasonCodeRepository;
    @Autowired
    private FileService fileService;
    @Autowired
    private WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient;

    @Value("${fileupload.test-data.clients:reason_codes}")
    String testDataFile;

    public ReasonCode findById(Long id) {
        return reasonCodeRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("reason code not found by id: " + id));
    }

    public List<ReasonCode> findAll( Long warehouseId) {
        return reasonCodeRepository.findAll(
                (Root<ReasonCode> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();


                    if (Objects.nonNull(warehouseId)) {
                        predicates.add(criteriaBuilder.equal(root.get("warehouseId"), warehouseId));
                    }


                    Predicate[] p = new Predicate[predicates.size()];
                    return criteriaBuilder.and(predicates.toArray(p));
                }
        );
    }

    public ReasonCode findByName(Long warehouseId, String name){
        return reasonCodeRepository.findByName(warehouseId, name);
    }
    public List<ReasonCode> findByType(ReasonCodeType type){
        return reasonCodeRepository.findByType(type);
    }
    public List<ReasonCode> findByType(String type){
        return findByType(ReasonCodeType.valueOf(type));
    }

    @Transactional
    public ReasonCode save(ReasonCode reasonCode) {
        return reasonCodeRepository.save(reasonCode);
    }
    // Save when the reasonCode's name doesn't exists
    // update when the reasonCode already exists
    @Transactional
    public ReasonCode saveOrUpdate(ReasonCode reasonCode) {
        if (reasonCode.getId() == null && findByName(reasonCode.getWarehouseId(), reasonCode.getName()) != null) {
            reasonCode.setId(findByName(reasonCode.getWarehouseId(), reasonCode.getName()).getId());
        }
        return save(reasonCode);
    }

    @Transactional
    public void delete(ReasonCode reasonCode) {
        reasonCodeRepository.delete(reasonCode);
    }
    @Transactional
    public void delete(Long id) {
        reasonCodeRepository.deleteById(id);
    }

    @Transactional
    public void delete(String reasonCodeIds) {
        // remove a list of location groups based upon the id passed in
        if (!reasonCodeIds.isEmpty()) {
            long[] reasonCodeIdArray = Arrays.asList(reasonCodeIds.split(",")).stream().mapToLong(Long::parseLong).toArray();
            for(long id : reasonCodeIdArray) {
                delete(id);
            }
        }
    }

    public List<ReasonCodeCSVWrapper> loadData(String fileName) throws IOException {
        return loadData(new File(fileName));
    }

    public List<ReasonCodeCSVWrapper> loadData(File file) throws IOException {

        CsvSchema schema = CsvSchema.builder().
                addColumn("company").
                addColumn("warehouse").
                addColumn("name").
                addColumn("description").
                addColumn("type").
                build().withHeader();

        return fileService.loadData(file, schema, ReasonCodeCSVWrapper.class);
    }

    public List<ReasonCodeCSVWrapper> loadData(InputStream inputStream) throws IOException {

        CsvSchema schema = CsvSchema.builder().
                addColumn("company").
                addColumn("warehouse").
                addColumn("name").
                addColumn("description").
                addColumn("type").
                build().withHeader();

        return fileService.loadData(inputStream, schema, ReasonCodeCSVWrapper.class);
    }

    @Transactional
    public void initTestData(String warehouseName) {
        try {
            String testDataFileName = StringUtils.isBlank(warehouseName) ?
                    testDataFile + ".csv" :
                    testDataFile + "-" + warehouseName + ".csv";
            InputStream inputStream = new ClassPathResource(testDataFileName).getInputStream();
            List<ReasonCodeCSVWrapper> reasonCodeCSVWrappers = loadData(inputStream);
            reasonCodeCSVWrappers.stream().forEach(reasonCodeCSVWrapper -> saveOrUpdate(convertFromWrapper(reasonCodeCSVWrapper)));
        }
        catch(IOException ex) {
            logger.debug("Exception while load test data: {}", ex.getMessage());
        }
    }
    private ReasonCode convertFromWrapper(ReasonCodeCSVWrapper reasonCodeCSVWrapper) {
        ReasonCode reasonCode = new ReasonCode();

        BeanUtils.copyProperties(reasonCodeCSVWrapper, reasonCode);
        Warehouse warehouse =warehouseLayoutServiceRestemplateClient.getWarehouseByName(
                reasonCodeCSVWrapper.getCompany(), reasonCodeCSVWrapper.getWarehouse()
        );
        reasonCode.setWarehouseId(warehouse.getId());
        return reasonCode;

    }
}

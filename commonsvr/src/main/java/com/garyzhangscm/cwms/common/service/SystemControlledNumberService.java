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
import com.garyzhangscm.cwms.common.exception.SystemControlledNumberException;
import com.garyzhangscm.cwms.common.model.*;
import com.garyzhangscm.cwms.common.repository.SystemControlledNumberRepository;
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
public class SystemControlledNumberService implements  TestDataInitiableService{

    private static final Logger logger = LoggerFactory.getLogger(SystemControlledNumberService.class);

    private SystemControlledNumberRepository systemControlledNumberRepository;
    private FileService fileService;
    private WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient;

    @Value("${fileupload.test-data.system-controlled-numbers:system-controlled-numbers}")
    String testDataFile;

    Map<String, String> systemControlledNumberLocks = new HashMap<>();
    @Autowired
    public SystemControlledNumberService(SystemControlledNumberRepository systemControlledNumberRepository,
                                         FileService fileService,
                                         WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient
    ) {
        this.systemControlledNumberRepository = systemControlledNumberRepository;
        this.fileService = fileService;

        this.warehouseLayoutServiceRestemplateClient = warehouseLayoutServiceRestemplateClient;

        // Initial the map of locks, which we will use later when we request the next number
        // for the variable. We will use the lock to make sure only one thread is requesting
        // the next number and update the database correctly
        initSystemControlledNumberLocks();
    }

    private void initSystemControlledNumberLocks() {
        logger.debug("Start to init system controller locks");
        findAll( null, "")
                .stream()
                .forEach(systemControlledNumber
                        -> {
                    String key =
                            systemControlledNumber.getWarehouseId() + "-" +
                            systemControlledNumber.getVariable();
                    systemControlledNumberLocks.put(key, systemControlledNumber.getVariable());
                });
        logger.debug("After initiated, we have\n{}", systemControlledNumberLocks);
    }

    public SystemControlledNumber findById(Long id) {
        return systemControlledNumberRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("system controlled number not found by id: " + id));
    }

    public List<SystemControlledNumber> findAll( Long warehouseId,
                                                 String variable) {
        return systemControlledNumberRepository.findAll(
                (Root<SystemControlledNumber> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();

                    predicates.add(criteriaBuilder.equal(root.get("warehouseId"), warehouseId));

                    if (Strings.isNotBlank(variable)) {
                        predicates.add(criteriaBuilder.equal(root.get("variable"), variable));
                    }
                    Predicate[] p = new Predicate[predicates.size()];
                    return criteriaBuilder.and(predicates.toArray(p));
                }
                ,
                Sort.by(Sort.Direction.ASC, "variable")
        );
    }

    public SystemControlledNumber findByVariable(Long warehouseId, String variable){
        return systemControlledNumberRepository.findByVariableIgnoreCase(warehouseId, variable.toLowerCase());
    }

    @Transactional
    public SystemControlledNumber save(SystemControlledNumber systemControlledNumber) {

        return save(systemControlledNumber, true);
    }
    public SystemControlledNumber save(SystemControlledNumber systemControlledNumber, boolean initSystemControlledNumberLocks) {

        systemControlledNumber.setVariable(systemControlledNumber.getVariable().toLowerCase());
        SystemControlledNumber newSystemControlledNumber = systemControlledNumberRepository.saveAndFlush(systemControlledNumber);
        if (initSystemControlledNumberLocks) {
            initSystemControlledNumberLocks();
        }
        return newSystemControlledNumber;
    }
    // Save when the client's name doesn't exists
    // update when the client already exists
    @Transactional
    public SystemControlledNumber saveOrUpdate(SystemControlledNumber systemControlledNumber) {
        systemControlledNumber.setVariable(systemControlledNumber.getVariable().toLowerCase());
        if (systemControlledNumber.getId() == null &&
                findByVariable(systemControlledNumber.getWarehouseId(), systemControlledNumber.getVariable()) != null) {
            systemControlledNumber.setId(
                    findByVariable(systemControlledNumber.getWarehouseId(), systemControlledNumber.getVariable()).getId());
        }
        return save(systemControlledNumber);
    }
    @Transactional
    public void delete(Long id) {
        systemControlledNumberRepository.deleteById(id);
    }

    public List<String> getNextNumbers(Long warehouseId, String variable, Integer batch) {
        List<String> systemControlledNumbers = new ArrayList<>();
        for(int i = 0; i < batch; i++) {
            SystemControlledNumber systemControlledNumber =
                    getNextNumber(
                            warehouseId, variable
                    );
            logger.debug("{} / {}, get next number for {}",
                    i, batch, variable);
            logger.debug(">> {}", systemControlledNumber.getNextNumber());

            systemControlledNumbers.add(systemControlledNumber.getNextNumber());
        }
        logger.debug("Get next batch of {}, quantity {}",
                variable, batch);
        logger.debug(systemControlledNumbers.toString());
        return systemControlledNumbers;

    }
    public SystemControlledNumber getNextNumber(Long warehouseId, String variable) {

        logger.debug("Will lock by ");
        String key = warehouseId + "-" + variable;
        logger.debug(">> key: {} ", key);
        logger.debug(">> value: {}", systemControlledNumberLocks.get(key));
        // in case we just added this number, we may need to add it to the
        // map

        if (Objects.isNull(systemControlledNumberLocks.get(key))) {
            systemControlledNumberLocks.put(key, variable);
        }
        synchronized (systemControlledNumberLocks.get(key)) {
            SystemControlledNumber systemControlledNumber = findByVariable(warehouseId, variable);
            logger.debug("{}'s current number is {}", systemControlledNumber.getVariable(),
                    systemControlledNumber.getCurrentNumber());
            // Check if we already reaches the maximum number allowed
            int maxNumber = (int)Math.pow(10, systemControlledNumber.getLength());
            int nextNumber = systemControlledNumber.getCurrentNumber() + 1;
            if (nextNumber > maxNumber && !systemControlledNumber.getRollover()) {
                throw SystemControlledNumberException.raiseException(variable + " has reached the maximum number allowed and Rollover now allowed for this variable");
            }
            else if (nextNumber > maxNumber) {
                // next number is bigger than the maximum number allowed but
                // we allow roll over. So start from 0 again
                nextNumber = 0;
            }

            logger.debug("{}'s next number is {}", systemControlledNumber.getVariable(),
                    nextNumber);
            systemControlledNumber.setCurrentNumber(nextNumber);

            systemControlledNumber = save(systemControlledNumber, false);

            systemControlledNumber.setNextNumber(
                    systemControlledNumber.getPrefix()
                            + String.format("%0" + systemControlledNumber.getLength() +"d", nextNumber)
                            + systemControlledNumber.getPostfix());
            return systemControlledNumber;
        }

    }


    public List<SystemControlledNumberCSVWrapper> loadData(String fileName) throws IOException {
        return loadData(new File(fileName));
    }

    public List<SystemControlledNumberCSVWrapper> loadData(File file) throws IOException {

        CsvSchema schema = CsvSchema.builder().
                addColumn("company").
                addColumn("warehouse").
                addColumn("variable").
                addColumn("prefix").
                addColumn("postfix").
                addColumn("length").
                addColumn("currentNumber").
                addColumn("rollover").
                build().withHeader();

        return fileService.loadData(file, schema, SystemControlledNumberCSVWrapper.class);
    }

    public List<SystemControlledNumberCSVWrapper> loadData(InputStream inputStream) throws IOException {

        CsvSchema schema = CsvSchema.builder().
                addColumn("company").
                addColumn("warehouse").
                addColumn("variable").
                addColumn("prefix").
                addColumn("postfix").
                addColumn("length").
                addColumn("currentNumber").
                addColumn("rollover").
                build().withHeader();

        return fileService.loadData(inputStream, schema, SystemControlledNumberCSVWrapper.class);
    }

    public void initTestData(Long companyId, String warehouseName) {
        try {
            String companyCode = warehouseLayoutServiceRestemplateClient.getCompanyById(companyId).getCode();

            String testDataFileName = StringUtils.isBlank(warehouseName) ?
                    testDataFile + ".csv" :
                    testDataFile + "-" + companyCode + "-" + warehouseName + ".csv";
            InputStream inputStream = new ClassPathResource(testDataFileName).getInputStream();
            List<SystemControlledNumberCSVWrapper> systemControlledNumberCSVWrappers = loadData(inputStream);
            systemControlledNumberCSVWrappers.stream().forEach(systemControlledNumberCSVWrapper -> saveOrUpdate(convertFromWrapper(systemControlledNumberCSVWrapper)));
        }
        catch(IOException ex) {
            logger.debug("Exception while load test data: {}", ex.getMessage());
        }
    }

    private SystemControlledNumber convertFromWrapper(SystemControlledNumberCSVWrapper systemControlledNumberCSVWrapper) {
        SystemControlledNumber systemControlledNumber = new SystemControlledNumber();

        BeanUtils.copyProperties(systemControlledNumberCSVWrapper, systemControlledNumber);
        Warehouse warehouse =warehouseLayoutServiceRestemplateClient.getWarehouseByName(
                systemControlledNumberCSVWrapper.getCompany(), systemControlledNumberCSVWrapper.getWarehouse()
        );
        systemControlledNumber.setWarehouseId(warehouse.getId());
        return systemControlledNumber;

    }

    public SystemControlledNumber addSystemControlledNumbers(SystemControlledNumber systemControlledNumber) {
        return saveOrUpdate(systemControlledNumber);
    }

    public SystemControlledNumber changeSystemControlledNumbers(SystemControlledNumber systemControlledNumber) {
        return saveOrUpdate(systemControlledNumber);
    }
}

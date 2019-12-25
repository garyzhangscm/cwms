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
import com.garyzhangscm.cwms.common.exception.GenericException;
import com.garyzhangscm.cwms.common.model.Client;
import com.garyzhangscm.cwms.common.model.SystemControlledNumber;
import com.garyzhangscm.cwms.common.repository.ClientRepository;
import com.garyzhangscm.cwms.common.repository.SystemControlledNumberRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SystemControlledNumberService implements  TestDataInitiableService{

    private static final Logger logger = LoggerFactory.getLogger(SystemControlledNumberService.class);

    private SystemControlledNumberRepository systemControlledNumberRepository;
    private FileService fileService;

    @Value("${fileupload.test-data.system-controlled-numbers:system-controlled-numbers.csv}")
    String testDataFile;

    Map<String, String> systemControlledNumberLocks = new HashMap<>();
    @Autowired
    public SystemControlledNumberService(SystemControlledNumberRepository systemControlledNumberRepository,
                                         FileService fileService) {
        this.systemControlledNumberRepository = systemControlledNumberRepository;
        this.fileService = fileService;

        // Initial the map of locks, which we will use later when we request the next number
        // for the variable. We will use the lock to make sure only one thread is requesting
        // the next number and update the database correctly
        initSystemControlledNumberLocks();
    }

    private void initSystemControlledNumberLocks() {
        systemControlledNumberLocks.clear();
        findAll().stream().forEach(systemControlledNumber -> systemControlledNumberLocks.put(systemControlledNumber.getVariable(), systemControlledNumber.getVariable()));
    }

    public SystemControlledNumber findById(Long id) {
        return systemControlledNumberRepository.findById(id).orElse(null);
    }

    public List<SystemControlledNumber> findAll() {

        return systemControlledNumberRepository.findAll();
    }

    public SystemControlledNumber findByVariable(String variable){
        return systemControlledNumberRepository.findByVariable(variable);
    }

    public SystemControlledNumber save(SystemControlledNumber systemControlledNumber) {

        SystemControlledNumber newSystemControlledNumber = systemControlledNumberRepository.save(systemControlledNumber);
        initSystemControlledNumberLocks();
        return newSystemControlledNumber;
    }
    // Save when the client's name doesn't exists
    // update when the client already exists
    public SystemControlledNumber saveOrUpdate(SystemControlledNumber systemControlledNumber) {
        if (systemControlledNumber.getId() == null && findByVariable(systemControlledNumber.getVariable()) != null) {
            systemControlledNumber.setId(findByVariable(systemControlledNumber.getVariable()).getId());
        }
        return save(systemControlledNumber);
    }

    public SystemControlledNumber getNextNumber(String variable) {
        synchronized (systemControlledNumberLocks.get(variable)) {
            SystemControlledNumber systemControlledNumber = findByVariable(variable);
            // Check if we already reaches the maximum number allowed
            int maxNumber = (int)Math.pow(10, systemControlledNumber.getLength());
            int nextNumber = systemControlledNumber.getCurrentNumber() + 1;
            if (nextNumber > maxNumber && !systemControlledNumber.getRollover()) {
                throw new GenericException(10000, variable + " has reached the maximum number allowed and Rollover now allowed for this variable");
            }
            else if (nextNumber > maxNumber) {
                // next number is bigger than the maximum number allowed but
                // we allow roll over. So start from 0 again
                nextNumber = 0;
            }

            systemControlledNumber.setCurrentNumber(nextNumber);

            systemControlledNumber = save(systemControlledNumber);

            systemControlledNumber.setNextNumber(systemControlledNumber.getPrefix() + String.format("%0" + systemControlledNumber.getLength() +"d", nextNumber) + systemControlledNumber.getPostfix());
            return systemControlledNumber;
        }

    }


    public List<SystemControlledNumber> loadData(String fileName) throws IOException {
        return loadData(new File(fileName));
    }

    public List<SystemControlledNumber> loadData(File file) throws IOException {

        CsvSchema schema = CsvSchema.builder().
                addColumn("variable").
                addColumn("prefix").
                addColumn("postfix").
                addColumn("length").
                addColumn("currentNumber").
                addColumn("rollover").
                build().withHeader();

        return fileService.loadData(file, schema, SystemControlledNumber.class);
    }

    public List<SystemControlledNumber> loadData(InputStream inputStream) throws IOException {

        CsvSchema schema = CsvSchema.builder().
                addColumn("variable").
                addColumn("prefix").
                addColumn("postfix").
                addColumn("length").
                addColumn("currentNumber").
                addColumn("rollover").
                build().withHeader();

        return fileService.loadData(inputStream, schema, SystemControlledNumber.class);
    }

    public void initTestData() {
        try {
            InputStream inputStream = new ClassPathResource(testDataFile).getInputStream();
            List<SystemControlledNumber> systemControlledNumbers = loadData(inputStream);
            systemControlledNumbers.stream().forEach(systemControlledNumber -> saveOrUpdate(systemControlledNumber));
        }
        catch(IOException ex) {
            logger.debug("Exception while load test data: {}", ex.getMessage());
        }
    }
}

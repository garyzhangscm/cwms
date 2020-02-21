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
import com.garyzhangscm.cwms.common.model.Client;
import com.garyzhangscm.cwms.common.model.ReasonCode;
import com.garyzhangscm.cwms.common.model.ReasonCodeType;
import com.garyzhangscm.cwms.common.repository.ClientRepository;
import com.garyzhangscm.cwms.common.repository.ReasonCodeRepository;
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
public class ReasonCodeService implements  TestDataInitiableService{

    private static final Logger logger = LoggerFactory.getLogger(ReasonCodeService.class);

    @Autowired
    private ReasonCodeRepository reasonCodeRepository;
    @Autowired
    private FileService fileService;

    @Value("${fileupload.test-data.clients:reason_codes}")
    String testDataFile;

    public ReasonCode findById(Long id) {
        return reasonCodeRepository.findById(id).orElse(null);
    }

    public List<ReasonCode> findAll() {

        return reasonCodeRepository.findAll();
    }

    public ReasonCode findByName(String name){
        return reasonCodeRepository.findByName(name);
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
        if (reasonCode.getId() == null && findByName(reasonCode.getName()) != null) {
            reasonCode.setId(findByName(reasonCode.getName()).getId());
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

    public List<ReasonCode> loadData(String fileName) throws IOException {
        return loadData(new File(fileName));
    }

    public List<ReasonCode> loadData(File file) throws IOException {

        CsvSchema schema = CsvSchema.builder().
                addColumn("name").
                addColumn("description").
                addColumn("type").
                build().withHeader();

        return fileService.loadData(file, schema, ReasonCode.class);
    }

    public List<ReasonCode> loadData(InputStream inputStream) throws IOException {

        CsvSchema schema = CsvSchema.builder().
                addColumn("name").
                addColumn("description").
                addColumn("type").
                build().withHeader();

        return fileService.loadData(inputStream, schema, ReasonCode.class);
    }

    @Transactional
    public void initTestData(String warehouseName) {
        try {
            String testDataFileName = StringUtils.isBlank(warehouseName) ?
                    testDataFile + ".csv" :
                    testDataFile + "-" + warehouseName + ".csv";
            InputStream inputStream = new ClassPathResource(testDataFileName).getInputStream();
            List<ReasonCode> reasonCodes = loadData(inputStream);
            reasonCodes.stream().forEach(reasonCode -> saveOrUpdate(reasonCode));
        }
        catch(IOException ex) {
            logger.debug("Exception while load test data: {}", ex.getMessage());
        }
    }
}

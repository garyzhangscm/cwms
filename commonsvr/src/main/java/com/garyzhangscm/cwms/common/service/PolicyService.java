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
import com.garyzhangscm.cwms.common.model.Policy;
import com.garyzhangscm.cwms.common.repository.ClientRepository;
import com.garyzhangscm.cwms.common.repository.PolicyRepository;
import org.apache.commons.lang.ArrayUtils;
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
public class PolicyService implements  TestDataInitiableService{

    private static final Logger logger = LoggerFactory.getLogger(PolicyService.class);

    @Autowired
    private PolicyRepository policyRepository;
    @Autowired
    private FileService fileService;

    @Value("${fileupload.test-data.policies:policies}")
    String testDataFile;

    public Policy findById(Long id) {
        return policyRepository.findById(id).orElse(null);
    }

    public List<Policy> findAll(String key) {

        if (StringUtils.isBlank(key)) {

            return policyRepository.findAll();
        }
        return Arrays.asList(new Policy[]{findByKey(key)});
    }

    public Policy findByKey(String key){
        return policyRepository.findByKeyIgnoreCase(key);
    }

    @Transactional
    public Policy save(Policy policy) {
        return policyRepository.save(policy);
    }
    // Save when the client's name doesn't exists
    // update when the client already exists
    @Transactional
    public Policy saveOrUpdate(Policy policy) {
        if (policy.getId() == null && findByKey(policy.getKey()) != null) {
            policy.setId(findByKey(policy.getKey()).getId());
        }
        return save(policy);
    }

    @Transactional
    public void delete(Policy policy) {
        policyRepository.delete(policy);
    }
    @Transactional
    public void delete(Long id) {
        policyRepository.deleteById(id);
    }

    @Transactional
    public void delete(String policyIds) {
        // remove a list of location groups based upon the id passed in
        if (!policyIds.isEmpty()) {
            long[] policyIdArray = Arrays.asList(policyIds.split(",")).stream().mapToLong(Long::parseLong).toArray();
            for(long id : policyIdArray) {
                delete(id);
            }
        }
    }


    @Transactional
    public List<Policy> loadData(File file) throws IOException {

        CsvSchema schema = CsvSchema.builder().
                addColumn("key").
                addColumn("value").
                addColumn("description").
                build().withHeader();

        return fileService.loadData(file, schema, Policy.class);
    }

    public List<Policy> loadData(InputStream inputStream) throws IOException {

        CsvSchema schema = CsvSchema.builder().
                addColumn("key").
                addColumn("value").
                addColumn("description").
                build().withHeader();

        return fileService.loadData(inputStream, schema, Policy.class);
    }

    @Transactional
    public void initTestData(String warehouseName) {
        try {
            String testDataFileName = StringUtils.isBlank(warehouseName) ?
                    testDataFile + ".csv" :
                    testDataFile + "-" + warehouseName + ".csv";
            InputStream inputStream = new ClassPathResource(testDataFileName).getInputStream();
            List<Policy> policies = loadData(inputStream);
            policies.stream().forEach(policy -> saveOrUpdate(policy));
        }
        catch(IOException ex) {
            logger.debug("Exception while load test data: {}", ex.getMessage());
        }
    }
}

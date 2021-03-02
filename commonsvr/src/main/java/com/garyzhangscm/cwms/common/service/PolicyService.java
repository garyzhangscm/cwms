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
import com.garyzhangscm.cwms.common.repository.PolicyRepository;
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
public class PolicyService implements  TestDataInitiableService{

    private static final Logger logger = LoggerFactory.getLogger(PolicyService.class);

    @Autowired
    private PolicyRepository policyRepository;
    @Autowired
    private FileService fileService;
    @Autowired
    private WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient;

    @Value("${fileupload.test-data.policies:policies}")
    String testDataFile;

    public Policy findById(Long id) {
        return policyRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("policy not found by id: " + id));
    }

    public List<Policy> findAll( Long warehouseId,
                                String key) {
        return policyRepository.findAll(
                (Root<Policy> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();


                    if (Objects.nonNull(warehouseId)) {
                        predicates.add(criteriaBuilder.equal(root.get("warehouseId"), warehouseId));
                    }


                    if (StringUtils.isNotBlank(key)) {
                        predicates.add(criteriaBuilder.equal(root.get("key"), key));
                    }
                    Predicate[] p = new Predicate[predicates.size()];
                    return criteriaBuilder.and(predicates.toArray(p));
                }
        );
    }

    public Policy findByKey(Long warehouseId, String key){
        return policyRepository.findByKeyIgnoreCase(warehouseId, key);
    }

    @Transactional
    public Policy save(Policy policy) {
        return policyRepository.save(policy);
    }
    // Save when the client's name doesn't exists
    // update when the client already exists
    @Transactional
    public Policy saveOrUpdate(Policy policy) {
        if (policy.getId() == null && findByKey(policy.getWarehouseId(), policy.getKey()) != null) {
            policy.setId(findByKey(policy.getWarehouseId(), policy.getKey()).getId());
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
    public List<PolicyCSVWrapper> loadData(File file) throws IOException {

        CsvSchema schema = CsvSchema.builder().
                addColumn("company").
                addColumn("warehouse").
                addColumn("key").
                addColumn("value").
                addColumn("description").
                build().withHeader();

        return fileService.loadData(file, schema, PolicyCSVWrapper.class);
    }

    public List<PolicyCSVWrapper> loadData(InputStream inputStream) throws IOException {

        CsvSchema schema = CsvSchema.builder().
                addColumn("company").
                addColumn("warehouse").
                addColumn("key").
                addColumn("value").
                addColumn("description").
                build().withHeader();

        return fileService.loadData(inputStream, schema, PolicyCSVWrapper.class);
    }

    @Transactional
    public void initTestData(Long companyId, String warehouseName) {
        try {
            String companyCode = warehouseLayoutServiceRestemplateClient.getCompanyById(companyId).getCode();

            String testDataFileName = StringUtils.isBlank(warehouseName) ?
                    testDataFile + ".csv" :
                    testDataFile + "-" + companyCode + "-" + warehouseName + ".csv";
            InputStream inputStream = new ClassPathResource(testDataFileName).getInputStream();
            List<PolicyCSVWrapper> policyCSVWrappers = loadData(inputStream);
            policyCSVWrappers.stream().forEach(policyCSVWrapper -> saveOrUpdate(convertFromWrapper(policyCSVWrapper)));
        }
        catch(IOException ex) {
            logger.debug("Exception while load test data: {}", ex.getMessage());
        }
    }
    private Policy convertFromWrapper(PolicyCSVWrapper policyCSVWrapper) {
        Policy policy = new Policy();

        BeanUtils.copyProperties(policyCSVWrapper, policy);
        Warehouse warehouse =warehouseLayoutServiceRestemplateClient.getWarehouseByName(
                policyCSVWrapper.getCompany(), policyCSVWrapper.getWarehouse()
        );
        policy.setWarehouseId(warehouse.getId());
        return policy;

    }
}
